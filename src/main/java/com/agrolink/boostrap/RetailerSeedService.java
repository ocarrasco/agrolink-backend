package com.agrolink.boostrap;

import com.agrolink.boostrap.dto.OrderGenerationSeed;
import com.agrolink.boostrap.dto.RetailerSeed;
import com.agrolink.boostrap.dto.SeedStatus;
import com.agrolink.model.*;
import com.agrolink.model.enums.OrderStatus;
import com.agrolink.model.enums.ShippingMethod;
import com.agrolink.model.enums.TransportStatus;
import com.agrolink.model.enums.UserRole;
import com.agrolink.repositories.*;
import com.agrolink.utils.StrUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RetailerSeedService {

  static final String SEED_FILE = "init/retailers.json";

  private static final Set<OrderStatus> HOLDS_STOCK = Set.of(OrderStatus.PLACED, OrderStatus.CONFIRMED, OrderStatus.FULFILLED);

  private static final int MIN_LINES_PER_ORDER = 1;
  private static final int MAX_LINES_PER_ORDER = 2;
  private static final int MIN_QTY = 5;
  private static final int MAX_QTY = 60;

  /**
   * 1 in N generated orders ships via a platform carrier (when at least one carrier exists).
   */
  private static final int PLATFORM_CARRIER_ODDS = 4;
  private static final int MAX_SEEDED_INTERESTS = 3;

  // Past (fully elapsed) months: mostly settled orders. ~50% FULFILLED, 20% REJECTED, 20% CANCELLED, 10% CONFIRMED.
  private static final OrderStatus[] PAST_MONTH_STATUS_POOL = {
      OrderStatus.FULFILLED, OrderStatus.FULFILLED, OrderStatus.FULFILLED, OrderStatus.FULFILLED, OrderStatus.FULFILLED,
      OrderStatus.REJECTED, OrderStatus.REJECTED,
      OrderStatus.CANCELLED, OrderStatus.CANCELLED,
      OrderStatus.CONFIRMED,
  };

  // Current month: mostly still in flight. ~40% PLACED, 20% CONFIRMED, 20% FULFILLED, 10% REJECTED, 10% CANCELLED.
  private static final OrderStatus[] CURRENT_MONTH_STATUS_POOL = {
      OrderStatus.PLACED, OrderStatus.PLACED, OrderStatus.PLACED, OrderStatus.PLACED,
      OrderStatus.CONFIRMED, OrderStatus.CONFIRMED,
      OrderStatus.FULFILLED, OrderStatus.FULFILLED,
      OrderStatus.REJECTED,
      OrderStatus.CANCELLED,
  };

  @NonNull
  private final ObjectMapper objectMapper;

  @NonNull
  private final IUserRepository userRepository;

  @NonNull
  private final IUserProfileRepository userProfileRepository;

  @NonNull
  private final IMasterProductRepository masterProductRepository;

  @NonNull
  private final ICatalogItemRepository catalogItemRepository;

  @NonNull
  private final IOrderRepository orderRepository;

  @NonNull
  private final ITransportInterestRepository transportInterestRepository;

  /**
   * Parsed entries of the seed file; empty list when the file is absent.
   */
  List<RetailerSeed> load() throws IOException {
    ClassPathResource resource = new ClassPathResource(SEED_FILE);
    if (!resource.exists()) {
      return List.of();
    }
    try (InputStream in = resource.getInputStream()) {
      return objectMapper.readValue(in, new TypeReference<>() {
      });
    }
  }

  @Transactional
  public Outcome seedRetailer(RetailerSeed seed) {
    if (seed.email() == null || seed.email().isBlank()) {
      return new Outcome(SeedStatus.SKIPPED_BLANK_EMAIL, false, 0);
    }
    var user = userRepository.findFirstByEmailIgnoreCase(seed.email().trim());
    if (user.isEmpty()) {
      return new Outcome(SeedStatus.UNMATCHED, false, 0);
    }
    Integer userId = user.get().getId();
    boolean profileWritten = applyProfile(userId, seed);
    int ordersCreated = generateOrders(userId, seed);
    return new Outcome(SeedStatus.SEEDED, profileWritten, ordersCreated);
  }

  private boolean applyProfile(Integer userId, RetailerSeed seed) {
    UserProfileModel profile = userProfileRepository.findByUserId(userId)
        .orElseGet(() -> {
          UserProfileModel fresh = new UserProfileModel();
          fresh.setUserId(userId);
          return fresh;
        });

    if (isConfigured(profile)) {
      return false;
    }

    profile.setDelivery(seed.delivery());
    profile.setAddress(StrUtils.blankToNull(seed.address()));
    profile.setPhone(StrUtils.blankToNull(seed.phone()));
    profile.setContactName(StrUtils.blankToNull(seed.contactName()));
    profile.setAvailability(seed.availability() == null
        ? WeeklyAvailability.empty()
        : seed.availability().normalized());
    userProfileRepository.save(profile);
    return true;
  }

  private boolean canGenerateOrder(Integer retailerId, RetailerSeed seed) {
    OrderGenerationSeed gen = seed.orderGeneration();
    if (gen == null || gen.ordersPerMonth() <= 0 || orderRepository.existsByRetailerId(retailerId)) {
      return false;
    }

    var products = masterProductRepository.findByActiveTrueOrderByNameAsc();
    if (products.isEmpty()) {
      log.warn("Retailer order generation for {} skipped: no active master products (enable classpath:seed)", seed.email());
      return false;
    }

    return true;
  }

  private int generateOrders(Integer retailerId, RetailerSeed seed) {
    if (!canGenerateOrder(retailerId, seed)) {
      return 0;
    }

    OrderGenerationSeed gen = seed.orderGeneration();
    Random random = new Random(seed.email().toLowerCase().hashCode());
    LocalDate today = LocalDate.now(ZoneId.systemDefault());
    YearMonth currentMonth = YearMonth.from(today);
    List<UserModel> carriers = userRepository.findAllByRole(UserRole.CARRIER);

    int created = 0;
    var products = masterProductRepository.findByActiveTrueOrderByNameAsc();

    for (int monthsAgo = gen.monthsBack(); monthsAgo >= 0; monthsAgo--) {
      YearMonth month = currentMonth.minusMonths(monthsAgo);
      boolean isCurrentMonth = month.equals(currentMonth);
      int lastDayToPick = isCurrentMonth ? today.getDayOfMonth() : month.lengthOfMonth();
      OrderStatus[] statusPool = isCurrentMonth ? CURRENT_MONTH_STATUS_POOL : PAST_MONTH_STATUS_POOL;

      for (int i = 0; i < gen.ordersPerMonth(); i++) {
        LocalDate date = month.atDay(1 + random.nextInt(lastDayToPick));
        OrderStatus status = statusPool[random.nextInt(statusPool.length)];
        try {
          if (tryGenerateOrder(retailerId, products, carriers, random, date, status)) {
            created++;
          }
        } catch (Exception e) {
          log.warn("Retailer order generation: entry dated {} failed (non-fatal)", date, e);
        }
      }
    }
    return created;
  }

  private boolean tryGenerateOrder(Integer retailerId, List<MasterProductModel> products, List<UserModel> carriers, Random random, LocalDate date, OrderStatus status) {
    List<MasterProductModel> shuffled = new ArrayList<>(products);
    Collections.shuffle(shuffled, random);
    int lineCount = Math.min(MIN_LINES_PER_ORDER + random.nextInt(MAX_LINES_PER_ORDER - MIN_LINES_PER_ORDER + 1), shuffled.size());

    Map<Integer, Integer> quantityByProduct = new LinkedHashMap<>();
    for (MasterProductModel product : shuffled.subList(0, lineCount)) {
      quantityByProduct.put(product.getId(), MIN_QTY + random.nextInt(MAX_QTY - MIN_QTY + 1));
    }

    Integer supplierId = resolveSupplier(quantityByProduct.keySet());
    if (supplierId == null) {
      log.debug("Retailer order generation: no single supplier's catalog covers the products picked for {} (skipped)", date);
      return false;
    }

    TransportPlan transport = planTransport(carriers, random, status);
    OrderModel saved = createOrder(retailerId, supplierId, quantityByProduct, status, date, transport);
    if (transport.transportStatus() == TransportStatus.AWAITING_CARRIER) {
      seedInterests(saved, carriers, random);
    }
    return true;
  }

  /**
   * Decides how a generated order ships. ~1 in {@link #PLATFORM_CARRIER_ODDS} go via a platform
   * carrier (when carriers exist); the transport sub-state is derived from the order status so the
   * demo has a realistic mix (settled orders delivered, current ones scattered across the leg).
   */
  private TransportPlan planTransport(List<UserModel> carriers, Random random, OrderStatus status) {
    if (carriers.isEmpty() || random.nextInt(PLATFORM_CARRIER_ODDS) != 0) {
      return new TransportPlan(ShippingMethod.PICKUP, null, null);
    }
    UserModel carrier = carriers.get(random.nextInt(carriers.size()));
    return switch (status) {
      case FULFILLED -> new TransportPlan(ShippingMethod.PLATFORM_CARRIER, carrier, TransportStatus.DELIVERED);
      case CONFIRMED -> switch (random.nextInt(4)) {
        case 0, 1 -> new TransportPlan(ShippingMethod.PLATFORM_CARRIER, null, TransportStatus.AWAITING_CARRIER);
        case 2 -> new TransportPlan(ShippingMethod.PLATFORM_CARRIER, carrier, TransportStatus.ASSIGNED);
        default -> new TransportPlan(ShippingMethod.PLATFORM_CARRIER, carrier, TransportStatus.IN_TRANSIT);
      };
      // PLACED / REJECTED / CANCELLED: chosen carrier transport, but no request open yet
      default -> new TransportPlan(ShippingMethod.PLATFORM_CARRIER, null, null);
    };
  }

  private void seedInterests(OrderModel order, List<UserModel> carriers, Random random) {
    List<UserModel> shuffled = new ArrayList<>(carriers);
    Collections.shuffle(shuffled, random);
    int count = 1 + random.nextInt(Math.min(MAX_SEEDED_INTERESTS, shuffled.size()));
    for (UserModel carrier : shuffled.subList(0, count)) {
      TransportInterestModel interest = new TransportInterestModel();
      interest.setOrder(order);
      interest.setCarrier(carrier);
      transportInterestRepository.save(interest);
    }
  }

  private OrderModel createOrder(Integer retailerId, Integer supplierId, Map<Integer, Integer> quantityByProduct, OrderStatus status, LocalDate date, TransportPlan transport) {
    var itemByProduct = catalogItemRepository.findBySupplierIdAndMasterProductIdIn(supplierId, quantityByProduct.keySet()).stream()
        .collect(Collectors.toMap(item -> item.getMasterProduct().getId(), Function.identity()));

    OrderModel order = new OrderModel();
    order.setRetailer(userRepository.getReferenceById(retailerId));
    order.setSupplier(userRepository.getReferenceById(supplierId));
    order.setStatus(status);
    order.setShippingMethod(transport.method());
    order.setTransportStatus(transport.transportStatus());
    if (transport.carrier() != null) {
      order.setCarrier(transport.carrier());
    }

    boolean holdsStock = HOLDS_STOCK.contains(status);
    int total = 0;
    for (var entry : quantityByProduct.entrySet()) {
      CatalogItemModel catalogItem = itemByProduct.get(entry.getKey());
      int quantity = entry.getValue();

      OrderItemModel item = new OrderItemModel();
      item.setCatalogItem(catalogItem);
      item.setMasterProduct(catalogItem.getMasterProduct());
      item.setProductName(catalogItem.getMasterProduct().getName());
      item.setUnit(catalogItem.getUnit());
      item.setQuantity(quantity);
      item.setUnitPrice(catalogItem.getPricePerUnit());
      item.setLineTotal(Math.multiplyExact(catalogItem.getPricePerUnit(), quantity));
      order.addItem(item);
      total = Math.addExact(total, item.getLineTotal());

      if (holdsStock) {
        catalogItem.setAvailableQuantity(Math.max(0, catalogItem.getAvailableQuantity() - quantity));
      }
    }
    order.setTotal(total);

    OrderModel saved = orderRepository.save(order);
    orderRepository.backdateCreatedAt(saved.getId(), date.atStartOfDay());
    return saved;
  }

  /**
   * How one generated order ships: method, assigned carrier (nullable), transport sub-state (nullable).
   */
  private record TransportPlan(ShippingMethod method, UserModel carrier, TransportStatus transportStatus) {

  }

  /**
   * The supplier id whose active catalog has an item for every given master product id, or {@code null}.
   */
  private Integer resolveSupplier(Set<Integer> masterProductIds) {
    Set<Integer> commonSuppliers = null;
    for (Integer masterProductId : masterProductIds) {
      Set<Integer> suppliersForProduct = catalogItemRepository.findActiveItems(masterProductId, null).stream()
          .map(item -> item.getSupplier().getId())
          .collect(Collectors.toSet());
      if (commonSuppliers == null) {
        commonSuppliers = suppliersForProduct;
      } else {
        commonSuppliers.retainAll(suppliersForProduct);
      }
      if (commonSuppliers.isEmpty()) {
        return null;
      }
    }
    return commonSuppliers == null ? null : commonSuppliers.stream().min(Integer::compareTo).orElse(null);
  }

  private static boolean isConfigured(UserProfileModel profile) {
    return profile.isDelivery()
        || profile.getAddress() != null
        || profile.getPhone() != null
        || profile.getContactName() != null
        || (profile.getAvailability() != null
        && !profile.getAvailability().normalized().equals(WeeklyAvailability.empty()));
  }

  record Outcome(SeedStatus status, boolean profileWritten, int ordersCreated) {

  }

}

package com.agrolink.services;

import com.agrolink.dto.request.CreateOrderRequest;
import com.agrolink.dto.request.DeliveryPreference;
import com.agrolink.dto.response.CarrierDeliveryResponse;
import com.agrolink.dto.response.OrderResponse;
import com.agrolink.dto.response.TransportInterestResponse;
import com.agrolink.dto.response.TransportRequestResponse;
import com.agrolink.events.OrderTerminatedEvent;
import com.agrolink.mappers.OrderMapper;
import com.agrolink.model.OrderItemModel;
import com.agrolink.model.OrderModel;
import com.agrolink.model.TransportInterestModel;
import com.agrolink.model.UserProfileModel;
import com.agrolink.model.enums.OrderStatus;
import com.agrolink.model.enums.ShippingMethod;
import com.agrolink.model.enums.TransportStatus;
import com.agrolink.repositories.ICatalogItemRepository;
import com.agrolink.repositories.IOrderRepository;
import com.agrolink.repositories.ITransportInterestRepository;
import com.agrolink.repositories.IUserProfileRepository;
import com.agrolink.security.LoggedUser;
import com.agrolink.utils.UserMessages;
import jakarta.persistence.EntityNotFoundException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

  @NonNull
  private final IOrderRepository orderRepository;

  @NonNull
  private final ICatalogItemRepository catalogItemRepository;

  @NonNull
  private final OrderMapper orderMapper;

  @NonNull
  private final UserService userService;

  @NonNull
  private final ApplicationEventPublisher eventPublisher;

  @NonNull
  private final IUserProfileRepository userProfileRepository;

  @NonNull
  private final ITransportInterestRepository transportInterestRepository;

  // ────────────────────────── Retailer ──────────────────────────

  @Transactional
  public OrderResponse create(LoggedUser retailer, CreateOrderRequest request) {
    // CreateOrderRequestValidator already checked: no duplicate product, the supplier offers
    // every requested master product + each offering is active, and the shipping method is usable.
    if (request.supplierId().equals(retailer.id())) {
      throw new IllegalStateException(UserMessages.ORDER_WITH_YOURSELF);
    }

    Map<Integer, Integer> quantityByProduct = new LinkedHashMap<>();
    for (var line : request.products()) {
      quantityByProduct.put(line.masterProductId(), line.quantity());
    }

    var itemByProduct = catalogItemRepository.findBySupplierIdAndMasterProductIdIn(request.supplierId(), quantityByProduct.keySet()).stream().collect(Collectors.toMap(item -> item.getMasterProduct().getId(), Function.identity()));

    OrderModel order = new OrderModel();
    order.setRetailer(userService.getReference(retailer.id()));
    order.setSupplier(userService.getReference(request.supplierId()));
    order.setStatus(OrderStatus.PLACED);
    order.setShippingMethod(request.shippingMethod());

    if (request.deliveryPreference() != null) {
      order.setDeliveryDay(request.deliveryPreference().day());
      order.setDeliverySlot(request.deliveryPreference().slot());
    }

    int total = 0;
    for (var entry : quantityByProduct.entrySet()) {
      var catalogItem = itemByProduct.get(entry.getKey());
      int quantity = entry.getValue();

      // reserve stock now — interim plain decrement (see improvements.md #1)
      if (catalogItem.getAvailableQuantity() < quantity) {
        throw new IllegalStateException(UserMessages.notEnoughStock(catalogItem.getMasterProduct().getName(), catalogItem.getAvailableQuantity(), quantity));
      }
      catalogItem.setAvailableQuantity(catalogItem.getAvailableQuantity() - quantity);

      int unitPrice = catalogItem.getPricePerUnit();
      int lineTotal = Math.multiplyExact(unitPrice, quantity); // CLP, no fractions

      OrderItemModel item = new OrderItemModel();
      item.setCatalogItem(catalogItem);
      item.setMasterProduct(catalogItem.getMasterProduct());
      item.setProductName(catalogItem.getMasterProduct().getName());
      item.setUnit(catalogItem.getUnit());
      item.setQuantity(quantity);
      item.setUnitPrice(unitPrice);
      item.setLineTotal(lineTotal);
      order.addItem(item);

      total = Math.addExact(total, lineTotal);
    }

    order.setTotal(total);
    return orderMapper.toResponse(orderRepository.save(order));
  }

  @Transactional(readOnly = true)
  public List<OrderResponse> listForRetailer(LoggedUser retailer, OrderStatus status, Integer year, Integer month) {
    List<OrderModel> orders = orderRepository.findForRetailer(retailer.id(), status, year, month);
    return orderMapper.toResponseList(orders);
  }

  @Transactional(readOnly = true)
  public OrderResponse getForRetailer(LoggedUser retailer, Integer id) {
    return orderMapper.toResponse(retailerOrderOrThrow(retailer, id));
  }

  @Transactional
  public OrderResponse cancel(LoggedUser retailer, Integer id) {
    OrderModel order = retailerOrderOrThrow(retailer, id);
    requireStatus(order, OrderStatus.PLACED, "cancelar");
    order.setStatus(OrderStatus.CANCELLED);
    OrderResponse response = orderMapper.toResponse(orderRepository.saveAndFlush(order));
    eventPublisher.publishEvent(new OrderTerminatedEvent(order.getId(), OrderStatus.CANCELLED));
    return response;
  }

  // ────────────────────────── Supplier ──────────────────────────

  @Transactional(readOnly = true)
  public List<OrderResponse> listForSupplier(LoggedUser supplier, OrderStatus status, Integer year, Integer month) {
    List<OrderModel> orders = orderRepository.findForSupplier(supplier.id(), status, year, month);
    return orderMapper.toResponseList(orders);
  }

  @Transactional(readOnly = true)
  public OrderResponse getForSupplier(LoggedUser supplier, Integer id) {
    return orderMapper.toResponse(supplierOrderOrThrow(supplier, id));
  }

  @Transactional
  public OrderResponse confirm(LoggedUser supplier, Integer id) {
    OrderModel order = supplierOrderOrThrow(supplier, id);
    requireStatus(order, OrderStatus.PLACED, "confirmar");
    // stock was already reserved when the order was placed
    order.setStatus(OrderStatus.CONFIRMED);
    if (order.getShippingMethod() == ShippingMethod.PLATFORM_CARRIER) {
      order.setTransportStatus(TransportStatus.AWAITING_CARRIER); // opens on the carrier market
    }
    return orderMapper.toResponse(orderRepository.saveAndFlush(order));
  }

  @Transactional
  public OrderResponse reject(LoggedUser supplier, Integer id, String note) {
    OrderModel order = supplierOrderOrThrow(supplier, id);
    requireStatus(order, OrderStatus.PLACED, "rechazar");
    order.setStatus(OrderStatus.REJECTED);
    order.setSupplierNote(note == null || note.isBlank() ? null : note.trim());
    OrderResponse response = orderMapper.toResponse(orderRepository.saveAndFlush(order));
    eventPublisher.publishEvent(new OrderTerminatedEvent(order.getId(), OrderStatus.REJECTED));
    return response;
  }

  @Transactional
  public OrderResponse fulfill(LoggedUser supplier, Integer id) {
    OrderModel order = supplierOrderOrThrow(supplier, id);
    requireStatus(order, OrderStatus.CONFIRMED, "completar");
    if (order.getShippingMethod() == ShippingMethod.PLATFORM_CARRIER) {
      throw new IllegalStateException(UserMessages.PLATFORM_CARRIER_FULFILL_BY_CARRIER);
    }
    order.setStatus(OrderStatus.FULFILLED);
    return orderMapper.toResponse(orderRepository.saveAndFlush(order));
  }

  // ────────────────────────── Transport (carrier open market) ──────────────────────────

  @Transactional(readOnly = true)
  public List<TransportRequestResponse> listOpenTransportRequests(LoggedUser carrier) {
    List<OrderModel> orders = orderRepository.findByShippingMethodAndStatusAndCarrierIsNull(ShippingMethod.PLATFORM_CARRIER, OrderStatus.CONFIRMED);

    Set<Integer> userIds = new LinkedHashSet<>();
    for (OrderModel order : orders) {
      userIds.add(order.getSupplier().getId());
      userIds.add(order.getRetailer().getId());
    }
    Map<Integer, UserProfileModel> profilesByUserId = userProfileRepository.findByUserIdIn(userIds).stream()
        .collect(Collectors.toMap(UserProfileModel::getUserId, Function.identity()));

    return orders.stream()
        .map(order -> toTransportRequestResponse(order, profilesByUserId,
            transportInterestRepository.existsByOrderIdAndCarrierId(order.getId(), carrier.id())))
        .toList();
  }

  @Transactional
  public void markInterested(LoggedUser carrier, Integer orderId) {
    OrderModel order = openTransportRequestOrThrow(orderId);
    if (transportInterestRepository.existsByOrderIdAndCarrierId(orderId, carrier.id())) {
      return; // already interested — idempotent
    }
    TransportInterestModel interest = new TransportInterestModel();
    interest.setOrder(order);
    interest.setCarrier(userService.getReference(carrier.id()));
    transportInterestRepository.save(interest);
  }

  /** Retract a prior expression of interest — no-op if the order is no longer open or there was none. */
  @Transactional
  public void withdrawInterest(LoggedUser carrier, Integer orderId) {
    OrderModel order = orderRepository.findWithItemsById(orderId)
        .orElseThrow(() -> new EntityNotFoundException(UserMessages.orderNotFound(orderId)));
    if (order.getTransportStatus() == TransportStatus.AWAITING_CARRIER) {
      transportInterestRepository.deleteByOrderIdAndCarrierId(orderId, carrier.id());
    }
  }

  @Transactional(readOnly = true)
  public List<TransportInterestResponse> listTransportInterests(LoggedUser retailer, Integer id) {
    retailerOrderOrThrow(retailer, id); // ownership check

    List<TransportInterestModel> interests = transportInterestRepository.findByOrderIdOrderByIdAsc(id);
    Set<Integer> carrierIds = interests.stream().map(i -> i.getCarrier().getId()).collect(Collectors.toSet());
    Map<Integer, UserProfileModel> profilesByUserId = userProfileRepository.findByUserIdIn(carrierIds).stream()
        .collect(Collectors.toMap(UserProfileModel::getUserId, Function.identity()));

    return interests.stream().map(interest -> toTransportInterestResponse(interest, profilesByUserId)).toList();
  }

  @Transactional
  public OrderResponse acceptCarrier(LoggedUser retailer, Integer id, Integer carrierId) {
    OrderModel order = retailerOrderOrThrow(retailer, id);
    requireAwaitingTransport(order);
    if (!transportInterestRepository.existsByOrderIdAndCarrierId(id, carrierId)) {
      throw new EntityNotFoundException(UserMessages.carrierNotInterested(carrierId));
    }
    order.setCarrier(userService.getReference(carrierId));
    order.setTransportStatus(TransportStatus.ASSIGNED);
    OrderResponse response = orderMapper.toResponse(orderRepository.saveAndFlush(order));
    transportInterestRepository.deleteByOrderId(id); // the other expressions of interest are moot now
    return response;
  }

  /** A retailer's PLATFORM_CARRIER orders still in progress (awaiting a carrier, assigned, or in transit). */
  @Transactional(readOnly = true)
  public List<OrderResponse> listInTransitForRetailer(LoggedUser retailer) {
    return orderMapper.toResponseList(
        orderRepository.findByRetailerIdAndShippingMethodAndStatusOrderByCreatedAtDesc(retailer.id(), ShippingMethod.PLATFORM_CARRIER, OrderStatus.CONFIRMED));
  }

  /**
   * The retailer — not the carrier — confirms delivery: they're the one who actually receives the
   * goods, so they're the one who can say it arrived. Closes the order (FULFILLED) same as the
   * carrier-driven flow used to.
   */
  @Transactional
  public OrderResponse confirmDelivery(LoggedUser retailer, Integer id) {
    OrderModel order = retailerOrderOrThrow(retailer, id);
    if (order.getTransportStatus() != TransportStatus.IN_TRANSIT) {
      throw new IllegalStateException(UserMessages.TRANSPORT_NOT_IN_TRANSIT);
    }
    order.setTransportStatus(TransportStatus.DELIVERED);
    order.setStatus(OrderStatus.FULFILLED);
    return orderMapper.toResponse(orderRepository.saveAndFlush(order));
  }

  // ────────────────────────── Transport (carrier execution) ──────────────────────────

  @Transactional(readOnly = true)
  public List<CarrierDeliveryResponse> listForCarrier(LoggedUser carrier, TransportStatus status, Integer year, Integer month) {
    List<OrderModel> orders = orderRepository.findForCarrier(carrier.id(), status, year, month);

    Set<Integer> userIds = new LinkedHashSet<>();
    for (OrderModel order : orders) {
      userIds.add(order.getSupplier().getId());
      userIds.add(order.getRetailer().getId());
    }
    Map<Integer, UserProfileModel> profilesByUserId = userProfileRepository.findByUserIdIn(userIds).stream()
        .collect(Collectors.toMap(UserProfileModel::getUserId, Function.identity()));

    return orders.stream().map(order -> toCarrierDeliveryResponse(order, profilesByUserId)).toList();
  }

  @Transactional
  public CarrierDeliveryResponse pickup(LoggedUser carrier, Integer orderId) {
    OrderModel order = carrierOrderOrThrow(carrier, orderId);
    if (order.getTransportStatus() != TransportStatus.ASSIGNED) {
      throw new IllegalStateException(UserMessages.TRANSPORT_NOT_ASSIGNED);
    }
    order.setTransportStatus(TransportStatus.IN_TRANSIT);
    return toCarrierDeliveryResponse(orderRepository.saveAndFlush(order), carrierRouteProfiles(order));
  }

  // ────────────────────────── helpers ──────────────────────────

  // Stock is returned to the catalog items by OrderStockListener, which reacts to the
  // OrderTerminatedEvent published above (synchronously, same transaction).

  private OrderModel retailerOrderOrThrow(LoggedUser retailer, Integer id) {
    return orderRepository.findWithItemsById(id).filter(o -> o.getRetailer().getId().equals(retailer.id())).orElseThrow(() -> new EntityNotFoundException(UserMessages.orderNotFound(id)));
  }

  private OrderModel supplierOrderOrThrow(LoggedUser supplier, Integer id) {
    return orderRepository.findWithItemsById(id).filter(o -> o.getSupplier().getId().equals(supplier.id())).orElseThrow(() -> new EntityNotFoundException(UserMessages.orderNotFound(id)));
  }

  private OrderModel carrierOrderOrThrow(LoggedUser carrier, Integer id) {
    return orderRepository.findWithItemsById(id)
        .filter(o -> o.getCarrier() != null && o.getCarrier().getId().equals(carrier.id()))
        .orElseThrow(() -> new EntityNotFoundException(UserMessages.orderNotFound(id)));
  }

  private void requireStatus(OrderModel order, OrderStatus expected, String action) {
    if (order.getStatus() != expected) {
      throw new IllegalStateException(UserMessages.orderCannotTransition(order.getId(), action, order.getStatus()));
    }
  }

  private OrderModel openTransportRequestOrThrow(Integer id) {
    OrderModel order = orderRepository.findWithItemsById(id)
        .orElseThrow(() -> new EntityNotFoundException(UserMessages.orderNotFound(id)));
    requireAwaitingTransport(order);
    return order;
  }

  private void requireAwaitingTransport(OrderModel order) {
    if (order.getTransportStatus() != TransportStatus.AWAITING_CARRIER) {
      throw new IllegalStateException(UserMessages.ORDER_NOT_AWAITING_TRANSPORT);
    }
  }

  /** Supplier + retailer profiles for one order, keyed by user id — for the carrier route DTO. */
  private Map<Integer, UserProfileModel> carrierRouteProfiles(OrderModel order) {
    return userProfileRepository.findByUserIdIn(Set.of(order.getSupplier().getId(), order.getRetailer().getId())).stream()
        .collect(Collectors.toMap(UserProfileModel::getUserId, Function.identity()));
  }

  private static TransportRequestResponse toTransportRequestResponse(OrderModel order, Map<Integer, UserProfileModel> profilesByUserId, boolean alreadyInterested) {
    UserProfileModel supplierProfile = profilesByUserId.get(order.getSupplier().getId());
    UserProfileModel retailerProfile = profilesByUserId.get(order.getRetailer().getId());
    return new TransportRequestResponse(
        order.getId(),
        order.getSupplier().getId(), order.getSupplier().getName(),
        supplierProfile == null ? null : supplierProfile.getAddress(),
        supplierProfile == null ? null : supplierProfile.getContactName(),
        supplierProfile == null ? null : supplierProfile.getPhone(),
        order.getRetailer().getId(), order.getRetailer().getName(),
        retailerProfile == null ? null : retailerProfile.getAddress(),
        retailerProfile == null ? null : retailerProfile.getContactName(),
        retailerProfile == null ? null : retailerProfile.getPhone(),
        order.getDeliveryDay() == null || order.getDeliverySlot() == null ? null : new DeliveryPreference(order.getDeliveryDay(), order.getDeliverySlot()),
        order.getTotal(),
        alreadyInterested,
        order.getCreatedAt());
  }

  private CarrierDeliveryResponse toCarrierDeliveryResponse(OrderModel order, Map<Integer, UserProfileModel> profilesByUserId) {
    UserProfileModel supplierProfile = profilesByUserId.get(order.getSupplier().getId());
    UserProfileModel retailerProfile = profilesByUserId.get(order.getRetailer().getId());
    return new CarrierDeliveryResponse(
        order.getId(),
        order.getTransportStatus(),
        order.getSupplier().getName(),
        supplierProfile == null ? null : supplierProfile.getAddress(),
        supplierProfile == null ? null : supplierProfile.getPhone(),
        order.getRetailer().getName(),
        retailerProfile == null ? null : retailerProfile.getAddress(),
        retailerProfile == null ? null : retailerProfile.getPhone(),
        order.getDeliveryDay() == null || order.getDeliverySlot() == null ? null : new DeliveryPreference(order.getDeliveryDay(), order.getDeliverySlot()),
        order.getTotal(),
        order.getItems().stream().map(orderMapper::toItemResponse).toList(),
        order.getCreatedAt());
  }

  private static TransportInterestResponse toTransportInterestResponse(TransportInterestModel interest, Map<Integer, UserProfileModel> profilesByUserId) {
    UserProfileModel profile = profilesByUserId.get(interest.getCarrier().getId());
    return new TransportInterestResponse(
        interest.getCarrier().getId(), interest.getCarrier().getName(),
        profile == null ? null : profile.getPhone(),
        interest.getCreatedAt());
  }

}

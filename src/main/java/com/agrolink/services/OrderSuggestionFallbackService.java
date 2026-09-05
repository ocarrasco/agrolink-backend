package com.agrolink.services;

import com.agrolink.dto.response.OrderSuggestionResponse;
import com.agrolink.model.CatalogItemModel;
import com.agrolink.model.OrderItemModel;
import com.agrolink.model.OrderModel;
import com.agrolink.model.UserModel;
import com.agrolink.model.enums.OrderStatus;
import com.agrolink.repositories.ICatalogItemRepository;
import com.agrolink.repositories.IOrderRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderSuggestionFallbackService {

  private static final List<OrderStatus> ELIGIBLE_STATUSES = List.of(OrderStatus.CONFIRMED, OrderStatus.FULFILLED);
  private static final List<OrderStatus> OPEN_STATUSES = List.of(OrderStatus.PLACED, OrderStatus.CONFIRMED);
  private static final double MIN_FACTOR = 0.6;
  private static final double MAX_FACTOR = 0.9;

  @NonNull
  private final IOrderRepository orderRepository;

  @NonNull
  private final ICatalogItemRepository catalogItemRepository;

  @Transactional(readOnly = true)
  public List<OrderSuggestionResponse> getFallbackOrderSuggestions(Integer retailerId) {
    Optional<OrderModel> lastOrder =
        orderRepository.findFirstByRetailerIdAndStatusInOrderByCreatedAtDescIdDesc(retailerId, ELIGIBLE_STATUSES);
    if (lastOrder.isEmpty()) {
      log.info("No fallback suggestions for retailer {}: no previous CONFIRMED/FULFILLED order", retailerId);
      return List.of();
    }

    Set<Integer> excludedProductIds =
        Set.copyOf(orderRepository.findMasterProductIdsWithOpenOrders(retailerId, OPEN_STATUSES));

    UserModel supplier = lastOrder.get().getSupplier();
    List<OrderItemModel> lastItems = lastOrder.get().getItems();
    Map<Integer, CatalogItemModel> currentCatalogById = catalogItemRepository
        .findByIdIn(lastItems.stream().map(i -> i.getCatalogItem().getId()).toList())
        .stream()
        .collect(Collectors.toMap(CatalogItemModel::getId, Function.identity()));

    ThreadLocalRandom random = ThreadLocalRandom.current();
    List<OrderSuggestionResponse> suggestions = new ArrayList<>();
    for (OrderItemModel item : lastItems) {
      if (excludedProductIds.contains(item.getMasterProduct().getId())) {
        continue;
      }

      CatalogItemModel current = currentCatalogById.get(item.getCatalogItem().getId());
      if (current == null || !current.isActive() || current.getAvailableQuantity() <= 0) {
        continue;
      }

      double factor = MIN_FACTOR + random.nextDouble() * (MAX_FACTOR - MIN_FACTOR);
      int quantity = Math.max(1, Math.round(item.getQuantity() * (float) factor));
      quantity = Math.min(quantity, current.getAvailableQuantity());

      suggestions.add(new OrderSuggestionResponse(
          item.getMasterProduct().getId(),
          item.getProductName(),
          item.getUnit(),
          null,
          quantity,
          quantity,
          current.getPricePerUnit(),
          supplier.getId(),
          supplier.getName()));
    }

    log.info("Fallback suggestions for retailer {}: {} item(s) from order {}",
        retailerId, suggestions.size(), lastOrder.get().getId());

    suggestions.sort(Comparator.comparing(OrderSuggestionResponse::productName));
    return suggestions;
  }

}

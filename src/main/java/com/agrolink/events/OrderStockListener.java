package com.agrolink.events;

import com.agrolink.model.CatalogItemModel;
import com.agrolink.repositories.ICatalogItemRepository;
import com.agrolink.repositories.IOrderRepository;
import com.agrolink.utils.UserMessages;
import jakarta.persistence.EntityNotFoundException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Returns the reserved quantities to their catalog items when an order is cancelled or rejected.
 * <p>
 * Runs <b>synchronously in the publisher's transaction</b> ({@code @EventListener} + {@code Propagation.MANDATORY}) so the stock release and the order's status change commit atomically — the interim plain-decrement reservation model needs
 * that (see {@code improvements.md} #1).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderStockListener {

  @NonNull
  private final IOrderRepository orderRepository;

  @NonNull
  private final ICatalogItemRepository catalogItemRepository;

  @EventListener
  @Transactional(propagation = Propagation.MANDATORY)
  public void releaseReservedStock(OrderTerminatedEvent event) {
    var order = orderRepository.findWithItemsById(event.orderId()).orElseThrow(() -> new EntityNotFoundException(UserMessages.orderNotFound(event.orderId())));

    List<Integer> catalogItemIds = order.getItems().stream().map(line -> line.getCatalogItem().getId()).toList();
    Map<Integer, CatalogItemModel> byId = catalogItemRepository.findByIdIn(catalogItemIds).stream().collect(Collectors.toMap(CatalogItemModel::getId, Function.identity()));

    for (var line : order.getItems()) {
      var catalogItem = byId.get(line.getCatalogItem().getId());
      if (catalogItem != null) {
        catalogItem.setAvailableQuantity(catalogItem.getAvailableQuantity() + line.getQuantity());
      }
    }

    log.info("Released reserved stock for {} order {}", event.status(), event.orderId());
  }

}

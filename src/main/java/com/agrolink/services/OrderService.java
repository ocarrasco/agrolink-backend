package com.agrolink.services;

import com.agrolink.dto.CreateOrderRequest;
import com.agrolink.dto.OrderResponse;
import com.agrolink.events.OrderTerminatedEvent;
import com.agrolink.mappers.OrderMapper;
import com.agrolink.model.OrderItemModel;
import com.agrolink.model.OrderModel;
import com.agrolink.model.enums.OrderStatus;
import com.agrolink.repositories.ICatalogItemRepository;
import com.agrolink.repositories.IOrderRepository;
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
import java.util.List;
import java.util.Map;
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
  public List<OrderResponse> listForRetailer(LoggedUser retailer, OrderStatus status) {
    List<OrderModel> orders = status == null ? orderRepository.findByRetailerIdOrderByIdDesc(retailer.id()) : orderRepository.findByRetailerIdAndStatusOrderByIdDesc(retailer.id(), status);
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
  public List<OrderResponse> listForSupplier(LoggedUser supplier, OrderStatus status) {
    List<OrderModel> orders = status == null ? orderRepository.findBySupplierIdOrderByIdDesc(supplier.id()) : orderRepository.findBySupplierIdAndStatusOrderByIdDesc(supplier.id(), status);
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
    order.setStatus(OrderStatus.FULFILLED);
    return orderMapper.toResponse(orderRepository.saveAndFlush(order));
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

  private void requireStatus(OrderModel order, OrderStatus expected, String action) {
    if (order.getStatus() != expected) {
      throw new IllegalStateException(UserMessages.orderCannotTransition(order.getId(), action, order.getStatus()));
    }
  }

}

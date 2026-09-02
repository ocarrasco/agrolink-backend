package com.agrolink.events;

import com.agrolink.model.CatalogItemModel;
import com.agrolink.model.OrderItemModel;
import com.agrolink.model.OrderModel;
import com.agrolink.model.enums.OrderStatus;
import com.agrolink.repositories.ICatalogItemRepository;
import com.agrolink.repositories.IOrderRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderStockListenerTest {

  @Mock
  private IOrderRepository orderRepository;

  @Mock
  private ICatalogItemRepository catalogItemRepository;

  @InjectMocks
  private OrderStockListener listener;

  @Test
  void shouldReturnReservedQuantitiesToCatalogItems() {
    CatalogItemModel tomatoes = catalogItem(10, 50);
    CatalogItemModel potatoes = catalogItem(20, 5);
    OrderModel order = orderWith(orderLine(tomatoes, 100), orderLine(potatoes, 20));

    when(orderRepository.findWithItemsById(7)).thenReturn(Optional.of(order));
    when(catalogItemRepository.findByIdIn(any())).thenReturn(List.of(tomatoes, potatoes));

    listener.releaseReservedStock(new OrderTerminatedEvent(7, OrderStatus.CANCELLED));

    assertThat(tomatoes.getAvailableQuantity()).isEqualTo(150);
    assertThat(potatoes.getAvailableQuantity()).isEqualTo(25);
  }

  @Test
  void shouldThrowWhenOrderNotFound() {
    when(orderRepository.findWithItemsById(99)).thenReturn(Optional.empty());
    var event = new OrderTerminatedEvent(99, OrderStatus.REJECTED);

    assertThatThrownBy(() -> listener.releaseReservedStock(event))
        .isInstanceOf(EntityNotFoundException.class);
  }

  private static CatalogItemModel catalogItem(int id, int available) {
    CatalogItemModel item = new CatalogItemModel();
    item.setId(id);
    item.setAvailableQuantity(available);
    return item;
  }

  private static OrderItemModel orderLine(CatalogItemModel catalogItem, int quantity) {
    OrderItemModel line = new OrderItemModel();
    line.setCatalogItem(catalogItem);
    line.setQuantity(quantity);
    return line;
  }

  private static OrderModel orderWith(OrderItemModel... lines) {
    OrderModel order = new OrderModel();
    for (OrderItemModel line : lines) {
      order.addItem(line);
    }
    return order;
  }

}

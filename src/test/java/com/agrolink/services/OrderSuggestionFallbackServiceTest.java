package com.agrolink.services;

import com.agrolink.dto.response.OrderSuggestionResponse;
import com.agrolink.model.CatalogItemModel;
import com.agrolink.model.MasterProductModel;
import com.agrolink.model.OrderItemModel;
import com.agrolink.model.OrderModel;
import com.agrolink.model.UserModel;
import com.agrolink.model.enums.OrderStatus;
import com.agrolink.model.enums.ProductUnit;
import com.agrolink.repositories.ICatalogItemRepository;
import com.agrolink.repositories.IOrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderSuggestionFallbackServiceTest {

  private static final int RETAILER_ID = 1;
  private static final int SUPPLIER_ID = 7;
  private static final String SUPPLIER_NAME = "Finca Los Andes";

  @Mock
  private IOrderRepository orderRepository;

  @Mock
  private ICatalogItemRepository catalogItemRepository;

  @InjectMocks
  private OrderSuggestionFallbackService service;

  private void noOpenOrders() {
    lenient().when(orderRepository.findMasterProductIdsWithOpenOrders(eq(RETAILER_ID), anyCollection())).thenReturn(List.of());
  }

  @Test
  void getFallbackOrderSuggestions_returnsEmpty_whenRetailerHasNoEligiblePreviousOrder() {
    when(orderRepository.findFirstByRetailerIdAndStatusInOrderByCreatedAtDescIdDesc(eq(RETAILER_ID), anyCollection()))
        .thenReturn(Optional.empty());

    assertThat(service.getFallbackOrderSuggestions(RETAILER_ID)).isEmpty();
  }

  @Test
  void getFallbackOrderSuggestions_suggestsBetween60And90PercentOfLastOrder_cappedByStock() {
    noOpenOrders();
    OrderItemModel tomato = orderItem(10, "Tomate", 100, 1); // last order: 100 units
    OrderModel lastOrder = orderWithItems(tomato);
    when(orderRepository.findFirstByRetailerIdAndStatusInOrderByCreatedAtDescIdDesc(eq(RETAILER_ID), anyCollection()))
        .thenReturn(Optional.of(lastOrder));
    when(catalogItemRepository.findByIdIn(anyCollection()))
        .thenReturn(List.of(catalogItem(1, 10, 1500, 1000, true))); // plenty of stock

    List<OrderSuggestionResponse> suggestions = service.getFallbackOrderSuggestions(RETAILER_ID);

    assertThat(suggestions).hasSize(1);
    OrderSuggestionResponse suggestion = suggestions.get(0);
    assertThat(suggestion.masterProductId()).isEqualTo(10);
    assertThat(suggestion.suggestedQuantity()).isBetween(60, 90);
    assertThat(suggestion.minStock()).isEqualTo(suggestion.suggestedQuantity());
    assertThat(suggestion.referencePrice()).isEqualTo(1500);
    assertThat(suggestion.supplierId()).isEqualTo(SUPPLIER_ID);
    assertThat(suggestion.supplierName()).isEqualTo(SUPPLIER_NAME);
  }

  @Test
  void getFallbackOrderSuggestions_capsQuantity_atCurrentAvailableStock() {
    noOpenOrders();
    OrderItemModel tomato = orderItem(10, "Tomate", 100, 1);
    OrderModel lastOrder = orderWithItems(tomato);
    when(orderRepository.findFirstByRetailerIdAndStatusInOrderByCreatedAtDescIdDesc(eq(RETAILER_ID), anyCollection()))
        .thenReturn(Optional.of(lastOrder));
    when(catalogItemRepository.findByIdIn(anyCollection()))
        .thenReturn(List.of(catalogItem(1, 10, 1500, 3, true))); // only 3 left in stock

    List<OrderSuggestionResponse> suggestions = service.getFallbackOrderSuggestions(RETAILER_ID);

    assertThat(suggestions).hasSize(1);
    assertThat(suggestions.get(0).suggestedQuantity()).isEqualTo(3);
  }

  @Test
  void getFallbackOrderSuggestions_skipsItems_withNoStockOrInactive() {
    noOpenOrders();
    OrderItemModel tomato = orderItem(10, "Tomate", 100, 1);
    OrderItemModel lettuce = orderItem(20, "Lechuga", 50, 2);
    OrderModel lastOrder = orderWithItems(tomato, lettuce);
    when(orderRepository.findFirstByRetailerIdAndStatusInOrderByCreatedAtDescIdDesc(eq(RETAILER_ID), anyCollection()))
        .thenReturn(Optional.of(lastOrder));
    when(catalogItemRepository.findByIdIn(anyCollection())).thenReturn(List.of(
        catalogItem(1, 10, 1500, 0, true),    // out of stock
        catalogItem(2, 20, 700, 20, false)));  // inactive

    assertThat(service.getFallbackOrderSuggestions(RETAILER_ID)).isEmpty();
  }

  @Test
  void getFallbackOrderSuggestions_skipsProduct_withAnOpenPlacedOrConfirmedOrder() {
    OrderItemModel tomato = orderItem(10, "Tomate", 100, 1);
    OrderItemModel lettuce = orderItem(20, "Lechuga", 50, 2);
    OrderModel lastOrder = orderWithItems(tomato, lettuce);
    when(orderRepository.findFirstByRetailerIdAndStatusInOrderByCreatedAtDescIdDesc(eq(RETAILER_ID), anyCollection()))
        .thenReturn(Optional.of(lastOrder));
    when(orderRepository.findMasterProductIdsWithOpenOrders(eq(RETAILER_ID), anyCollection())).thenReturn(List.of(10));
    when(catalogItemRepository.findByIdIn(anyCollection())).thenReturn(List.of(
        catalogItem(1, 10, 1500, 100, true),
        catalogItem(2, 20, 700, 20, true)));

    List<OrderSuggestionResponse> suggestions = service.getFallbackOrderSuggestions(RETAILER_ID);

    assertThat(suggestions).extracting(OrderSuggestionResponse::masterProductId).containsExactly(20);
  }

  private static OrderModel orderWithItems(OrderItemModel... items) {
    OrderModel order = new OrderModel();
    order.setId(99);
    UserModel supplier = new UserModel();
    supplier.setId(SUPPLIER_ID);
    supplier.setName(SUPPLIER_NAME);
    order.setSupplier(supplier);
    order.getItems().addAll(List.of(items));
    return order;
  }

  private static OrderItemModel orderItem(int masterProductId, String name, int quantity, int catalogItemId) {
    MasterProductModel masterProduct = new MasterProductModel();
    masterProduct.setId(masterProductId);
    masterProduct.setName(name);
    masterProduct.setUnit(ProductUnit.KILOGRAMO);

    CatalogItemModel catalogItem = new CatalogItemModel();
    catalogItem.setId(catalogItemId);

    OrderItemModel item = new OrderItemModel();
    item.setMasterProduct(masterProduct);
    item.setCatalogItem(catalogItem);
    item.setProductName(name);
    item.setUnit(ProductUnit.KILOGRAMO);
    item.setQuantity(quantity);
    return item;
  }

  private static CatalogItemModel catalogItem(int id, int masterProductId, int pricePerUnit, int availableQuantity, boolean active) {
    MasterProductModel masterProduct = new MasterProductModel();
    masterProduct.setId(masterProductId);

    CatalogItemModel item = new CatalogItemModel();
    item.setId(id);
    item.setMasterProduct(masterProduct);
    item.setPricePerUnit(pricePerUnit);
    item.setAvailableQuantity(availableQuantity);
    item.setActive(active);
    return item;
  }

}

package com.agrolink.mappers;

import com.agrolink.dto.response.OrderItemResponse;
import com.agrolink.dto.response.OrderResponse;
import com.agrolink.model.CatalogItemModel;
import com.agrolink.model.MasterProductModel;
import com.agrolink.model.OrderItemModel;
import com.agrolink.model.OrderModel;
import com.agrolink.model.UserModel;
import com.agrolink.model.enums.OrderStatus;
import com.agrolink.model.enums.ProductUnit;
import com.agrolink.model.enums.ShippingMethod;
import com.agrolink.model.enums.TimeSlot;
import com.agrolink.model.enums.WeekDay;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

class OrderMapperTest {

  private final OrderMapper orderMapper = Mappers.getMapper(OrderMapper.class);

  @Test
  void toResponse_mapsHeaderFieldsAndItems() {
    OrderModel order = order(1, retailer(10, "Almacen Ana"), supplier(20, "Verduras SPA"),
        OrderStatus.CONFIRMED, 15000, "Entregar antes de las 9am", ShippingMethod.SUPPLIER_DELIVERY, WeekDay.MONDAY, TimeSlot.AM);
    order.addItem(item(order, 100, 200, "Tomate", ProductUnit.KILOGRAMO, 10, 1500, 15000));

    OrderResponse response = orderMapper.toResponse(order);

    assertThat(response.id()).isEqualTo(1);
    assertThat(response.status()).isEqualTo(OrderStatus.CONFIRMED);
    assertThat(response.retailerId()).isEqualTo(10);
    assertThat(response.retailerName()).isEqualTo("Almacen Ana");
    assertThat(response.supplierId()).isEqualTo(20);
    assertThat(response.supplierName()).isEqualTo("Verduras SPA");
    assertThat(response.total()).isEqualTo(15000);
    assertThat(response.supplierNote()).isEqualTo("Entregar antes de las 9am");
    assertThat(response.shippingMethod()).isEqualTo(ShippingMethod.SUPPLIER_DELIVERY);

    assertThat(response.items()).hasSize(1);
    OrderItemResponse itemResponse = response.items().get(0);
    assertThat(itemResponse.catalogItemId()).isEqualTo(100);
    assertThat(itemResponse.masterProductId()).isEqualTo(200);
    assertThat(itemResponse.productName()).isEqualTo("Tomate");
    assertThat(itemResponse.unit()).isEqualTo(ProductUnit.KILOGRAMO);
    assertThat(itemResponse.quantity()).isEqualTo(10);
    assertThat(itemResponse.unitPrice()).isEqualTo(1500);
    assertThat(itemResponse.lineTotal()).isEqualTo(15000);
  }

  @Test
  void toResponse_buildsTheDeliveryPreference_whenDayAndSlotAreBothSet() {
    OrderModel order = order(1, retailer(10, "Almacen Ana"), supplier(20, "Verduras SPA"),
        OrderStatus.PLACED, 0, null, ShippingMethod.PICKUP, WeekDay.FRIDAY, TimeSlot.PM);

    OrderResponse response = orderMapper.toResponse(order);

    assertThat(response.deliveryPreference()).isNotNull();
    assertThat(response.deliveryPreference().day()).isEqualTo(WeekDay.FRIDAY);
    assertThat(response.deliveryPreference().slot()).isEqualTo(TimeSlot.PM);
  }

  @Test
  void toResponse_deliveryPreferenceIsNull_whenDayIsMissing() {
    OrderModel order = order(1, retailer(10, "Almacen Ana"), supplier(20, "Verduras SPA"),
        OrderStatus.PLACED, 0, null, ShippingMethod.PICKUP, null, TimeSlot.PM);

    assertThat(orderMapper.toResponse(order).deliveryPreference()).isNull();
  }

  @Test
  void toResponse_deliveryPreferenceIsNull_whenSlotIsMissing() {
    OrderModel order = order(1, retailer(10, "Almacen Ana"), supplier(20, "Verduras SPA"),
        OrderStatus.PLACED, 0, null, ShippingMethod.PICKUP, WeekDay.FRIDAY, null);

    assertThat(orderMapper.toResponse(order).deliveryPreference()).isNull();
  }

  @Test
  void toResponse_deliveryPreferenceIsNull_whenNeitherIsSet() {
    OrderModel order = order(1, retailer(10, "Almacen Ana"), supplier(20, "Verduras SPA"),
        OrderStatus.PLACED, 0, null, ShippingMethod.PICKUP, null, null);

    assertThat(orderMapper.toResponse(order).deliveryPreference()).isNull();
  }

  @Test
  void toResponseList_mapsEveryOrder() {
    OrderModel first = order(1, retailer(10, "Almacen Ana"), supplier(20, "Verduras SPA"), OrderStatus.PLACED, 0, null, ShippingMethod.PICKUP, null, null);
    OrderModel second = order(2, retailer(10, "Almacen Ana"), supplier(20, "Verduras SPA"), OrderStatus.FULFILLED, 5000, null, ShippingMethod.PICKUP, null, null);

    assertThat(orderMapper.toResponseList(java.util.List.of(first, second)))
        .extracting(OrderResponse::id)
        .containsExactly(1, 2);
  }

  @Test
  void toItemResponse_mapsCatalogItemAndMasterProductIds() {
    OrderModel order = order(1, retailer(10, "Almacen Ana"), supplier(20, "Verduras SPA"), OrderStatus.PLACED, 0, null, ShippingMethod.PICKUP, null, null);
    OrderItemModel orderItem = item(order, 100, 200, "Papa", ProductUnit.SACO, 3, 8000, 24000);

    OrderItemResponse response = orderMapper.toItemResponse(orderItem);

    assertThat(response.catalogItemId()).isEqualTo(100);
    assertThat(response.masterProductId()).isEqualTo(200);
    assertThat(response.productName()).isEqualTo("Papa");
    assertThat(response.lineTotal()).isEqualTo(24000);
  }

  private static UserModel retailer(Integer id, String name) {
    UserModel model = new UserModel();
    model.setId(id);
    model.setName(name);
    return model;
  }

  private static UserModel supplier(Integer id, String name) {
    return retailer(id, name);
  }

  private static OrderModel order(Integer id, UserModel retailer, UserModel supplier, OrderStatus status, Integer total,
      String supplierNote, ShippingMethod shippingMethod, WeekDay deliveryDay, TimeSlot deliverySlot) {
    OrderModel order = new OrderModel();
    order.setId(id);
    order.setRetailer(retailer);
    order.setSupplier(supplier);
    order.setStatus(status);
    order.setTotal(total);
    order.setSupplierNote(supplierNote);
    order.setShippingMethod(shippingMethod);
    order.setDeliveryDay(deliveryDay);
    order.setDeliverySlot(deliverySlot);
    return order;
  }

  private static OrderItemModel item(OrderModel order, Integer catalogItemId, Integer masterProductId, String productName,
      ProductUnit unit, Integer quantity, Integer unitPrice, Integer lineTotal) {
    CatalogItemModel catalogItem = new CatalogItemModel();
    catalogItem.setId(catalogItemId);
    MasterProductModel masterProduct = new MasterProductModel();
    masterProduct.setId(masterProductId);

    OrderItemModel orderItem = new OrderItemModel();
    orderItem.setOrder(order);
    orderItem.setCatalogItem(catalogItem);
    orderItem.setMasterProduct(masterProduct);
    orderItem.setProductName(productName);
    orderItem.setUnit(unit);
    orderItem.setQuantity(quantity);
    orderItem.setUnitPrice(unitPrice);
    orderItem.setLineTotal(lineTotal);
    return orderItem;
  }
}

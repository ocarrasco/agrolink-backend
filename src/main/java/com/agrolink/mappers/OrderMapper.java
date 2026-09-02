package com.agrolink.mappers;

import com.agrolink.dto.request.DeliveryPreference;
import com.agrolink.dto.response.OrderItemResponse;
import com.agrolink.dto.response.OrderResponse;
import com.agrolink.model.OrderItemModel;
import com.agrolink.model.OrderModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrderMapper {

  @Mapping(target = "retailerId", source = "retailer.id")
  @Mapping(target = "retailerName", source = "retailer.name")
  @Mapping(target = "supplierId", source = "supplier.id")
  @Mapping(target = "supplierName", source = "supplier.name")
  @Mapping(target = "deliveryPreference", expression = "java(deliveryPreferenceOf(order))")
  @Mapping(target = "carrierId", source = "carrier.id")
  @Mapping(target = "carrierName", source = "carrier.name")
  OrderResponse toResponse(OrderModel order);

  List<OrderResponse> toResponseList(List<OrderModel> orders);

  @Mapping(target = "catalogItemId", source = "catalogItem.id")
  @Mapping(target = "masterProductId", source = "masterProduct.id")
  OrderItemResponse toItemResponse(OrderItemModel item);

  default DeliveryPreference deliveryPreferenceOf(OrderModel order) {
    if (order.getDeliveryDay() == null || order.getDeliverySlot() == null) {
      return null;
    }
    return new DeliveryPreference(order.getDeliveryDay(), order.getDeliverySlot());
  }

}

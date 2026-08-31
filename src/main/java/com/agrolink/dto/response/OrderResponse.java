package com.agrolink.dto.response;

import com.agrolink.dto.request.DeliveryPreference;
import com.agrolink.model.enums.OrderStatus;
import com.agrolink.model.enums.ShippingMethod;

import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse( //@formatter:off
    Integer id,
    OrderStatus status,
    Integer retailerId,
    String retailerName,
    Integer supplierId,
    String supplierName,
    Integer total,
    String supplierNote,
    ShippingMethod shippingMethod,
    DeliveryPreference deliveryPreference,
    List<OrderItemResponse> items,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) { //@formatter:on

}

package com.agrolink.dto.response;

import com.agrolink.dto.request.DeliveryPreference;
import com.agrolink.model.enums.OrderStatus;
import com.agrolink.model.enums.ShippingMethod;
import com.agrolink.model.enums.TransportStatus;

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
    Integer carrierId,
    String carrierName,
    /** Transport-leg sub-state — non-null only for {@code shippingMethod = PLATFORM_CARRIER}. */
    TransportStatus transportStatus,
    List<OrderItemResponse> items,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) { //@formatter:on

}

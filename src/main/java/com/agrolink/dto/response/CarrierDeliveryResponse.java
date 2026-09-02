package com.agrolink.dto.response;

import com.agrolink.dto.request.DeliveryPreference;
import com.agrolink.model.enums.TransportStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A transport job assigned to the carrier viewing it (order's {@code carrier_id = me}), for the
 * carrier's "my trips" + history views. Carries the pickup/dropoff details the carrier needs to
 * execute the leg.
 */
public record CarrierDeliveryResponse( //@formatter:off
    Integer orderId,
    TransportStatus transportStatus,
    String supplierName,
    String supplierAddress,
    String supplierPhone,
    String retailerName,
    String retailerAddress,
    String retailerPhone,
    DeliveryPreference deliveryPreference,
    Integer total,
    List<OrderItemResponse> items,
    LocalDateTime createdAt
) { //@formatter:on

}

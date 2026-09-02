package com.agrolink.dto.response;

import com.agrolink.dto.request.DeliveryPreference;

import java.time.LocalDateTime;

/** An order awaiting a platform carrier, as seen by carriers browsing the open market. */
public record TransportRequestResponse(
    Integer orderId,
    Integer supplierId,
    String supplierName,
    String supplierAddress,
    String supplierContactName,
    String supplierPhone,
    Integer retailerId,
    String retailerName,
    String retailerAddress,
    String retailerContactName,
    String retailerPhone,
    DeliveryPreference deliveryPreference,
    Integer total,
    /** Whether the carrier viewing this list has already marked interest in this order. */
    boolean alreadyInterested,
    LocalDateTime createdAt
) {

}

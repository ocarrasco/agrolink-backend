package com.agrolink.dto.response;

import com.agrolink.model.WeeklyAvailability;

import java.util.List;

public record SupplierResponse(
    Integer supplierId,
    String name,
    String contact,
    String phone,
    String contactName,
    boolean delivery,
    String address,
    WeeklyAvailability availability,
    List<SupplierProductResponse> products
) {

}

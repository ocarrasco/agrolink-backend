package com.agrolink.dto;

import com.agrolink.model.enums.ProductUnit;

import java.time.LocalDateTime;

public record CatalogItemResponse(
    Integer id,
    Integer masterProductId,
    String masterProductName,
    ProductUnit unit,
    Integer pricePerUnit,
    Integer availableQuantity,
    boolean active
) {

}

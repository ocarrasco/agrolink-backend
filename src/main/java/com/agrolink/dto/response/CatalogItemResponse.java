package com.agrolink.dto.response;

import com.agrolink.model.enums.ProductUnit;

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

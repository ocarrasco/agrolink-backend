package com.agrolink.dto.response;

import com.agrolink.model.enums.ProductUnit;

public record OrderItemResponse(
    Integer id,
    Integer catalogItemId,
    Integer masterProductId,
    String productName,
    ProductUnit unit,
    Integer quantity,
    Integer unitPrice,
    Integer lineTotal
) {

}

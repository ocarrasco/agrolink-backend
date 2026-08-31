package com.agrolink.dto.response;

import com.agrolink.model.enums.ProductUnit;

public record SupplierProductResponse(
    Integer productId,
    String productName,
    ProductUnit unit,
    Integer price,
    Integer stock
) {

}

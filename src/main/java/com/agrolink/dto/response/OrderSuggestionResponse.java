package com.agrolink.dto.response;

import com.agrolink.model.enums.ProductUnit;

/**
 * One line of a retailer's purchase suggestion, tied to a specific supplier — see
 * {@code design_plan.md} iteración 6.
 */
public record OrderSuggestionResponse(
    Integer masterProductId,
    String productName,
    ProductUnit unit,
    Integer avgWeeklySales,
    Integer minStock,
    Integer suggestedQuantity,
    Integer referencePrice,
    Integer supplierId,
    String supplierName
) {

}

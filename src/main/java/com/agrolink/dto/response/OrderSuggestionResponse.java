package com.agrolink.dto.response;

import com.agrolink.model.enums.ProductUnit;

/**
 * One line of a retailer's purchase suggestion. <b>Placeholder heuristic</b> (random active
 * catalog items + small quantities) just to exercise the endpoint end-to-end — the real
 * demand-driven model is {@code design_plan.md} iteración 6.
 */
public record OrderSuggestionResponse(
    Integer masterProductId,
    String productName,
    ProductUnit unit,
    Integer avgWeeklySales,
    Integer minStock,
    Integer suggestedQuantity,
    Integer referencePrice
) {

}

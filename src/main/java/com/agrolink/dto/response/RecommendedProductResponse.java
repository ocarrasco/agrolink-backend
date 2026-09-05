package com.agrolink.dto.response;

import com.agrolink.model.enums.ProductUnit;

/**
 * Raw wire shape of {@code recommender-services}' {@code POST /api/recommendations/{consumerId}}
 * response — {@code supplierId} only, no name (the recommender never touches {@code platform_user}).
 * {@link com.agrolink.services.OrderSuggestionService} enriches this into {@link OrderSuggestionResponse}.
 */
public record RecommendedProductResponse(
    Integer masterProductId,
    String productName,
    ProductUnit unit,
    Integer avgWeeklySales,
    Integer minStock,
    Integer suggestedQuantity,
    Integer referencePrice,
    Integer supplierId
) {

}

package com.agrolink.dto.request;

import com.agrolink.model.enums.ProductUnit;
import com.agrolink.utils.UserMessages;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record CreateCatalogItemRequest(
    @NotNull(message = UserMessages.MASTER_PRODUCT_ID_REQUIRED)
    Integer masterProductId,

    ProductUnit unit,

    @NotNull(message = UserMessages.PRICE_REQUIRED)
    @Positive(message = UserMessages.PRICE_POSITIVE)
    Integer pricePerUnit,

    @NotNull(message = UserMessages.STOCK_REQUIRED)
    @PositiveOrZero(message = UserMessages.STOCK_NOT_NEGATIVE)
    Integer availableQuantity
) {

}

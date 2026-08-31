package com.agrolink.dto;

import com.agrolink.utils.UserMessages;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** One line of a {@link CreateOrderRequest}: a master product and how much of it. */
public record CreateOrderItemRequest(
    @NotNull(message = UserMessages.MASTER_PRODUCT_ID_REQUIRED)
    Integer masterProductId,

    @NotNull(message = UserMessages.QUANTITY_REQUIRED)
    @Positive(message = UserMessages.QUANTITY_POSITIVE)
    Integer quantity
) {

}

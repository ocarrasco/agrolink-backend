package com.agrolink.dto.request;

import com.agrolink.model.enums.ShippingMethod;
import com.agrolink.utils.UserMessages;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * A retailer's request to order from one supplier: which supplier and which master products
 * (with quantities). The backend resolves each {@code masterProductId} to that supplier's
 * catalog item, snapshots price/unit and checks stock.
 */
public record CreateOrderRequest(
    @NotNull(message = UserMessages.SUPPLIER_ID_REQUIRED)
    Integer supplierId,

    @NotEmpty(message = UserMessages.ORDER_PRODUCTS_REQUIRED)
    @Valid
    List<CreateOrderItemRequest> products,

    @NotNull(message = UserMessages.SHIPPING_METHOD_REQUIRED)
    ShippingMethod shippingMethod,

    /** Optional: preferred delivery / pickup window. Null = no preference stated. */
    @Valid
    DeliveryPreference deliveryPreference
) {

}

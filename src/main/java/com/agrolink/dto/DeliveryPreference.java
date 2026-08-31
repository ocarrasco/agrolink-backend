package com.agrolink.dto;

import com.agrolink.model.enums.TimeSlot;
import com.agrolink.model.enums.WeekDay;
import com.agrolink.utils.UserMessages;
import jakarta.validation.constraints.NotNull;

/**
 * The retailer's preferred delivery / pickup window for an order: one weekday + AM/PM slot.
 * The transport method itself (own delivery / platform carrier) is not modelled yet — see
 * {@code improvements.md} #5.
 */
public record DeliveryPreference(
    @NotNull(message = UserMessages.DELIVERY_DAY_REQUIRED) WeekDay day,
    @NotNull(message = UserMessages.DELIVERY_SLOT_REQUIRED) TimeSlot slot
) {

}

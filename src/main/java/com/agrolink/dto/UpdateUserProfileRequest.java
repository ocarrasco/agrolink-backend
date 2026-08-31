package com.agrolink.dto;

import com.agrolink.model.WeeklyAvailability;
import com.agrolink.utils.UserMessages;
import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(
    boolean delivery,
    @Size(max = 255, message = UserMessages.ADDRESS_TOO_LONG) String address,
    @Size(max = 30, message = UserMessages.PHONE_TOO_LONG) String phone,
    @Size(max = 120, message = UserMessages.CONTACT_NAME_TOO_LONG) String contactName,
    WeeklyAvailability availability
) {

}

package com.agrolink.dto;

import com.agrolink.model.WeeklyAvailability;

public record UserProfileResponse(
    boolean delivery,
    String address,
    String phone,
    String contactName,
    WeeklyAvailability availability
) {

}

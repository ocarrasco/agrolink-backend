package com.agrolink.boostrap.dto;

import com.agrolink.model.WeeklyAvailability;

public record RetailerSeed(
    String email,
    String contactName,
    String address,
    String phone,
    boolean delivery,
    WeeklyAvailability availability,
    OrderGenerationSeed orderGeneration
) {

}

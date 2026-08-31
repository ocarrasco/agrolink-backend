package com.agrolink.dto;

import com.agrolink.model.enums.UserRole;

import java.util.UUID;

public record KeycloakUserDto(
    UUID id,
    String email,
    String firstName,
    String lastName,
    boolean enabled,
    UserRole role
) {

}

package com.agrolink.dto.response;

import com.agrolink.model.enums.UserRole;
import com.agrolink.model.enums.UserStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(
    Integer id,
    UUID keycloakId,
    String email,
    String name,
    UserRole role,
    UserStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

}

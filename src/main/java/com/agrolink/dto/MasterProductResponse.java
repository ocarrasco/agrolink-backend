package com.agrolink.dto;

import com.agrolink.model.enums.ProductUnit;

import java.time.LocalDateTime;

public record MasterProductResponse(
    Integer id,
    String name,
    ProductUnit unit,
    boolean active,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

}

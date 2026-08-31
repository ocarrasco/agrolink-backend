package com.agrolink.dto.request;

import com.agrolink.model.enums.ProductUnit;
import com.agrolink.utils.UserMessages;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateMasterProductRequest(
    @NotBlank(message = UserMessages.NAME_REQUIRED)
    @Size(max = 120, message = UserMessages.NAME_TOO_LONG)
    String name,

    @NotNull(message = UserMessages.UNIT_REQUIRED)
    ProductUnit unit,

    boolean active
) {

}

package com.agrolink.dto;

import com.agrolink.utils.UserMessages;
import jakarta.validation.constraints.Size;

public record RejectOrderRequest(
    @Size(max = 500, message = UserMessages.NOTE_TOO_LONG) String note
) {

}

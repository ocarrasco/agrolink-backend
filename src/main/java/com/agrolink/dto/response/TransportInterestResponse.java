package com.agrolink.dto.response;

import java.time.LocalDateTime;

/** A carrier's expression of interest in one order, as seen by the retailer choosing among them. */
public record TransportInterestResponse(
    Integer carrierId,
    String carrierName,
    String carrierPhone,
    LocalDateTime createdAt
) {

}

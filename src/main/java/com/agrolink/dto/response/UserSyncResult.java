package com.agrolink.dto.response;

public record UserSyncResult(
    int totalFromKeycloak,
    int created,
    int updated,
    int skipped,
    int userProfilesCreated
) {

}

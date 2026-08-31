package com.agrolink.security;

import com.agrolink.model.enums.UserRole;

import java.util.UUID;

/**
 * The authenticated platform user, resolved once per request by
 * {@link LoggedUserJwtAuthenticationConverter} and exposed as the Spring Security principal
 * ({@code @AuthenticationPrincipal LoggedUser} or
 * {@link com.agrolink.controllers.BaseController#loggedUser()}).
 *
 * @param id
 *     {@code platform_user.id} — {@code null} for ADMIN (admins aren't marketplace actors and
 *     have no {@code platform_user} row). Only {@code /me/**} and marketplace endpoints read it,
 *     and admins never reach those.
 * @param keycloakId
 *     JWT {@code sub}
 * @param role
 *     the user's single AgroLink role (JWT and {@code platform_user.role} agree by this point)
 */
public record LoggedUser(Integer id, UUID keycloakId, UserRole role) {

  public boolean isAdmin() {
    return role == UserRole.ADMIN;
  }

}

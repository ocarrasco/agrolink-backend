package com.agrolink.model.enums;

import java.util.Optional;

public enum UserRole {

  ADMIN, SUPPLIER, RETAILER, CARRIER;

  /**
   * Spring Security authority for this role, e.g. {@code ROLE_ADMIN}.
   */
  public String authority() {
    return "ROLE_" + name();
  }

  /**
   * Maps a {@code ROLE_*} authority back to a role, empty if it is not one of ours.
   */
  public static Optional<UserRole> fromAuthority(String authority) {
    if (authority == null || !authority.startsWith("ROLE_")) {
      return Optional.empty();
    }
    return fromName(authority.substring("ROLE_".length()));
  }

  /**
   * Maps a bare role name (e.g. a Keycloak realm role) to a role, empty if it is not one of ours.
   */
  public static Optional<UserRole> fromName(String name) {
    try {
      return Optional.of(valueOf(name));
    } catch (IllegalArgumentException | NullPointerException e) {
      return Optional.empty();
    }
  }

}

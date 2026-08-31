package com.agrolink.model.enums;

import java.util.Optional;

public enum UserRole {

  ADMIN, SUPPLIER, RETAILER, CARRIER;

  static final String ROLE_PREFIX = "ROLE_";

  public String authority() {
    return ROLE_PREFIX + name();
  }

  public static Optional<UserRole> fromAuthority(String authority) {
    if (authority == null || !authority.startsWith(ROLE_PREFIX)) {
      return Optional.empty();
    }
    return fromName(authority.substring(ROLE_PREFIX.length()));
  }

  public static Optional<UserRole> fromName(String name) {
    try {
      return Optional.of(valueOf(name));
    } catch (IllegalArgumentException | NullPointerException e) {
      return Optional.empty();
    }
  }

}

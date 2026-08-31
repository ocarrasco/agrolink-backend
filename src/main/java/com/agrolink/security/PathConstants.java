package com.agrolink.security;

/**
 * HTTP path patterns shared by the security wiring.
 */
public final class PathConstants {

  private PathConstants() {
  }

  /** Reachable without authentication — mapped to {@code permitAll} in {@link SecurityConfig}. */
  public static final String[] PUBLIC_PATHS = {
      "/error",
      "/actuator/health",
      "/actuator/prometheus",
      "/swagger-ui/**",
      "/swagger-ui.html",
      "/v3/api-docs/**",
      "/v3/api-docs.yaml",
  };

}

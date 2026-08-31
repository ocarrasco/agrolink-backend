package com.agrolink.security;

public final class PathConstants {

  private PathConstants() {
  }

  static final String[] PUBLIC_PATHS = {
      "/error",
      "/actuator/health",
      "/actuator/prometheus",
      "/swagger-ui/**",
      "/swagger-ui.html",
      "/v3/api-docs/**",
      "/v3/api-docs.yaml",
  };

}

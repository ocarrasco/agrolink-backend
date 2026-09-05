package com.agrolink.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "recommender")
public record RecommenderClientProperties(String baseUrl, Retry retry) {

  /**
   * Exponential backoff: attempt {@code n} waits {@code initialIntervalMs * multiplier^(n-1)},
   * capped at {@code maxIntervalMs}.
   */
  public record Retry(
      @DefaultValue("3") int maxAttempts,
      @DefaultValue("200") long initialIntervalMs,
      @DefaultValue("2.0") double multiplier,
      @DefaultValue("2000") long maxIntervalMs) {

  }

}

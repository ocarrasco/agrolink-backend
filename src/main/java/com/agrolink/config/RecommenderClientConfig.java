package com.agrolink.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Configuration
@EnableConfigurationProperties(RecommenderClientProperties.class)
public class RecommenderClientConfig {

  @Bean
  public RestClient recommenderRestClient(RecommenderClientProperties properties) {
    return RestClient.builder().baseUrl(properties.baseUrl()).build();
  }

  @Bean
  public RetryTemplate recommenderRetryTemplate(RecommenderClientProperties properties) {
    RecommenderClientProperties.Retry retry = properties.retry();
    return RetryTemplate.builder()
        .maxAttempts(retry.maxAttempts())
        .exponentialBackoff(retry.initialIntervalMs(), retry.multiplier(), retry.maxIntervalMs())
        .retryOn(RestClientException.class)
        .withListener(retryLoggingListener())
        .build();
  }

  private RetryListener retryLoggingListener() {
    return new RetryListener() {
      @Override
      public <T, E extends Throwable> void onError(
          RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
        log.warn("Recommender call failed (attempt {}): {}", context.getRetryCount(), throwable.getMessage());
      }
    };
  }

}

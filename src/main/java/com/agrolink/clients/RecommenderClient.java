package com.agrolink.clients;

import com.agrolink.dto.response.RecommendedProductResponse;
import com.agrolink.services.OrderSuggestionService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;

/**
 * Thin HTTP client for the {@code recommender-services} microservice, wrapped in a retry with
 * exponential backoff ({@code recommender.retry.*}). Throws {@code RestClientException} if every
 * attempt fails — callers decide how to degrade (see {@link OrderSuggestionService}).
 */
@Service
@RequiredArgsConstructor
public class RecommenderClient {

  @NonNull
  private final RestClient recommenderRestClient;

  @NonNull
  private final RetryTemplate recommenderRetryTemplate;

  public List<RecommendedProductResponse> fetchRecommendations(Integer consumerId) {
    return recommenderRetryTemplate.execute(context -> {
      RecommendedProductResponse[] suggestions = recommenderRestClient.post()
          .uri("/api/recommendations/{consumerId}", consumerId)
          .retrieve()
          .body(RecommendedProductResponse[].class);
      return suggestions == null ? Collections.emptyList() : List.of(suggestions);
    });
  }

}

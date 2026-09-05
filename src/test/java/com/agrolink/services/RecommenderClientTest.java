package com.agrolink.services;

import com.agrolink.clients.RecommenderClient;
import com.agrolink.dto.response.RecommendedProductResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RecommenderClientTest {

  private static final String RECOMMENDATION_JSON = """
      [{"masterProductId":10,"productName":"Tomate","unit":"KILOGRAMO","avgWeeklySales":8,"minStock":4,"suggestedQuantity":12,"referencePrice":1500,"supplierId":5}]
      """;

  private MockRestServiceServer server;
  private RecommenderClient client;

  private void givenClientWithMaxAttempts(int maxAttempts) {
    RestClient.Builder builder = RestClient.builder();
    server = MockRestServiceServer.bindTo(builder).build();
    RetryTemplate retryTemplate = RetryTemplate.builder()
        .maxAttempts(maxAttempts)
        .exponentialBackoff(1, 2.0, 10)
        .retryOn(RestClientException.class)
        .build();
    client = new RecommenderClient(builder.build(), retryTemplate);
  }

  @Test
  void fetchRecommendations_returnsResult_onFirstAttempt() {
    givenClientWithMaxAttempts(3);
    server.expect(requestTo("/api/recommendations/1"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess(RECOMMENDATION_JSON, MediaType.APPLICATION_JSON));

    List<RecommendedProductResponse> result = client.fetchRecommendations(1);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).masterProductId()).isEqualTo(10);
    assertThat(result.get(0).supplierId()).isEqualTo(5);
    server.verify();
  }

  @Test
  void fetchRecommendations_retriesAndSucceeds_afterTransientFailures() {
    givenClientWithMaxAttempts(3);
    server.expect(requestTo("/api/recommendations/1")).andRespond(withServerError());
    server.expect(requestTo("/api/recommendations/1")).andRespond(withServerError());
    server.expect(requestTo("/api/recommendations/1")).andRespond(withSuccess(RECOMMENDATION_JSON, MediaType.APPLICATION_JSON));

    List<RecommendedProductResponse> result = client.fetchRecommendations(1);

    assertThat(result).hasSize(1);
    server.verify();
  }

  @Test
  void fetchRecommendations_throwsAfterExhaustingAllAttempts() {
    givenClientWithMaxAttempts(3);
    server.expect(requestTo("/api/recommendations/1")).andRespond(withServerError());
    server.expect(requestTo("/api/recommendations/1")).andRespond(withServerError());
    server.expect(requestTo("/api/recommendations/1")).andRespond(withServerError());

    assertThatThrownBy(() -> client.fetchRecommendations(1)).isInstanceOf(RestClientException.class);
    server.verify();
  }

}

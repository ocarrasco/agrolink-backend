package com.agrolink.services;

import com.agrolink.clients.RecommenderClient;
import com.agrolink.dto.response.OrderSuggestionResponse;
import com.agrolink.dto.response.RecommendedProductResponse;
import com.agrolink.model.UserModel;
import com.agrolink.repositories.IUserRepository;
import com.agrolink.security.LoggedUser;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderSuggestionService {

  @NonNull
  private final RecommenderClient recommenderClient;

  @NonNull
  private final OrderSuggestionFallbackService fallbackService;

  @NonNull
  private final IUserRepository userRepository;

  public List<OrderSuggestionResponse> suggestForRetailer(LoggedUser retailer) {
    try {
      List<RecommendedProductResponse> raw = recommenderClient.fetchRecommendations(retailer.id());
      List<OrderSuggestionResponse> result = enrichWithSupplierName(raw);
      log.info("Order suggestions for retailer {}: {} item(s)", retailer.id(), result.size());
      return result;
    } catch (RestClientException e) {
      log.warn("Recommender service unavailable for retailer {} after retries: {}", retailer.id(), e.getMessage());
      return fallbackService.getFallbackOrderSuggestions(retailer.id());
    }
  }

  /** Recommender responses only carry {@code supplierId} — it never touches {@code platform_user}. */
  private List<OrderSuggestionResponse> enrichWithSupplierName(List<RecommendedProductResponse> raw) {
    List<Integer> supplierIds = raw.stream().map(RecommendedProductResponse::supplierId).distinct().toList();
    Map<Integer, String> nameById = userRepository.findAllById(supplierIds).stream()
        .collect(Collectors.toMap(UserModel::getId, UserModel::getName));

    return raw.stream()
        .map(r -> new OrderSuggestionResponse(
            r.masterProductId(), r.productName(), r.unit(), r.avgWeeklySales(), r.minStock(),
            r.suggestedQuantity(), r.referencePrice(), r.supplierId(),
            nameById.getOrDefault(r.supplierId(), "Proveedor #" + r.supplierId())))
        .toList();
  }

}

package com.agrolink.services;

import com.agrolink.clients.RecommenderClient;
import com.agrolink.dto.response.OrderSuggestionResponse;
import com.agrolink.dto.response.RecommendedProductResponse;
import com.agrolink.model.UserModel;
import com.agrolink.model.enums.ProductUnit;
import com.agrolink.model.enums.UserRole;
import com.agrolink.repositories.IUserRepository;
import com.agrolink.security.LoggedUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderSuggestionServiceTest {

  @Mock
  private RecommenderClient recommenderClient;

  @Mock
  private OrderSuggestionFallbackService fallbackService;

  @Mock
  private IUserRepository userRepository;

  @InjectMocks
  private OrderSuggestionService service;

  private final LoggedUser retailer = new LoggedUser(1, UUID.randomUUID(), UserRole.RETAILER);

  @Test
  void suggestForRetailer_enrichesRecommenderResponse_withSupplierName() {
    RecommendedProductResponse raw =
        new RecommendedProductResponse(10, "Tomate", ProductUnit.KILOGRAMO, 8, 4, 12, 1500, 5);
    when(recommenderClient.fetchRecommendations(1)).thenReturn(List.of(raw));
    when(userRepository.findAllById(anyCollection())).thenReturn(List.of(supplier(5, "Finca Los Andes")));

    List<OrderSuggestionResponse> result = service.suggestForRetailer(retailer);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).masterProductId()).isEqualTo(10);
    assertThat(result.get(0).supplierId()).isEqualTo(5);
    assertThat(result.get(0).supplierName()).isEqualTo("Finca Los Andes");
    verifyNoInteractions(fallbackService);
  }

  @Test
  void suggestForRetailer_usesPlaceholderName_whenSupplierNotFound() {
    RecommendedProductResponse raw =
        new RecommendedProductResponse(10, "Tomate", ProductUnit.KILOGRAMO, 8, 4, 12, 1500, 5);
    when(recommenderClient.fetchRecommendations(1)).thenReturn(List.of(raw));
    when(userRepository.findAllById(anyCollection())).thenReturn(List.of());

    List<OrderSuggestionResponse> result = service.suggestForRetailer(retailer);

    assertThat(result.get(0).supplierName()).isEqualTo("Proveedor #5");
  }

  @Test
  void suggestForRetailer_fallsBackToLocalHeuristic_whenRecommenderClientExhaustsRetries() {
    OrderSuggestionResponse fallbackSuggestion =
        new OrderSuggestionResponse(20, "Papa", ProductUnit.KILOGRAMO, null, 5, 5, 600, 7, "Chacra del Sol");
    when(recommenderClient.fetchRecommendations(1)).thenThrow(new RestClientException("boom"));
    when(fallbackService.getFallbackOrderSuggestions(1)).thenReturn(List.of(fallbackSuggestion));

    assertThat(service.suggestForRetailer(retailer)).containsExactly(fallbackSuggestion);
  }

  private static UserModel supplier(int id, String name) {
    UserModel user = new UserModel();
    user.setId(id);
    user.setName(name);
    return user;
  }

}

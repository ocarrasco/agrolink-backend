package com.agrolink.services;

import com.agrolink.dto.response.OrderSuggestionResponse;
import com.agrolink.model.CatalogItemModel;
import com.agrolink.model.MasterProductModel;
import com.agrolink.model.enums.ProductUnit;
import com.agrolink.model.enums.UserRole;
import com.agrolink.repositories.ICatalogItemRepository;
import com.agrolink.security.LoggedUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderSuggestionServiceTest {

  @Mock
  private ICatalogItemRepository catalogItemRepository;

  @InjectMocks
  private OrderSuggestionService service;

  private final LoggedUser retailer = new LoggedUser(1, UUID.randomUUID(), UserRole.RETAILER);

  @Test
  void suggestForRetailer_returnsAtMostFourDistinctProducts_withPositiveQuantities() {
    when(catalogItemRepository.findActiveItems(isNull(), isNull())).thenReturn(List.of(
        item(10, "Tomate", 1500), item(10, "Tomate", 1600),
        item(20, "Papa", 800), item(30, "Cebolla", 900),
        item(40, "Zapallo", 1200), item(50, "Lechuga", 700)));

    List<OrderSuggestionResponse> suggestions = service.suggestForRetailer(retailer);

    assertThat(suggestions).hasSizeLessThanOrEqualTo(4);
    assertThat(suggestions).extracting(OrderSuggestionResponse::masterProductId).doesNotHaveDuplicates();
    assertThat(suggestions).allSatisfy(s -> {
      assertThat(s.suggestedQuantity()).isPositive();
      assertThat(s.referencePrice()).isPositive();
      assertThat(s.productName()).isNotBlank();
    });
  }

  @Test
  void suggestForRetailer_returnsEmpty_whenNoActiveCatalogItems() {
    when(catalogItemRepository.findActiveItems(isNull(), isNull())).thenReturn(List.of());

    assertThat(service.suggestForRetailer(retailer)).isEmpty();
  }

  private static CatalogItemModel item(int masterProductId, String name, int price) {
    MasterProductModel mp = new MasterProductModel();
    mp.setId(masterProductId);
    mp.setName(name);
    mp.setUnit(ProductUnit.KILOGRAMO);

    CatalogItemModel item = new CatalogItemModel();
    item.setMasterProduct(mp);
    item.setUnit(ProductUnit.KILOGRAMO);
    item.setPricePerUnit(price);
    item.setAvailableQuantity(100);
    return item;
  }
}

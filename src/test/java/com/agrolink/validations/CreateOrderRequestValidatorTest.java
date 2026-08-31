package com.agrolink.validations;

import com.agrolink.dto.request.CreateOrderItemRequest;
import com.agrolink.dto.request.CreateOrderRequest;
import com.agrolink.model.CatalogItemModel;
import com.agrolink.model.MasterProductModel;
import com.agrolink.model.UserProfileModel;
import com.agrolink.model.enums.ShippingMethod;
import com.agrolink.repositories.ICatalogItemRepository;
import com.agrolink.repositories.IUserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateOrderRequestValidatorTest {

  @Mock
  private ICatalogItemRepository catalogItemRepository;

  @Mock
  private IUserProfileRepository userProfileRepository;

  @InjectMocks
  private CreateOrderRequestValidator validator;

  @BeforeEach
  void noOfferingsByDefault() {
    lenient().when(catalogItemRepository.findBySupplierIdAndMasterProductIdIn(any(), any())).thenReturn(List.of());
  }

  @Test
  void rejectsPlatformCarrier() {
    Errors errors = validate(request(ShippingMethod.PLATFORM_CARRIER));

    assertThat(errors.getFieldError("shippingMethod")).isNotNull();
  }

  @Test
  void rejectsSupplierDeliveryWhenSupplierDoesNotOfferIt() {
    when(userProfileRepository.findByUserId(2)).thenReturn(Optional.of(profile(false)));

    Errors errors = validate(request(ShippingMethod.SUPPLIER_DELIVERY));

    assertThat(errors.getFieldError("shippingMethod")).isNotNull();
  }

  @Test
  void acceptsSupplierDeliveryWhenSupplierOffersIt() {
    when(userProfileRepository.findByUserId(2)).thenReturn(Optional.of(profile(true)));

    Errors errors = validate(request(ShippingMethod.SUPPLIER_DELIVERY));

    assertThat(errors.getFieldError("shippingMethod")).isNull();
  }

  @Test
  void acceptsPickup() {
    Errors errors = validate(request(ShippingMethod.PICKUP));

    assertThat(errors.getFieldError("shippingMethod")).isNull();
  }

  @Test
  void acceptsAllProducts_whenEveryOneIsOfferedAndActive() {
    var products = List.of(new CreateOrderItemRequest(5, 10), new CreateOrderItemRequest(6, 3));
    when(catalogItemRepository.findBySupplierIdAndMasterProductIdIn(2, Set.of(5, 6)))
        .thenReturn(List.of(offering(5, "Tomate", true), offering(6, "Papa", true)));

    Errors errors = validate(request(products));

    assertThat(errors.hasErrors()).isFalse();
  }

  @Test
  void rejectsTheExactIndex_whenAProductIsNotOfferedBySupplier() {
    var products = List.of(new CreateOrderItemRequest(5, 10), new CreateOrderItemRequest(6, 3));
    when(catalogItemRepository.findBySupplierIdAndMasterProductIdIn(2, Set.of(5, 6)))
        .thenReturn(List.of(offering(5, "Tomate", true)));

    Errors errors = validate(request(products));

    assertThat(errors.getFieldError("products[0].masterProductId")).isNull();
    assertThat(errors.getFieldError("products[1].masterProductId")).isNotNull();
    assertThat(errors.getFieldError("products[1].masterProductId").getCode()).isEqualTo("order.products.notOffered");
  }

  @Test
  void rejectsTheExactIndex_whenAProductIsInactive() {
    var products = List.of(new CreateOrderItemRequest(5, 10), new CreateOrderItemRequest(6, 3));
    when(catalogItemRepository.findBySupplierIdAndMasterProductIdIn(2, Set.of(5, 6)))
        .thenReturn(List.of(offering(5, "Tomate", true), offering(6, "Papa", false)));

    Errors errors = validate(request(products));

    assertThat(errors.getFieldError("products[0].masterProductId")).isNull();
    assertThat(errors.getFieldError("products[1].masterProductId")).isNotNull();
    assertThat(errors.getFieldError("products[1].masterProductId").getCode()).isEqualTo("order.products.inactive");
  }

  @Test
  void rejectsEachIndexIndependently_whenDifferentProductsFailForDifferentReasons() {
    var products = List.of(new CreateOrderItemRequest(5, 10), new CreateOrderItemRequest(6, 3), new CreateOrderItemRequest(7, 1));
    when(catalogItemRepository.findBySupplierIdAndMasterProductIdIn(2, Set.of(5, 6, 7)))
        .thenReturn(List.of(offering(5, "Tomate", true), offering(6, "Papa", false)));

    Errors errors = validate(request(products));

    assertThat(errors.getFieldError("products[0].masterProductId")).isNull();
    assertThat(errors.getFieldError("products[1].masterProductId").getCode()).isEqualTo("order.products.inactive");
    assertThat(errors.getFieldError("products[2].masterProductId").getCode()).isEqualTo("order.products.notOffered");
  }

  @Test
  void rejectsTheWholeProductsField_whenTheSameProductIsRepeated() {
    var products = List.of(new CreateOrderItemRequest(5, 10), new CreateOrderItemRequest(5, 3));
    when(catalogItemRepository.findBySupplierIdAndMasterProductIdIn(2, Set.of(5)))
        .thenReturn(List.of(offering(5, "Tomate", true)));

    Errors errors = validate(request(products));

    assertThat(errors.getFieldError("products").getCode()).isEqualTo("order.products.duplicate");
  }

  private Errors validate(CreateOrderRequest request) {
    var errors = new BeanPropertyBindingResult(request, "createOrderRequest");
    validator.validate(request, errors);
    return errors;
  }

  private static CreateOrderRequest request(ShippingMethod method) {
    return request(List.of(new CreateOrderItemRequest(5, 10)), method);
  }

  private static CreateOrderRequest request(List<CreateOrderItemRequest> products) {
    return request(products, ShippingMethod.PICKUP);
  }

  private static CreateOrderRequest request(List<CreateOrderItemRequest> products, ShippingMethod method) {
    return new CreateOrderRequest(2, products, method, null);
  }

  private static UserProfileModel profile(boolean delivery) {
    UserProfileModel profile = new UserProfileModel();
    profile.setDelivery(delivery);
    return profile;
  }

  private static CatalogItemModel offering(Integer masterProductId, String masterProductName, boolean active) {
    MasterProductModel masterProduct = new MasterProductModel();
    masterProduct.setId(masterProductId);
    masterProduct.setName(masterProductName);

    CatalogItemModel item = new CatalogItemModel();
    item.setMasterProduct(masterProduct);
    item.setActive(active);
    return item;
  }

}

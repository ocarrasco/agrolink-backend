package com.agrolink.validations;

import com.agrolink.dto.CreateOrderItemRequest;
import com.agrolink.dto.CreateOrderRequest;
import com.agrolink.model.UserProfileModel;
import com.agrolink.model.enums.ShippingMethod;
import com.agrolink.repositories.ICatalogItemRepository;
import com.agrolink.repositories.IUserProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateOrderRequestValidatorTest {

  @Mock
  private ICatalogItemRepository catalogItemRepository;

  @Mock
  private IUserProfileRepository userProfileRepository;

  @InjectMocks
  private CreateOrderRequestValidator validator;

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

  private Errors validate(CreateOrderRequest request) {
    when(catalogItemRepository.findBySupplierIdAndMasterProductIdIn(any(), any())).thenReturn(List.of());
    Errors errors = new BeanPropertyBindingResult(request, "createOrderRequest");
    validator.validate(request, errors);
    return errors;
  }

  private static CreateOrderRequest request(ShippingMethod method) {
    return new CreateOrderRequest(2, List.of(new CreateOrderItemRequest(5, 10)), method, null);
  }

  private static UserProfileModel profile(boolean delivery) {
    UserProfileModel profile = new UserProfileModel();
    profile.setDelivery(delivery);
    return profile;
  }

}

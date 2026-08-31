package com.agrolink.validations;

import com.agrolink.dto.request.CreateCatalogItemRequest;
import com.agrolink.model.MasterProductModel;
import com.agrolink.model.enums.ProductUnit;
import com.agrolink.repositories.IMasterProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateCatalogItemRequestValidatorTest {

  @Mock
  private IMasterProductRepository masterProductRepository;

  @InjectMocks
  private CreateCatalogItemRequestValidator validator;

  @Test
  void supports_returnsTrue_forCreateCatalogItemRequest() {
    assertThat(validator.supports(CreateCatalogItemRequest.class)).isTrue();
  }

  @Test
  void supports_returnsFalse_forAnotherClass() {
    assertThat(validator.supports(String.class)).isFalse();
  }

  @Test
  void validate_doesNothing_whenMasterProductIdIsNull() {
    Errors errors = validate(request(null));

    assertThat(errors.hasErrors()).isFalse();
    verify(masterProductRepository, never()).findById(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void validate_rejectsMasterProductId_whenProductDoesNotExist() {
    when(masterProductRepository.findById(1)).thenReturn(Optional.empty());

    Errors errors = validate(request(1));

    assertThat(errors.getFieldError("masterProductId")).isNotNull();
    assertThat(errors.getFieldError("masterProductId").getCode()).isEqualTo("catalogItem.masterProduct.notFound");
  }

  @Test
  void validate_rejectsMasterProductId_whenProductIsInactive() {
    when(masterProductRepository.findById(1)).thenReturn(Optional.of(masterProduct(1, "Tomate", false)));

    Errors errors = validate(request(1));

    assertThat(errors.getFieldError("masterProductId")).isNotNull();
    assertThat(errors.getFieldError("masterProductId").getCode()).isEqualTo("catalogItem.masterProduct.inactive");
  }

  @Test
  void validate_acceptsMasterProductId_whenProductIsActive() {
    when(masterProductRepository.findById(1)).thenReturn(Optional.of(masterProduct(1, "Tomate", true)));

    Errors errors = validate(request(1));

    assertThat(errors.hasErrors()).isFalse();
  }

  private Errors validate(CreateCatalogItemRequest request) {
    Errors errors = new BeanPropertyBindingResult(request, "createCatalogItemRequest");
    validator.validate(request, errors);
    return errors;
  }

  private static CreateCatalogItemRequest request(Integer masterProductId) {
    return new CreateCatalogItemRequest(masterProductId, ProductUnit.KILOGRAMO, 1500, 10);
  }

  private static MasterProductModel masterProduct(Integer id, String name, boolean active) {
    MasterProductModel model = new MasterProductModel();
    model.setId(id);
    model.setName(name);
    model.setActive(active);
    return model;
  }
}

package com.agrolink.validations;

import com.agrolink.dto.request.CreateMasterProductRequest;
import com.agrolink.model.MasterProductModel;
import com.agrolink.model.enums.ProductUnit;
import com.agrolink.repositories.IMasterProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateMasterProductRequestValidatorTest {

  @Mock
  private IMasterProductRepository masterProductRepository;

  @InjectMocks
  private CreateMasterProductRequestValidator createMasterProductRequestValidator;

  @Test
  void supports_returnsTrue_forCreateMasterProductRequest() {
    assertThat(createMasterProductRequestValidator.supports(CreateMasterProductRequest.class)).isTrue();
  }

  @Test
  void supports_returnsFalse_forOtherType() {
    assertThat(createMasterProductRequestValidator.supports(String.class)).isFalse();
  }

  @Test
  void validate() {
    var request = new CreateMasterProductRequest("name", ProductUnit.UNIDAD);
    var errors = new BeanPropertyBindingResult(request, "createMasterProductRequest");

    when(masterProductRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.of(new MasterProductModel()));

    createMasterProductRequestValidator.validate(request, errors);

    assertThat(errors.getAllErrors()).hasSize(1);
  }

  @Test
  void validate_doesNothing_whenTargetIsNotACreateMasterProductRequest() {
    var target = "not a request";
    var errors = new BeanPropertyBindingResult(target, "createMasterProductRequest");

    createMasterProductRequestValidator.validate(target, errors);

    assertThat(errors.getAllErrors()).isEmpty();
    verifyNoInteractions(masterProductRepository);
  }

  @Test
  void validate_doesNothing_whenNameIsNull() {
    var request = new CreateMasterProductRequest(null, ProductUnit.UNIDAD);
    var errors = new BeanPropertyBindingResult(request, "createMasterProductRequest");

    createMasterProductRequestValidator.validate(request, errors);

    assertThat(errors.getAllErrors()).isEmpty();
    verifyNoInteractions(masterProductRepository);
  }

  @Test
  void validate_doesNothing_whenNameIsBlank() {
    var request = new CreateMasterProductRequest("   ", ProductUnit.UNIDAD);
    var errors = new BeanPropertyBindingResult(request, "createMasterProductRequest");

    createMasterProductRequestValidator.validate(request, errors);

    assertThat(errors.getAllErrors()).isEmpty();
    verifyNoInteractions(masterProductRepository);
  }
}
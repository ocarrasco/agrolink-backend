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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateMasterProductRequestValidatorTest {

  @Mock
  private IMasterProductRepository masterProductRepository;

  @InjectMocks
  private CreateMasterProductRequestValidator createMasterProductRequestValidator;

  @Test
  void validate() {
    var request = new CreateMasterProductRequest("name", ProductUnit.UNIDAD);
    var errors = new BeanPropertyBindingResult(request, "createMasterProductRequest");

    when(masterProductRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.of(new MasterProductModel()));

    createMasterProductRequestValidator.validate(request, errors);

    assertThat(errors.getAllErrors()).hasSize(1);
  }
}
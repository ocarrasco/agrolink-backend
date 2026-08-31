package com.agrolink.validations;

import com.agrolink.dto.CreateMasterProductRequest;
import com.agrolink.repositories.IMasterProductRepository;
import com.agrolink.services.MasterProductService;
import com.agrolink.utils.UserMessages;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * Checks that the (normalized) product name is not already taken. {@code @NotBlank} / {@code @Size}
 * are left to bean validation.
 */
@Component
@RequiredArgsConstructor
public class CreateMasterProductRequestValidator implements Validator {

  @NonNull
  private final IMasterProductRepository masterProductRepository;

  @Override
  public boolean supports(@NonNull Class<?> clazz) {
    return CreateMasterProductRequest.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(@NonNull Object target, @NonNull Errors errors) {
    if (!(target instanceof CreateMasterProductRequest request)
        || request.name() == null || request.name().isBlank()) {
      return;
    }

    String name = MasterProductService.normalizeName(request.name());
    masterProductRepository.findByNameIgnoreCase(name).ifPresent(existing ->
        errors.rejectValue("name", "masterProduct.name.duplicate", UserMessages.productNameTaken(name)));
  }

}

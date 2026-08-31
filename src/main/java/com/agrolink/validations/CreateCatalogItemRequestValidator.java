package com.agrolink.validations;

import com.agrolink.dto.CreateCatalogItemRequest;
import com.agrolink.repositories.IMasterProductRepository;
import com.agrolink.utils.UserMessages;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * Checks that the referenced master product exists and is active. The (supplier, product)
 * uniqueness needs the caller identity and stays in {@code CatalogItemService}.
 */
@Component
@RequiredArgsConstructor
public class CreateCatalogItemRequestValidator implements Validator {

  @NonNull
  private final IMasterProductRepository masterProductRepository;

  @Override
  public boolean supports(@NonNull Class<?> clazz) {
    return CreateCatalogItemRequest.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(@NonNull Object target, @NonNull Errors errors) {
    if (!(target instanceof CreateCatalogItemRequest request) || request.masterProductId() == null) {
      return;
    }

    var masterProduct = masterProductRepository.findById(request.masterProductId()).orElse(null);
    if (masterProduct == null) {
      errors.rejectValue("masterProductId", "catalogItem.masterProduct.notFound",
          UserMessages.masterProductNotFound(request.masterProductId()));
    } else if (!masterProduct.isActive()) {
      errors.rejectValue("masterProductId", "catalogItem.masterProduct.inactive",
          UserMessages.productNotAvailable(masterProduct.getName()));
    }
  }

}

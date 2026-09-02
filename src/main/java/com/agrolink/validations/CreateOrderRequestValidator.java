package com.agrolink.validations;

import com.agrolink.dto.request.CreateOrderItemRequest;
import com.agrolink.dto.request.CreateOrderRequest;
import com.agrolink.model.UserProfileModel;
import com.agrolink.model.enums.ShippingMethod;
import com.agrolink.repositories.ICatalogItemRepository;
import com.agrolink.repositories.IUserProfileRepository;
import com.agrolink.utils.UserMessages;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CreateOrderRequestValidator implements Validator {

  @NonNull
  private final ICatalogItemRepository catalogItemRepository;

  @NonNull
  private final IUserProfileRepository userProfileRepository;

  @Override
  public boolean supports(@NonNull Class<?> clazz) {
    return CreateOrderRequest.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(@NonNull Object target, @NonNull Errors errors) {
    if (!(target instanceof CreateOrderRequest request)
        || request.supplierId() == null
        || request.products() == null || request.products().isEmpty()) {
      return;
    }

    validateShippingMethod(request, errors);
    validateProducts(request, errors);
  }

  private void validateShippingMethod(CreateOrderRequest request, Errors errors) {
    var method = request.shippingMethod();
    if (method == ShippingMethod.SUPPLIER_DELIVERY) {
      boolean offersDelivery = userProfileRepository.findByUserId(request.supplierId())
          .map(UserProfileModel::isDelivery)
          .orElse(false);
      if (!offersDelivery) {
        errors.rejectValue("shippingMethod", "order.shipping.supplierHasNoDelivery",
            UserMessages.SUPPLIER_HAS_NO_DELIVERY);
      }
    }
  }

  private void validateProducts(CreateOrderRequest request, Errors errors) {
    List<CreateOrderItemRequest> products = request.products();

    List<Integer> productIds = products.stream()
        .map(CreateOrderItemRequest::masterProductId)
        .filter(Objects::nonNull)
        .toList();
    Set<Integer> distinctProductIds = new LinkedHashSet<>(productIds);

    if (distinctProductIds.size() != productIds.size()) {
      errors.rejectValue("products", "order.products.duplicate", UserMessages.ORDER_PRODUCTS_DUPLICATE);
    }

    var offerings = catalogItemRepository.findBySupplierIdAndMasterProductIdIn(request.supplierId(), distinctProductIds);
    var offeringsByProductId = offerings.stream()
        .collect(Collectors.toMap(item -> item.getMasterProduct().getId(), Function.identity()));

    for (int i = 0; i < products.size(); i++) {
      Integer masterProductId = products.get(i).masterProductId();
      if (masterProductId == null) {
        continue; // flagged by @NotNull on the item itself
      }

      String field = "products[%d].masterProductId".formatted(i);
      var offering = offeringsByProductId.get(masterProductId);
      if (offering == null) {
        errors.rejectValue(field, "order.products.notOffered", UserMessages.ORDER_PRODUCTS_NOT_OFFERED);
      } else if (!offering.isActive()) {
        errors.rejectValue(field, "order.products.inactive",
            UserMessages.itemNoLongerAvailable(offering.getMasterProduct().getName()));
      }
    }
  }

}

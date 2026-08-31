package com.agrolink.validations;

import com.agrolink.dto.CreateOrderItemRequest;
import com.agrolink.dto.CreateOrderRequest;
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
import java.util.stream.Collectors;

/**
 * Request-shape validation for {@link CreateOrderRequest}: no duplicate product; the given
 * supplier actually offers every requested master product (and each offering is active); and the
 * chosen shipping method is usable. Stock and "can't order from yourself" are business rules
 * that need the caller identity and stay in {@code OrderService}.
 */
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

  /**
   * Only {@code PICKUP} and {@code SUPPLIER_DELIVERY} are usable today; {@code SUPPLIER_DELIVERY}
   * also needs the supplier to offer it ({@code user_profile.delivery = true}). Platform-carrier
   * shipping is deferred — see {@code transporte_carrier.md}. A null method is left to {@code @NotNull}.
   */
  private void validateShippingMethod(CreateOrderRequest request, Errors errors) {
    ShippingMethod method = request.shippingMethod();
    if (method == ShippingMethod.PLATFORM_CARRIER) {
      errors.rejectValue("shippingMethod", "order.shipping.platformCarrierUnavailable",
          UserMessages.PLATFORM_CARRIER_NOT_AVAILABLE);
    } else if (method == ShippingMethod.SUPPLIER_DELIVERY) {
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
    List<Integer> productIds = request.products().stream()
        .map(CreateOrderItemRequest::masterProductId)
        .filter(Objects::nonNull)
        .toList();
    Set<Integer> distinctProductIds = new LinkedHashSet<>(productIds);

    if (distinctProductIds.size() != productIds.size()) {
      errors.rejectValue("products", "order.products.duplicate", UserMessages.ORDER_PRODUCTS_DUPLICATE);
    }

    var offerings = catalogItemRepository.findBySupplierIdAndMasterProductIdIn(
        request.supplierId(), distinctProductIds);
    Set<Integer> offeredProductIds = offerings.stream()
        .map(item -> item.getMasterProduct().getId())
        .collect(Collectors.toSet());

    if (!offeredProductIds.containsAll(distinctProductIds)) {
      errors.rejectValue("products", "order.products.notOffered", UserMessages.ORDER_PRODUCTS_NOT_OFFERED);
      return;
    }

    offerings.stream().filter(item -> !item.isActive()).findFirst().ifPresent(inactive ->
        errors.rejectValue("products", "order.products.inactive",
            UserMessages.itemNoLongerAvailable(inactive.getMasterProduct().getName())));
  }

}

package com.agrolink.services;

import com.agrolink.dto.SupplierResponse;
import com.agrolink.mappers.SupplierMapper;
import com.agrolink.model.CatalogItemModel;
import com.agrolink.model.UserProfileModel;
import com.agrolink.model.WeeklyAvailability;
import com.agrolink.repositories.ICatalogItemRepository;
import com.agrolink.repositories.IUserProfileRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CatalogService {

  @NonNull
  private final ICatalogItemRepository catalogItemRepository;

  @NonNull
  private final IUserProfileRepository userProfileRepository;

  @NonNull
  private final SupplierMapper supplierMapper;

  @Transactional(readOnly = true)
  public List<SupplierResponse> listSuppliers(Integer masterProductId, String q) {
    String query = (q == null || q.isBlank()) ? null : q.trim();
    var itemsBySupplier = catalogItemRepository.findActiveItems(masterProductId, query).stream()
        .collect(Collectors.groupingBy(c -> c.getSupplier().getId(), LinkedHashMap::new, Collectors.toList()));
    var profiles = userProfileRepository.findByUserIdIn(itemsBySupplier.keySet()).stream()
        .collect(Collectors.toMap(UserProfileModel::getUserId, Function.identity()));
    return itemsBySupplier.values().stream().map(items -> toSupplier(items, profiles)).toList();
  }

  private SupplierResponse toSupplier(List<CatalogItemModel> items, Map<Integer, UserProfileModel> profiles) {
    var supplier = items.get(0).getSupplier();
    var profile = profiles.get(supplier.getId());

    boolean delivery = profile != null && profile.isDelivery();
    String address = profile == null ? null : profile.getAddress();
    String phone = profile == null ? null : profile.getPhone();
    String contactName = profile == null ? null : profile.getContactName();
    var availability = profile == null || profile.getAvailability() == null
        ? WeeklyAvailability.empty()
        : profile.getAvailability().normalized();

    return new SupplierResponse(supplier.getId(), supplier.getName(), supplier.getEmail(), phone, contactName,
        delivery, address, availability, supplierMapper.toSupplierProducts(items));
  }

}

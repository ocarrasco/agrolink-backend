package com.agrolink.boostrap;

import com.agrolink.boostrap.dto.SeedStatus;
import com.agrolink.boostrap.dto.SupplierSeed;
import com.agrolink.model.CatalogItemModel;
import com.agrolink.model.MasterProductModel;
import com.agrolink.model.UserProfileModel;
import com.agrolink.model.WeeklyAvailability;
import com.agrolink.repositories.ICatalogItemRepository;
import com.agrolink.repositories.IMasterProductRepository;
import com.agrolink.repositories.IUserProfileRepository;
import com.agrolink.repositories.IUserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Slf4j
@Component
@RequiredArgsConstructor
public class SupplierSeedService {

  static final String SEED_FILE = "init/suppliers.json";

  private static final int PRICE_MIN = 300;
  private static final int PRICE_MAX = 6000;
  private static final int QTY_MIN = 150;
  private static final int QTY_MAX = 3000;

  @NonNull
  private final ObjectMapper objectMapper;

  @NonNull
  private final IUserRepository userRepository;

  @NonNull
  private final IUserProfileRepository userProfileRepository;

  @NonNull
  private final IMasterProductRepository masterProductRepository;

  @NonNull
  private final ICatalogItemRepository catalogItemRepository;

  /**
   * Parsed entries of the seed file; empty list when the file is absent.
   */
  List<SupplierSeed> load() throws IOException {
    ClassPathResource resource = new ClassPathResource(SEED_FILE);
    if (!resource.exists()) {
      return List.of();
    }
    try (InputStream in = resource.getInputStream()) {
      return objectMapper.readValue(in, new TypeReference<List<SupplierSeed>>() {
      });
    }
  }

  @Transactional
  public Outcome seedSupplier(SupplierSeed seed) {
    if (seed.email() == null || seed.email().isBlank()) {
      return new Outcome(SeedStatus.SKIPPED_BLANK_EMAIL, false, 0);
    }
    var user = userRepository.findFirstByEmailIgnoreCase(seed.email().trim());
    if (user.isEmpty()) {
      return new Outcome(SeedStatus.UNMATCHED, false, 0);
    }
    Integer userId = user.get().getId();
    boolean profileWritten = applyProfile(userId, seed);
    int catalogItems = seedCatalog(userId, seed);
    return new Outcome(SeedStatus.SEEDED, profileWritten, catalogItems);
  }

  private boolean applyProfile(Integer userId, SupplierSeed seed) {
    UserProfileModel profile = userProfileRepository.findByUserId(userId)
        .orElseGet(() -> {
          UserProfileModel fresh = new UserProfileModel();
          fresh.setUserId(userId);
          return fresh;
        });

    if (isConfigured(profile)) {
      return false;
    }

    profile.setDelivery(seed.delivery());
    profile.setAddress(blankToNull(seed.address()));
    profile.setPhone(blankToNull(seed.phone()));
    profile.setContactName(blankToNull(seed.contactName()));
    profile.setAvailability(seed.availability() == null
        ? WeeklyAvailability.empty()
        : seed.availability().normalized());
    userProfileRepository.save(profile);
    return true;
  }

  private int seedCatalog(Integer userId, SupplierSeed seed) {
    int size = seed.catalogSize();
    if (size <= 0 || catalogItemRepository.existsBySupplierId(userId)) {
      return 0;
    }

    List<MasterProductModel> products = new ArrayList<>(masterProductRepository.findByActiveTrueOrderByNameAsc());
    if (products.isEmpty()) {
      log.warn("Catalog seed for user {} skipped: no active master products (enable classpath:seed)", userId);
      return 0;
    }

    Random random = new Random(seed.email().toLowerCase().hashCode());
    Collections.shuffle(products, random);
    int count = Math.min(size, products.size());

    List<CatalogItemModel> items = new ArrayList<>(count);
    for (MasterProductModel product : products.subList(0, count)) {
      CatalogItemModel item = new CatalogItemModel();
      item.setSupplier(userRepository.getReferenceById(userId));
      item.setMasterProduct(product);
      item.setUnit(product.getUnit());
      item.setPricePerUnit(roundTo(random.nextInt(PRICE_MIN, PRICE_MAX + 1), 100));
      item.setAvailableQuantity(roundTo(random.nextInt(QTY_MIN, QTY_MAX + 1), 50));
      item.setActive(true);
      items.add(item);
    }
    catalogItemRepository.saveAll(items);
    return count;
  }

  private static boolean isConfigured(UserProfileModel profile) {
    return profile.isDelivery()
        || profile.getAddress() != null
        || profile.getPhone() != null
        || profile.getContactName() != null
        || (profile.getAvailability() != null
        && !profile.getAvailability().normalized().equals(WeeklyAvailability.empty()));
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private static int roundTo(int value, int step) {
    return Math.max(step, (value / step) * step);
  }

  record Outcome(SeedStatus status, boolean profileWritten, int catalogItems) {

  }

}

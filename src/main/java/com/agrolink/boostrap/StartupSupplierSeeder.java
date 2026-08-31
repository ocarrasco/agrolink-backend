package com.agrolink.boostrap;

import com.agrolink.boostrap.SupplierSeedService.Outcome;
import com.agrolink.boostrap.SupplierSeedService.SeedStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Thin trigger: once the app is up (after {@link StartupUserSync}), runs {@link SupplierSeedService}
 * over every entry of {@code init/suppliers.json}. Per-entry failures are logged and skipped; a
 * broken seed file is logged and ignored. Toggle with {@code agrolink.startup-seed.enabled}.
 */
@Slf4j
@Component
@Order(20)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "agrolink.startup-seed.enabled", havingValue = "true", matchIfMissing = true)
public class StartupSupplierSeeder {

  private final SupplierSeedService seedService;

  @EventListener(ApplicationReadyEvent.class)
  @Order(20)
  public void seedOnStartup() {
    log.info("Supplier seed from {}: {}", SupplierSeedService.SEED_FILE, run());
  }

  /** Package-visible for testing. */
  String run() {
    List<SupplierSeed> seeds;
    try {
      seeds = seedService.load();
    } catch (Exception e) {
      log.warn("Supplier seed: could not read {} (non-fatal)", SupplierSeedService.SEED_FILE, e);
      return "seed file unreadable";
    }

    int profiles = 0;
    int catalogItems = 0;
    int unmatched = 0;
    for (SupplierSeed seed : seeds) {
      try {
        Outcome outcome = seedService.seedSupplier(seed);
        if (outcome.profileWritten()) {
          profiles++;
        }
        catalogItems += outcome.catalogItems();
        if (outcome.status() == SeedStatus.UNMATCHED) {
          unmatched++;
        }
      } catch (Exception e) {
        log.warn("Supplier seed: entry {} failed (non-fatal)", seed.email(), e);
      }
    }

    return "profiles=%d, catalogItems=%d, unmatched=%d, entries=%d"
        .formatted(profiles, catalogItems, unmatched, seeds.size());
  }

}

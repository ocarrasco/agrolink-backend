package com.agrolink.boostrap;

import com.agrolink.boostrap.RetailerSeedService.Outcome;
import com.agrolink.boostrap.dto.RetailerSeed;
import com.agrolink.boostrap.dto.SeedStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Thin trigger: once {@link StartupSupplierSeeder} has seeded supplier catalogs (needed to
 * resolve each demo order's supplier), runs {@link RetailerSeedService} over every entry of
 * {@code init/retailers.json}. Per-entry failures are logged and skipped. Toggle with
 * {@code agrolink.startup-seed.enabled} (same flag as the supplier seeder).
 */
@Slf4j
@Component
@Order(30)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "agrolink.startup-seed.enabled", havingValue = "true", matchIfMissing = true)
public class StartupRetailerSeeder {

  private final RetailerSeedService seedService;

  @EventListener(ApplicationReadyEvent.class)
  @Order(30)
  public void seedOnStartup() {
    log.info("Retailer seed from {}: {}", RetailerSeedService.SEED_FILE, run());
  }

  /** Package-visible for testing. */
  String run() {
    List<RetailerSeed> seeds;
    try {
      seeds = seedService.load();
    } catch (Exception e) {
      log.warn("Retailer seed: could not read {} (non-fatal)", RetailerSeedService.SEED_FILE, e);
      return "seed file unreadable";
    }

    int profiles = 0;
    int orders = 0;
    int unmatched = 0;
    for (RetailerSeed seed : seeds) {
      try {
        Outcome outcome = seedService.seedRetailer(seed);
        if (outcome.profileWritten()) {
          profiles++;
        }
        orders += outcome.ordersCreated();
        if (outcome.status() == SeedStatus.UNMATCHED) {
          unmatched++;
        }
      } catch (Exception e) {
        log.warn("Retailer seed: entry {} failed (non-fatal)", seed.email(), e);
      }
    }

    return "profiles=%d, orders=%d, unmatched=%d, entries=%d"
        .formatted(profiles, orders, unmatched, seeds.size());
  }

}

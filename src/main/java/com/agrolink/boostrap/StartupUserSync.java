package com.agrolink.boostrap;

import com.agrolink.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Once the app is up, runs {@link UserService#syncFromKeycloak()}: upserts every non-admin
 * Keycloak user into {@code platform_user} (mirroring role + status) and provisions a default
 * {@code user_profile} for each. Stands in for the admin's user-provisioning flow (out of
 * scope). A failure here (e.g. Keycloak not reachable yet) is logged, not fatal —
 * {@code POST /users/sync} stays available as a manual retry.
 */
@Slf4j
@Component
@Order(10)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "agrolink.startup-sync.enabled", havingValue = "true", matchIfMissing = true)
public class StartupUserSync {

  private final UserService userService;

  // runs before StartupProfileSeeder (@Order 20), which needs platform_user rows to exist
  @EventListener(ApplicationReadyEvent.class)
  @Order(10)
  public void syncOnStartup() {
    try {
      log.info("Startup Keycloak user sync: {}", userService.syncFromKeycloak());
    } catch (RuntimeException e) {
      log.error("Startup Keycloak user sync failed; run POST /users/sync once Keycloak is reachable", e);
    }
  }

}

package com.agrolink.services;

import com.agrolink.config.KeycloakAdminProperties;
import com.agrolink.dto.KeycloakUserDto;
import com.agrolink.model.enums.UserRole;
import com.agrolink.utils.UserMessages;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakService {

  private static final int PAGE_SIZE = 100;

  private final Keycloak keycloak;
  private final KeycloakAdminProperties properties;

  /**
   * Every user of the configured realm, with its AgroLink {@link UserRole} resolved from the
   * per-user realm role mappings. Paginates the user list; one role-mapping call per user
   * (the realm is small and this is a startup / on-demand operation).
   */
  public List<KeycloakUserDto> fetchAllUsers() {
    List<KeycloakUserDto> users = new ArrayList<>();
    try {
      var realm = keycloak.realm(properties.realm());
      int first = 0;
      List<UserRepresentation> page;
      do {
        page = realm.users().list(first, PAGE_SIZE);
        for (UserRepresentation rep : page) {
          UserRole role = agrolinkRole(rep.getId());
          users.add(toDto(rep, role));
        }
        first += PAGE_SIZE;
      } while (page.size() == PAGE_SIZE);
    } catch (ProcessingException | WebApplicationException e) {
      throw new KeycloakSyncException(UserMessages.keycloakFetchFailed(properties.realm()), e);
    }

    log.info("Fetched {} users from Keycloak realm {}", users.size(), properties.realm());
    return users;
  }

  /** The user's AgroLink realm role, or {@code null} (Keycloak default roles are ignored). */
  private UserRole agrolinkRole(String userId) {
    List<RoleRepresentation> realmRoles = keycloak.realm(properties.realm())
        .users().get(userId).roles().realmLevel().listAll();
    return realmRoles.stream()
        .map(RoleRepresentation::getName)
        .map(UserRole::fromName)
        .flatMap(java.util.Optional::stream)
        .findFirst()
        .orElse(null);
  }

  private static KeycloakUserDto toDto(UserRepresentation rep, UserRole role) {
    return new KeycloakUserDto(
        UUID.fromString(rep.getId()),
        rep.getEmail(),
        rep.getFirstName(),
        rep.getLastName(),
        Boolean.TRUE.equals(rep.isEnabled()),
        role
    );
  }

}

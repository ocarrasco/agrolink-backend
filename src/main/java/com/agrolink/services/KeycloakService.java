package com.agrolink.services;

import com.agrolink.config.KeycloakAdminProperties;
import com.agrolink.dto.KeycloakUserDto;
import com.agrolink.model.enums.UserRole;
import com.agrolink.utils.UserMessages;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import lombok.NonNull;
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

    @NonNull
    private final Keycloak keycloak;

    @NonNull
    private final KeycloakAdminProperties properties;

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

    private KeycloakUserDto toDto(UserRepresentation rep, UserRole role) {
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

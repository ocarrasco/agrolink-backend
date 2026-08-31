package com.agrolink.services;

import com.agrolink.config.KeycloakAdminProperties;
import com.agrolink.dto.KeycloakUserDto;
import com.agrolink.model.enums.UserRole;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KeycloakServiceTest {

    private static final String REALM = "AgroLink";

    @Mock
    private Keycloak keycloak;

    @Mock
    private KeycloakAdminProperties properties;

    @Mock
    private RealmResource realmResource;

    @Mock
    private UsersResource usersResource;

    @Mock
    private UserResource userResource;

    @Mock
    private RoleMappingResource roleMappingResource;

    @Mock
    private RoleScopeResource roleScopeResource;

    @InjectMocks
    private KeycloakService keycloakService;

    @BeforeEach
    void setUp() {
        when(properties.realm()).thenReturn(REALM);
        when(keycloak.realm(REALM)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
    }

    @Test
    void fetchAllUsers_returnsEmptyList_whenRealmHasNoUsers() {
        when(usersResource.list(0, 100)).thenReturn(List.of());

        assertThat(keycloakService.fetchAllUsers()).isEmpty();
    }

    @Test
    void fetchAllUsers_mapsUserFieldsAndRealmRole() {
        String id = UUID.randomUUID().toString();
        UserRepresentation rep = userRep(id, "juan@agrolink.com", "Juan", "Perez", true);
        when(usersResource.list(0, 100)).thenReturn(List.of(rep));
        stubRoles(id, roleRep("SUPPLIER"));

        List<KeycloakUserDto> users = keycloakService.fetchAllUsers();

        assertThat(users).hasSize(1);
        KeycloakUserDto dto = users.get(0);
        assertThat(dto.id()).isEqualTo(UUID.fromString(id));
        assertThat(dto.email()).isEqualTo("juan@agrolink.com");
        assertThat(dto.firstName()).isEqualTo("Juan");
        assertThat(dto.lastName()).isEqualTo("Perez");
        assertThat(dto.enabled()).isTrue();
        assertThat(dto.role()).isEqualTo(UserRole.SUPPLIER);
    }

    @Test
    void fetchAllUsers_setsRoleNull_whenUserHasNoAgrolinkRealmRole() {
        String id = UUID.randomUUID().toString();
        UserRepresentation rep = userRep(id, "sin-rol@agrolink.com", "Sin", "Rol", true);
        when(usersResource.list(0, 100)).thenReturn(List.of(rep));
        stubRoles(id, roleRep("offline_access"), roleRep("uma_authorization"));

        KeycloakUserDto dto = keycloakService.fetchAllUsers().get(0);

        assertThat(dto.role()).isNull();
    }

    @Test
    void fetchAllUsers_picksFirstAgrolinkRole_whenUserHasSeveral() {
        String id = UUID.randomUUID().toString();
        UserRepresentation rep = userRep(id, "multi@agrolink.com", "Multi", "Rol", true);
        when(usersResource.list(0, 100)).thenReturn(List.of(rep));
        stubRoles(id, roleRep("ADMIN"), roleRep("RETAILER"));

        KeycloakUserDto dto = keycloakService.fetchAllUsers().get(0);

        assertThat(dto.role()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    void fetchAllUsers_mapsEnabledFalse_whenIsEnabledIsNull() {
        String id = UUID.randomUUID().toString();
        UserRepresentation rep = userRep(id, "deshabilitado@agrolink.com", "No", "Habilitado", null);
        when(usersResource.list(0, 100)).thenReturn(List.of(rep));
        stubRoles(id);

        KeycloakUserDto dto = keycloakService.fetchAllUsers().get(0);

        assertThat(dto.enabled()).isFalse();
    }

    @Test
    void fetchAllUsers_paginates_whenFirstPageIsFull() {
        List<UserRepresentation> firstPage = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            firstPage.add(userRep(UUID.randomUUID().toString(), "user" + i + "@agrolink.com", "User", "" + i, true));
        }
        List<UserRepresentation> secondPage = List.of(userRep(UUID.randomUUID().toString(), "last@agrolink.com", "Last", "User", true));
        when(usersResource.list(0, 100)).thenReturn(firstPage);
        when(usersResource.list(100, 100)).thenReturn(secondPage);
        when(usersResource.get(anyString())).thenReturn(userResource);
        when(userResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.realmLevel()).thenReturn(roleScopeResource);
        when(roleScopeResource.listAll()).thenReturn(List.of());

        List<KeycloakUserDto> users = keycloakService.fetchAllUsers();

        assertThat(users).hasSize(101);
    }

    @Test
    void fetchAllUsers_throwsKeycloakSyncException_onProcessingException() {
        when(usersResource.list(0, 100)).thenThrow(new ProcessingException("timeout"));

        assertThatThrownBy(() -> keycloakService.fetchAllUsers())
                .isInstanceOf(KeycloakSyncException.class)
                .hasMessageContaining(REALM)
                .hasCauseInstanceOf(ProcessingException.class);
    }

    @Test
    void fetchAllUsers_throwsKeycloakSyncException_onWebApplicationException() {
        when(usersResource.list(0, 100)).thenThrow(new WebApplicationException("unauthorized"));

        assertThatThrownBy(() -> keycloakService.fetchAllUsers())
                .isInstanceOf(KeycloakSyncException.class)
                .hasMessageContaining(REALM)
                .hasCauseInstanceOf(WebApplicationException.class);
    }

    private void stubRoles(String userId, RoleRepresentation... roles) {
        when(usersResource.get(userId)).thenReturn(userResource);
        when(userResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.realmLevel()).thenReturn(roleScopeResource);
        when(roleScopeResource.listAll()).thenReturn(List.of(roles));
    }

    private static UserRepresentation userRep(String id, String email, String firstName, String lastName, Boolean enabled) {
        UserRepresentation rep = new UserRepresentation();
        rep.setId(id);
        rep.setEmail(email);
        rep.setFirstName(firstName);
        rep.setLastName(lastName);
        rep.setEnabled(enabled);
        return rep;
    }

    private static RoleRepresentation roleRep(String name) {
        RoleRepresentation role = new RoleRepresentation();
        role.setName(name);
        return role;
    }
}

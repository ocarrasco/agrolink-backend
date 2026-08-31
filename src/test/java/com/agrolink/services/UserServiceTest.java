package com.agrolink.services;

import com.agrolink.dto.KeycloakUserDto;
import com.agrolink.dto.response.UserResponse;
import com.agrolink.dto.response.UserSyncResult;
import com.agrolink.mappers.UserMapper;
import com.agrolink.model.UserModel;
import com.agrolink.model.enums.UserRole;
import com.agrolink.model.enums.UserStatus;
import com.agrolink.repositories.IUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock
  private IUserRepository userRepository;

  @Mock
  private KeycloakService keycloakService;

  @Spy
  private UserMapper userMapper = Mappers.getMapper(UserMapper.class);

  @Mock
  private UserProfileService userProfileService;

  @InjectMocks
  private UserService userService;

  @Test
  void findActiveUserByKeycloakId_returnsTheUser_whenAccepted() {
    UUID keycloakId = UUID.randomUUID();
    UserModel user = user(1, UserStatus.ACCEPTED);
    when(userRepository.findByKeycloakId(keycloakId)).thenReturn(Optional.of(user));

    assertThat(userService.findActiveUserByKeycloakId(keycloakId)).contains(user);
  }

  @Test
  void findActiveUserByKeycloakId_returnsEmpty_whenNotAccepted() {
    UUID keycloakId = UUID.randomUUID();
    when(userRepository.findByKeycloakId(keycloakId)).thenReturn(Optional.of(user(1, UserStatus.PENDING)));

    assertThat(userService.findActiveUserByKeycloakId(keycloakId)).isEmpty();
  }

  @Test
  void findActiveUserByKeycloakId_returnsEmpty_whenUserDoesNotExist() {
    UUID keycloakId = UUID.randomUUID();
    when(userRepository.findByKeycloakId(keycloakId)).thenReturn(Optional.empty());

    assertThat(userService.findActiveUserByKeycloakId(keycloakId)).isEmpty();
  }

  @Test
  void getReference_delegatesToTheRepository() {
    UserModel reference = user(7, UserStatus.ACCEPTED);
    when(userRepository.getReferenceById(7)).thenReturn(reference);

    assertThat(userService.getReference(7)).isEqualTo(reference);
  }

  @Test
  void list_returnsEveryUserMapped() {
    when(userRepository.findAll()).thenReturn(List.of(user(1, UserStatus.ACCEPTED), user(2, UserStatus.PENDING)));

    assertThat(userService.list()).extracting(UserResponse::id).containsExactly(1, 2);
  }

  @Test
  void syncFromKeycloak_createsAUserForANewEligibleKeycloakUser() {
    KeycloakUserDto kcUser = kcUser(UUID.randomUUID(), "nueva@agrolink.cl", "Nueva", "Retailer", UserRole.RETAILER, true);
    when(keycloakService.fetchAllUsers()).thenReturn(List.of(kcUser));
    when(userRepository.findByKeycloakId(kcUser.id())).thenReturn(Optional.empty());
    when(userRepository.findAll()).thenReturn(List.of());
    when(userProfileService.ensureProfilesFor(List.of())).thenReturn(0);

    UserSyncResult result = userService.syncFromKeycloak();

    assertThat(result.totalFromKeycloak()).isEqualTo(1);
    assertThat(result.created()).isEqualTo(1);
    assertThat(result.updated()).isZero();
    assertThat(result.skipped()).isZero();
    verify(userRepository).save(any());
  }

  @Test
  void syncFromKeycloak_updatesAnExistingUser() {
    KeycloakUserDto kcUser = kcUser(UUID.randomUUID(), "existente@agrolink.cl", "Ana", "Perez", UserRole.SUPPLIER, true);
    UserModel existing = user(5, UserStatus.ACCEPTED);
    when(keycloakService.fetchAllUsers()).thenReturn(List.of(kcUser));
    when(userRepository.findByKeycloakId(kcUser.id())).thenReturn(Optional.of(existing));
    when(userRepository.findAll()).thenReturn(List.of(existing));
    when(userProfileService.ensureProfilesFor(List.of(5))).thenReturn(0);

    UserSyncResult result = userService.syncFromKeycloak();

    assertThat(result.created()).isZero();
    assertThat(result.updated()).isEqualTo(1);
    verify(userRepository).save(existing);
  }

  @Test
  void syncFromKeycloak_skipsUsersWithoutARole_withoutTouchingTheRepository() {
    KeycloakUserDto roleless = kcUser(UUID.randomUUID(), "sinrol@agrolink.cl", "Sin", "Rol", null, true);
    when(keycloakService.fetchAllUsers()).thenReturn(List.of(roleless));
    when(userRepository.findAll()).thenReturn(List.of());
    when(userProfileService.ensureProfilesFor(List.of())).thenReturn(0);

    UserSyncResult result = userService.syncFromKeycloak();

    assertThat(result.skipped()).isEqualTo(1);
    assertThat(result.created()).isZero();
    assertThat(result.updated()).isZero();
    verify(userRepository, never()).findByKeycloakId(any());
    verify(userRepository, never()).save(any());
  }

  @Test
  void syncFromKeycloak_skipsAdmins() {
    KeycloakUserDto admin = kcUser(UUID.randomUUID(), "admin@agrolink.cl", "Ad", "Min", UserRole.ADMIN, true);
    when(keycloakService.fetchAllUsers()).thenReturn(List.of(admin));
    when(userRepository.findAll()).thenReturn(List.of());
    when(userProfileService.ensureProfilesFor(List.of())).thenReturn(0);

    UserSyncResult result = userService.syncFromKeycloak();

    assertThat(result.skipped()).isEqualTo(1);
    verify(userRepository, never()).findByKeycloakId(any());
  }

  @Test
  void syncFromKeycloak_skipsUsersWithoutAnEmail() {
    KeycloakUserDto noEmail = kcUser(UUID.randomUUID(), null, "Sin", "Correo", UserRole.RETAILER, true);
    KeycloakUserDto blankEmail = kcUser(UUID.randomUUID(), "   ", "Sin", "Correo", UserRole.RETAILER, true);
    when(keycloakService.fetchAllUsers()).thenReturn(List.of(noEmail, blankEmail));
    when(userRepository.findAll()).thenReturn(List.of());
    when(userProfileService.ensureProfilesFor(List.of())).thenReturn(0);

    UserSyncResult result = userService.syncFromKeycloak();

    assertThat(result.skipped()).isEqualTo(2);
    verify(userRepository, never()).findByKeycloakId(any());
  }

  @Test
  void syncFromKeycloak_countsSkippedAsTheDifferenceBetweenTotalAndEligible() {
    KeycloakUserDto eligible = kcUser(UUID.randomUUID(), "retailer@agrolink.cl", "Re", "Tailer", UserRole.RETAILER, true);
    KeycloakUserDto admin = kcUser(UUID.randomUUID(), "admin@agrolink.cl", "Ad", "Min", UserRole.ADMIN, true);
    KeycloakUserDto noEmail = kcUser(UUID.randomUUID(), null, "Sin", "Correo", UserRole.SUPPLIER, true);
    when(keycloakService.fetchAllUsers()).thenReturn(List.of(eligible, admin, noEmail));
    when(userRepository.findByKeycloakId(eligible.id())).thenReturn(Optional.empty());
    when(userRepository.findAll()).thenReturn(List.of());
    when(userProfileService.ensureProfilesFor(List.of())).thenReturn(0);

    UserSyncResult result = userService.syncFromKeycloak();

    assertThat(result.totalFromKeycloak()).isEqualTo(3);
    assertThat(result.created()).isEqualTo(1);
    assertThat(result.skipped()).isEqualTo(2);
    verify(userRepository, times(1)).findByKeycloakId(any());
  }

  @Test
  void syncFromKeycloak_ensuresProfilesForEveryPersistedUser() {
    when(keycloakService.fetchAllUsers()).thenReturn(List.of());
    when(userRepository.findAll()).thenReturn(List.of(user(1, UserStatus.ACCEPTED), user(2, UserStatus.ACCEPTED)));
    when(userProfileService.ensureProfilesFor(List.of(1, 2))).thenReturn(2);

    UserSyncResult result = userService.syncFromKeycloak();

    assertThat(result.userProfilesCreated()).isEqualTo(2);
    verify(userProfileService).ensureProfilesFor(List.of(1, 2));
  }

  private static UserModel user(Integer id, UserStatus status) {
    UserModel model = new UserModel();
    model.setId(id);
    model.setStatus(status);
    return model;
  }

  private static KeycloakUserDto kcUser(UUID id, String email, String firstName, String lastName, UserRole role, boolean enabled) {
    return new KeycloakUserDto(id, email, firstName, lastName, enabled, role);
  }
}

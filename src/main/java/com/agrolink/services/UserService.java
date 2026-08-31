package com.agrolink.services;

import com.agrolink.dto.KeycloakUserDto;
import com.agrolink.dto.UserResponse;
import com.agrolink.dto.UserSyncResult;
import com.agrolink.mappers.UserMapper;
import com.agrolink.model.UserModel;
import com.agrolink.model.enums.UserRole;
import com.agrolink.model.enums.UserStatus;
import com.agrolink.repositories.IUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

  private final IUserRepository userRepository;
  private final KeycloakService keycloakService;
  private final UserMapper userMapper;
  private final UserProfileService userProfileService;

  public Optional<UserModel> findActiveUserByKeycloakId(UUID keycloakId) {
    return userRepository.findByKeycloakId(keycloakId)
        .filter(user -> user.getStatus() == UserStatus.ACCEPTED);
  }

  /**
   * Lazy {@code platform_user} reference for setting a FK association without a SELECT.
   * The id comes from {@link com.agrolink.security.LoggedUser}, so the row is known to exist.
   */
  public UserModel getReference(Integer id) {
    return userRepository.getReferenceById(id);
  }

  public List<UserResponse> list() {
    return userMapper.toResponseList(userRepository.findAll());
  }

  /**
   * Full provisioning from Keycloak (stands in for the admin's user-creation flow, out of scope):
   * <ol>
   *   <li>Upsert every non-admin Keycloak user into {@code platform_user}. Admins and role-less
   *       users are skipped — {@code platform_user} only holds marketplace actors.</li>
   *   <li>Mirror {@code role} (from the Keycloak realm roles) and {@code status} (enabled ->
   *       ACCEPTED, disabled -> DECLINED) on every run.</li>
   *   <li>Create a default {@code user_profile} for every {@code platform_user} that lacks one.</li>
   * </ol>
   * Users without an email are skipped (the column is NOT NULL).
   */
  @Transactional
  public UserSyncResult syncFromKeycloak() {
    List<KeycloakUserDto> keycloakUsers = keycloakService.fetchAllUsers();

    int created = 0;
    int updated = 0;
    int skipped = 0;

    for (KeycloakUserDto kcUser : keycloakUsers) {
      if (kcUser.email() == null || kcUser.email().isBlank()) {
        log.warn("Skipping Keycloak user {} without email", kcUser.id());
        skipped++;
        continue;
      }

      if (kcUser.role() == null || kcUser.role() == UserRole.ADMIN) {
        // admins operate the platform, they are not marketplace actors; role-less users can't act
        skipped++;
        continue;
      }

      Optional<UserModel> existing = userRepository.findByKeycloakId(kcUser.id());
      UserModel user = existing.orElseGet(UserModel::new);

      userMapper.updateEntityFromKeycloak(kcUser, user); // maps role + status too
      userRepository.save(user);

      if (existing.isPresent()) {
        updated++;
      } else {
        created++;
      }
    }

    List<Integer> allUserIds = userRepository.findAll().stream().map(UserModel::getId).toList();
    int userProfilesCreated = userProfileService.ensureProfilesFor(allUserIds);

    UserSyncResult result =
        new UserSyncResult(keycloakUsers.size(), created, updated, skipped, userProfilesCreated);
    log.info("Keycloak user sync finished: {}", result);
    return result;
  }

}

package com.agrolink.services;

import com.agrolink.dto.KeycloakUserDto;
import com.agrolink.dto.response.UserResponse;
import com.agrolink.dto.response.UserSyncResult;
import com.agrolink.mappers.UserMapper;
import com.agrolink.model.UserModel;
import com.agrolink.model.enums.UserRole;
import com.agrolink.model.enums.UserStatus;
import com.agrolink.repositories.IUserRepository;
import lombok.NonNull;
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

  @NonNull
  private final IUserRepository userRepository;

  @NonNull
  private final KeycloakService keycloakService;

  @NonNull
  private final UserMapper userMapper;

  @NonNull
  private final UserProfileService userProfileService;

  public Optional<UserModel> findActiveUserByKeycloakId(UUID keycloakId) {
    return userRepository.findByKeycloakId(keycloakId)
        .filter(user -> user.getStatus() == UserStatus.ACCEPTED);
  }

  public UserModel getReference(Integer id) {
    return userRepository.getReferenceById(id);
  }

  public List<UserResponse> list() {
    return userMapper.toResponseList(userRepository.findAll());
  }

  @Transactional
  public UserSyncResult syncFromKeycloak() {
    List<KeycloakUserDto> keycloakUsers = keycloakService.fetchAllUsers();

    int created = 0;
    int updated = 0;

    var elegibleKcUserList = keycloakUsers.stream().filter(this::isEligibleForSync).toList();
    int skipped = keycloakUsers.size() - elegibleKcUserList.size();

    for (var kcUser : elegibleKcUserList) {
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
    var result = new UserSyncResult(keycloakUsers.size(), created, updated, skipped, userProfilesCreated);
    log.info("Keycloak user sync finished: {}", result);
    return result;
  }

  private boolean isEligibleForSync(KeycloakUserDto kcUser) {
    if (kcUser.email() == null || kcUser.email().isBlank()) {
      log.warn("Skipping Keycloak user {} without email", kcUser.id());
      return false;
    }

    // admins operate the platform, they are not marketplace actors; role-less users can't act
    return kcUser.role() != null && kcUser.role() != UserRole.ADMIN;
  }

}

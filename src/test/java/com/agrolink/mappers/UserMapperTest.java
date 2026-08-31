package com.agrolink.mappers;

import com.agrolink.dto.KeycloakUserDto;
import com.agrolink.dto.response.UserResponse;
import com.agrolink.model.UserModel;
import com.agrolink.model.enums.UserRole;
import com.agrolink.model.enums.UserStatus;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

  private final UserMapper userMapper = Mappers.getMapper(UserMapper.class);

  @Test
  void toResponse_mapsEveryField() {
    UUID keycloakId = UUID.randomUUID();
    LocalDateTime createdAt = LocalDateTime.of(2026, Month.JANUARY, 1, 10, 0);
    LocalDateTime updatedAt = LocalDateTime.of(2026, Month.FEBRUARY, 1, 10, 0);
    UserModel user = new UserModel();
    user.setId(1);
    user.setKeycloakId(keycloakId);
    user.setEmail("ana@agrolink.cl");
    user.setName("Ana Perez");
    user.setRole(UserRole.RETAILER);
    user.setStatus(UserStatus.ACCEPTED);
    user.setCreatedAt(createdAt);
    user.setUpdatedAt(updatedAt);

    UserResponse response = userMapper.toResponse(user);

    assertThat(response.id()).isEqualTo(1);
    assertThat(response.keycloakId()).isEqualTo(keycloakId);
    assertThat(response.email()).isEqualTo("ana@agrolink.cl");
    assertThat(response.name()).isEqualTo("Ana Perez");
    assertThat(response.role()).isEqualTo(UserRole.RETAILER);
    assertThat(response.status()).isEqualTo(UserStatus.ACCEPTED);
    assertThat(response.createdAt()).isEqualTo(createdAt);
    assertThat(response.updatedAt()).isEqualTo(updatedAt);
  }

  @Test
  void toResponseList_mapsEveryUser() {
    UserModel first = new UserModel();
    first.setId(1);
    UserModel second = new UserModel();
    second.setId(2);

    assertThat(userMapper.toResponseList(List.of(first, second)))
        .extracting(UserResponse::id)
        .containsExactly(1, 2);
  }

  @Test
  void updateEntityFromKeycloak_copiesKeycloakIdEmailAndRole() {
    UUID keycloakId = UUID.randomUUID();
    KeycloakUserDto dto = new KeycloakUserDto(keycloakId, "ana@agrolink.cl", "Ana", "Perez", true, UserRole.RETAILER);
    UserModel entity = new UserModel();

    userMapper.updateEntityFromKeycloak(dto, entity);

    assertThat(entity.getKeycloakId()).isEqualTo(keycloakId);
    assertThat(entity.getEmail()).isEqualTo("ana@agrolink.cl");
    assertThat(entity.getRole()).isEqualTo(UserRole.RETAILER);
  }

  @Test
  void updateEntityFromKeycloak_doesNotTouchIdOrTimestamps() {
    LocalDateTime createdAt = LocalDateTime.of(2026, Month.JANUARY, 1, 10, 0);
    UserModel entity = new UserModel();
    entity.setId(99);
    entity.setCreatedAt(createdAt);
    entity.setUpdatedAt(createdAt);
    KeycloakUserDto dto = new KeycloakUserDto(UUID.randomUUID(), "ana@agrolink.cl", "Ana", "Perez", true, UserRole.RETAILER);

    userMapper.updateEntityFromKeycloak(dto, entity);

    assertThat(entity.getId()).isEqualTo(99);
    assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
    assertThat(entity.getUpdatedAt()).isEqualTo(createdAt);
  }

  @Test
  void updateEntityFromKeycloak_setsStatusAccepted_whenEnabled() {
    UserModel entity = new UserModel();
    userMapper.updateEntityFromKeycloak(dto("Ana", "Perez", "ana@agrolink.cl", true), entity);

    assertThat(entity.getStatus()).isEqualTo(UserStatus.ACCEPTED);
  }

  @Test
  void updateEntityFromKeycloak_setsStatusDeclined_whenDisabled() {
    UserModel entity = new UserModel();
    userMapper.updateEntityFromKeycloak(dto("Ana", "Perez", "ana@agrolink.cl", false), entity);

    assertThat(entity.getStatus()).isEqualTo(UserStatus.DECLINED);
  }

  @Test
  void updateEntityFromKeycloak_buildsNameFromFirstAndLastName() {
    UserModel entity = new UserModel();
    userMapper.updateEntityFromKeycloak(dto("  Ana  ", "  Perez  ", "ana@agrolink.cl", true), entity);

    assertThat(entity.getName()).isEqualTo("Ana Perez");
  }

  @Test
  void updateEntityFromKeycloak_fallsBackToEmailLocalPart_whenNamesAreMissing() {
    UserModel entity = new UserModel();
    userMapper.updateEntityFromKeycloak(dto(null, null, "beto@agrolink.cl", true), entity);

    assertThat(entity.getName()).isEqualTo("beto");
  }

  @Test
  void updateEntityFromKeycloak_fallsBackToEmailLocalPart_whenNamesAreBlank() {
    UserModel entity = new UserModel();
    userMapper.updateEntityFromKeycloak(dto("   ", "   ", "beto@agrolink.cl", true), entity);

    assertThat(entity.getName()).isEqualTo("beto");
  }

  @Test
  void updateEntityFromKeycloak_usesOnlyTheFirstName_whenLastNameIsMissing() {
    UserModel entity = new UserModel();
    userMapper.updateEntityFromKeycloak(dto("Ana", null, "ana@agrolink.cl", true), entity);

    assertThat(entity.getName()).isEqualTo("Ana");
  }

  @Test
  void updateEntityFromKeycloak_returnsTheFullEmail_whenItHasNoAtSign() {
    UserModel entity = new UserModel();
    userMapper.updateEntityFromKeycloak(dto(null, null, "sin-arroba", true), entity);

    assertThat(entity.getName()).isEqualTo("sin-arroba");
  }

  private static KeycloakUserDto dto(String firstName, String lastName, String email, boolean enabled) {
    return new KeycloakUserDto(UUID.randomUUID(), email, firstName, lastName, enabled, UserRole.RETAILER);
  }
}

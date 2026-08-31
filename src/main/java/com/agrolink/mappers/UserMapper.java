package com.agrolink.mappers;

import com.agrolink.dto.KeycloakUserDto;
import com.agrolink.dto.UserResponse;
import com.agrolink.model.UserModel;
import com.agrolink.model.enums.UserStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", imports = UserStatus.class, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

  UserResponse toResponse(UserModel user);

  List<UserResponse> toResponseList(List<UserModel> users);

  /**
   * Applies the mutable fields coming from Keycloak onto an existing (or brand new) entity.
   * <p>
   * Generated/managed columns (id, createdAt, updatedAt) are left untouched. {@code status} is mirrored from Keycloak's {@code enabled} flag.
   */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "keycloakId", source = "id")
  @Mapping(target = "name", expression = "java(resolveName(dto))")
  @Mapping(target = "status", expression = "java(dto.enabled() ? UserStatus.ACCEPTED : UserStatus.DECLINED)")
  void updateEntityFromKeycloak(KeycloakUserDto dto, @MappingTarget UserModel entity);

  /**
   * Full name from Keycloak's first/last name; falls back to the email local-part because {@code platform_user.name} is NOT NULL.
   */
  default String resolveName(KeycloakUserDto dto) {
    String first = dto.firstName() == null ? "" : dto.firstName().trim();
    String last = dto.lastName() == null ? "" : dto.lastName().trim();
    String fullName = (first + " " + last).trim();

    if (!fullName.isEmpty()) {
      return fullName;
    }
    String email = dto.email();
    int at = email.indexOf('@');
    return at > 0 ? email.substring(0, at) : email;
  }

}

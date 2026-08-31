package com.agrolink.mappers;

import com.agrolink.dto.KeycloakUserDto;
import com.agrolink.dto.response.UserResponse;
import com.agrolink.model.UserModel;
import com.agrolink.model.enums.UserStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(imports = UserStatus.class, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

  UserResponse toResponse(UserModel user);

  List<UserResponse> toResponseList(List<UserModel> users);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "keycloakId", source = "id")
  @Mapping(target = "name", expression = "java(resolveName(dto))")
  @Mapping(target = "status", expression = "java(dto.enabled() ? UserStatus.ACCEPTED : UserStatus.DECLINED)")
  void updateEntityFromKeycloak(KeycloakUserDto dto, @MappingTarget UserModel entity);

  default String resolveName(KeycloakUserDto dto) {
    var first = dto.firstName() == null ? "" : dto.firstName().trim();
    var last = dto.lastName() == null ? "" : dto.lastName().trim();
    var fullName = (first + " " + last).trim();

    if (!fullName.isEmpty()) {
      return fullName;
    }
    String email = dto.email();
    int at = email.indexOf('@');
    return at > 0 ? email.substring(0, at) : email;
  }

}

package com.agrolink.repositories;

import com.agrolink.model.UserModel;
import com.agrolink.model.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IUserRepository extends JpaRepository<UserModel, Integer> {

  Optional<UserModel> findByKeycloakId(UUID keycloakId);

  /** {@code email} isn't unique in the schema; {@code findFirst} avoids a size exception on dupes. */
  Optional<UserModel> findFirstByEmailIgnoreCase(String email);

  List<UserModel> findAllByRole(UserRole role);

}

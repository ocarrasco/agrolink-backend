package com.agrolink.repositories;

import com.agrolink.model.UserProfileModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface IUserProfileRepository extends JpaRepository<UserProfileModel, Integer> {

  Optional<UserProfileModel> findByUserId(Integer userId);

  List<UserProfileModel> findByUserIdIn(Collection<Integer> userIds);

}

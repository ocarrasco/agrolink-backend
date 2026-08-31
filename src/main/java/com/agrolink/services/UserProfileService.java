package com.agrolink.services;

import com.agrolink.dto.UpdateUserProfileRequest;
import com.agrolink.dto.UserProfileResponse;
import com.agrolink.mappers.UserProfileMapper;
import com.agrolink.model.UserProfileModel;
import com.agrolink.model.WeeklyAvailability;
import com.agrolink.repositories.IUserProfileRepository;
import com.agrolink.security.LoggedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserProfileService {

  private final IUserProfileRepository userProfileRepository;
  private final UserProfileMapper userProfileMapper;

  public UserProfileResponse getMine(LoggedUser user) {
    return userProfileRepository.findByUserId(user.id())
        .map(userProfileMapper::toResponse)
        .orElseGet(() -> new UserProfileResponse(false, null, null, null, WeeklyAvailability.empty()));
  }

  @Transactional
  public UserProfileResponse upsert(LoggedUser user, UpdateUserProfileRequest request) {
    UserProfileModel profile = userProfileRepository.findByUserId(user.id())
        .orElseGet(() -> {
          UserProfileModel fresh = new UserProfileModel();
          fresh.setUserId(user.id());
          return fresh;
        });

    profile.setDelivery(request.delivery());
    profile.setAddress(blankToNull(request.address()));
    profile.setPhone(blankToNull(request.phone()));
    profile.setContactName(blankToNull(request.contactName()));
    profile.setAvailability(request.availability() == null
        ? WeeklyAvailability.empty()
        : request.availability().normalized());

    return userProfileMapper.toResponse(userProfileRepository.saveAndFlush(profile));
  }

  /**
   * Creates a default {@code user_profile} row for each of the given users that lacks one.
   * Called by {@link UserService#syncFromKeycloak()} so every non-admin user is provisioned
   * with a profile (stands in for the admin's user-provisioning flow). Idempotent.
   *
   * @return how many rows were created
   */
  @Transactional
  public int ensureProfilesFor(Collection<Integer> userIds) {
    if (userIds == null || userIds.isEmpty()) {
      return 0;
    }

    Set<Integer> existing = userProfileRepository.findByUserIdIn(userIds).stream()
        .map(UserProfileModel::getUserId)
        .collect(Collectors.toSet());

    List<UserProfileModel> toCreate = userIds.stream()
        .distinct()
        .filter(id -> !existing.contains(id))
        .map(id -> {
          UserProfileModel profile = new UserProfileModel();
          profile.setUserId(id);
          return profile;
        })
        .toList();

    userProfileRepository.saveAll(toCreate);
    return toCreate.size();
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

}

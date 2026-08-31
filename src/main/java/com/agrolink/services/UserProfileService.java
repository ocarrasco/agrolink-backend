package com.agrolink.services;

import com.agrolink.dto.request.UpdateUserProfileRequest;
import com.agrolink.dto.response.UserProfileResponse;
import com.agrolink.mappers.UserProfileMapper;
import com.agrolink.model.UserProfileModel;
import com.agrolink.model.WeeklyAvailability;
import com.agrolink.repositories.IUserProfileRepository;
import com.agrolink.security.LoggedUser;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.agrolink.utils.StrUtils.blankToNull;

@Service
@RequiredArgsConstructor
public class UserProfileService {

  @NonNull
  private final IUserProfileRepository userProfileRepository;

  @NonNull
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

}

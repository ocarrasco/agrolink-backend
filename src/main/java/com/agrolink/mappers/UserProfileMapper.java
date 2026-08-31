package com.agrolink.mappers;

import com.agrolink.dto.response.UserProfileResponse;
import com.agrolink.model.UserProfileModel;
import com.agrolink.model.WeeklyAvailability;
import org.mapstruct.Mapper;

@Mapper
public interface UserProfileMapper {

  default UserProfileResponse toResponse(UserProfileModel profile) {
    var availability = profile.getAvailability() == null
        ? WeeklyAvailability.empty()
        : profile.getAvailability().normalized();
    return new UserProfileResponse(profile.isDelivery(), profile.getAddress(), profile.getPhone(),
        profile.getContactName(), availability);
  }

}

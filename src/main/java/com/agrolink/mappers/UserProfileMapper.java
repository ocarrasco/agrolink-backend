package com.agrolink.mappers;

import com.agrolink.dto.UserProfileResponse;
import com.agrolink.model.UserProfileModel;
import com.agrolink.model.WeeklyAvailability;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserProfileMapper {

  default UserProfileResponse toResponse(UserProfileModel profile) {
    WeeklyAvailability availability = profile.getAvailability() == null
        ? WeeklyAvailability.empty()
        : profile.getAvailability().normalized();
    return new UserProfileResponse(profile.isDelivery(), profile.getAddress(), profile.getPhone(),
        profile.getContactName(), availability);
  }

}

package com.agrolink.controllers;

import com.agrolink.dto.request.UpdateUserProfileRequest;
import com.agrolink.dto.response.UserProfileResponse;
import com.agrolink.services.UserProfileService;
import jakarta.validation.Valid;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPPLIER', 'RETAILER', 'CARRIER')")
public class ProfileController extends BaseController {

  @NonNull
  private final UserProfileService userProfileService;

  @GetMapping
  public UserProfileResponse getMine() {
    var user = loggedUser();
    log.info("Fetching profile for user {}", user.id());
    return userProfileService.getMine(user);
  }

  @PutMapping
  public UserProfileResponse update(@Valid @RequestBody UpdateUserProfileRequest request) {
    var user = loggedUser();
    log.info("Updating profile for user {} (delivery={})", user.id(), request.delivery());
    return userProfileService.upsert(user, request);
  }

}

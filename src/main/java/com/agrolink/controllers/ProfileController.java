package com.agrolink.controllers;

import com.agrolink.dto.UpdateUserProfileRequest;
import com.agrolink.dto.UserProfileResponse;
import com.agrolink.services.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Delivery preferences of the authenticated user (any non-admin role). */
@Slf4j
@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPPLIER', 'RETAILER', 'CARRIER')")
public class ProfileController extends BaseController {

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

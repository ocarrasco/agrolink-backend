package com.agrolink.controllers;

import com.agrolink.dto.UserResponse;
import com.agrolink.dto.UserSyncResult;
import com.agrolink.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public List<UserResponse> list() {
    log.info("Listing users");
    return userService.list();
  }

  @PostMapping("/sync")
  @PreAuthorize("hasRole('ADMIN')")
  public UserSyncResult sync() {
    log.info("Syncing users from Keycloak");
    return userService.syncFromKeycloak();
  }

}

package com.agrolink.controllers;

import com.agrolink.dto.response.UserResponse;
import com.agrolink.dto.response.UserSyncResult;
import com.agrolink.model.enums.UserRole;
import com.agrolink.model.enums.UserStatus;
import com.agrolink.security.LoggedUserJwtAuthenticationConverter;
import com.agrolink.security.SecurityConfig;
import com.agrolink.services.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(SecurityConfig.class)
@WebMvcTest(UserController.class)
class UserControllerTest extends ControllerTestSupport {

  private static final UserResponse USER = new UserResponse(
      1, UUID.randomUUID(), "retailer@agrolink.com", "Retailer Uno",
      UserRole.RETAILER, UserStatus.ACCEPTED, LocalDateTime.now(), LocalDateTime.now());

  @MockBean
  private UserService userService;

  @MockBean
  private LoggedUserJwtAuthenticationConverter loggedUserJwtAuthenticationConverter;

  @MockBean
  private JwtDecoder jwtDecoder;

  @Autowired
  private MockMvc mockMvc;

  @Test
  void shouldListForAdmin() throws Exception {
    when(userService.list()).thenReturn(List.of(USER));

    mockMvc.perform(get("/users").with(loggedAs(UserRole.ADMIN)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].email").value("retailer@agrolink.com"));

    verify(userService).list();
  }

  @Test
  void shouldRejectListForNonAdmin() throws Exception {
    mockMvc.perform(get("/users").with(loggedAs(UserRole.RETAILER)))
        .andExpect(status().isForbidden());

    verifyNoInteractions(userService);
  }

  @Test
  void shouldRejectListWhenAnonymous() throws Exception {
    mockMvc.perform(get("/users"))
        .andExpect(status().isUnauthorized());

    verifyNoInteractions(userService);
  }

  @Test
  void shouldSyncAsAdmin() throws Exception {
    when(userService.syncFromKeycloak()).thenReturn(new UserSyncResult(10, 2, 1, 7, 2));

    mockMvc.perform(post("/users/sync").with(loggedAs(UserRole.ADMIN)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalFromKeycloak").value(10))
        .andExpect(jsonPath("$.created").value(2));

    verify(userService).syncFromKeycloak();
  }

  @Test
  void shouldRejectSyncForNonAdmin() throws Exception {
    mockMvc.perform(post("/users/sync").with(loggedAs(UserRole.SUPPLIER)))
        .andExpect(status().isForbidden());

    verifyNoInteractions(userService);
  }

}

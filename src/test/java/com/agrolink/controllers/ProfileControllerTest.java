package com.agrolink.controllers;

import com.agrolink.dto.response.UserProfileResponse;
import com.agrolink.model.WeeklyAvailability;
import com.agrolink.model.enums.UserRole;
import com.agrolink.security.LoggedUserJwtAuthenticationConverter;
import com.agrolink.security.SecurityConfig;
import com.agrolink.services.UserProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(SecurityConfig.class)
@WebMvcTest(ProfileController.class)
class ProfileControllerTest extends ControllerTestSupport {

  private static final UserProfileResponse PROFILE = new UserProfileResponse(
      true, "Camino Real 456", "+56933333333", "Carla", WeeklyAvailability.empty());

  @MockBean
  private UserProfileService userProfileService;

  // SecurityConfig constructor dependency; its UserService dep is outside the web slice
  @MockBean
  private LoggedUserJwtAuthenticationConverter loggedUserJwtAuthenticationConverter;

  // required to build SecurityConfig's .oauth2ResourceServer(...jwt...) without hitting Keycloak
  @MockBean
  private JwtDecoder jwtDecoder;

  @Autowired
  private MockMvc mockMvc;

  @Test
  void shouldGetOwnProfileForSupplier() throws Exception {
    when(userProfileService.getMine(any())).thenReturn(PROFILE);

    mockMvc.perform(get("/profile").with(loggedAs(UserRole.SUPPLIER)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.address").value("Camino Real 456"))
        .andExpect(jsonPath("$.delivery").value(true));

    verify(userProfileService).getMine(any());
  }

  @Test
  void shouldGetOwnProfileForRetailer() throws Exception {
    when(userProfileService.getMine(any())).thenReturn(PROFILE);

    mockMvc.perform(get("/profile").with(loggedAs(UserRole.RETAILER)))
        .andExpect(status().isOk());
  }

  @Test
  void shouldGetOwnProfileForCarrier() throws Exception {
    when(userProfileService.getMine(any())).thenReturn(PROFILE);

    mockMvc.perform(get("/profile").with(loggedAs(UserRole.CARRIER)))
        .andExpect(status().isOk());
  }

  @Test
  void shouldRejectGetForAdmin() throws Exception {
    mockMvc.perform(get("/profile").with(loggedAs(UserRole.ADMIN)))
        .andExpect(status().isForbidden());

    verifyNoInteractions(userProfileService);
  }

  @Test
  void shouldRejectGetWhenAnonymous() throws Exception {
    mockMvc.perform(get("/profile"))
        .andExpect(status().isUnauthorized());

    verifyNoInteractions(userProfileService);
  }

  @Test
  void shouldUpdateOwnProfile() throws Exception {
    when(userProfileService.upsert(any(), any())).thenReturn(PROFILE);

    mockMvc.perform(put("/profile")
            .with(loggedAs(UserRole.SUPPLIER))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"delivery":true,"address":"Camino Real 456","phone":"+56933333333","contactName":"Carla","availability":null}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.contactName").value("Carla"));

    verify(userProfileService).upsert(any(), any());
  }

  @Test
  void shouldRejectUpdateForAdmin() throws Exception {
    mockMvc.perform(put("/profile")
            .with(loggedAs(UserRole.ADMIN))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"delivery":false,"address":null,"phone":null,"contactName":null,"availability":null}
                """))
        .andExpect(status().isForbidden());

    verifyNoInteractions(userProfileService);
  }

  @Test
  void shouldRejectUpdateWhenAddressTooLong() throws Exception {
    String tooLong = "a".repeat(256);

    mockMvc.perform(put("/profile")
            .with(loggedAs(UserRole.SUPPLIER))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"delivery\":false,\"address\":\"" + tooLong + "\",\"phone\":null,\"contactName\":null,\"availability\":null}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.address").exists());

    verifyNoInteractions(userProfileService);
  }

}

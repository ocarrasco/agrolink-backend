package com.agrolink.controllers;

import com.agrolink.dto.response.MasterProductResponse;
import com.agrolink.model.enums.ProductUnit;
import com.agrolink.model.enums.UserRole;
import com.agrolink.security.LoggedUserJwtAuthenticationConverter;
import com.agrolink.security.SecurityConfig;
import com.agrolink.services.MasterProductService;
import com.agrolink.validations.CreateMasterProductRequestValidator;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(SecurityConfig.class)
@WebMvcTest(MasterProductController.class)
class MasterProductControllerTest extends ControllerTestSupport {

  private static final MasterProductResponse PAPA = new MasterProductResponse(
      1, "Papa", ProductUnit.KILOGRAMO, true, LocalDateTime.now(), LocalDateTime.now());

  @MockBean
  private MasterProductService masterProductService;

  @MockBean
  private CreateMasterProductRequestValidator createMasterProductRequestValidator;

  @MockBean
  private LoggedUserJwtAuthenticationConverter loggedUserJwtAuthenticationConverter;

  @MockBean
  private JwtDecoder jwtDecoder;

  @Autowired
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    when(createMasterProductRequestValidator.supports(any())).thenReturn(true);
  }

  @Test
  void shouldListForAdmin() throws Exception {
    when(masterProductService.list(false)).thenReturn(List.of(PAPA));

    mockMvc.perform(get("/master-products").with(loggedAs(UserRole.ADMIN)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Papa"));

    verify(masterProductService).list(false);
  }

  @Test
  void shouldHonorIncludeInactive() throws Exception {
    mockMvc.perform(get("/master-products").param("includeInactive", "true").with(loggedAs(UserRole.ADMIN)))
        .andExpect(status().isOk());

    verify(masterProductService).list(true);
  }

  @Test
  void shouldRejectListForNonAdmin() throws Exception {
    mockMvc.perform(get("/master-products").with(loggedAs(UserRole.SUPPLIER)))
        .andExpect(status().isForbidden());

    verifyNoInteractions(masterProductService);
  }

  @Test
  void shouldGetById() throws Exception {
    when(masterProductService.getById(1)).thenReturn(PAPA);

    mockMvc.perform(get("/master-products/{id}", 1).with(loggedAs(UserRole.ADMIN)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1));
  }

  @Test
  void shouldReturn404WhenNotFound() throws Exception {
    when(masterProductService.getById(99)).thenThrow(new EntityNotFoundException("no existe"));

    mockMvc.perform(get("/master-products/{id}", 99).with(loggedAs(UserRole.ADMIN)))
        .andExpect(status().isNotFound());
  }

  @Test
  void shouldCreateAsAdmin() throws Exception {
    when(masterProductService.create(any())).thenReturn(PAPA);

    mockMvc.perform(post("/master-products")
            .with(loggedAs(UserRole.ADMIN))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Papa\",\"unit\":\"KILOGRAMO\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1));

    verify(masterProductService).create(any());
  }

  @Test
  void shouldRejectCreateForNonAdmin() throws Exception {
    mockMvc.perform(post("/master-products")
            .with(loggedAs(UserRole.SUPPLIER))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Papa\",\"unit\":\"KILOGRAMO\"}"))
        .andExpect(status().isForbidden());

    verifyNoInteractions(masterProductService);
  }

  @Test
  void shouldReturn400WhenCreateBodyInvalid() throws Exception {
    mockMvc.perform(post("/master-products")
            .with(loggedAs(UserRole.ADMIN))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"  \",\"unit\":\"KILOGRAMO\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.name").exists());
  }

  @Test
  void shouldUpdateAsAdmin() throws Exception {
    when(masterProductService.update(eq(1), any())).thenReturn(PAPA);

    mockMvc.perform(put("/master-products/{id}", 1)
            .with(loggedAs(UserRole.ADMIN))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Papa\",\"unit\":\"KILOGRAMO\",\"active\":true}"))
        .andExpect(status().isOk());

    verify(masterProductService).update(eq(1), any());
  }

  @Test
  void shouldDeactivateAsAdmin() throws Exception {
    mockMvc.perform(delete("/master-products/{id}", 1).with(loggedAs(UserRole.ADMIN)))
        .andExpect(status().isNoContent());

    verify(masterProductService).deactivate(1);
  }

  @Test
  void shouldRejectDeactivateForNonAdmin() throws Exception {
    mockMvc.perform(delete("/master-products/{id}", 1).with(loggedAs(UserRole.SUPPLIER)))
        .andExpect(status().isForbidden());

    verifyNoInteractions(masterProductService);
  }

}

package com.agrolink.controllers;

import com.agrolink.dto.response.SupplierResponse;
import com.agrolink.model.enums.UserRole;
import com.agrolink.security.LoggedUserJwtAuthenticationConverter;
import com.agrolink.security.SecurityConfig;
import com.agrolink.services.CatalogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(SecurityConfig.class)
@WebMvcTest(SupplierCatalogController.class)
class SupplierCatalogControllerTest extends ControllerTestSupport {

  private static final SupplierResponse SUPPLIER = new SupplierResponse(
      1, "Agropecuaria Sur", "contacto@agrosur.com", "099123456", "Juan Perez",
      true, "Ruta 5 km 12", null, List.of());

  @MockBean
  private CatalogService catalogService;

  @MockBean
  private LoggedUserJwtAuthenticationConverter loggedUserJwtAuthenticationConverter;

  @MockBean
  private JwtDecoder jwtDecoder;

  @Autowired
  private MockMvc mockMvc;

  @Test
  void shouldListForRetailer() throws Exception {
    when(catalogService.listSuppliers(isNull(), isNull())).thenReturn(List.of(SUPPLIER));

    mockMvc.perform(get("/supplier-catalog").with(loggedAs(UserRole.RETAILER)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Agropecuaria Sur"));

    verify(catalogService).listSuppliers(null, null);
  }

  @Test
  void shouldListForAdmin() throws Exception {
    when(catalogService.listSuppliers(isNull(), isNull())).thenReturn(List.of(SUPPLIER));

    mockMvc.perform(get("/supplier-catalog").with(loggedAs(UserRole.ADMIN)))
        .andExpect(status().isOk());

    verify(catalogService).listSuppliers(null, null);
  }

  @Test
  void shouldForwardFilters() throws Exception {
    when(catalogService.listSuppliers(3, "papa")).thenReturn(List.of(SUPPLIER));

    mockMvc.perform(get("/supplier-catalog")
            .param("masterProductId", "3")
            .param("q", "papa")
            .with(loggedAs(UserRole.RETAILER)))
        .andExpect(status().isOk());

    verify(catalogService).listSuppliers(3, "papa");
  }

  @Test
  void shouldRejectForSupplier() throws Exception {
    mockMvc.perform(get("/supplier-catalog").with(loggedAs(UserRole.SUPPLIER)))
        .andExpect(status().isForbidden());

    verifyNoInteractions(catalogService);
  }

  @Test
  void shouldRejectForCarrier() throws Exception {
    mockMvc.perform(get("/supplier-catalog").with(loggedAs(UserRole.CARRIER)))
        .andExpect(status().isForbidden());

    verifyNoInteractions(catalogService);
  }

  @Test
  void shouldRejectWhenAnonymous() throws Exception {
    mockMvc.perform(get("/supplier-catalog"))
        .andExpect(status().isUnauthorized());

    verifyNoInteractions(catalogService);
  }

}

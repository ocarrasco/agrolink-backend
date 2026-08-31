package com.agrolink.controllers;

import com.agrolink.dto.response.ProductResponse;
import com.agrolink.model.enums.ProductUnit;
import com.agrolink.model.enums.UserRole;
import com.agrolink.security.LoggedUserJwtAuthenticationConverter;
import com.agrolink.security.SecurityConfig;
import com.agrolink.services.MasterProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(SecurityConfig.class)
@WebMvcTest(ProductController.class)
class ProductControllerTest extends ControllerTestSupport {

  @MockBean
  private MasterProductService masterProductService;

  @MockBean
  private LoggedUserJwtAuthenticationConverter loggedUserJwtAuthenticationConverter;

  @MockBean
  private JwtDecoder jwtDecoder;

  @Autowired
  private MockMvc mockMvc;

  @Test
  void shouldListActiveProductsForAnyAuthenticatedUser() throws Exception {
    when(masterProductService.listActive())
        .thenReturn(List.of(new ProductResponse(1, "Papa", ProductUnit.KILOGRAMO)));

    mockMvc.perform(get("/products").with(loggedAs(UserRole.SUPPLIER)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Papa"))
        .andExpect(jsonPath("$[0].unit").value("KILOGRAMO"))
        .andExpect(jsonPath("$[0].active").doesNotExist());

    verify(masterProductService).listActive();
  }

  @Test
  void shouldRejectWithoutAuthentication() throws Exception {
    mockMvc.perform(get("/products"))
        .andExpect(status().isUnauthorized());

    verifyNoInteractions(masterProductService);
  }

}

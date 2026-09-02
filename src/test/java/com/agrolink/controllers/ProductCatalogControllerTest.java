package com.agrolink.controllers;

import com.agrolink.dto.response.CatalogItemResponse;
import com.agrolink.model.enums.ProductUnit;
import com.agrolink.model.enums.UserRole;
import com.agrolink.security.LoggedUserJwtAuthenticationConverter;
import com.agrolink.security.SecurityConfig;
import com.agrolink.services.CatalogItemService;
import com.agrolink.validations.CreateCatalogItemRequestValidator;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(SecurityConfig.class)
@WebMvcTest(ProductCatalogController.class)
class ProductCatalogControllerTest extends ControllerTestSupport {

  private static final CatalogItemResponse TOMATE = new CatalogItemResponse(
      1, 10, "Tomate", ProductUnit.KILOGRAMO, 1500, 100, true);

  @MockBean
  private CatalogItemService catalogItemService;

  @MockBean
  private CreateCatalogItemRequestValidator createCatalogItemRequestValidator;

  @MockBean
  private LoggedUserJwtAuthenticationConverter loggedUserJwtAuthenticationConverter;

  @MockBean
  private JwtDecoder jwtDecoder;

  @Autowired
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    when(createCatalogItemRequestValidator.supports(any())).thenReturn(true);
  }

  @Test
  void shouldListCatalogItemsForSupplier() throws Exception {
    when(catalogItemService.getCatalogItems(any())).thenReturn(List.of(TOMATE));

    mockMvc.perform(get("/product-catalog").with(loggedAs(UserRole.SUPPLIER)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].masterProductName").value("Tomate"));

    verify(catalogItemService).getCatalogItems(any());
  }

  @Test
  void shouldRejectListForNonSupplier() throws Exception {
    mockMvc.perform(get("/product-catalog").with(loggedAs(UserRole.RETAILER)))
        .andExpect(status().isForbidden());

    verifyNoInteractions(catalogItemService);
  }

  @Test
  void shouldRejectListWhenAnonymous() throws Exception {
    mockMvc.perform(get("/product-catalog"))
        .andExpect(status().isUnauthorized());

    verifyNoInteractions(catalogItemService);
  }

  @Test
  void shouldGetOwnCatalogItemById() throws Exception {
    when(catalogItemService.getMine(any(), eq(1))).thenReturn(TOMATE);

    mockMvc.perform(get("/product-catalog/{id}", 1).with(loggedAs(UserRole.SUPPLIER)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1));

    verify(catalogItemService).getMine(any(), eq(1));
  }

  @Test
  void shouldReturn404WhenCatalogItemNotFound() throws Exception {
    when(catalogItemService.getMine(any(), eq(99))).thenThrow(new EntityNotFoundException("no existe"));

    mockMvc.perform(get("/product-catalog/{id}", 99).with(loggedAs(UserRole.SUPPLIER)))
        .andExpect(status().isNotFound());
  }

  @Test
  void shouldRejectGetByIdForNonSupplier() throws Exception {
    mockMvc.perform(get("/product-catalog/{id}", 1).with(loggedAs(UserRole.RETAILER)))
        .andExpect(status().isForbidden());

    verifyNoInteractions(catalogItemService);
  }

  @Test
  void shouldCreateCatalogItem() throws Exception {
    when(catalogItemService.create(any(), any())).thenReturn(TOMATE);

    mockMvc.perform(post("/product-catalog")
            .with(loggedAs(UserRole.SUPPLIER))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"masterProductId":10,"unit":"KILOGRAMO","pricePerUnit":1500,"availableQuantity":100}
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1));

    verify(catalogItemService).create(any(), any());
  }

  @Test
  void shouldRejectCreateForNonSupplier() throws Exception {
    mockMvc.perform(post("/product-catalog")
            .with(loggedAs(UserRole.RETAILER))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"masterProductId":10,"unit":"KILOGRAMO","pricePerUnit":1500,"availableQuantity":100}
                """))
        .andExpect(status().isForbidden());

    verifyNoInteractions(catalogItemService);
  }

  @Test
  void shouldReturn400WhenCreateBodyInvalid() throws Exception {
    mockMvc.perform(post("/product-catalog")
            .with(loggedAs(UserRole.SUPPLIER))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"masterProductId":null,"unit":"KILOGRAMO","pricePerUnit":-5,"availableQuantity":-1}
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.masterProductId").exists());

    verifyNoInteractions(catalogItemService);
  }

  @Test
  void shouldUpdateCatalogItem() throws Exception {
    when(catalogItemService.update(any(), eq(1), any())).thenReturn(TOMATE);

    mockMvc.perform(put("/product-catalog/{id}", 1)
            .with(loggedAs(UserRole.SUPPLIER))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"unit":"KILOGRAMO","pricePerUnit":2000,"availableQuantity":50,"active":true}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1));

    verify(catalogItemService).update(any(), eq(1), any());
  }

  @Test
  void shouldRejectUpdateForNonSupplier() throws Exception {
    mockMvc.perform(put("/product-catalog/{id}", 1)
            .with(loggedAs(UserRole.RETAILER))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"unit":"KILOGRAMO","pricePerUnit":2000,"availableQuantity":50,"active":true}
                """))
        .andExpect(status().isForbidden());

    verifyNoInteractions(catalogItemService);
  }

  @Test
  void shouldReturn400WhenUpdateBodyInvalid() throws Exception {
    mockMvc.perform(put("/product-catalog/{id}", 1)
            .with(loggedAs(UserRole.SUPPLIER))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"unit":null,"pricePerUnit":null,"availableQuantity":null,"active":true}
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.unit").exists());

    verifyNoInteractions(catalogItemService);
  }

  @Test
  void shouldDeactivateCatalogItem() throws Exception {
    mockMvc.perform(delete("/product-catalog/{id}", 1).with(loggedAs(UserRole.SUPPLIER)))
        .andExpect(status().isNoContent());

    verify(catalogItemService).deactivate(any(), eq(1));
  }

  @Test
  void shouldRejectDeactivateForNonSupplier() throws Exception {
    mockMvc.perform(delete("/product-catalog/{id}", 1).with(loggedAs(UserRole.RETAILER)))
        .andExpect(status().isForbidden());

    verifyNoInteractions(catalogItemService);
  }

  @Test
  void shouldReturn404WhenDeactivatingUnknownItem() throws Exception {
    doThrow(new EntityNotFoundException("no existe"))
        .when(catalogItemService).deactivate(any(), eq(99));

    mockMvc.perform(delete("/product-catalog/{id}", 99).with(loggedAs(UserRole.SUPPLIER)))
        .andExpect(status().isNotFound());
  }

}

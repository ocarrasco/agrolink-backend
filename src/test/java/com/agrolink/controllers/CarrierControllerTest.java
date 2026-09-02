package com.agrolink.controllers;

import com.agrolink.model.enums.TransportStatus;
import com.agrolink.model.enums.UserRole;
import com.agrolink.security.LoggedUserJwtAuthenticationConverter;
import com.agrolink.security.SecurityConfig;
import com.agrolink.services.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(SecurityConfig.class)
@WebMvcTest(CarrierController.class)
class CarrierControllerTest extends ControllerTestSupport {

  @MockBean
  private OrderService orderService;

  @MockBean
  private LoggedUserJwtAuthenticationConverter loggedUserJwtAuthenticationConverter;

  @MockBean
  private JwtDecoder jwtDecoder;

  @Autowired
  private MockMvc mockMvc;

  @Test
  void shouldListOpenRequests() throws Exception {
    when(orderService.listOpenTransportRequests(any())).thenReturn(List.of());

    mockMvc.perform(get("/carrier/transport-requests").with(loggedAs(UserRole.CARRIER)))
        .andExpect(status().isOk());

    verify(orderService).listOpenTransportRequests(any());
  }

  @Test
  void shouldRejectRequestsForNonCarrier() throws Exception {
    mockMvc.perform(get("/carrier/transport-requests").with(loggedAs(UserRole.RETAILER)))
        .andExpect(status().isForbidden());

    verifyNoInteractions(orderService);
  }

  @Test
  void shouldRejectRequestsWithoutAuthentication() throws Exception {
    mockMvc.perform(get("/carrier/transport-requests"))
        .andExpect(status().isUnauthorized());

    verifyNoInteractions(orderService);
  }

  @Test
  void shouldMarkInterest() throws Exception {
    mockMvc.perform(post("/carrier/transport-requests/{id}/interest", 7).with(loggedAs(UserRole.CARRIER)))
        .andExpect(status().isNoContent());

    verify(orderService).markInterested(any(), eq(7));
  }

  @Test
  void shouldWithdrawInterest() throws Exception {
    mockMvc.perform(delete("/carrier/transport-requests/{id}/interest", 7).with(loggedAs(UserRole.CARRIER)))
        .andExpect(status().isNoContent());

    verify(orderService).withdrawInterest(any(), eq(7));
  }

  @Test
  void shouldListDeliveries() throws Exception {
    when(orderService.listForCarrier(any(), any(), any(), any())).thenReturn(List.of());

    mockMvc.perform(get("/carrier/deliveries").with(loggedAs(UserRole.CARRIER)))
        .andExpect(status().isOk());

    verify(orderService).listForCarrier(any(), isNull(), isNull(), isNull());
  }

  @Test
  void shouldForwardDeliveryFilters() throws Exception {
    when(orderService.listForCarrier(any(), any(), any(), any())).thenReturn(List.of());

    mockMvc.perform(get("/carrier/deliveries")
            .param("status", "IN_TRANSIT")
            .param("year", "2026")
            .param("month", "9")
            .with(loggedAs(UserRole.CARRIER)))
        .andExpect(status().isOk());

    verify(orderService).listForCarrier(any(), eq(TransportStatus.IN_TRANSIT), eq(2026), eq(9));
  }

  @Test
  void shouldPickup() throws Exception {
    mockMvc.perform(post("/carrier/deliveries/{id}/pickup", 7).with(loggedAs(UserRole.CARRIER)))
        .andExpect(status().isOk());

    verify(orderService).pickup(any(), eq(7));
  }

  @Test
  void shouldDeliver() throws Exception {
    mockMvc.perform(post("/carrier/deliveries/{id}/deliver", 7).with(loggedAs(UserRole.CARRIER)))
        .andExpect(status().isOk());

    verify(orderService).deliver(any(), eq(7));
  }

  @Test
  void shouldRejectDeliveriesForNonCarrier() throws Exception {
    mockMvc.perform(get("/carrier/deliveries").with(loggedAs(UserRole.SUPPLIER)))
        .andExpect(status().isForbidden());

    verifyNoInteractions(orderService);
  }
}

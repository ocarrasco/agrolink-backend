package com.agrolink.controllers;

import com.agrolink.dto.enums.Trend;
import com.agrolink.dto.response.MonthOverMonth;
import com.agrolink.dto.response.OrderResponse;
import com.agrolink.dto.response.RetailerDashboardResponse;
import com.agrolink.model.enums.OrderStatus;
import com.agrolink.model.enums.ShippingMethod;
import com.agrolink.model.enums.UserRole;
import com.agrolink.security.LoggedUserJwtAuthenticationConverter;
import com.agrolink.security.SecurityConfig;
import com.agrolink.services.OrderService;
import com.agrolink.services.RetailerDashboardService;
import com.agrolink.validations.CreateOrderRequestValidator;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(SecurityConfig.class)
@WebMvcTest(RetailerController.class)
class RetailerControllerTest extends ControllerTestSupport {

  @MockBean
  private OrderService orderService;

  // @InitBinder dependency of the controller
  @MockBean
  private CreateOrderRequestValidator createOrderRequestValidator;

  @MockBean
  private RetailerDashboardService retailerDashboardService;

  // SecurityConfig constructor dependency; its UserService dep is outside the web slice
  @MockBean
  private LoggedUserJwtAuthenticationConverter loggedUserJwtAuthenticationConverter;

  // required to build SecurityConfig's .oauth2ResourceServer(...jwt...) without hitting Keycloak
  @MockBean
  private JwtDecoder jwtDecoder;

  @Autowired
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    when(createOrderRequestValidator.supports(any())).thenReturn(true);
  }

  @Test
  void shouldPlaceOrderAsRetailer() throws Exception {
    when(orderService.create(any(), any())).thenReturn(sampleOrder(OrderStatus.PLACED));

    mockMvc.perform(post("/retailer/orders")
            .with(loggedAs(UserRole.RETAILER))
            .contentType(MediaType.APPLICATION_JSON)
            .content(readResource("request/CreateOrderRequest_OK.json")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("PLACED"))
        .andExpect(jsonPath("$.supplierId").value(2));

    verify(orderService).create(any(), any());
  }

  @Test
  void shouldRejectPlaceOrderForNonRetailer() throws Exception {
    mockMvc.perform(post("/retailer/orders")
            .with(loggedAs(UserRole.SUPPLIER))
            .contentType(MediaType.APPLICATION_JSON)
            .content(readResource("request/CreateOrderRequest_OK.json")))
        .andExpect(status().isForbidden());

    verifyNoInteractions(orderService);
  }

  @Test
  void shouldRejectPlaceOrderWithoutAuthentication() throws Exception {
    mockMvc.perform(post("/retailer/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .content(readResource("request/CreateOrderRequest_OK.json")))
        .andExpect(status().isUnauthorized());

    verifyNoInteractions(orderService);
  }

  @Test
  void shouldReturnTheDashboard() throws Exception {
    when(retailerDashboardService.getDashboard(any())).thenReturn(new RetailerDashboardResponse(
        new MonthOverMonth(5, 4, 1, 25, Trend.UP),
        new MonthOverMonth(6, 6, 0, 0, Trend.FLAT),
        new MonthOverMonth(500_000, 400_000, 100_000, 25, Trend.UP)));

    mockMvc.perform(get("/retailer/dashboard").with(loggedAs(UserRole.RETAILER)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.completedOrders.currentMonth").value(5))
        .andExpect(jsonPath("$.completedOrders.trend").value("UP"))
        .andExpect(jsonPath("$.placedOrders.trend").value("FLAT"))
        .andExpect(jsonPath("$.investment.currentMonth").value(500_000))
        .andExpect(jsonPath("$.investment.percentChange").value(25));
  }

  @Test
  void shouldRejectDashboardForNonRetailer() throws Exception {
    mockMvc.perform(get("/retailer/dashboard").with(loggedAs(UserRole.SUPPLIER)))
        .andExpect(status().isForbidden());

    verifyNoInteractions(retailerDashboardService);
  }

  @Test
  void shouldListMyOrders() throws Exception {
    when(orderService.listForRetailer(any(), any())).thenReturn(List.of());

    mockMvc.perform(get("/retailer/orders").with(loggedAs(UserRole.RETAILER)))
        .andExpect(status().isOk());

    verify(orderService).listForRetailer(any(), any());
  }

  @Test
  void shouldRejectListForNonRetailer() throws Exception {
    mockMvc.perform(get("/retailer/orders").with(loggedAs(UserRole.SUPPLIER)))
        .andExpect(status().isForbidden());

    verifyNoInteractions(orderService);
  }

  @Test
  void shouldCancelMyOrder() throws Exception {
    when(orderService.cancel(any(), eq(7))).thenReturn(sampleOrder(OrderStatus.CANCELLED));

    mockMvc.perform(post("/retailer/orders/{id}/cancel", 7).with(loggedAs(UserRole.RETAILER)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELLED"));

    verify(orderService).cancel(any(), eq(7));
  }

  private static OrderResponse sampleOrder(OrderStatus status) {
    return new OrderResponse(7, status, 1, "Verdulería Belgrano", 2, "Finca Los Andes",
        120000, null, ShippingMethod.PICKUP, null, List.of(), LocalDateTime.now(), LocalDateTime.now());
  }

}

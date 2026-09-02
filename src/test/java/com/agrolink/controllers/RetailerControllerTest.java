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
import com.agrolink.dto.response.OrderSuggestionResponse;
import com.agrolink.model.enums.ProductUnit;
import com.agrolink.services.OrderService;
import com.agrolink.services.OrderSuggestionService;
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
import static org.mockito.ArgumentMatchers.isNull;
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

  @MockBean
  private OrderSuggestionService orderSuggestionService;

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
    when(orderService.listForRetailer(any(), any(), any(), any())).thenReturn(List.of());

    mockMvc.perform(get("/retailer/orders").with(loggedAs(UserRole.RETAILER)))
        .andExpect(status().isOk());

    verify(orderService).listForRetailer(any(), isNull(), isNull(), isNull());
  }

  @Test
  void shouldForwardStatusYearAndMonthFilters() throws Exception {
    when(orderService.listForRetailer(any(), any(), any(), any())).thenReturn(List.of());

    mockMvc.perform(get("/retailer/orders")
            .param("status", "PLACED")
            .param("year", "2026")
            .param("month", "9")
            .with(loggedAs(UserRole.RETAILER)))
        .andExpect(status().isOk());

    verify(orderService).listForRetailer(any(), eq(OrderStatus.PLACED), eq(2026), eq(9));
  }

  @Test
  void shouldRejectListForNonRetailer() throws Exception {
    mockMvc.perform(get("/retailer/orders").with(loggedAs(UserRole.SUPPLIER)))
        .andExpect(status().isForbidden());

    verifyNoInteractions(orderService);
  }

  @Test
  void shouldGetOneOrder() throws Exception {
    when(orderService.getForRetailer(any(), eq(7))).thenReturn(sampleOrder(OrderStatus.CONFIRMED));

    mockMvc.perform(get("/retailer/orders/{id}", 7).with(loggedAs(UserRole.RETAILER)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(7))
        .andExpect(jsonPath("$.status").value("CONFIRMED"));

    verify(orderService).getForRetailer(any(), eq(7));
  }

  @Test
  void shouldRejectGetOneOrderForNonRetailer() throws Exception {
    mockMvc.perform(get("/retailer/orders/{id}", 7).with(loggedAs(UserRole.SUPPLIER)))
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

  @Test
  void shouldReturnOrderSuggestions() throws Exception {
    when(orderSuggestionService.suggestForRetailer(any())).thenReturn(List.of(
        new OrderSuggestionResponse(10, "Tomate", ProductUnit.KILOGRAMO, 8, 3, 12, 1500)));

    mockMvc.perform(get("/retailer/order-suggestions").with(loggedAs(UserRole.RETAILER)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].productName").value("Tomate"))
        .andExpect(jsonPath("$[0].suggestedQuantity").value(12));

    verify(orderSuggestionService).suggestForRetailer(any());
  }

  @Test
  void shouldRejectSuggestionsForNonRetailer() throws Exception {
    mockMvc.perform(get("/retailer/order-suggestions").with(loggedAs(UserRole.SUPPLIER)))
        .andExpect(status().isForbidden());

    verifyNoInteractions(orderSuggestionService);
  }

  @Test
  void shouldListTransportInterests() throws Exception {
    when(orderService.listTransportInterests(any(), eq(7))).thenReturn(List.of());

    mockMvc.perform(get("/retailer/orders/{id}/transport-interests", 7).with(loggedAs(UserRole.RETAILER)))
        .andExpect(status().isOk());

    verify(orderService).listTransportInterests(any(), eq(7));
  }

  @Test
  void shouldAcceptCarrier() throws Exception {
    when(orderService.acceptCarrier(any(), eq(7), eq(3))).thenReturn(sampleOrder(OrderStatus.CONFIRMED));

    mockMvc.perform(post("/retailer/orders/{id}/transport/{carrierId}/accept", 7, 3).with(loggedAs(UserRole.RETAILER)))
        .andExpect(status().isOk());

    verify(orderService).acceptCarrier(any(), eq(7), eq(3));
  }

  private OrderResponse sampleOrder(OrderStatus status) {
    return new OrderResponse(7, status, 1, "Verdulería Belgrano", 2, "Finca Los Andes",
        120000, null, ShippingMethod.PICKUP, null, null, null, null, List.of(), LocalDateTime.now(), LocalDateTime.now());
  }

}

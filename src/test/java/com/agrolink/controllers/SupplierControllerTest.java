package com.agrolink.controllers;

import com.agrolink.dto.response.MonthOverMonth;
import com.agrolink.dto.enums.Trend;
import com.agrolink.dto.response.OrderResponse;
import com.agrolink.dto.response.SupplierDashboardResponse;
import com.agrolink.dto.response.SupplierDashboardResponse.MonthlyAmount;
import com.agrolink.dto.response.SupplierDashboardResponse.ProductShare;
import com.agrolink.model.enums.OrderStatus;
import com.agrolink.model.enums.ShippingMethod;
import com.agrolink.model.enums.UserRole;
import com.agrolink.security.LoggedUserJwtAuthenticationConverter;
import com.agrolink.security.SecurityConfig;
import com.agrolink.services.OrderService;
import com.agrolink.services.SupplierDashboardService;
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
@WebMvcTest(SupplierController.class)
class SupplierControllerTest extends ControllerTestSupport {

  @MockBean
  private OrderService orderService;

  @MockBean
  private SupplierDashboardService supplierDashboardService;

  // SecurityConfig constructor dependency; its UserService dep is outside the web slice
  @MockBean
  private LoggedUserJwtAuthenticationConverter loggedUserJwtAuthenticationConverter;

  // required to build SecurityConfig's .oauth2ResourceServer(...jwt...) without hitting Keycloak
  @MockBean
  private JwtDecoder jwtDecoder;

  @Autowired
  private MockMvc mockMvc;

  @Test
  void shouldListMySales() throws Exception {
    when(orderService.listForSupplier(any(), any(), any(), any())).thenReturn(List.of());

    mockMvc.perform(get("/supplier/orders").with(loggedAs(UserRole.SUPPLIER)))
        .andExpect(status().isOk());

    verify(orderService).listForSupplier(any(), isNull(), isNull(), isNull());
  }

  @Test
  void shouldForwardStatusYearAndMonthFilters() throws Exception {
    when(orderService.listForSupplier(any(), any(), any(), any())).thenReturn(List.of());

    mockMvc.perform(get("/supplier/orders")
            .param("status", "FULFILLED")
            .param("year", "2026")
            .param("month", "8")
            .with(loggedAs(UserRole.SUPPLIER)))
        .andExpect(status().isOk());

    verify(orderService).listForSupplier(any(), eq(OrderStatus.FULFILLED), eq(2026), eq(8));
  }

  @Test
  void shouldReturnTheDashboard() throws Exception {
    when(supplierDashboardService.getDashboard(any())).thenReturn(new SupplierDashboardResponse(
        new MonthOverMonth(500_000, 400_000, 100_000, 25, Trend.UP),
        new MonthOverMonth(5, 4, 1, 25, Trend.UP),
        List.of(new MonthlyAmount(2026, 6, 200_000), new MonthlyAmount(2026, 7, 400_000),
            new MonthlyAmount(2026, 8, 500_000)),
        List.of(new ProductShare(10, "Tomate", 300_000, 60), new ProductShare(null, "Otros", 200_000, 40))));

    mockMvc.perform(get("/supplier/dashboard").with(loggedAs(UserRole.SUPPLIER)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sales.currentMonth").value(500_000))
        .andExpect(jsonPath("$.sales.percentChange").value(25))
        .andExpect(jsonPath("$.sales.trend").value("UP"))
        .andExpect(jsonPath("$.completedOrders.absoluteChange").value(1))
        .andExpect(jsonPath("$.salesTrend[2].amount").value(500_000))
        .andExpect(jsonPath("$.topProducts[0].productName").value("Tomate"))
        .andExpect(jsonPath("$.topProducts[0].percent").value(60))
        .andExpect(jsonPath("$.topProducts[1].productName").value("Otros"));
  }

  @Test
  void shouldRejectDashboardForNonSupplier() throws Exception {
    mockMvc.perform(get("/supplier/dashboard").with(loggedAs(UserRole.RETAILER)))
        .andExpect(status().isForbidden());

    verifyNoInteractions(supplierDashboardService);
  }

  @Test
  void shouldRejectSalesForNonSupplier() throws Exception {
    mockMvc.perform(get("/supplier/orders").with(loggedAs(UserRole.RETAILER)))
        .andExpect(status().isForbidden());

    verifyNoInteractions(orderService);
  }

  @Test
  void shouldConfirmOrder() throws Exception {
    when(orderService.confirm(any(), eq(7))).thenReturn(sampleOrder(OrderStatus.CONFIRMED));

    mockMvc.perform(post("/supplier/orders/{id}/confirm", 7).with(loggedAs(UserRole.SUPPLIER)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CONFIRMED"));

    verify(orderService).confirm(any(), eq(7));
  }

  @Test
  void shouldRejectOrderWithNote() throws Exception {
    when(orderService.reject(any(), eq(7), eq("sin stock esta semana")))
        .thenReturn(sampleOrder(OrderStatus.REJECTED));

    mockMvc.perform(post("/supplier/orders/{id}/reject", 7)
            .with(loggedAs(UserRole.SUPPLIER))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"note\":\"sin stock esta semana\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("REJECTED"));

    verify(orderService).reject(any(), eq(7), eq("sin stock esta semana"));
  }

  @Test
  void shouldRejectOrderWithoutBody() throws Exception {
    when(orderService.reject(any(), eq(7), isNull())).thenReturn(sampleOrder(OrderStatus.REJECTED));

    mockMvc.perform(post("/supplier/orders/{id}/reject", 7).with(loggedAs(UserRole.SUPPLIER)))
        .andExpect(status().isOk());

    verify(orderService).reject(any(), eq(7), isNull());
  }

  @Test
  void shouldFulfillOrder() throws Exception {
    when(orderService.fulfill(any(), eq(7))).thenReturn(sampleOrder(OrderStatus.FULFILLED));

    mockMvc.perform(post("/supplier/orders/{id}/fulfill", 7).with(loggedAs(UserRole.SUPPLIER)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("FULFILLED"));

    verify(orderService).fulfill(any(), eq(7));
  }

  private static OrderResponse sampleOrder(OrderStatus status) {
    return new OrderResponse(7, status, 1, "Verdulería Belgrano", 2, "Finca Los Andes",
        120000, null, ShippingMethod.PICKUP, null, null, null, null, List.of(), LocalDateTime.now(), LocalDateTime.now());
  }

}

package com.agrolink.controllers;

import com.agrolink.dto.response.OrderResponse;
import com.agrolink.dto.request.RejectOrderRequest;
import com.agrolink.dto.response.SupplierDashboardResponse;
import com.agrolink.model.enums.OrderStatus;
import com.agrolink.services.OrderService;
import com.agrolink.services.SupplierDashboardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/supplier")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPPLIER')")
public class SupplierController extends BaseController {

  private final OrderService orderService;
  private final SupplierDashboardService supplierDashboardService;

  @GetMapping("/dashboard")
  public SupplierDashboardResponse dashboard() {
    var supplier = loggedUser();
    log.info("Building dashboard for supplier {}", supplier.id());
    return supplierDashboardService.getDashboard(supplier);
  }

  @GetMapping("/orders")
  public List<OrderResponse> listOrders(@RequestParam(required = false) OrderStatus status,
                                        @RequestParam(required = false) Integer year,
                                        @RequestParam(required = false) Integer month) {
    var supplier = loggedUser();
    log.info("Listing sales for supplier {} (status={}, year={}, month={})", supplier.id(), status, year, month);
    return orderService.listForSupplier(supplier, status, year, month);
  }

  @GetMapping("/orders/{id}")
  public OrderResponse getOrder(@PathVariable Integer id) {
    var supplier = loggedUser();
    log.info("Fetching sale {} for supplier {}", id, supplier.id());
    return orderService.getForSupplier(supplier, id);
  }

  @PostMapping("/orders/{id}/confirm")
  public OrderResponse confirmOrder(@PathVariable Integer id) {
    var supplier = loggedUser();
    log.info("Supplier {} confirming order {}", supplier.id(), id);
    return orderService.confirm(supplier, id);
  }

  @PostMapping("/orders/{id}/reject")
  public OrderResponse rejectOrder(@PathVariable Integer id, @Valid @RequestBody(required = false) RejectOrderRequest request) {
    var supplier = loggedUser();
    log.info("Supplier {} rejecting order {}", supplier.id(), id);
    return orderService.reject(supplier, id, request == null ? null : request.note());
  }

  @PostMapping("/orders/{id}/fulfill")
  public OrderResponse fulfillOrder(@PathVariable Integer id) {
    var supplier = loggedUser();
    log.info("Supplier {} marking order {} fulfilled", supplier.id(), id);
    return orderService.fulfill(supplier, id);
  }

}

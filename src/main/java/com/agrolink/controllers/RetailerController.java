package com.agrolink.controllers;

import com.agrolink.dto.request.CreateOrderRequest;
import com.agrolink.dto.response.OrderResponse;
import com.agrolink.dto.response.OrderSuggestionResponse;
import com.agrolink.dto.response.RetailerDashboardResponse;
import com.agrolink.dto.response.TransportInterestResponse;
import com.agrolink.model.enums.OrderStatus;
import com.agrolink.services.OrderService;
import com.agrolink.services.OrderSuggestionService;
import com.agrolink.services.RetailerDashboardService;
import com.agrolink.validations.CreateOrderRequestValidator;
import jakarta.validation.Valid;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/retailer")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RETAILER')")
public class RetailerController extends BaseController {

  @NonNull
  private final OrderService orderService;

  @NonNull
  private final CreateOrderRequestValidator createOrderRequestValidator;

  @NonNull
  private final RetailerDashboardService retailerDashboardService;

  @NonNull
  private final OrderSuggestionService orderSuggestionService;

  @InitBinder("createOrderRequest")
  void bindCreateOrderRequest(WebDataBinder binder) {
    binder.addValidators(createOrderRequestValidator);
  }

  @GetMapping("/dashboard")
  public RetailerDashboardResponse dashboard() {
    var retailer = loggedUser();
    log.info("Building dashboard for retailer {}", retailer.id());
    return retailerDashboardService.getDashboard(retailer);
  }

  @GetMapping("/order-suggestions")
  public List<OrderSuggestionResponse> orderSuggestions() {
    var retailer = loggedUser();
    return orderSuggestionService.suggestForRetailer(retailer);
  }

  @PostMapping("/orders")
  @ResponseStatus(HttpStatus.CREATED)
  public OrderResponse placeOrder(@Valid @RequestBody CreateOrderRequest request) {
    var retailer = loggedUser();
    log.info("Retailer {} placing an order to supplier {} ({} product(s))",
        retailer.id(), request.supplierId(), request.products().size());
    return orderService.create(retailer, request);
  }

  @GetMapping("/orders")
  public List<OrderResponse> listOrders(@RequestParam(required = false) OrderStatus status,
                                        @RequestParam(required = false) Integer year,
                                        @RequestParam(required = false) Integer month) {
    var retailer = loggedUser();
    log.info("Listing orders for retailer {} (status={}, year={}, month={})", retailer.id(), status, year, month);
    return orderService.listForRetailer(retailer, status, year, month);
  }

  @GetMapping("/orders/{id}")
  public OrderResponse getOrder(@PathVariable Integer id) {
    var retailer = loggedUser();
    log.info("Fetching order {} for retailer {}", id, retailer.id());
    return orderService.getForRetailer(retailer, id);
  }

  @PostMapping("/orders/{id}/cancel")
  public OrderResponse cancelOrder(@PathVariable Integer id) {
    var retailer = loggedUser();
    log.info("Retailer {} cancelling order {}", retailer.id(), id);
    return orderService.cancel(retailer, id);
  }

  /**
   * Carriers who marked interest in transporting this order.
   */
  @GetMapping("/orders/{id}/transport-interests")
  public List<TransportInterestResponse> listTransportInterests(@PathVariable Integer id) {
    var retailer = loggedUser();
    log.info("Retailer {} listing transport interests for order {}", retailer.id(), id);
    return orderService.listTransportInterests(retailer, id);
  }

  @PostMapping("/orders/{id}/transport/{carrierId}/accept")
  public OrderResponse acceptCarrier(@PathVariable Integer id, @PathVariable Integer carrierId) {
    var retailer = loggedUser();
    log.info("Retailer {} accepting carrier {} for order {}", retailer.id(), carrierId, id);
    return orderService.acceptCarrier(retailer, id, carrierId);
  }

  /** PLATFORM_CARRIER orders still in progress: awaiting a carrier, assigned, or in transit. */
  @GetMapping("/orders/in-transit")
  public List<OrderResponse> listInTransit() {
    var retailer = loggedUser();
    log.info("Listing in-transit orders for retailer {}", retailer.id());
    return orderService.listInTransitForRetailer(retailer);
  }

  /** The retailer confirms they received the goods — closes the order (FULFILLED). */
  @PostMapping("/orders/{id}/transport/confirm-delivery")
  public OrderResponse confirmDelivery(@PathVariable Integer id) {
    var retailer = loggedUser();
    log.info("Retailer {} confirming delivery for order {}", retailer.id(), id);
    return orderService.confirmDelivery(retailer, id);
  }

}

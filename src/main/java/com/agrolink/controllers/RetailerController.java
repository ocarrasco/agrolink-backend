package com.agrolink.controllers;

import com.agrolink.dto.CreateOrderRequest;
import com.agrolink.dto.OrderResponse;
import com.agrolink.model.enums.OrderStatus;
import com.agrolink.services.OrderService;
import com.agrolink.validations.CreateOrderRequestValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Everything a retailer does, from their side (as the buyer). */
@Slf4j
@RestController
@RequestMapping("/retailer")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RETAILER')")
public class RetailerController extends BaseController {

  private final OrderService orderService;
  private final CreateOrderRequestValidator createOrderRequestValidator;

  @InitBinder("createOrderRequest")
  void bindCreateOrderRequest(WebDataBinder binder) {
    binder.addValidators(createOrderRequestValidator);
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
  public List<OrderResponse> listOrders(@RequestParam(required = false) OrderStatus status) {
    var retailer = loggedUser();
    log.info("Listing orders for retailer {} (status={})", retailer.id(), status);
    return orderService.listForRetailer(retailer, status);
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

}

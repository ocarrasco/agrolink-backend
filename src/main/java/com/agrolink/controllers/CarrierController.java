package com.agrolink.controllers;

import com.agrolink.dto.response.CarrierDeliveryResponse;
import com.agrolink.dto.response.TransportRequestResponse;
import com.agrolink.model.enums.TransportStatus;
import com.agrolink.services.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/carrier")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CARRIER')")
public class CarrierController extends BaseController {

  private final OrderService orderService;

  // ── Open market: orders confirmed for platform-carrier transport, no carrier assigned yet ──

  @GetMapping("/transport-requests")
  public List<TransportRequestResponse> listOpenRequests() {
    var carrier = loggedUser();
    log.info("Carrier {} listing open transport requests", carrier.id());
    return orderService.listOpenTransportRequests(carrier);
  }

  @PostMapping("/transport-requests/{orderId}/interest")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void markInterested(@PathVariable Integer orderId) {
    var carrier = loggedUser();
    log.info("Carrier {} interested in transport request for order {}", carrier.id(), orderId);
    orderService.markInterested(carrier, orderId);
  }

  @DeleteMapping("/transport-requests/{orderId}/interest")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void withdrawInterest(@PathVariable Integer orderId) {
    var carrier = loggedUser();
    log.info("Carrier {} withdrawing interest in order {}", carrier.id(), orderId);
    orderService.withdrawInterest(carrier, orderId);
  }

  // ── My trips: orders assigned to me (carrier execution) ──

  @GetMapping("/deliveries")
  public List<CarrierDeliveryResponse> listDeliveries(@RequestParam(required = false) TransportStatus status,
                                                      @RequestParam(required = false) Integer year,
                                                      @RequestParam(required = false) Integer month) {
    var carrier = loggedUser();
    log.info("Carrier {} listing deliveries (status={}, year={}, month={})", carrier.id(), status, year, month);
    return orderService.listForCarrier(carrier, status, year, month);
  }

  /** ASSIGNED → IN_TRANSIT. Delivery confirmation is the retailer's call, not the carrier's — see {@code RetailerController#confirmDelivery}. */
  @PostMapping("/deliveries/{orderId}/pickup")
  public CarrierDeliveryResponse pickup(@PathVariable Integer orderId) {
    var carrier = loggedUser();
    log.info("Carrier {} picking up order {}", carrier.id(), orderId);
    return orderService.pickup(carrier, orderId);
  }

}

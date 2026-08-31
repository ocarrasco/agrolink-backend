package com.agrolink.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/carrier")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CARRIER')")
public class CarrierController extends BaseController {

  @GetMapping("/dashboard")
  public void dashboard() {
    log.info("Carrier Dashboard");
  }

}

package com.agrolink.controllers;

import com.agrolink.dto.response.SupplierResponse;
import com.agrolink.services.CatalogService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Retailer-facing directory: which suppliers exist and what they offer (with stock + price).
 * <p>
 * The supplier's own offerings live in {@code ProductCatalogController} ({@code /product-catalog}).
 */
@Slf4j
@RestController
@RequestMapping("/supplier-catalog")
@RequiredArgsConstructor
public class SupplierCatalogController {

  @NonNull
  private final CatalogService catalogService;

  @GetMapping
  @PreAuthorize("hasAnyRole('RETAILER', 'ADMIN')")
  public List<SupplierResponse> list(@RequestParam(required = false) Integer masterProductId, @RequestParam(required = false) String q) {
    log.info("Listing supplier catalog (masterProductId={}, q={})", masterProductId, q);
    return catalogService.listSuppliers(masterProductId, q);
  }

}

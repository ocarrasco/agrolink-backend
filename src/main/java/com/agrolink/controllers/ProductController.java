package com.agrolink.controllers;

import com.agrolink.dto.response.ProductResponse;
import com.agrolink.services.MasterProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The master product catalog as seen by suppliers / retailers: active products only, trimmed to
 * {@code id / name / unit}. Admin management is {@code /master-products} ({@code MasterProductController}).
 */
@Slf4j
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

  private final MasterProductService masterProductService;

  @GetMapping
  public List<ProductResponse> list() {
    log.info("Listing active products");
    return masterProductService.listActive();
  }

}

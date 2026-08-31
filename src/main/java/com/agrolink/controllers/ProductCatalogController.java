package com.agrolink.controllers;

import com.agrolink.dto.CatalogItemResponse;
import com.agrolink.dto.CreateCatalogItemRequest;
import com.agrolink.dto.UpdateCatalogItemRequest;
import com.agrolink.services.CatalogItemService;
import com.agrolink.validations.CreateCatalogItemRequestValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The authenticated supplier's own product catalog — the {@code catalog_item}s they offer.
 * Retailers browse everyone's via {@code SupplierCatalogController} ({@code /supplier-catalog}).
 */
@Slf4j
@RestController
@RequestMapping("/product-catalog")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPPLIER')")
public class ProductCatalogController extends BaseController {

  private final CatalogItemService catalogItemService;
  private final CreateCatalogItemRequestValidator createCatalogItemRequestValidator;

  @InitBinder("createCatalogItemRequest")
  void bindCreateCatalogItemRequest(WebDataBinder binder) {
    binder.addValidators(createCatalogItemRequestValidator);
  }

  @GetMapping
  public List<CatalogItemResponse> getProductCatalog() {
    var supplier = loggedUser();
    log.info("Listing product catalog items for supplier {}", supplier.id());
    return catalogItemService.getCatalogItems(supplier);
  }

  @GetMapping("/{id}")
  public CatalogItemResponse getMine(@PathVariable Integer id) {
    var supplier = loggedUser();
    log.info("Fetching catalog item {} for supplier {}", id, supplier.id());
    return catalogItemService.getMine(supplier, id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public CatalogItemResponse create(@Valid @RequestBody CreateCatalogItemRequest request) {
    var supplier = loggedUser();
    log.info("Supplier {} creating catalog item for master product {}", supplier.id(), request.masterProductId());
    return catalogItemService.create(supplier, request);
  }

  @PutMapping("/{id}")
  public CatalogItemResponse update(@PathVariable Integer id, @Valid @RequestBody UpdateCatalogItemRequest request) {
    var supplier = loggedUser();
    log.info("Supplier {} updating catalog item {}", supplier.id(), id);
    return catalogItemService.update(supplier, id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deactivate(@PathVariable Integer id) {
    var supplier = loggedUser();
    log.info("Supplier {} deactivating catalog item {}", supplier.id(), id);
    catalogItemService.deactivate(supplier, id);
  }

}

package com.agrolink.controllers;

import com.agrolink.dto.CreateMasterProductRequest;
import com.agrolink.dto.MasterProductResponse;
import com.agrolink.dto.UpdateMasterProductRequest;
import com.agrolink.services.MasterProductService;
import com.agrolink.validations.CreateMasterProductRequestValidator;
import jakarta.validation.Valid;
import lombok.NonNull;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Admin management of the master product catalog. Suppliers / retailers use {@code /products}. */
@Slf4j
@RestController
@RequestMapping("/master-products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class MasterProductController {

  @NonNull
  private final MasterProductService masterProductService;

  @NonNull
  private final CreateMasterProductRequestValidator createMasterProductRequestValidator;

  @InitBinder("createMasterProductRequest")
  void bindCreateMasterProductRequest(WebDataBinder binder) {
    binder.addValidators(createMasterProductRequestValidator);
  }

  @GetMapping
  public List<MasterProductResponse> list(@RequestParam(name = "includeInactive", defaultValue = "false") boolean includeInactive) {
    log.info("Listing master products (includeInactive={})", includeInactive);
    return masterProductService.list(includeInactive);
  }

  @GetMapping("/{id}")
  public MasterProductResponse getById(@PathVariable Integer id) {
    log.info("Fetching master product {}", id);
    return masterProductService.getById(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public MasterProductResponse create(@Valid @RequestBody CreateMasterProductRequest request) {
    log.info("Creating master product '{}'", request.name());
    return masterProductService.create(request);
  }

  @PutMapping("/{id}")
  public MasterProductResponse update(@PathVariable Integer id, @Valid @RequestBody UpdateMasterProductRequest request) {
    log.info("Updating master product {}", id);
    return masterProductService.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deactivate(@PathVariable Integer id) {
    log.info("Deactivating master product {}", id);
    masterProductService.deactivate(id);
  }

}

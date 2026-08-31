package com.agrolink.services;

import com.agrolink.dto.CatalogItemResponse;
import com.agrolink.dto.CreateCatalogItemRequest;
import com.agrolink.dto.UpdateCatalogItemRequest;
import com.agrolink.mappers.CatalogItemMapper;
import com.agrolink.model.CatalogItemModel;
import com.agrolink.model.MasterProductModel;
import com.agrolink.repositories.ICatalogItemRepository;
import com.agrolink.security.LoggedUser;
import com.agrolink.utils.UserMessages;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CatalogItemService {

  private final ICatalogItemRepository catalogItemRepository;
  private final CatalogItemMapper catalogItemMapper;
  private final MasterProductService masterProductService;
  private final UserService userService;

  public List<CatalogItemResponse> getCatalogItems(LoggedUser supplier) {
    var itemModelList = catalogItemRepository.findBySupplierIdOrderByIdAsc(supplier.id());
    return catalogItemMapper.toResponseList(itemModelList);
  }

  public CatalogItemResponse getMine(LoggedUser supplier, Integer id) {
    return catalogItemMapper.toResponse(findOwnedOrThrow(supplier, id));
  }

  @Transactional
  public CatalogItemResponse create(LoggedUser supplier, CreateCatalogItemRequest request) {
    // master product existence + active state are checked by CreateCatalogItemRequestValidator
    MasterProductModel masterProduct = masterProductService.getEntity(request.masterProductId());
    // "supplier already offers this product" needs the caller identity, so it stays here
    // (also enforced by the catalog_item UNIQUE(supplier_id, master_product_id) constraint)
    if (catalogItemRepository.existsBySupplierIdAndMasterProductId(supplier.id(), masterProduct.getId())) {
      throw new DuplicateResourceException(UserMessages.alreadyOffering(masterProduct.getName()));
    }

    CatalogItemModel item = new CatalogItemModel();
    item.setSupplier(userService.getReference(supplier.id()));
    item.setMasterProduct(masterProduct);
    item.setUnit(request.unit() != null ? request.unit() : masterProduct.getUnit());
    item.setPricePerUnit(request.pricePerUnit());
    item.setAvailableQuantity(request.availableQuantity());
    item.setActive(true);
    return catalogItemMapper.toResponse(catalogItemRepository.save(item));
  }

  @Transactional
  public CatalogItemResponse update(LoggedUser supplier, Integer id, UpdateCatalogItemRequest request) {
    CatalogItemModel item = findOwnedOrThrow(supplier, id);
    item.setUnit(request.unit());
    item.setPricePerUnit(request.pricePerUnit());
    item.setAvailableQuantity(request.availableQuantity());
    item.setActive(request.active());
    // flush so @UpdateTimestamp is populated before we map the response
    return catalogItemMapper.toResponse(catalogItemRepository.saveAndFlush(item));
  }

  @Transactional
  public void deactivate(LoggedUser supplier, Integer id) {
    CatalogItemModel item = findOwnedOrThrow(supplier, id);
    item.setActive(false);
    catalogItemRepository.save(item);
  }

  private CatalogItemModel findOwnedOrThrow(LoggedUser supplier, Integer id) {
    return catalogItemRepository.findByIdAndSupplierId(id, supplier.id()).orElseThrow(() -> new EntityNotFoundException(UserMessages.catalogItemNotFound(id)));
  }

}

package com.agrolink.services;

import com.agrolink.dto.response.CatalogItemResponse;
import com.agrolink.dto.request.CreateCatalogItemRequest;
import com.agrolink.dto.request.UpdateCatalogItemRequest;
import com.agrolink.mappers.CatalogItemMapper;
import com.agrolink.model.CatalogItemModel;
import com.agrolink.repositories.ICatalogItemRepository;
import com.agrolink.security.LoggedUser;
import com.agrolink.utils.UserMessages;
import jakarta.persistence.EntityNotFoundException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CatalogItemService {

  @NonNull
  private final ICatalogItemRepository catalogItemRepository;

  @NonNull
  private final CatalogItemMapper catalogItemMapper;

  @NonNull
  private final MasterProductService masterProductService;

  @NonNull
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
    var masterProduct = masterProductService.getEntity(request.masterProductId());
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
    return catalogItemMapper.toResponse(catalogItemRepository.saveAndFlush(item));
  }

  @Transactional
  public void deactivate(LoggedUser supplier, Integer id) {
    CatalogItemModel item = findOwnedOrThrow(supplier, id);
    item.setActive(false);
    catalogItemRepository.save(item);
  }

  private CatalogItemModel findOwnedOrThrow(LoggedUser supplier, Integer id) {
    return catalogItemRepository.findByIdAndSupplierId(id, supplier.id())
        .orElseThrow(() -> new EntityNotFoundException(UserMessages.catalogItemNotFound(id)));
  }

}

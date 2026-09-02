package com.agrolink.services;

import com.agrolink.dto.request.CreateMasterProductRequest;
import com.agrolink.dto.request.UpdateMasterProductRequest;
import com.agrolink.dto.response.MasterProductResponse;
import com.agrolink.dto.response.ProductResponse;
import com.agrolink.mappers.MasterProductMapper;
import com.agrolink.model.MasterProductModel;
import com.agrolink.repositories.IMasterProductRepository;
import com.agrolink.utils.UserMessages;
import jakarta.persistence.EntityNotFoundException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.agrolink.utils.StrUtils.normalizeName;

@Service
@RequiredArgsConstructor
public class MasterProductService {

  @NonNull
  private final IMasterProductRepository masterProductRepository;

  @NonNull
  private final MasterProductMapper masterProductMapper;

  public List<MasterProductResponse> list(boolean includeInactive) {
    List<MasterProductModel> products = includeInactive
        ? masterProductRepository.findAllByOrderByNameAsc()
        : masterProductRepository.findByActiveTrueOrderByNameAsc();
    return masterProductMapper.toResponseList(products);
  }

  public MasterProductResponse getById(Integer id) {
    return masterProductMapper.toResponse(getEntity(id));
  }

  /**
   * Trimmed list of active products, for suppliers / retailers.json (see {@code ProductController}).
   */
  public List<ProductResponse> listActive() {
    return masterProductMapper.toBasicList(masterProductRepository.findByActiveTrueOrderByNameAsc());
  }

  /**
   * Master product entity accessor for other services (e.g. {@link CatalogItemService}).
   */
  public MasterProductModel getEntity(Integer id) {
    return masterProductRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException(UserMessages.masterProductNotFound(id)));
  }


  @Transactional
  public MasterProductResponse create(CreateMasterProductRequest request) {
    // name availability is checked by CreateMasterProductRequestValidator
    String name = normalizeName(request.name());

    MasterProductModel product = masterProductMapper.toEntity(request);
    product.setName(name);
    product.setActive(true);
    return masterProductMapper.toResponse(masterProductRepository.save(product));
  }

  @Transactional
  public MasterProductResponse update(Integer id, UpdateMasterProductRequest request) {
    MasterProductModel product = getEntity(id);
    String name = normalizeName(request.name());
    // "name unique excluding self" needs the target id, so it stays here rather than in a validator
    requireNameAvailable(name, id);

    masterProductMapper.updateEntity(request, product);
    product.setName(name);
    // flush so @UpdateTimestamp is populated before we map the response
    return masterProductMapper.toResponse(masterProductRepository.saveAndFlush(product));
  }

  @Transactional
  public void deactivate(Integer id) {
    MasterProductModel product = getEntity(id);
    product.setActive(false);
    masterProductRepository.save(product);
  }

  private void requireNameAvailable(String name, Integer selfId) {
    var existing = masterProductRepository.findByNameIgnoreCase(name);
    if (existing.isPresent() && !existing.get().getId().equals(selfId)) {
      throw new DuplicateResourceException(UserMessages.productNameTaken(name));
    }
  }

}

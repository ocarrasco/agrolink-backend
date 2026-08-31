package com.agrolink.mappers;

import com.agrolink.dto.SupplierProductResponse;
import com.agrolink.model.CatalogItemModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * Maps a supplier's catalog item to the trimmed line-item shown in the retailer-facing
 * {@code /catalog} view.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SupplierMapper {

  @Mapping(target = "productId", source = "masterProduct.id")
  @Mapping(target = "productName", source = "masterProduct.name")
  @Mapping(target = "price", source = "pricePerUnit")
  @Mapping(target = "stock", source = "availableQuantity")
  SupplierProductResponse toSupplierProduct(CatalogItemModel item);

  List<SupplierProductResponse> toSupplierProducts(List<CatalogItemModel> items);

}

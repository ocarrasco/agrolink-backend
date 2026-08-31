package com.agrolink.mappers;

import com.agrolink.dto.response.SupplierProductResponse;
import com.agrolink.model.CatalogItemModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SupplierMapper {

  @Mapping(target = "productId", source = "masterProduct.id")
  @Mapping(target = "productName", source = "masterProduct.name")
  @Mapping(target = "price", source = "pricePerUnit")
  @Mapping(target = "stock", source = "availableQuantity")
  SupplierProductResponse toSupplierProduct(CatalogItemModel item);

  List<SupplierProductResponse> toSupplierProducts(List<CatalogItemModel> items);

}

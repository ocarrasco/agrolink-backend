package com.agrolink.mappers;

import com.agrolink.dto.CatalogItemResponse;
import com.agrolink.model.CatalogItemModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CatalogItemMapper {

  @Mapping(target = "masterProductId", source = "masterProduct.id")
  @Mapping(target = "masterProductName", source = "masterProduct.name")
  CatalogItemResponse toResponse(CatalogItemModel item);

  List<CatalogItemResponse> toResponseList(List<CatalogItemModel> items);

}

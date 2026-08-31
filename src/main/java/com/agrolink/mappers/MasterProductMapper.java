package com.agrolink.mappers;

import com.agrolink.dto.CreateMasterProductRequest;
import com.agrolink.dto.MasterProductResponse;
import com.agrolink.dto.ProductResponse;
import com.agrolink.dto.UpdateMasterProductRequest;
import com.agrolink.model.MasterProductModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MasterProductMapper {

  MasterProductResponse toResponse(MasterProductModel masterProduct);

  List<MasterProductResponse> toResponseList(List<MasterProductModel> masterProducts);

  /** Trimmed view for suppliers / retailers. */
  ProductResponse toBasic(MasterProductModel masterProduct);

  List<ProductResponse> toBasicList(List<MasterProductModel> masterProducts);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "active", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  MasterProductModel toEntity(CreateMasterProductRequest request);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  void updateEntity(UpdateMasterProductRequest request, @MappingTarget MasterProductModel masterProduct);

}

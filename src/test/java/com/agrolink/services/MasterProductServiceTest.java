package com.agrolink.services;

import com.agrolink.dto.request.CreateMasterProductRequest;
import com.agrolink.dto.request.UpdateMasterProductRequest;
import com.agrolink.dto.response.MasterProductResponse;
import com.agrolink.dto.response.ProductResponse;
import com.agrolink.mappers.MasterProductMapper;
import com.agrolink.model.MasterProductModel;
import com.agrolink.model.enums.ProductUnit;
import com.agrolink.repositories.IMasterProductRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MasterProductServiceTest {

  @Mock
  private IMasterProductRepository masterProductRepository;

  @Spy
  private MasterProductMapper masterProductMapper = Mappers.getMapper(MasterProductMapper.class);

  @InjectMocks
  private MasterProductService masterProductService;

  @Test
  void list_returnsOnlyActiveProducts_whenIncludeInactiveIsFalse() {
    when(masterProductRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(product(1, "Tomate", ProductUnit.KILOGRAMO, true)));

    List<MasterProductResponse> result = masterProductService.list(false);

    assertThat(result).extracting(MasterProductResponse::name).containsExactly("Tomate");
    verify(masterProductRepository, never()).findAllByOrderByNameAsc();
  }

  @Test
  void list_returnsEveryProduct_whenIncludeInactiveIsTrue() {
    when(masterProductRepository.findAllByOrderByNameAsc()).thenReturn(List.of(
        product(1, "Tomate", ProductUnit.KILOGRAMO, true),
        product(2, "Papa Vieja", ProductUnit.SACO, false)));

    List<MasterProductResponse> result = masterProductService.list(true);

    assertThat(result).extracting(MasterProductResponse::name).containsExactly("Tomate", "Papa Vieja");
    assertThat(result).extracting(MasterProductResponse::active).containsExactly(true, false);
  }

  @Test
  void listActive_returnsTrimmedProductResponses() {
    when(masterProductRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(product(1, "Tomate", ProductUnit.KILOGRAMO, true)));

    List<ProductResponse> result = masterProductService.listActive();

    assertThat(result).extracting(ProductResponse::name).containsExactly("Tomate");
  }

  @Test
  void getById_returnsMappedResponse_whenProductExists() {
    when(masterProductRepository.findById(1)).thenReturn(Optional.of(product(1, "Tomate", ProductUnit.KILOGRAMO, true)));

    MasterProductResponse response = masterProductService.getById(1);

    assertThat(response.id()).isEqualTo(1);
    assertThat(response.name()).isEqualTo("Tomate");
    assertThat(response.unit()).isEqualTo(ProductUnit.KILOGRAMO);
    assertThat(response.active()).isTrue();
  }

  @Test
  void getById_throwsEntityNotFoundException_whenProductDoesNotExist() {
    when(masterProductRepository.findById(99)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> masterProductService.getById(99))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessageContaining("99");
  }

  @Test
  void getEntity_throwsEntityNotFoundException_whenProductDoesNotExist() {
    when(masterProductRepository.findById(99)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> masterProductService.getEntity(99))
        .isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  void create_setsActiveTrueAndSavesTheNormalizedName() {
    CreateMasterProductRequest request = new CreateMasterProductRequest("  Tomate   Cherry  ", ProductUnit.KILOGRAMO);
    when(masterProductRepository.save(any())).thenAnswer(invocation -> {
      MasterProductModel saved = invocation.getArgument(0);
      saved.setId(1);
      return saved;
    });

    MasterProductResponse response = masterProductService.create(request);

    ArgumentCaptor<MasterProductModel> captor = ArgumentCaptor.forClass(MasterProductModel.class);
    verify(masterProductRepository).save(captor.capture());
    assertThat(captor.getValue().getName()).isEqualTo("Tomate Cherry");
    assertThat(captor.getValue().isActive()).isTrue();

    assertThat(response.id()).isEqualTo(1);
    assertThat(response.name()).isEqualTo("Tomate Cherry");
    assertThat(response.active()).isTrue();
  }

  @Test
  void update_updatesFieldsAndFlushesWhenNameIsAvailable() {
    MasterProductModel existing = product(1, "Tomate", ProductUnit.KILOGRAMO, true);
    UpdateMasterProductRequest request = new UpdateMasterProductRequest("Tomate  Cherry", ProductUnit.CAJON, false);
    when(masterProductRepository.findById(1)).thenReturn(Optional.of(existing));
    when(masterProductRepository.findByNameIgnoreCase("Tomate Cherry")).thenReturn(Optional.empty());
    when(masterProductRepository.saveAndFlush(existing)).thenReturn(existing);

    MasterProductResponse response = masterProductService.update(1, request);

    assertThat(response.name()).isEqualTo("Tomate Cherry");
    assertThat(response.unit()).isEqualTo(ProductUnit.CAJON);
    assertThat(response.active()).isFalse();
  }

  @Test
  void update_allowsKeepingItsOwnName() {
    MasterProductModel existing = product(1, "Tomate", ProductUnit.KILOGRAMO, true);
    UpdateMasterProductRequest request = new UpdateMasterProductRequest("Tomate", ProductUnit.KILOGRAMO, true);
    when(masterProductRepository.findById(1)).thenReturn(Optional.of(existing));
    when(masterProductRepository.findByNameIgnoreCase("Tomate")).thenReturn(Optional.of(existing));
    when(masterProductRepository.saveAndFlush(existing)).thenReturn(existing);

    MasterProductResponse response = masterProductService.update(1, request);

    assertThat(response.name()).isEqualTo("Tomate");
  }

  @Test
  void update_throwsDuplicateResourceException_whenNameBelongsToAnotherProduct() {
    MasterProductModel existing = product(1, "Tomate", ProductUnit.KILOGRAMO, true);
    MasterProductModel other = product(2, "Papa", ProductUnit.SACO, true);
    UpdateMasterProductRequest request = new UpdateMasterProductRequest("Papa", ProductUnit.KILOGRAMO, true);
    when(masterProductRepository.findById(1)).thenReturn(Optional.of(existing));
    when(masterProductRepository.findByNameIgnoreCase("Papa")).thenReturn(Optional.of(other));

    assertThatThrownBy(() -> masterProductService.update(1, request))
        .isInstanceOf(DuplicateResourceException.class)
        .hasMessageContaining("Papa");

    verify(masterProductRepository, never()).saveAndFlush(any());
  }

  @Test
  void update_throwsEntityNotFoundException_whenProductDoesNotExist() {
    when(masterProductRepository.findById(99)).thenReturn(Optional.empty());
    UpdateMasterProductRequest request = new UpdateMasterProductRequest("Tomate", ProductUnit.KILOGRAMO, true);

    assertThatThrownBy(() -> masterProductService.update(99, request))
        .isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  void deactivate_setsActiveFalseAndSaves() {
    MasterProductModel existing = product(1, "Tomate", ProductUnit.KILOGRAMO, true);
    when(masterProductRepository.findById(1)).thenReturn(Optional.of(existing));

    masterProductService.deactivate(1);

    ArgumentCaptor<MasterProductModel> captor = ArgumentCaptor.forClass(MasterProductModel.class);
    verify(masterProductRepository).save(captor.capture());
    assertThat(captor.getValue().isActive()).isFalse();
  }

  @Test
  void deactivate_throwsEntityNotFoundException_whenProductDoesNotExist() {
    when(masterProductRepository.findById(99)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> masterProductService.deactivate(99))
        .isInstanceOf(EntityNotFoundException.class);
    verify(masterProductRepository, never()).save(any());
  }

  private static MasterProductModel product(Integer id, String name, ProductUnit unit, boolean active) {
    MasterProductModel model = new MasterProductModel();
    model.setId(id);
    model.setName(name);
    model.setUnit(unit);
    model.setActive(active);
    return model;
  }
}

package com.agrolink.services;

import com.agrolink.dto.request.CreateCatalogItemRequest;
import com.agrolink.dto.request.UpdateCatalogItemRequest;
import com.agrolink.dto.response.CatalogItemResponse;
import com.agrolink.mappers.CatalogItemMapper;
import com.agrolink.model.CatalogItemModel;
import com.agrolink.model.MasterProductModel;
import com.agrolink.model.UserModel;
import com.agrolink.model.enums.ProductUnit;
import com.agrolink.model.enums.UserRole;
import com.agrolink.repositories.ICatalogItemRepository;
import com.agrolink.security.LoggedUser;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogItemServiceTest {

  @Mock
  private ICatalogItemRepository catalogItemRepository;

  @Spy
  private CatalogItemMapper catalogItemMapper = Mappers.getMapper(CatalogItemMapper.class);

  @Mock
  private MasterProductService masterProductService;

  @Mock
  private UserService userService;

  @InjectMocks
  private CatalogItemService catalogItemService;

  // ────────────────────────── getCatalogItems ──────────────────────────

  @Test
  void getCatalogItems_listsTheSuppliersOwnItems() {
    LoggedUser supplier = loggedUser(1);
    CatalogItemModel item = catalogItem(10, masterProduct(100, "Tomate", ProductUnit.KILOGRAMO), ProductUnit.KILOGRAMO, 1500, 50, true);
    when(catalogItemRepository.findBySupplierIdOrderByIdAsc(1)).thenReturn(List.of(item));

    List<CatalogItemResponse> responses = catalogItemService.getCatalogItems(supplier);

    assertThat(responses).hasSize(1);
    assertThat(responses.get(0).masterProductId()).isEqualTo(100);
    assertThat(responses.get(0).masterProductName()).isEqualTo("Tomate");
  }

  // ────────────────────────── getMine ──────────────────────────

  @Test
  void getMine_returnsItem_whenOwnedBySupplier() {
    LoggedUser supplier = loggedUser(1);
    CatalogItemModel item = catalogItem(10, masterProduct(100, "Tomate", ProductUnit.KILOGRAMO), ProductUnit.KILOGRAMO, 1500, 50, true);
    when(catalogItemRepository.findByIdAndSupplierId(10, 1)).thenReturn(Optional.of(item));

    CatalogItemResponse response = catalogItemService.getMine(supplier, 10);

    assertThat(response.id()).isEqualTo(10);
  }

  @Test
  void getMine_throws_whenItemDoesNotBelongToSupplier() {
    LoggedUser supplier = loggedUser(1);
    when(catalogItemRepository.findByIdAndSupplierId(10, 1)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> catalogItemService.getMine(supplier, 10))
        .isInstanceOf(EntityNotFoundException.class);
  }

  // ────────────────────────── create ──────────────────────────

  @Test
  void create_savesNewItem_withRequestedPriceUnitAndStock() {
    LoggedUser supplier = loggedUser(1);
    MasterProductModel tomate = masterProduct(100, "Tomate", ProductUnit.KILOGRAMO);
    UserModel supplierModel = new UserModel();
    supplierModel.setId(1);

    when(masterProductService.getEntity(100)).thenReturn(tomate);
    when(catalogItemRepository.existsBySupplierIdAndMasterProductId(1, 100)).thenReturn(false);
    when(userService.getReference(1)).thenReturn(supplierModel);
    when(catalogItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    CreateCatalogItemRequest request = new CreateCatalogItemRequest(100, ProductUnit.SACO, 1500, 50);

    CatalogItemResponse response = catalogItemService.create(supplier, request);

    assertThat(response.unit()).isEqualTo(ProductUnit.SACO);
    assertThat(response.pricePerUnit()).isEqualTo(1500);
    assertThat(response.availableQuantity()).isEqualTo(50);
    assertThat(response.active()).isTrue();
    assertThat(response.masterProductId()).isEqualTo(100);
  }

  @Test
  void create_defaultsToTheMasterProductUnit_whenUnitNotProvided() {
    LoggedUser supplier = loggedUser(1);
    MasterProductModel tomate = masterProduct(100, "Tomate", ProductUnit.KILOGRAMO);

    when(masterProductService.getEntity(100)).thenReturn(tomate);
    when(catalogItemRepository.existsBySupplierIdAndMasterProductId(1, 100)).thenReturn(false);
    when(userService.getReference(1)).thenReturn(new UserModel());
    when(catalogItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    CreateCatalogItemRequest request = new CreateCatalogItemRequest(100, null, 1500, 50);

    CatalogItemResponse response = catalogItemService.create(supplier, request);

    assertThat(response.unit()).isEqualTo(ProductUnit.KILOGRAMO);
  }

  @Test
  void create_rejects_whenSupplierAlreadyOffersTheProduct() {
    LoggedUser supplier = loggedUser(1);
    MasterProductModel tomate = masterProduct(100, "Tomate", ProductUnit.KILOGRAMO);

    when(masterProductService.getEntity(100)).thenReturn(tomate);
    when(catalogItemRepository.existsBySupplierIdAndMasterProductId(1, 100)).thenReturn(true);

    CreateCatalogItemRequest request = new CreateCatalogItemRequest(100, null, 1500, 50);

    assertThatThrownBy(() -> catalogItemService.create(supplier, request))
        .isInstanceOf(DuplicateResourceException.class);

    verify(catalogItemRepository, never()).save(any());
  }

  @Test
  void create_propagatesNotFound_whenMasterProductDoesNotExist() {
    LoggedUser supplier = loggedUser(1);
    when(masterProductService.getEntity(999)).thenThrow(new EntityNotFoundException("no existe"));

    CreateCatalogItemRequest request = new CreateCatalogItemRequest(999, null, 1500, 50);

    assertThatThrownBy(() -> catalogItemService.create(supplier, request))
        .isInstanceOf(EntityNotFoundException.class);

    verify(catalogItemRepository, never()).save(any());
  }

  // ────────────────────────── update ──────────────────────────

  @Test
  void update_overwritesUnitPriceStockAndActive() {
    LoggedUser supplier = loggedUser(1);
    CatalogItemModel item = catalogItem(10, masterProduct(100, "Tomate", ProductUnit.KILOGRAMO), ProductUnit.KILOGRAMO, 1500, 50, true);
    when(catalogItemRepository.findByIdAndSupplierId(10, 1)).thenReturn(Optional.of(item));
    when(catalogItemRepository.saveAndFlush(item)).thenReturn(item);

    UpdateCatalogItemRequest request = new UpdateCatalogItemRequest(ProductUnit.SACO, 2000, 10, false);

    CatalogItemResponse response = catalogItemService.update(supplier, 10, request);

    assertThat(response.unit()).isEqualTo(ProductUnit.SACO);
    assertThat(response.pricePerUnit()).isEqualTo(2000);
    assertThat(response.availableQuantity()).isEqualTo(10);
    assertThat(response.active()).isFalse();
  }

  @Test
  void update_throws_whenItemDoesNotBelongToSupplier() {
    LoggedUser supplier = loggedUser(1);
    when(catalogItemRepository.findByIdAndSupplierId(10, 1)).thenReturn(Optional.empty());

    UpdateCatalogItemRequest request = new UpdateCatalogItemRequest(ProductUnit.SACO, 2000, 10, false);

    assertThatThrownBy(() -> catalogItemService.update(supplier, 10, request))
        .isInstanceOf(EntityNotFoundException.class);

    verify(catalogItemRepository, never()).saveAndFlush(any());
  }

  // ────────────────────────── deactivate ──────────────────────────

  @Test
  void deactivate_setsItemInactive() {
    LoggedUser supplier = loggedUser(1);
    CatalogItemModel item = catalogItem(10, masterProduct(100, "Tomate", ProductUnit.KILOGRAMO), ProductUnit.KILOGRAMO, 1500, 50, true);
    when(catalogItemRepository.findByIdAndSupplierId(10, 1)).thenReturn(Optional.of(item));

    catalogItemService.deactivate(supplier, 10);

    assertThat(item.isActive()).isFalse();
    verify(catalogItemRepository).save(item);
  }

  @Test
  void deactivate_throws_whenItemDoesNotBelongToSupplier() {
    LoggedUser supplier = loggedUser(1);
    when(catalogItemRepository.findByIdAndSupplierId(10, 1)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> catalogItemService.deactivate(supplier, 10))
        .isInstanceOf(EntityNotFoundException.class);

    verify(catalogItemRepository, never()).save(any());
  }

  // ────────────────────────── helpers ──────────────────────────

  private static LoggedUser loggedUser(Integer id) {
    return new LoggedUser(id, UUID.randomUUID(), UserRole.SUPPLIER);
  }

  private static MasterProductModel masterProduct(Integer id, String name, ProductUnit unit) {
    MasterProductModel model = new MasterProductModel();
    model.setId(id);
    model.setName(name);
    model.setUnit(unit);
    return model;
  }

  private static CatalogItemModel catalogItem(Integer id, MasterProductModel masterProduct, ProductUnit unit, int price, int stock, boolean active) {
    CatalogItemModel item = new CatalogItemModel();
    item.setId(id);
    item.setMasterProduct(masterProduct);
    item.setUnit(unit);
    item.setPricePerUnit(price);
    item.setAvailableQuantity(stock);
    item.setActive(active);
    return item;
  }

}

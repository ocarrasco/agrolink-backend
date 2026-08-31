package com.agrolink.services;

import com.agrolink.dto.response.SupplierProductResponse;
import com.agrolink.dto.response.SupplierResponse;
import com.agrolink.mappers.SupplierMapper;
import com.agrolink.model.*;
import com.agrolink.model.enums.ProductUnit;
import com.agrolink.model.enums.TimeSlot;
import com.agrolink.repositories.ICatalogItemRepository;
import com.agrolink.repositories.IUserProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

  @Mock
  private ICatalogItemRepository catalogItemRepository;

  @Mock
  private IUserProfileRepository userProfileRepository;

  @Spy
  private SupplierMapper supplierMapper = Mappers.getMapper(SupplierMapper.class);

  @InjectMocks
  private CatalogService catalogService;

  @Test
  void listSuppliers_returnsEmptyList_whenThereAreNoActiveItems() {
    when(catalogItemRepository.findActiveItems(isNull(), isNull())).thenReturn(List.of());
    when(userProfileRepository.findByUserIdIn(Set.of())).thenReturn(List.of());

    var result = catalogService.listSuppliers(null, null);
    assertThat(result).isEmpty();
  }

  @Test
  void listSuppliers_passesNullQuery_whenQIsBlank() {
    when(catalogItemRepository.findActiveItems(any(), any())).thenReturn(List.of());
    when(userProfileRepository.findByUserIdIn(Set.of())).thenReturn(List.of());

    catalogService.listSuppliers(5, "   ");

    verify(catalogItemRepository).findActiveItems(5, null);
  }

  @Test
  void listSuppliers_trimsQuery_whenQHasSurroundingWhitespace() {
    when(catalogItemRepository.findActiveItems(any(), any())).thenReturn(List.of());
    when(userProfileRepository.findByUserIdIn(Set.of())).thenReturn(List.of());

    catalogService.listSuppliers(null, "  tomate  ");

    verify(catalogItemRepository).findActiveItems(isNull(), eq("tomate"));
  }

  @Test
  void listSuppliers_groupsItemsBySupplierAndMapsProducts() {
    UserModel supplier = supplier(1, "Verduras SPA", "ventas@verduras.cl");
    CatalogItemModel tomate = item(supplier, masterProduct(10, "Tomate"), ProductUnit.KILOGRAMO, 1500, 100);
    CatalogItemModel papa = item(supplier, masterProduct(20, "Papa"), ProductUnit.SACO, 8000, 30);
    when(catalogItemRepository.findActiveItems(null, null)).thenReturn(List.of(tomate, papa));
    when(userProfileRepository.findByUserIdIn(Set.of(1))).thenReturn(List.of());

    List<SupplierResponse> suppliers = catalogService.listSuppliers(null, null);

    assertThat(suppliers).hasSize(1);
    SupplierResponse response = suppliers.get(0);
    assertThat(response.supplierId()).isEqualTo(1);
    assertThat(response.name()).isEqualTo("Verduras SPA");
    assertThat(response.contact()).isEqualTo("ventas@verduras.cl");
    assertThat(response.products()).extracting(SupplierProductResponse::productName).containsExactly("Tomate", "Papa");
    assertThat(response.products()).extracting(SupplierProductResponse::price).containsExactly(1500, 8000);
    assertThat(response.products()).extracting(SupplierProductResponse::stock).containsExactly(100, 30);
  }

  @Test
  void listSuppliers_defaultsToNoDeliveryAndEmptyAvailability_whenSupplierHasNoProfile() {
    UserModel supplier = supplier(1, "Verduras SPA", "ventas@verduras.cl");
    when(catalogItemRepository.findActiveItems(null, null)).thenReturn(List.of(item(supplier, masterProduct(10, "Tomate"), ProductUnit.KILOGRAMO, 1500, 100)));
    when(userProfileRepository.findByUserIdIn(Set.of(1))).thenReturn(List.of());

    SupplierResponse response = catalogService.listSuppliers(null, null).get(0);

    assertThat(response.delivery()).isFalse();
    assertThat(response.address()).isNull();
    assertThat(response.phone()).isNull();
    assertThat(response.contactName()).isNull();
    assertThat(response.availability()).isEqualTo(WeeklyAvailability.empty());
  }

  @Test
  void listSuppliers_usesTheSupplierProfile_whenItExists() {
    UserModel supplier = supplier(1, "Verduras SPA", "ventas@verduras.cl");
    when(catalogItemRepository.findActiveItems(null, null)).thenReturn(List.of(item(supplier, masterProduct(10, "Tomate"), ProductUnit.KILOGRAMO, 1500, 100)));
    UserProfileModel profile = new UserProfileModel();
    profile.setUserId(1);
    profile.setDelivery(true);
    profile.setAddress("Camino Real 456");
    profile.setPhone("+56933333333");
    profile.setContactName("Carla");
    profile.setAvailability(new WeeklyAvailability(List.of(TimeSlot.AM), null, null, null, null, null, null));
    when(userProfileRepository.findByUserIdIn(Set.of(1))).thenReturn(List.of(profile));

    SupplierResponse response = catalogService.listSuppliers(null, null).get(0);

    assertThat(response.delivery()).isTrue();
    assertThat(response.address()).isEqualTo("Camino Real 456");
    assertThat(response.phone()).isEqualTo("+56933333333");
    assertThat(response.contactName()).isEqualTo("Carla");
    assertThat(response.availability().monday()).containsExactly(TimeSlot.AM);
    assertThat(response.availability().tuesday()).isEmpty();
  }

  @Test
  void listSuppliers_preservesRepositoryOrder_forMultipleSuppliers() {
    UserModel supplierB = supplier(2, "Frutas B", "b@agrolink.cl");
    UserModel supplierA = supplier(1, "Verduras A", "a@agrolink.cl");
    MasterProductModel masterProduct = masterProduct(10, "Tomate");
    when(catalogItemRepository.findActiveItems(null, null)).thenReturn(List.of(
        item(supplierB, masterProduct, ProductUnit.KILOGRAMO, 1000, 10),
        item(supplierA, masterProduct, ProductUnit.KILOGRAMO, 900, 5)));
    when(userProfileRepository.findByUserIdIn(Set.of(2, 1))).thenReturn(List.of());

    List<SupplierResponse> suppliers = catalogService.listSuppliers(null, null);

    assertThat(suppliers).extracting(SupplierResponse::supplierId).containsExactly(2, 1);
  }

  private static UserModel supplier(Integer id, String name, String email) {
    UserModel model = new UserModel();
    model.setId(id);
    model.setName(name);
    model.setEmail(email);
    return model;
  }

  private static MasterProductModel masterProduct(Integer id, String name) {
    MasterProductModel model = new MasterProductModel();
    model.setId(id);
    model.setName(name);
    return model;
  }

  private static CatalogItemModel item(UserModel supplier, MasterProductModel masterProduct, ProductUnit unit, Integer price, Integer stock) {
    CatalogItemModel item = new CatalogItemModel();
    item.setSupplier(supplier);
    item.setMasterProduct(masterProduct);
    item.setUnit(unit);
    item.setPricePerUnit(price);
    item.setAvailableQuantity(stock);
    return item;
  }
}

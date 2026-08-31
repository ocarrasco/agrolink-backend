package com.agrolink.repositories;

import com.agrolink.model.CatalogItemModel;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ICatalogItemRepository extends JpaRepository<CatalogItemModel, Integer> {

  @EntityGraph(attributePaths = "masterProduct")
  List<CatalogItemModel> findBySupplierIdOrderByIdAsc(Integer supplierId);

  @EntityGraph(attributePaths = {"supplier", "masterProduct"})
  List<CatalogItemModel> findByIdIn(Collection<Integer> ids);

  /** A supplier's offerings for a set of master products (active + inactive). */
  @EntityGraph(attributePaths = {"supplier", "masterProduct"})
  List<CatalogItemModel> findBySupplierIdAndMasterProductIdIn(Integer supplierId, Collection<Integer> masterProductIds);

  @EntityGraph(attributePaths = "masterProduct")
  Optional<CatalogItemModel> findByIdAndSupplierId(Integer id, Integer supplierId);

  boolean existsBySupplierIdAndMasterProductId(Integer supplierId, Integer masterProductId);

  boolean existsBySupplierId(Integer supplierId);

  /**
   * Active catalog items (item + master product both active) for the retailer-facing catalog,
   * optionally filtered by master product id or product-name substring.
   */
  @EntityGraph(attributePaths = {"supplier", "masterProduct"})
  @Query("""
      select c from CatalogItemModel c
      where c.active = true and c.masterProduct.active = true
        and (:masterProductId is null or c.masterProduct.id = :masterProductId)
        and (cast(:q as string) is null
             or lower(c.masterProduct.name) like lower(concat('%', cast(:q as string), '%')))
      order by c.supplier.name asc, c.masterProduct.name asc
      """)
  List<CatalogItemModel> findActiveItems(Integer masterProductId, String q);

}

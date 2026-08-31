package com.agrolink.model;

import com.agrolink.model.enums.ProductUnit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * A supplier's catalog entry for a {@link MasterProductModel}: a price and an available
 * quantity (a single number — no lots/batches). Managed by the owning SUPPLIER.
 */
@Entity
@Table(name = "catalog_item")
@Getter
@Setter
@NoArgsConstructor
public class CatalogItemModel {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "supplier_id", nullable = false)
  private UserModel supplier;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "master_product_id", nullable = false)
  private MasterProductModel masterProduct;

  @Enumerated(EnumType.STRING)
  @Column(name = "unit", nullable = false)
  private ProductUnit unit;

  /** Price per unit in CLP (no fractions). */
  @Column(name = "price_per_unit", nullable = false)
  private Integer pricePerUnit;

  /** Whole units on offer (no fractional stock). */
  @Column(name = "available_quantity", nullable = false)
  private Integer availableQuantity = 0;

  @Column(name = "active", nullable = false)
  private boolean active = true;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

}

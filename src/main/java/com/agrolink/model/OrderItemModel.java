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

/**
 * One line of an {@link OrderModel}. Product name / unit / price are snapshotted at order
 * time so the order still reads correctly if the catalog item later changes.
 */
@Entity
@Table(name = "order_item")
@Getter
@Setter
@NoArgsConstructor
public class OrderItemModel {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_id", nullable = false)
  private OrderModel order;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "catalog_item_id", nullable = false)
  private CatalogItemModel catalogItem;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "master_product_id", nullable = false)
  private MasterProductModel masterProduct;

  @Column(name = "product_name", nullable = false, length = 120)
  private String productName;

  @Enumerated(EnumType.STRING)
  @Column(name = "unit", nullable = false)
  private ProductUnit unit;

  /** Whole units of the catalog item (no fractional quantities). */
  @Column(name = "quantity", nullable = false)
  private Integer quantity;

  /** Snapshot of the catalog item's price per unit, in CLP. */
  @Column(name = "unit_price", nullable = false)
  private Integer unitPrice;

  /** {@code unitPrice * quantity}, in CLP. */
  @Column(name = "line_total", nullable = false)
  private Integer lineTotal;

}

package com.agrolink.model;

import com.agrolink.model.enums.OrderStatus;
import com.agrolink.model.enums.ShippingMethod;
import com.agrolink.model.enums.TimeSlot;
import com.agrolink.model.enums.TransportStatus;
import com.agrolink.model.enums.WeekDay;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A retailer's purchase order to a single supplier. Aggregate root: it owns its
 * {@link OrderItemModel} lines. {@code supplier_id} is kept on the header (denormalized from
 * the lines' catalog items) to enforce the one-supplier invariant and to query a supplier's
 * sales efficiently.
 */
@Entity
@Table(name = "purchase_order")
@Getter
@Setter
@NoArgsConstructor
public class OrderModel {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "retailer_id", nullable = false)
  private UserModel retailer;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "supplier_id", nullable = false)
  private UserModel supplier;

  /** Assigned platform carrier — only set once the retailer accepts one's interest. Null otherwise. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "carrier_id")
  private UserModel carrier;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private OrderStatus status;

  /**
   * Transport-leg sub-state — only set for {@code shippingMethod = PLATFORM_CARRIER} (null
   * otherwise). Drives the carrier open market + execution; see {@code transporte_carrier.md}.
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "transport_status", length = 20)
  private TransportStatus transportStatus;

  /** Order total in CLP, sum of the line totals. */
  @Column(name = "total", nullable = false)
  private Integer total = 0;

  @Column(name = "supplier_note", length = 500)
  private String supplierNote;

  /** How the order reaches the retailer. {@code PLATFORM_CARRIER} not usable yet — see {@code transporte_carrier.md}. */
  @Enumerated(EnumType.STRING)
  @Column(name = "shipping_method", length = 20, nullable = false)
  private ShippingMethod shippingMethod = ShippingMethod.PICKUP;

  /** Retailer's preferred delivery / pickup weekday. Nullable — transport is not modelled yet. */
  @Enumerated(EnumType.STRING)
  @Column(name = "delivery_day", length = 10)
  private WeekDay deliveryDay;

  /** Retailer's preferred delivery / pickup slot (AM/PM). Nullable. */
  @Enumerated(EnumType.STRING)
  @Column(name = "delivery_slot", length = 5)
  private TimeSlot deliverySlot;

  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<OrderItemModel> items = new ArrayList<>();

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  public void addItem(OrderItemModel item) {
    item.setOrder(this);
    items.add(item);
  }

}

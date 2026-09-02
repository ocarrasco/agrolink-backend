package com.agrolink.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.time.LocalDateTime;

/**
 * A carrier's expression of interest in an {@link OrderModel} awaiting platform-carrier transport
 * ({@code shippingMethod = PLATFORM_CARRIER}, status {@code CONFIRMED}, no carrier assigned yet).
 * The retailer picks one to assign — see {@code OrderService#acceptCarrier}.
 */
@Entity
@Table(name = "transport_interest")
@Getter
@Setter
@NoArgsConstructor
public class TransportInterestModel {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_id", nullable = false)
  private OrderModel order;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "carrier_id", nullable = false)
  private UserModel carrier;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

}

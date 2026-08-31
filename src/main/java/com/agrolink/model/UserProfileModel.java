package com.agrolink.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Per-user profile, shared by every non-admin role.
 * {@code availability}: the weekly windows when the user is available — to dispatch OR to be
 * picked up from (supplier) / to receive (retailer) / to transport (carrier). It is <b>independent
 * of {@code delivery}</b>: a supplier who doesn't deliver still declares when pickup is possible.
 * {@code delivery}: only meaningful for suppliers ("¿despacho propio?").
 * Shared-PK 1:1 with {@code platform_user} — kept as a plain {@code @Id Integer}, no JPA association.
 */
@Entity
@Table(name = "user_profile")
@Getter
@Setter
@NoArgsConstructor
public class UserProfileModel {

  @Id
  @Column(name = "user_id")
  private Integer userId;

  @Column(name = "delivery", nullable = false)
  private boolean delivery = false;

  /** Physical address (free text, scoped to the Valparaíso region). Used for pickup and transport routing. */
  @Column(name = "address")
  private String address;

  /** Contact phone number. */
  @Column(name = "phone")
  private String phone;

  /** Name of the contact person (pairs with {@link #phone}). */
  @Column(name = "contact_name", length = 120)
  private String contactName;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "availability", nullable = false)
  private WeeklyAvailability availability = WeeklyAvailability.empty();

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

}

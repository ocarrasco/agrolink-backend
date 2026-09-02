package com.agrolink.model.enums;

/**
 * Sub-state of a {@code PLATFORM_CARRIER} order's transport leg, kept on
 * {@code purchase_order.transport_status} (null for every other shipping method). The order's own
 * {@link OrderStatus} stays {@code CONFIRMED} until {@link #DELIVERED}, when it flips to
 * {@code FULFILLED}. See {@code transporte_carrier.md}.
 */
public enum TransportStatus {

  /** Order is CONFIRMED, no carrier assigned — open on the carrier market. */
  AWAITING_CARRIER,
  /** The retailer picked a carrier; waiting for pickup. */
  ASSIGNED,
  /** Carrier picked the goods up; on the way. */
  IN_TRANSIT,
  /** Carrier delivered; the order is now FULFILLED (terminal). */
  DELIVERED

}

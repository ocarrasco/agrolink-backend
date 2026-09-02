package com.agrolink.model.enums;

/**
 * How an order gets from the supplier to the retailer. For {@code PLATFORM_CARRIER} the transport
 * leg is tracked separately on {@code purchase_order.transport_status} — see
 * {@code transporte_carrier.md}.
 */
public enum ShippingMethod {

  /** Retailer picks up at the supplier's farm. */
  PICKUP,

  /** Supplier delivers with their own logistics (only if {@code user_profile.delivery = true}). */
  SUPPLIER_DELIVERY,

  /** An independent platform carrier picked from the open market carries it. */
  PLATFORM_CARRIER

}

package com.agrolink.model.enums;

/**
 * How an order gets from the supplier to the retailer.
 * <p>
 * {@code PLATFORM_CARRIER} is declared but not usable yet — see {@code transporte_carrier.md}.
 */
public enum ShippingMethod {

  /** Retailer picks up at the supplier's farm. */
  PICKUP,

  /** Supplier delivers with their own logistics (only if {@code user_profile.delivery = true}). */
  SUPPLIER_DELIVERY,

  /** Platform carrier — not available yet. */
  PLATFORM_CARRIER

}

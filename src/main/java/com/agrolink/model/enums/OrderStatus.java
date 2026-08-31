package com.agrolink.model.enums;

public enum OrderStatus {

  /** Retailer placed it; waiting for the supplier. */
  PLACED,
  /** Supplier accepted it; stock has been decremented. */
  CONFIRMED,
  /** Goods delivered (set manually for now; wired to transport in iteration 5). */
  FULFILLED,
  /** Supplier declined it (terminal). */
  REJECTED,
  /** Retailer cancelled it while still PLACED (terminal). */
  CANCELLED

}

package com.agrolink.repositories.projections;

/** A master product and how much a supplier has sold of it (FULFILLED orders). */
public interface ProductSales {

  Integer getProductId();

  String getProductName();

  long getAmount();

}

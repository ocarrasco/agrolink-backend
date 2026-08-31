package com.agrolink.repositories.projections;

public interface MonthlyFulfilled {

  int getYr();

  int getMo();

  long getAmount();

  long getOrderCount();

}

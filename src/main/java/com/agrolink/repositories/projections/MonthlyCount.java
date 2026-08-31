package com.agrolink.repositories.projections;

/** How many orders were placed in a given month, regardless of status. */
public interface MonthlyCount {

  int getYr();

  int getMo();

  long getOrderCount();

}

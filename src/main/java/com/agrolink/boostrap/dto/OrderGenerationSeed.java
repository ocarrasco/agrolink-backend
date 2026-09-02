package com.agrolink.boostrap.dto;

/**
 * Demo purchase orders to generate for a retailer: {@code ordersPerMonth} random orders for the
 * current month plus each of the {@code monthsBack} previous ones. See
 * {@code RetailerSeedService#generateOrders}.
 */
public record OrderGenerationSeed(int monthsBack, int ordersPerMonth) {

}

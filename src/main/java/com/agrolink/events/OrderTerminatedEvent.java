package com.agrolink.events;

import com.agrolink.model.enums.OrderStatus;

/**
 * Published by {@code OrderService} when an order reaches a terminal state that frees its
 * reserved stock — {@code CANCELLED} (retailer) or {@code REJECTED} (supplier).
 * <p>
 * Handled synchronously, within the publisher's transaction (see {@code OrderStockListener}).
 * A future notification listener can subscribe to the same event.
 */
public record OrderTerminatedEvent(Integer orderId, OrderStatus status) {

}

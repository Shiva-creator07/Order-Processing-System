package com.orderprocessing.order.events;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Published by inventory-service to "inventory.events" after attempting to reserve stock.
 * status is one of: RESERVED, FAILED
 * Enriched with customerId/totalAmount so payment-service doesn't need a lookup.
 */
public record InventoryResultEvent(
        UUID orderId,
        String customerId,
        String productId,
        BigDecimal totalAmount,
        String status,
        String reason
) {
}

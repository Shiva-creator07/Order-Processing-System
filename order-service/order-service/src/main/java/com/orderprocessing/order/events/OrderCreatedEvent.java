package com.orderprocessing.order.events;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Published by order-service to the "order.created" topic when a new order is placed.
 * Carries enough context for downstream consumers (inventory, notification) to act
 * without needing to call back into order-service.
 */
public record OrderCreatedEvent(
        UUID orderId,
        String customerId,
        String productId,
        Integer quantity,
        BigDecimal totalAmount
) {
}

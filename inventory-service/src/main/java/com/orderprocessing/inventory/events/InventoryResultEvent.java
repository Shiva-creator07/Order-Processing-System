package com.orderprocessing.inventory.events;

import java.math.BigDecimal;
import java.util.UUID;

public record InventoryResultEvent(
        UUID orderId,
        String customerId,
        String productId,
        BigDecimal totalAmount,
        String status,
        String reason
) {
}

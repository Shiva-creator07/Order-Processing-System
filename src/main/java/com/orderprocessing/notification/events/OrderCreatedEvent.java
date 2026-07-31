package com.orderprocessing.notification.events;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID orderId,
        String customerId,
        String productId,
        Integer quantity,
        BigDecimal totalAmount
) {
}

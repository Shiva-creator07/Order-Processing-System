package com.orderprocessing.notification.events;

import java.util.UUID;

public record PaymentResultEvent(
        UUID orderId,
        String status,
        String transactionId,
        String reason
) {
}

package com.orderprocessing.order.events;

import java.util.UUID;

/**
 * Published by payment-service to "payment.events" after simulating a payment attempt.
 * status is one of: COMPLETED, FAILED
 */
public record PaymentResultEvent(
        UUID orderId,
        String status,
        String transactionId,
        String reason
) {
}

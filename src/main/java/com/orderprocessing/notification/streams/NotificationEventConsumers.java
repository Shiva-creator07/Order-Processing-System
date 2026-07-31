package com.orderprocessing.notification.streams;

import com.orderprocessing.notification.events.InventoryResultEvent;
import com.orderprocessing.notification.events.OrderCreatedEvent;
import com.orderprocessing.notification.events.PaymentResultEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/**
 * Fans in from all three topics and "sends" a notification for each meaningful
 * state change. In a real system this would call an email/SMS provider (SES, Twilio);
 * here it's simulated with structured logging so the flow is visible end-to-end.
 */
@Slf4j
@Component
public class NotificationEventConsumers {

    @Bean
    public Consumer<OrderCreatedEvent> orderCreatedNotifier() {
        return event -> log.info(
                "[NOTIFY] customer={} -> \"Your order {} has been received and is being processed.\"",
                event.customerId(), event.orderId());
    }

    @Bean
    public Consumer<InventoryResultEvent> inventoryResultNotifier() {
        return event -> {
            if ("FAILED".equalsIgnoreCase(event.status())) {
                log.info(
                        "[NOTIFY] customer={} -> \"We're sorry, order {} could not be fulfilled: {}.\"",
                        event.customerId(), event.orderId(), event.reason());
            } else {
                log.info(
                        "[NOTIFY] customer={} -> \"Good news - items for order {} are reserved and payment is being processed.\"",
                        event.customerId(), event.orderId());
            }
        };
    }

    @Bean
    public Consumer<PaymentResultEvent> paymentResultNotifier() {
        return event -> {
            if ("COMPLETED".equalsIgnoreCase(event.status())) {
                log.info(
                        "[NOTIFY] order={} -> \"Payment confirmed (txn {}). Your order is on its way!\"",
                        event.orderId(), event.transactionId());
            } else {
                log.info(
                        "[NOTIFY] order={} -> \"Payment failed: {}. Please update your payment method.\"",
                        event.orderId(), event.reason());
            }
        };
    }
}

package com.orderprocessing.order.streams;

import com.orderprocessing.order.events.InventoryResultEvent;
import com.orderprocessing.order.events.PaymentResultEvent;
import com.orderprocessing.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/**
 * Function-style consumers wired via spring.cloud.function.definition.
 * These close the saga loop: order-service reacts to the outcomes of the
 * downstream services it doesn't directly control.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumers {

    private final OrderService orderService;

    @Bean
    public Consumer<InventoryResultEvent> inventoryResultConsumer() {
        return event -> {
            log.info("Received inventory result: {}", event);
            boolean reserved = "RESERVED".equalsIgnoreCase(event.status());
            orderService.updateStatusFromInventory(event.orderId(), reserved, event.reason());
        };
    }

    @Bean
    public Consumer<PaymentResultEvent> paymentResultConsumer() {
        return event -> {
            log.info("Received payment result: {}", event);
            boolean completed = "COMPLETED".equalsIgnoreCase(event.status());
            orderService.updateStatusFromPayment(event.orderId(), completed, event.transactionId(), event.reason());
        };
    }
}

package com.orderprocessing.payment.streams;

import com.orderprocessing.payment.events.InventoryResultEvent;
import com.orderprocessing.payment.events.PaymentResultEvent;
import com.orderprocessing.payment.model.PaymentTransaction;
import com.orderprocessing.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

/**
 * Consumes inventory.events. Only attempts payment when inventory was RESERVED -
 * there's no point charging a customer for stock that isn't available.
 * Returning null from a Spring Cloud Stream Function skips publishing to the
 * output binding, which is how we "swallow" the FAILED-inventory case here.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventProcessor {

    private final PaymentRepository paymentRepository;

    @Value("${payment.simulated-failure-rate:0.1}")
    private double simulatedFailureRate;

    @Bean
    public Function<InventoryResultEvent, PaymentResultEvent> paymentProcessor() {
        return event -> {
            if (!"RESERVED".equalsIgnoreCase(event.status())) {
                log.info("Skipping payment for order {} - inventory status was {}", event.orderId(), event.status());
                return null;
            }
            return processPayment(event);
        };
    }

    @Transactional
    protected PaymentResultEvent processPayment(InventoryResultEvent event) {
        boolean success = ThreadLocalRandom.current().nextDouble() > simulatedFailureRate;
        String transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        PaymentTransaction transaction = PaymentTransaction.builder()
                .orderId(event.orderId())
                .customerId(event.customerId())
                .amount(event.totalAmount())
                .status(success ? "COMPLETED" : "FAILED")
                .failureReason(success ? null : "Simulated gateway decline")
                .build();

        paymentRepository.save(transaction);

        log.info("Payment {} for order {}: transactionId={}, amount={}",
                success ? "COMPLETED" : "FAILED", event.orderId(), transactionId, event.totalAmount());

        return new PaymentResultEvent(
                event.orderId(),
                success ? "COMPLETED" : "FAILED",
                transactionId,
                success ? null : "Simulated gateway decline"
        );
    }
}

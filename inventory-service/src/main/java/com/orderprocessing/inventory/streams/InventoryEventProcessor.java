package com.orderprocessing.inventory.streams;

import java.util.function.Function;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.orderprocessing.inventory.events.InventoryResultEvent;
import com.orderprocessing.inventory.events.OrderCreatedEvent;
import com.orderprocessing.inventory.repository.InventoryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Consumes order.created, attempts to reserve stock, and produces the outcome
 * onto inventory.events. This is the first step in the choreographed saga.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventProcessor {

    private final InventoryRepository inventoryRepository;

    @Bean
    public Function<OrderCreatedEvent, InventoryResultEvent> inventoryProcessor() {
        return event -> {
            log.info("Processing order.created for order {}: product={}, qty={}",
                    event.orderId(), event.productId(), event.quantity());

            return reserveStock(event);
        };
    }

    @Transactional
    protected InventoryResultEvent reserveStock(OrderCreatedEvent event) {
        return inventoryRepository.findByProductId(event.productId())
                        .map(item -> {
                    if (item.getAvailableQty() >= event.quantity()) {
                        item.setAvailableQty(item.getAvailableQty() - event.quantity());
                        item.setReservedQty(item.getReservedQty() + event.quantity());
                        inventoryRepository.save(item);

                        log.info("Reserved {} units of {} for order {}", event.quantity(), event.productId(), event.orderId());
                        return new InventoryResultEvent(
                                event.orderId(), event.customerId(), event.productId(),
                                event.totalAmount(), "RESERVED", "Stock reserved successfully"
                        );
                    } else {
                        log.warn("Insufficient stock for {} (order {}): available={}, requested={}",
                                event.productId(), event.orderId(), item.getAvailableQty(), event.quantity());
                        return new InventoryResultEvent(
                                event.orderId(), event.customerId(), event.productId(),
                                event.totalAmount(), "FAILED", "Insufficient stock"
                        );
                    }
                })
                .orElseGet(() -> {
                    log.warn("Unknown product {} for order {}", event.productId(), event.orderId());
                    return new InventoryResultEvent(
                            event.orderId(), event.customerId(), event.productId(),
                            event.totalAmount(), "FAILED", "Product not found"
                    );
                });
    }
}

package com.orderprocessing.order.service;

import com.orderprocessing.order.dto.CreateOrderRequest;
import com.orderprocessing.order.events.OrderCreatedEvent;
import com.orderprocessing.order.exception.OrderNotFoundException;
import com.orderprocessing.order.model.Order;
import com.orderprocessing.order.model.OrderStatus;
import com.orderprocessing.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final StreamBridge streamBridge;

    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        Order order = Order.builder()
                .customerId(request.customerId())
                .productId(request.productId())
                .quantity(request.quantity())
                .totalAmount(request.totalAmount())
                .status(OrderStatus.PENDING)
                .build();

        Order saved = orderRepository.save(order);

        OrderCreatedEvent event = new OrderCreatedEvent(
                saved.getId(),
                saved.getCustomerId(),
                saved.getProductId(),
                saved.getQuantity(),
                saved.getTotalAmount()
        );

        boolean sent = streamBridge.send("orderCreatedSupplier-out-0", event);
        log.info("Order {} created, event published to order.created: {}", saved.getId(), sent);

        return saved;
    }

    public Order getOrder(UUID id) {
        return orderRepository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @Transactional
    public void updateStatusFromInventory(UUID orderId, boolean reserved, String reason) {
        orderRepository.findById(orderId).ifPresentOrElse(order -> {
            order.setStatus(reserved ? OrderStatus.INVENTORY_RESERVED : OrderStatus.INVENTORY_FAILED);
            order.setStatusReason(reason);
            if (!reserved) {
                order.setStatus(OrderStatus.CANCELLED);
            }
            orderRepository.save(order);
            log.info("Order {} status updated from inventory result -> {}", orderId, order.getStatus());
        }, () -> log.warn("Received inventory result for unknown order {}", orderId));
    }

    @Transactional
    public void updateStatusFromPayment(UUID orderId, boolean completed, String transactionId, String reason) {
        orderRepository.findById(orderId).ifPresentOrElse(order -> {
            order.setStatus(completed ? OrderStatus.CONFIRMED : OrderStatus.PAYMENT_FAILED);
            order.setStatusReason(completed ? "Payment completed: " + transactionId : reason);
            orderRepository.save(order);
            log.info("Order {} status updated from payment result -> {}", orderId, order.getStatus());
        }, () -> log.warn("Received payment result for unknown order {}", orderId));
    }
}

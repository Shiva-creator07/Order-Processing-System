package com.orderprocessing.order.dto;

import com.orderprocessing.order.model.Order;
import com.orderprocessing.order.model.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String customerId,
        String productId,
        Integer quantity,
        BigDecimal totalAmount,
        OrderStatus status,
        String statusReason,
        Instant createdAt,
        Instant updatedAt
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getProductId(),
                order.getQuantity(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getStatusReason(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}

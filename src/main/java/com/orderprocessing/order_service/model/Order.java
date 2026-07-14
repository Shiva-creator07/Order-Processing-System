package com.orderprocessing.order_service.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "orders")
public class Order {
    @Id
    private UUID id;
    private String customerId;
    private String productId;
    private Integer quantity;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private Instant createdAt;
    private Instant updatedAt;



public Order() {
    this.id = UUID.randomUUID();
    this.status = OrderStatus.PENDING;
    this.createdAt = Instant.now();
    this.updatedAt = Instant.now();
    }
    public Order(String customerId, String productId, Integer quantity, BigDecimal totalAmount) {
    this();
    this.customerId = customerId;
    this.productId = productId;
    this.quantity = quantity;
    this.totalAmount = totalAmount;
    }

    public String getCustomerId() {
    return customerId;
}

public void setCustomerId(String customerId) {
    this.customerId = customerId;
}

public String getProductId() {
    return productId;
}

public void setProductId(String productId) {
    this.productId = productId;
}

public Integer getQuantity() {
    return quantity;
}

public void setQuantity(Integer quantity) {
    this.quantity = quantity;
}

public BigDecimal getTotalAmount() {
    return totalAmount;
}

public void setTotalAmount(BigDecimal totalAmount) {
    this.totalAmount = totalAmount;
}

public OrderStatus getStatus() {
    return status;
}

public void setStatus(OrderStatus status) {
    this.status = status;
    this.updatedAt = Instant.now();
}

public Instant getCreatedAt() {
    return createdAt;
}

public Instant getUpdatedAt() {
    return updatedAt;
    }
}
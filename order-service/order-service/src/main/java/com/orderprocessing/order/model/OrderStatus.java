package com.orderprocessing.order.model;

public enum OrderStatus {
    PENDING,
    INVENTORY_RESERVED,
    INVENTORY_FAILED,
    PAYMENT_COMPLETED,
    PAYMENT_FAILED,
    CONFIRMED,
    CANCELLED
}

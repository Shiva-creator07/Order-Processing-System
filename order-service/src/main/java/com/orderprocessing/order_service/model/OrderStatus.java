package com.orderprocessing.order_service.model;

public enum OrderStatus {
    PENDING,
    INVENTORY_RESERVED,
    INVENTORY_FAILED,
    PAYMENT_CPMPLETED,
    PAYMENT_FAILED,
    CONFIRMED,
    CANCELLED,
}

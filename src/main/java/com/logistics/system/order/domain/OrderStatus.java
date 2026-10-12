package com.logistics.system.order.domain;

public enum OrderStatus {
    CREATED,
    CONFIRMED,
    STOCK_RESERVED,
    PROCESSING,
    READY_TO_SHIP,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    FAILED,
    RETURNED
}

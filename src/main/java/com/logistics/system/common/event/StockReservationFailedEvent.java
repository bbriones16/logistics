package com.logistics.system.common.event;

public record StockReservationFailedEvent(Long orderId, String failedSku, String reason) {}

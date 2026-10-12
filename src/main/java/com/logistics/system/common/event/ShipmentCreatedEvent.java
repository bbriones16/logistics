package com.logistics.system.common.event;

public record ShipmentCreatedEvent(Long shipmentId, Long orderId) {}

package com.logistics.system.shipment.domain;

public enum ShipmentStatus {
    CREATED,
    PROCESSING,
    READY_FOR_PICKUP,
    PICKED_UP,
    IN_TRANSIT,
    ARRIVED_AT_HUB,
    OUT_FOR_DELIVERY,
    DELIVERED,
    FAILED,
    CANCELLED
}

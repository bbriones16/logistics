package com.logistics.system.common.event;

import com.logistics.system.shipment.domain.ShipmentStatus;

public record ShipmentStatusChangedEvent(Long shipmentId, ShipmentStatus status, String location, String notes) {}

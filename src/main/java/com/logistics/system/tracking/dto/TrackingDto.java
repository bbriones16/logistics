package com.logistics.system.tracking.dto;

import com.logistics.system.shipment.domain.ShipmentStatus;

import java.time.Instant;
import java.util.List;

public class TrackingDto {

    public record EventResponse(
            Long id,
            ShipmentStatus status,
            String location,
            String notes,
            Instant occurredAt
    ) {}

    public record TrackingResponse(
            Long shipmentId,
            String trackingNumber,
            ShipmentStatus currentStatus,
            List<EventResponse> events
    ) {}
}

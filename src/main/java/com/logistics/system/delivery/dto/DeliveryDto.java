package com.logistics.system.delivery.dto;

import com.logistics.system.delivery.domain.DeliveryStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public class DeliveryDto {

    public record CreateRequest(Long shipmentId, Long driverId, Long vehicleId,
                                LocalDate scheduledDate, BigDecimal deliveryCost, String notes) {}

    public record UpdateStatusRequest(DeliveryStatus status, String proofOfDelivery) {}

    public record Response(Long id, Long shipmentId, Long driverId, String driverName,
                           Long vehicleId, String vehiclePlate, DeliveryStatus status,
                           LocalDate scheduledDate, BigDecimal deliveryCost, String notes, Instant createdAt) {}
}

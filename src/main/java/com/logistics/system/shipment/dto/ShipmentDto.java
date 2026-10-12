package com.logistics.system.shipment.dto;

import com.logistics.system.shipment.domain.ShipmentMedium;
import com.logistics.system.shipment.domain.ShipmentStatus;
import com.logistics.system.shipment.domain.ShipmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public class ShipmentDto {

    public record CreateRequest(
            @NotNull ShipmentType shipmentType,
            Long orderId,                   // required for FULFILLMENT, null for STANDALONE
            @NotNull Long carrierId,
            @NotNull ShipmentMedium medium,
            String trackingNumber,          // required for third-party carriers, null for company-owned
            String cargoDescription,        // required for STANDALONE
            Integer quantity,
            BigDecimal weightKg,
            BigDecimal freightCost,
            @NotBlank String shipToAddress,
            LocalDate estimatedDeliveryDate,
            String notes
    ) {}

    public record UpdateStatusRequest(
            @NotNull ShipmentStatus status,
            String proofOfDelivery          // required when status is DELIVERED
    ) {}

    public record Response(
            Long id,
            ShipmentType shipmentType,
            Long orderId,
            Long carrierId,
            String carrierName,
            ShipmentMedium medium,
            String trackingNumber,
            ShipmentStatus status,
            String cargoDescription,
            Integer quantity,
            BigDecimal weightKg,
            BigDecimal freightCost,
            String shipToAddress,
            LocalDate estimatedDeliveryDate,
            LocalDate actualDeliveryDate,
            String proofOfDelivery,
            String notes,
            Instant createdAt
    ) {}
}

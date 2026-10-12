package com.logistics.system.delivery.dto;

import com.logistics.system.delivery.domain.VehicleStatus;
import com.logistics.system.delivery.domain.VehicleType;

import java.math.BigDecimal;
import java.time.Instant;

public class VehicleDto {

    public record CreateRequest(String plateNumber, VehicleType vehicleType, BigDecimal capacityKg) {}

    public record Response(Long id, String plateNumber, VehicleType vehicleType,
                           BigDecimal capacityKg, VehicleStatus status, Instant createdAt) {}
}

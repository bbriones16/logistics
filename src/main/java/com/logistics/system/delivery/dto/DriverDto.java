package com.logistics.system.delivery.dto;

import com.logistics.system.delivery.domain.DriverStatus;

import java.time.Instant;

public class DriverDto {

    public record CreateRequest(String name, String licenseNumber, String phone) {}

    public record Response(Long id, String name, String licenseNumber, String phone,
                           DriverStatus status, Instant createdAt) {}
}

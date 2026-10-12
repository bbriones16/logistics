package com.logistics.system.warehouse.dto;

import jakarta.validation.constraints.NotBlank;

public class WarehouseDto {

    public record CreateRequest(
            @NotBlank String code,
            @NotBlank String name,
            String address
    ) {}

    public record Response(
            Long id,
            String code,
            String name,
            String address,
            boolean active
    ) {}
}

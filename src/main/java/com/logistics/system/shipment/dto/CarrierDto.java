package com.logistics.system.shipment.dto;

import com.logistics.system.shipment.domain.ShipmentMedium;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CarrierDto {

    public record CreateRequest(
            @NotBlank String code,
            @NotBlank String name,
            @NotNull ShipmentMedium medium,
            boolean companyOwned,
            String contactName,
            String contactPhone
    ) {}

    public record Response(
            Long id,
            String code,
            String name,
            ShipmentMedium medium,
            boolean companyOwned,
            String contactName,
            String contactPhone,
            boolean active
    ) {}
}

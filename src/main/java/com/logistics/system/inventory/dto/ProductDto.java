package com.logistics.system.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class ProductDto {

    public record CreateRequest(
            @NotBlank String sku,
            @NotBlank String name,
            String description,
            @NotBlank String unitOfMeasure,
            @Min(0) int lowStockThreshold
    ) {}

    public record Response(
            Long id,
            String sku,
            String name,
            String description,
            String unitOfMeasure,
            int lowStockThreshold,
            boolean active
    ) {}
}

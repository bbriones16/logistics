package com.logistics.system.inventory.dto;

import com.logistics.system.inventory.domain.MovementType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public class InventoryDto {

    public record ReceiveRequest(
            @NotNull Long productId,
            @NotNull Long warehouseId,
            @Min(1) int quantity,
            String notes
    ) {}

    public record ReserveRequest(
            @NotNull Long productId,
            @NotNull Long warehouseId,
            @Min(1) int quantity,
            @NotNull Long orderId
    ) {}

    public record PickRequest(
            @NotNull Long productId,
            @NotNull Long warehouseId,
            @Min(1) int quantity,
            @NotNull Long orderId
    ) {}

    public record PackRequest(
            @NotNull Long productId,
            @NotNull Long warehouseId,
            @Min(1) int quantity,
            @NotNull Long orderId
    ) {}

    public record StockResponse(
            Long id,
            Long productId,
            String productSku,
            String productName,
            Long warehouseId,
            String warehouseName,
            int quantityOnHand,
            int quantityReserved,
            int availableQuantity
    ) {}

    public record MovementResponse(
            Long id,
            Long productId,
            String productSku,
            Long warehouseId,
            MovementType movementType,
            int quantity,
            Long referenceId,
            String notes,
            Instant createdAt
    ) {}
}

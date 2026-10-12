package com.logistics.system.order.dto;

import com.logistics.system.order.domain.OrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class OrderDto {

    public record ItemRequest(
            @NotBlank String sku,
            @NotBlank String productName,
            @Min(1) int quantity,
            @NotNull BigDecimal unitPrice
    ) {}

    public record CreateRequest(
            @NotNull Long customerId,
            @NotEmpty @Valid List<ItemRequest> items,
            String notes
    ) {}

    public record UpdateStatusRequest(
            @NotNull OrderStatus status
    ) {}

    public record ItemResponse(
            Long id,
            String sku,
            String productName,
            int quantity,
            BigDecimal unitPrice
    ) {}

    public record Response(
            Long id,
            Long customerId,
            String customerName,
            OrderStatus status,
            Instant createdAt,
            String notes,
            String failureReason,
            List<ItemResponse> items
    ) {}
}

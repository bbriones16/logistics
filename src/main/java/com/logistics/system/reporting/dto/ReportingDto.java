package com.logistics.system.reporting.dto;

import com.logistics.system.inventory.dto.InventoryDto;

import java.util.List;
import java.util.Map;

public class ReportingDto {

    public record OrderSummary(
            long total,
            long createdToday,
            Map<String, Long> byStatus
    ) {}

    public record ShipmentSummary(
            long total,
            Map<String, Long> byStatus
    ) {}

    public record DeliverySummary(
            long total,
            long scheduledToday,
            Map<String, Long> byStatus
    ) {}

    public record InventorySummary(
            long totalProducts,
            long totalWarehouses,
            long totalStockOnHand,
            long lowStockItemCount
    ) {}

    public record LowStockReport(
            long count,
            List<InventoryDto.StockResponse> items
    ) {}
}

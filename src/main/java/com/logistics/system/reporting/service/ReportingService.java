package com.logistics.system.reporting.service;

import com.logistics.system.delivery.domain.DeliveryStatus;
import com.logistics.system.delivery.repository.DeliveryRepository;
import com.logistics.system.inventory.domain.Inventory;
import com.logistics.system.inventory.dto.InventoryDto;
import com.logistics.system.inventory.repository.InventoryRepository;
import com.logistics.system.order.domain.OrderStatus;
import com.logistics.system.order.repository.OrderRepository;
import com.logistics.system.reporting.dto.ReportingDto;
import com.logistics.system.shipment.domain.ShipmentStatus;
import com.logistics.system.shipment.repository.ShipmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class ReportingService {

    private final OrderRepository orderRepository;
    private final ShipmentRepository shipmentRepository;
    private final DeliveryRepository deliveryRepository;
    private final InventoryRepository inventoryRepository;

    public ReportingService(OrderRepository orderRepository, ShipmentRepository shipmentRepository,
                            DeliveryRepository deliveryRepository, InventoryRepository inventoryRepository) {
        this.orderRepository = orderRepository;
        this.shipmentRepository = shipmentRepository;
        this.deliveryRepository = deliveryRepository;
        this.inventoryRepository = inventoryRepository;
    }

    public ReportingDto.OrderSummary getOrderSummary() {
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (OrderStatus status : OrderStatus.values()) {
            byStatus.put(status.name(), orderRepository.countByStatus(status));
        }
        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
        long createdToday = orderRepository.countByCreatedAtAfter(Instant.now().truncatedTo(ChronoUnit.DAYS));
        return new ReportingDto.OrderSummary(total, createdToday, byStatus);
    }

    public ReportingDto.ShipmentSummary getShipmentSummary() {
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (ShipmentStatus status : ShipmentStatus.values()) {
            byStatus.put(status.name(), shipmentRepository.countByStatus(status));
        }
        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
        return new ReportingDto.ShipmentSummary(total, byStatus);
    }

    public ReportingDto.DeliverySummary getDeliverySummary() {
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (DeliveryStatus status : DeliveryStatus.values()) {
            byStatus.put(status.name(), deliveryRepository.countByStatus(status));
        }
        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
        long scheduledToday = deliveryRepository.countByScheduledDate(LocalDate.now());
        return new ReportingDto.DeliverySummary(total, scheduledToday, byStatus);
    }

    public ReportingDto.InventorySummary getInventorySummary() {
        long totalProducts = inventoryRepository.countDistinctProducts();
        long totalWarehouses = inventoryRepository.countDistinctWarehouses();
        long totalStockOnHand = inventoryRepository.sumTotalStockOnHand();
        long lowStockCount = inventoryRepository.findLowStock().size();
        return new ReportingDto.InventorySummary(totalProducts, totalWarehouses, totalStockOnHand, lowStockCount);
    }

    public ReportingDto.LowStockReport getLowStockReport() {
        List<InventoryDto.StockResponse> items = inventoryRepository.findLowStock()
                .stream().map(this::toStockResponse).toList();
        return new ReportingDto.LowStockReport(items.size(), items);
    }

    private InventoryDto.StockResponse toStockResponse(Inventory i) {
        return new InventoryDto.StockResponse(
                i.getId(),
                i.getProduct().getId(),
                i.getProduct().getSku(),
                i.getProduct().getName(),
                i.getWarehouse().getId(),
                i.getWarehouse().getName(),
                i.getQuantityOnHand(),
                i.getQuantityReserved(),
                i.getAvailableQuantity()
        );
    }
}

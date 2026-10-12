package com.logistics.system.reporting.service;

import com.logistics.system.delivery.domain.DeliveryStatus;
import com.logistics.system.delivery.repository.DeliveryRepository;
import com.logistics.system.inventory.domain.Inventory;
import com.logistics.system.inventory.domain.Product;
import com.logistics.system.inventory.repository.InventoryRepository;
import com.logistics.system.order.domain.OrderStatus;
import com.logistics.system.order.repository.OrderRepository;
import com.logistics.system.reporting.dto.ReportingDto;
import com.logistics.system.shipment.domain.ShipmentStatus;
import com.logistics.system.shipment.repository.ShipmentRepository;
import com.logistics.system.warehouse.domain.Warehouse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportingServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private ShipmentRepository shipmentRepository;
    @Mock private DeliveryRepository deliveryRepository;
    @Mock private InventoryRepository inventoryRepository;

    @InjectMocks
    private ReportingService reportingService;

    @Test
    void getOrderSummary_returnsTotalAndByStatus() {
        for (OrderStatus s : OrderStatus.values()) {
            when(orderRepository.countByStatus(s)).thenReturn(s == OrderStatus.CREATED ? 3L : s == OrderStatus.DELIVERED ? 10L : 0L);
        }
        when(orderRepository.countByCreatedAtAfter(any())).thenReturn(2L);

        ReportingDto.OrderSummary result = reportingService.getOrderSummary();

        assertThat(result.byStatus().get("CREATED")).isEqualTo(3L);
        assertThat(result.byStatus().get("DELIVERED")).isEqualTo(10L);
        assertThat(result.createdToday()).isEqualTo(2L);
        assertThat(result.total()).isEqualTo(13L);
        assertThat(result.byStatus()).containsKeys("CREATED", "CONFIRMED", "STOCK_RESERVED",
                "PROCESSING", "READY_TO_SHIP", "SHIPPED", "DELIVERED", "CANCELLED", "FAILED", "RETURNED");
    }

    @Test
    void getShipmentSummary_returnsTotalAndByStatus() {
        for (ShipmentStatus s : ShipmentStatus.values()) {
            when(shipmentRepository.countByStatus(s)).thenReturn(s == ShipmentStatus.IN_TRANSIT ? 5L : s == ShipmentStatus.DELIVERED ? 20L : 0L);
        }

        ReportingDto.ShipmentSummary result = reportingService.getShipmentSummary();

        assertThat(result.byStatus().get("IN_TRANSIT")).isEqualTo(5L);
        assertThat(result.byStatus().get("DELIVERED")).isEqualTo(20L);
        assertThat(result.total()).isEqualTo(25L);
        assertThat(result.byStatus()).containsKeys("CREATED", "IN_TRANSIT", "DELIVERED", "FAILED", "CANCELLED");
    }

    @Test
    void getDeliverySummary_returnsTotalScheduledTodayAndByStatus() {
        for (DeliveryStatus s : DeliveryStatus.values()) {
            when(deliveryRepository.countByStatus(s)).thenReturn(s == DeliveryStatus.OUT_FOR_DELIVERY ? 4L : s == DeliveryStatus.DELIVERED ? 8L : 0L);
        }
        when(deliveryRepository.countByScheduledDate(LocalDate.now())).thenReturn(3L);

        ReportingDto.DeliverySummary result = reportingService.getDeliverySummary();

        assertThat(result.byStatus().get("OUT_FOR_DELIVERY")).isEqualTo(4L);
        assertThat(result.byStatus().get("DELIVERED")).isEqualTo(8L);
        assertThat(result.scheduledToday()).isEqualTo(3L);
        assertThat(result.total()).isEqualTo(12L);
        assertThat(result.byStatus()).containsKeys("ASSIGNED", "OUT_FOR_DELIVERY", "DELIVERED", "FAILED", "CANCELLED");
    }

    @Test
    void getInventorySummary_returnsAggregates() {
        when(inventoryRepository.countDistinctProducts()).thenReturn(15L);
        when(inventoryRepository.countDistinctWarehouses()).thenReturn(3L);
        when(inventoryRepository.sumTotalStockOnHand()).thenReturn(5000L);
        when(inventoryRepository.findLowStock()).thenReturn(List.of(buildInventory(), buildInventory()));

        ReportingDto.InventorySummary result = reportingService.getInventorySummary();

        assertThat(result.totalProducts()).isEqualTo(15L);
        assertThat(result.totalWarehouses()).isEqualTo(3L);
        assertThat(result.totalStockOnHand()).isEqualTo(5000L);
        assertThat(result.lowStockItemCount()).isEqualTo(2L);
    }

    @Test
    void getLowStockReport_returnsItemsWithCount() {
        Inventory inv = buildInventory();
        when(inventoryRepository.findLowStock()).thenReturn(List.of(inv));

        ReportingDto.LowStockReport result = reportingService.getLowStockReport();

        assertThat(result.count()).isEqualTo(1L);
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).productSku()).isEqualTo("SKU-001");
    }

    @Test
    void getLowStockReport_noLowStockItems_returnsEmptyList() {
        when(inventoryRepository.findLowStock()).thenReturn(List.of());

        ReportingDto.LowStockReport result = reportingService.getLowStockReport();

        assertThat(result.count()).isEqualTo(0L);
        assertThat(result.items()).isEmpty();
    }

    private Inventory buildInventory() {
        Product product = new Product();
        product.setSku("SKU-001");
        product.setName("Test Product");
        product.setLowStockThreshold(10);

        Warehouse warehouse = new Warehouse();
        warehouse.setName("Main Warehouse");

        Inventory inventory = new Inventory();
        inventory.setProduct(product);
        inventory.setWarehouse(warehouse);
        inventory.setQuantityOnHand(5);
        inventory.setQuantityReserved(0);
        return inventory;
    }
}

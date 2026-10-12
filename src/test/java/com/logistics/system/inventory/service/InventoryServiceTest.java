package com.logistics.system.inventory.service;

import com.logistics.system.common.exception.BusinessException;
import com.logistics.system.common.exception.ResourceNotFoundException;
import com.logistics.system.inventory.domain.Inventory;
import com.logistics.system.inventory.domain.Product;
import com.logistics.system.inventory.dto.InventoryDto;
import com.logistics.system.inventory.repository.InventoryRepository;
import com.logistics.system.inventory.repository.StockMovementRepository;
import com.logistics.system.warehouse.domain.Warehouse;
import com.logistics.system.warehouse.service.WarehouseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock private InventoryRepository inventoryRepository;
    @Mock private StockMovementRepository stockMovementRepository;
    @Mock private ProductService productService;
    @Mock private WarehouseService warehouseService;

    @InjectMocks
    private InventoryService inventoryService;

    private Product product;
    private Warehouse warehouse;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setSku("SKU-001");
        product.setName("Product A");
        product.setUnitOfMeasure("EACH");

        warehouse = new Warehouse();
        warehouse.setCode("WH-01");
        warehouse.setName("Main Warehouse");

        inventory = new Inventory();
        inventory.setProduct(product);
        inventory.setWarehouse(warehouse);
        inventory.setQuantityOnHand(100);
        inventory.setQuantityReserved(10);
    }

    @Test
    void receive_newInventory_createsAndAddsStock() {
        when(productService.getProduct(any())).thenReturn(product);
        when(warehouseService.getWarehouse(any())).thenReturn(warehouse);
        when(inventoryRepository.findByProductIdAndWarehouseId(any(), any())).thenReturn(Optional.empty());
        when(inventoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InventoryDto.StockResponse result = inventoryService.receive(
                new InventoryDto.ReceiveRequest(1L, 1L, 50, "Initial stock"));

        assertThat(result.quantityOnHand()).isEqualTo(50);
        verify(inventoryRepository).save(any());
        verify(stockMovementRepository).save(any());
    }

    @Test
    void receive_existingInventory_addsToExistingStock() {
        when(productService.getProduct(any())).thenReturn(product);
        when(warehouseService.getWarehouse(any())).thenReturn(warehouse);
        when(inventoryRepository.findByProductIdAndWarehouseId(any(), any())).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InventoryDto.StockResponse result = inventoryService.receive(
                new InventoryDto.ReceiveRequest(1L, 1L, 20, null));

        assertThat(result.quantityOnHand()).isEqualTo(120);
    }

    @Test
    void reserve_sufficientStock_reservesQuantity() {
        when(inventoryRepository.findByProductIdAndWarehouseId(1L, 1L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(productService.getProduct(any())).thenReturn(product);
        when(warehouseService.getWarehouse(any())).thenReturn(warehouse);

        InventoryDto.StockResponse result = inventoryService.reserve(1L, 1L, 10, 100L);

        assertThat(result.quantityReserved()).isEqualTo(20);
        assertThat(result.availableQuantity()).isEqualTo(80);
    }

    @Test
    void reserve_insufficientStock_throwsBusinessException() {
        when(inventoryRepository.findByProductIdAndWarehouseId(1L, 1L)).thenReturn(Optional.of(inventory));

        assertThatThrownBy(() -> inventoryService.reserve(1L, 1L, 200, 100L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    void reserve_noInventory_throwsResourceNotFoundException() {
        when(inventoryRepository.findByProductIdAndWarehouseId(1L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.reserve(1L, 1L, 5, 100L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void pick_sufficientReserved_reducesOnHandAndReserved() {
        when(inventoryRepository.findByProductIdAndWarehouseId(1L, 1L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(productService.getProduct(any())).thenReturn(product);
        when(warehouseService.getWarehouse(any())).thenReturn(warehouse);

        InventoryDto.StockResponse result = inventoryService.pick(
                new InventoryDto.PickRequest(1L, 1L, 5, 100L));

        assertThat(result.quantityOnHand()).isEqualTo(95);
        assertThat(result.quantityReserved()).isEqualTo(5);
    }

    @Test
    void pick_moreThanReserved_throwsBusinessException() {
        when(inventoryRepository.findByProductIdAndWarehouseId(1L, 1L)).thenReturn(Optional.of(inventory));

        assertThatThrownBy(() -> inventoryService.pick(new InventoryDto.PickRequest(1L, 1L, 50, 100L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("reserved");
    }

    @Test
    void validateReservation_sufficientStock_doesNotThrow() {
        when(inventoryRepository.findAll()).thenReturn(List.of(inventory));

        // available = 100 - 10 = 90, requesting 50 — should pass silently
        inventoryService.validateReservation("SKU-001", 50);
    }

    @Test
    void validateReservation_insufficientStock_throwsBusinessException() {
        when(inventoryRepository.findAll()).thenReturn(List.of(inventory));

        // available = 90, requesting 100
        assertThatThrownBy(() -> inventoryService.validateReservation("SKU-001", 100))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    void validateReservation_noInventoryForSku_throwsBusinessException() {
        when(inventoryRepository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> inventoryService.validateReservation("SKU-UNKNOWN", 1))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("No inventory record found for SKU");
    }

    @Test
    void validateReservation_exactAvailableQuantity_doesNotThrow() {
        when(inventoryRepository.findAll()).thenReturn(List.of(inventory));

        // available = 90, requesting exactly 90
        inventoryService.validateReservation("SKU-001", 90);
    }
}

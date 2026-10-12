package com.logistics.system.inventory.event;

import com.logistics.system.common.event.OrderCreatedEvent;
import com.logistics.system.common.event.StockReservationFailedEvent;
import com.logistics.system.common.event.StockReservedEvent;
import com.logistics.system.common.exception.BusinessException;
import com.logistics.system.inventory.domain.Inventory;
import com.logistics.system.inventory.domain.Product;
import com.logistics.system.inventory.repository.InventoryRepository;
import com.logistics.system.inventory.service.InventoryService;
import com.logistics.system.warehouse.domain.Warehouse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryEventListenerTest {

    @Mock private InventoryRepository inventoryRepository;
    @Mock private InventoryService inventoryService;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private InventoryEventListener listener;

    private Inventory inventory;

    @BeforeEach
    void setUp() throws Exception {
        Product product = new Product();
        setId(product, 1L);
        product.setSku("SKU-001");

        Warehouse warehouse = new Warehouse();
        setId(warehouse, 1L);

        inventory = new Inventory();
        inventory.setProduct(product);
        inventory.setWarehouse(warehouse);
        inventory.setQuantityOnHand(100);
        inventory.setQuantityReserved(0);
    }

    private void setId(Object target, Long value) throws Exception {
        Field field = target.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void onOrderCreated_singleItemValid_reservesAndPublishesStockReservedEvent() {
        when(inventoryRepository.findAll()).thenReturn(List.of(inventory));

        listener.onOrderCreated(new OrderCreatedEvent(1L, 10L, List.of(
                new OrderCreatedEvent.OrderItem("SKU-001", "Product A", 5, BigDecimal.TEN)
        )));

        verify(inventoryService).validateReservation("SKU-001", 5);
        verify(inventoryService).reserve(1L, 1L, 5, 1L);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(StockReservedEvent.class);
        assertThat(((StockReservedEvent) captor.getValue()).orderId()).isEqualTo(1L);
    }

    @Test
    void onOrderCreated_firstItemFails_publishesFailedEventWithCorrectSku() {
        doThrow(new BusinessException("INSUFFICIENT_STOCK", "Insufficient stock for SKU-001"))
                .when(inventoryService).validateReservation("SKU-001", 5);

        listener.onOrderCreated(new OrderCreatedEvent(1L, 10L, List.of(
                new OrderCreatedEvent.OrderItem("SKU-001", "Product A", 5, BigDecimal.TEN)
        )));

        verify(inventoryService, never()).reserve(any(), any(), anyInt(), any());

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(StockReservationFailedEvent.class);
        StockReservationFailedEvent failed = (StockReservationFailedEvent) captor.getValue();
        assertThat(failed.orderId()).isEqualTo(1L);
        assertThat(failed.failedSku()).isEqualTo("SKU-001");
    }

    @Test
    void onOrderCreated_secondItemFails_noPartialReservation() {
        // Phase 1: SKU-001 passes, SKU-002 fails — neither should be reserved
        doNothing().when(inventoryService).validateReservation("SKU-001", 5);
        doThrow(new BusinessException("INSUFFICIENT_STOCK", "Insufficient stock for SKU-002"))
                .when(inventoryService).validateReservation("SKU-002", 3);

        listener.onOrderCreated(new OrderCreatedEvent(1L, 10L, List.of(
                new OrderCreatedEvent.OrderItem("SKU-001", "Product A", 5, BigDecimal.TEN),
                new OrderCreatedEvent.OrderItem("SKU-002", "Product B", 3, BigDecimal.TEN)
        )));

        verify(inventoryService, never()).reserve(any(), any(), anyInt(), any());

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(captor.capture());
        StockReservationFailedEvent failed = (StockReservationFailedEvent) captor.getValue();
        assertThat(failed.failedSku()).isEqualTo("SKU-002");
        assertThat(failed.orderId()).isEqualTo(1L);
    }

    @Test
    void onOrderCreated_multipleItemsAllValid_reservesEachItem() throws Exception {
        Product product2 = new Product();
        setId(product2, 2L);
        product2.setSku("SKU-002");

        Warehouse warehouse2 = new Warehouse();
        setId(warehouse2, 1L);

        Inventory inventory2 = new Inventory();
        inventory2.setProduct(product2);
        inventory2.setWarehouse(warehouse2);
        inventory2.setQuantityOnHand(50);
        inventory2.setQuantityReserved(0);

        when(inventoryRepository.findAll()).thenReturn(List.of(inventory, inventory2));

        listener.onOrderCreated(new OrderCreatedEvent(1L, 10L, List.of(
                new OrderCreatedEvent.OrderItem("SKU-001", "Product A", 5, BigDecimal.TEN),
                new OrderCreatedEvent.OrderItem("SKU-002", "Product B", 3, BigDecimal.TEN)
        )));

        verify(inventoryService).reserve(1L, 1L, 5, 1L);
        verify(inventoryService).reserve(2L, 1L, 3, 1L);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(StockReservedEvent.class);
    }
}

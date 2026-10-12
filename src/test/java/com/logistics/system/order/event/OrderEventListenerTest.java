package com.logistics.system.order.event;

import com.logistics.system.common.event.ShipmentCreatedEvent;
import com.logistics.system.common.event.StockReservationFailedEvent;
import com.logistics.system.common.event.StockReservedEvent;
import com.logistics.system.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderEventListenerTest {

    @Mock private OrderService orderService;

    @InjectMocks
    private OrderEventListener listener;

    @Test
    void onStockReserved_callsMarkStockReserved() {
        listener.onStockReserved(new StockReservedEvent(42L));
        verify(orderService).markStockReserved(42L);
    }

    @Test
    void onStockReservationFailed_callsMarkFailedWithFormattedMessage() {
        listener.onStockReservationFailed(new StockReservationFailedEvent(42L, "SKU-001", "Insufficient stock"));
        verify(orderService).markFailed(42L, "Stock reservation failed for SKU SKU-001: Insufficient stock");
    }

    @Test
    void onShipmentCreated_callsMarkReadyToShip() {
        listener.onShipmentCreated(new ShipmentCreatedEvent(99L, 42L));
        verify(orderService).markReadyToShip(42L);
    }
}

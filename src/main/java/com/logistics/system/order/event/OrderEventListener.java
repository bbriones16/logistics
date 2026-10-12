package com.logistics.system.order.event;

import com.logistics.system.common.event.ShipmentCreatedEvent;
import com.logistics.system.common.event.StockReservationFailedEvent;
import com.logistics.system.common.event.StockReservedEvent;
import com.logistics.system.order.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    private final OrderService orderService;

    public OrderEventListener(OrderService orderService) {
        this.orderService = orderService;
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onShipmentCreated(ShipmentCreatedEvent event) {
        log.info("Shipment created event received: shipmentId={}, orderId={}", event.shipmentId(), event.orderId());
        orderService.markReadyToShip(event.orderId());
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onStockReserved(StockReservedEvent event) {
        log.info("Stock reserved event received: orderId={}", event.orderId());
        orderService.markStockReserved(event.orderId());
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onStockReservationFailed(StockReservationFailedEvent event) {
        log.info("Stock reservation failed event received: orderId={}, failedSku={}, reason={}",
                event.orderId(), event.failedSku(), event.reason());
        orderService.markFailed(event.orderId(), "Stock reservation failed for SKU " + event.failedSku() + ": " + event.reason());
    }
}

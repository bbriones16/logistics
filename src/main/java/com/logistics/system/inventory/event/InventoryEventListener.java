package com.logistics.system.inventory.event;

import com.logistics.system.common.event.OrderCreatedEvent;
import com.logistics.system.common.event.StockReservationFailedEvent;
import com.logistics.system.common.event.StockReservedEvent;
import com.logistics.system.inventory.repository.InventoryRepository;
import com.logistics.system.inventory.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class InventoryEventListener {

    private static final Logger log = LoggerFactory.getLogger(InventoryEventListener.class);

    private final InventoryRepository inventoryRepository;
    private final InventoryService inventoryService;
    private final ApplicationEventPublisher eventPublisher;

    public InventoryEventListener(InventoryRepository inventoryRepository,
                                  InventoryService inventoryService,
                                  ApplicationEventPublisher eventPublisher) {
        this.inventoryRepository = inventoryRepository;
        this.inventoryService = inventoryService;
        this.eventPublisher = eventPublisher;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("Order created event received: orderId={}, items={}", event.orderId(), event.items().size());

        // Phase 1 — validate all items, no DB writes
        for (OrderCreatedEvent.OrderItem item : event.items()) {
            try {
                inventoryService.validateReservation(item.sku(), item.quantity());
            } catch (Exception e) {
                log.warn("Stock validation failed: orderId={}, sku={}, reason={}", event.orderId(), item.sku(), e.getMessage());
                eventPublisher.publishEvent(new StockReservationFailedEvent(event.orderId(), item.sku(), e.getMessage()));
                return;
            }
        }

        // Phase 2 — all items validated, reserve all
        for (OrderCreatedEvent.OrderItem item : event.items()) {
            inventoryRepository.findAll().stream()
                    .filter(inv -> inv.getProduct().getSku().equals(item.sku()))
                    .findFirst()
                    .ifPresent(inv -> inventoryService.reserve(
                            inv.getProduct().getId(), inv.getWarehouse().getId(),
                            item.quantity(), event.orderId()));
        }

        log.info("All stock reserved for orderId={}", event.orderId());
        eventPublisher.publishEvent(new StockReservedEvent(event.orderId()));
    }
}

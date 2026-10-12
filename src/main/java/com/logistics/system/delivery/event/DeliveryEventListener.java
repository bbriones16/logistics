package com.logistics.system.delivery.event;

import com.logistics.system.common.event.DeliveryAssignedEvent;
import com.logistics.system.common.event.DeliveryCompletedEvent;
import com.logistics.system.common.event.DeliveryFailedEvent;
import com.logistics.system.common.event.ShipmentStatusChangedEvent;
import com.logistics.system.delivery.domain.DriverStatus;
import com.logistics.system.delivery.domain.VehicleStatus;
import com.logistics.system.delivery.repository.DeliveryRepository;
import com.logistics.system.shipment.domain.Shipment;
import com.logistics.system.shipment.domain.ShipmentStatus;
import com.logistics.system.shipment.repository.ShipmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class DeliveryEventListener {

    private static final Logger log = LoggerFactory.getLogger(DeliveryEventListener.class);

    private final ShipmentRepository shipmentRepository;
    private final DeliveryRepository deliveryRepository;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    public DeliveryEventListener(ShipmentRepository shipmentRepository, DeliveryRepository deliveryRepository,
                                 org.springframework.context.ApplicationEventPublisher eventPublisher) {
        this.shipmentRepository = shipmentRepository;
        this.deliveryRepository = deliveryRepository;
        this.eventPublisher = eventPublisher;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onDeliveryAssigned(DeliveryAssignedEvent event) {
        log.info("Delivery assigned: deliveryId={}, shipmentId={}", event.deliveryId(), event.shipmentId());

        Shipment shipment = getShipment(event.shipmentId());
        shipment.setStatus(ShipmentStatus.OUT_FOR_DELIVERY);
        shipmentRepository.save(shipment);
        eventPublisher.publishEvent(new ShipmentStatusChangedEvent(shipment.getId(), ShipmentStatus.OUT_FOR_DELIVERY, null, null));

        deliveryRepository.findById(event.deliveryId()).ifPresent(delivery -> {
            delivery.getDriver().setStatus(DriverStatus.ON_DELIVERY);
            delivery.getVehicle().setStatus(VehicleStatus.IN_USE);
        });
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onDeliveryCompleted(DeliveryCompletedEvent event) {
        log.info("Delivery completed: deliveryId={}, shipmentId={}", event.deliveryId(), event.shipmentId());

        Shipment shipment = getShipment(event.shipmentId());
        shipment.setStatus(ShipmentStatus.DELIVERED);
        shipment.setProofOfDelivery(event.proofOfDelivery());
        shipment.setActualDeliveryDate(LocalDate.now());
        shipmentRepository.save(shipment);
        eventPublisher.publishEvent(new ShipmentStatusChangedEvent(shipment.getId(), ShipmentStatus.DELIVERED, null, null));

        releaseDriverAndVehicle(event.deliveryId());
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onDeliveryFailed(DeliveryFailedEvent event) {
        log.info("Delivery failed/cancelled: deliveryId={}, shipmentId={}, reason={}",
                event.deliveryId(), event.shipmentId(), event.reason());

        Shipment shipment = getShipment(event.shipmentId());
        shipment.setStatus(ShipmentStatus.FAILED);
        shipmentRepository.save(shipment);
        eventPublisher.publishEvent(new ShipmentStatusChangedEvent(shipment.getId(), ShipmentStatus.FAILED, null, null));

        releaseDriverAndVehicle(event.deliveryId());
    }

    private void releaseDriverAndVehicle(Long deliveryId) {
        deliveryRepository.findById(deliveryId).ifPresent(delivery -> {
            delivery.getDriver().setStatus(DriverStatus.AVAILABLE);
            delivery.getVehicle().setStatus(VehicleStatus.AVAILABLE);
        });
    }

    private Shipment getShipment(Long shipmentId) {
        return shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new IllegalStateException("Shipment not found: " + shipmentId));
    }
}

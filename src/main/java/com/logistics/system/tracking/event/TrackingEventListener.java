package com.logistics.system.tracking.event;

import com.logistics.system.common.event.ShipmentStatusChangedEvent;
import com.logistics.system.shipment.repository.ShipmentRepository;
import com.logistics.system.tracking.domain.TrackingEvent;
import com.logistics.system.tracking.repository.TrackingEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class TrackingEventListener {

    private static final Logger log = LoggerFactory.getLogger(TrackingEventListener.class);

    private final TrackingEventRepository trackingEventRepository;
    private final ShipmentRepository shipmentRepository;

    public TrackingEventListener(TrackingEventRepository trackingEventRepository,
                                 ShipmentRepository shipmentRepository) {
        this.trackingEventRepository = trackingEventRepository;
        this.shipmentRepository = shipmentRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onShipmentStatusChanged(ShipmentStatusChangedEvent event) {
        shipmentRepository.findById(event.shipmentId()).ifPresent(shipment -> {
            TrackingEvent trackingEvent = new TrackingEvent();
            trackingEvent.setShipmentId(shipment.getId());
            trackingEvent.setTrackingNumber(shipment.getTrackingNumber());
            trackingEvent.setStatus(event.status());
            trackingEvent.setLocation(event.location());
            trackingEvent.setNotes(event.notes());
            trackingEventRepository.save(trackingEvent);
            log.info("Tracking event recorded: shipmentId={}, status={}", shipment.getId(), event.status());
        });
    }
}

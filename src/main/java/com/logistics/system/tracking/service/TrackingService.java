package com.logistics.system.tracking.service;

import com.logistics.system.common.exception.ResourceNotFoundException;
import com.logistics.system.shipment.repository.ShipmentRepository;
import com.logistics.system.tracking.domain.TrackingEvent;
import com.logistics.system.tracking.dto.TrackingDto;
import com.logistics.system.tracking.repository.TrackingEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class TrackingService {

    private final TrackingEventRepository trackingEventRepository;
    private final ShipmentRepository shipmentRepository;

    public TrackingService(TrackingEventRepository trackingEventRepository, ShipmentRepository shipmentRepository) {
        this.trackingEventRepository = trackingEventRepository;
        this.shipmentRepository = shipmentRepository;
    }

    public TrackingDto.TrackingResponse getByTrackingNumber(String trackingNumber) {
        List<TrackingEvent> events = trackingEventRepository.findByTrackingNumberOrderByOccurredAtAsc(trackingNumber);
        if (events.isEmpty()) {
            throw new ResourceNotFoundException("Shipment with tracking number " + trackingNumber, 0L);
        }
        TrackingEvent latest = events.getLast();
        return toResponse(latest.getShipmentId(), trackingNumber, latest, events);
    }

    public TrackingDto.TrackingResponse getByShipmentId(Long shipmentId) {
        shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", shipmentId));
        List<TrackingEvent> events = trackingEventRepository.findByShipmentIdOrderByOccurredAtAsc(shipmentId);
        if (events.isEmpty()) {
            throw new ResourceNotFoundException("Tracking events for shipment", shipmentId);
        }
        TrackingEvent latest = events.getLast();
        return toResponse(shipmentId, latest.getTrackingNumber(), latest, events);
    }

    private TrackingDto.TrackingResponse toResponse(Long shipmentId, String trackingNumber,
                                                     TrackingEvent latest, List<TrackingEvent> events) {
        List<TrackingDto.EventResponse> eventResponses = events.stream()
                .map(e -> new TrackingDto.EventResponse(e.getId(), e.getStatus(), e.getLocation(), e.getNotes(), e.getOccurredAt()))
                .toList();
        return new TrackingDto.TrackingResponse(shipmentId, trackingNumber, latest.getStatus(), eventResponses);
    }
}

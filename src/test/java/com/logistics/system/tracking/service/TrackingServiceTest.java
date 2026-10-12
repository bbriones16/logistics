package com.logistics.system.tracking.service;

import com.logistics.system.common.exception.ResourceNotFoundException;
import com.logistics.system.shipment.domain.Shipment;
import com.logistics.system.shipment.domain.ShipmentStatus;
import com.logistics.system.shipment.repository.ShipmentRepository;
import com.logistics.system.tracking.domain.TrackingEvent;
import com.logistics.system.tracking.dto.TrackingDto;
import com.logistics.system.tracking.repository.TrackingEventRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackingServiceTest {

    @Mock private TrackingEventRepository trackingEventRepository;
    @Mock private ShipmentRepository shipmentRepository;

    @InjectMocks
    private TrackingService trackingService;

    private TrackingEvent createdEvent;
    private TrackingEvent inTransitEvent;

    @BeforeEach
    void setUp() {
        createdEvent = buildEvent(1L, "TRK-001", ShipmentStatus.CREATED);
        inTransitEvent = buildEvent(1L, "TRK-001", ShipmentStatus.IN_TRANSIT);
    }

    @Test
    void getByTrackingNumber_returnsEventsInOrder() {
        when(trackingEventRepository.findByTrackingNumberOrderByOccurredAtAsc("TRK-001"))
                .thenReturn(List.of(createdEvent, inTransitEvent));

        TrackingDto.TrackingResponse result = trackingService.getByTrackingNumber("TRK-001");

        assertThat(result.trackingNumber()).isEqualTo("TRK-001");
        assertThat(result.currentStatus()).isEqualTo(ShipmentStatus.IN_TRANSIT);
        assertThat(result.events()).hasSize(2);
        assertThat(result.events().get(0).status()).isEqualTo(ShipmentStatus.CREATED);
    }

    @Test
    void getByTrackingNumber_noEvents_throwsResourceNotFoundException() {
        when(trackingEventRepository.findByTrackingNumberOrderByOccurredAtAsc("UNKNOWN"))
                .thenReturn(List.of());

        assertThatThrownBy(() -> trackingService.getByTrackingNumber("UNKNOWN"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getByShipmentId_returnsEventsInOrder() {
        Shipment shipment = new Shipment();
        when(shipmentRepository.findById(1L)).thenReturn(Optional.of(shipment));
        when(trackingEventRepository.findByShipmentIdOrderByOccurredAtAsc(1L))
                .thenReturn(List.of(createdEvent, inTransitEvent));

        TrackingDto.TrackingResponse result = trackingService.getByShipmentId(1L);

        assertThat(result.shipmentId()).isEqualTo(1L);
        assertThat(result.currentStatus()).isEqualTo(ShipmentStatus.IN_TRANSIT);
        assertThat(result.events()).hasSize(2);
    }

    @Test
    void getByShipmentId_shipmentNotFound_throwsResourceNotFoundException() {
        when(shipmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trackingService.getByShipmentId(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getByShipmentId_noEvents_throwsResourceNotFoundException() {
        when(shipmentRepository.findById(1L)).thenReturn(Optional.of(new Shipment()));
        when(trackingEventRepository.findByShipmentIdOrderByOccurredAtAsc(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> trackingService.getByShipmentId(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getByTrackingNumber_singleEvent_currentStatusMatchesThatEvent() {
        when(trackingEventRepository.findByTrackingNumberOrderByOccurredAtAsc("TRK-001"))
                .thenReturn(List.of(createdEvent));

        TrackingDto.TrackingResponse result = trackingService.getByTrackingNumber("TRK-001");

        assertThat(result.currentStatus()).isEqualTo(ShipmentStatus.CREATED);
        assertThat(result.events()).hasSize(1);
    }

    private TrackingEvent buildEvent(Long shipmentId, String trackingNumber, ShipmentStatus status) {
        TrackingEvent e = new TrackingEvent();
        e.setShipmentId(shipmentId);
        e.setTrackingNumber(trackingNumber);
        e.setStatus(status);
        return e;
    }
}

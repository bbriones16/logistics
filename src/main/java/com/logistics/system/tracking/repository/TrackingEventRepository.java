package com.logistics.system.tracking.repository;

import com.logistics.system.tracking.domain.TrackingEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrackingEventRepository extends JpaRepository<TrackingEvent, Long> {
    List<TrackingEvent> findByShipmentIdOrderByOccurredAtAsc(Long shipmentId);
    List<TrackingEvent> findByTrackingNumberOrderByOccurredAtAsc(String trackingNumber);
}

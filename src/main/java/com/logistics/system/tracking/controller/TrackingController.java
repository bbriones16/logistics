package com.logistics.system.tracking.controller;

import com.logistics.system.tracking.dto.TrackingDto;
import com.logistics.system.tracking.service.TrackingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class TrackingController {

    private final TrackingService trackingService;

    public TrackingController(TrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @GetMapping("/api/tracking/{trackingNumber}")
    public ResponseEntity<TrackingDto.TrackingResponse> getByTrackingNumber(@PathVariable String trackingNumber) {
        return ResponseEntity.ok(trackingService.getByTrackingNumber(trackingNumber));
    }

    @GetMapping("/api/shipments/{id}/tracking")
    public ResponseEntity<TrackingDto.TrackingResponse> getByShipmentId(@PathVariable Long id) {
        return ResponseEntity.ok(trackingService.getByShipmentId(id));
    }
}

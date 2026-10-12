package com.logistics.system.reporting.controller;

import com.logistics.system.reporting.dto.ReportingDto;
import com.logistics.system.reporting.service.ReportingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportingController {

    private final ReportingService reportingService;

    public ReportingController(ReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @GetMapping("/orders")
    public ResponseEntity<ReportingDto.OrderSummary> getOrderSummary() {
        return ResponseEntity.ok(reportingService.getOrderSummary());
    }

    @GetMapping("/shipments")
    public ResponseEntity<ReportingDto.ShipmentSummary> getShipmentSummary() {
        return ResponseEntity.ok(reportingService.getShipmentSummary());
    }

    @GetMapping("/deliveries")
    public ResponseEntity<ReportingDto.DeliverySummary> getDeliverySummary() {
        return ResponseEntity.ok(reportingService.getDeliverySummary());
    }

    @GetMapping("/inventory/summary")
    public ResponseEntity<ReportingDto.InventorySummary> getInventorySummary() {
        return ResponseEntity.ok(reportingService.getInventorySummary());
    }

    @GetMapping("/inventory/low-stock")
    public ResponseEntity<ReportingDto.LowStockReport> getLowStockReport() {
        return ResponseEntity.ok(reportingService.getLowStockReport());
    }
}

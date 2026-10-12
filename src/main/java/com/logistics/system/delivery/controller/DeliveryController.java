package com.logistics.system.delivery.controller;

import com.logistics.system.delivery.dto.DeliveryDto;
import com.logistics.system.delivery.service.DeliveryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/deliveries")
public class DeliveryController {

    private final DeliveryService deliveryService;

    public DeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @GetMapping
    public Page<DeliveryDto.Response> getAll(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return deliveryService.findAll(pageable);
    }

    @GetMapping("/{id}")
    public DeliveryDto.Response getById(@PathVariable Long id) {
        return deliveryService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DeliveryDto.Response create(@RequestBody DeliveryDto.CreateRequest request) {
        return deliveryService.create(request);
    }

    @PutMapping("/{id}/status")
    public DeliveryDto.Response updateStatus(@PathVariable Long id,
                                             @RequestBody DeliveryDto.UpdateStatusRequest request) {
        return deliveryService.updateStatus(id, request);
    }
}

package com.logistics.system.order.controller;

import com.logistics.system.order.dto.CustomerDto;
import com.logistics.system.order.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public List<CustomerDto.Response> getAll() {
        return customerService.findAll();
    }

    @GetMapping("/{id}")
    public CustomerDto.Response getById(@PathVariable Long id) {
        return customerService.findById(id);
    }

    @PostMapping
    public ResponseEntity<CustomerDto.Response> create(@Valid @RequestBody CustomerDto.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.create(request));
    }
}

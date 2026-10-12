package com.logistics.system.inventory.controller;

import com.logistics.system.inventory.dto.ProductDto;
import com.logistics.system.inventory.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<ProductDto.Response> getAll() {
        return productService.findAll();
    }

    @GetMapping("/{id}")
    public ProductDto.Response getById(@PathVariable Long id) {
        return productService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDto.Response create(@Valid @RequestBody ProductDto.CreateRequest request) {
        return productService.create(request);
    }
}

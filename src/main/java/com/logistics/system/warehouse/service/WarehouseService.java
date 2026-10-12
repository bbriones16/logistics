package com.logistics.system.warehouse.service;

import com.logistics.system.common.exception.BusinessException;
import com.logistics.system.common.exception.ResourceNotFoundException;
import com.logistics.system.warehouse.domain.Warehouse;
import com.logistics.system.warehouse.dto.WarehouseDto;
import com.logistics.system.warehouse.repository.WarehouseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class WarehouseService {

    private static final Logger log = LoggerFactory.getLogger(WarehouseService.class);

    private final WarehouseRepository warehouseRepository;

    public WarehouseService(WarehouseRepository warehouseRepository) {
        this.warehouseRepository = warehouseRepository;
    }

    public List<WarehouseDto.Response> findAll() {
        return warehouseRepository.findByActiveTrue().stream().map(this::toResponse).toList();
    }

    public WarehouseDto.Response findById(Long id) {
        return toResponse(getWarehouse(id));
    }

    @Transactional
    public WarehouseDto.Response create(WarehouseDto.CreateRequest request) {
        if (warehouseRepository.existsByCode(request.code())) {
            throw new BusinessException("WAREHOUSE_DUPLICATE_CODE", "Warehouse with code already exists: " + request.code());
        }
        Warehouse warehouse = new Warehouse();
        warehouse.setCode(request.code());
        warehouse.setName(request.name());
        warehouse.setAddress(request.address());
        Warehouse saved = warehouseRepository.save(warehouse);
        log.info("Warehouse created: id={}, code={}", saved.getId(), saved.getCode());
        return toResponse(saved);
    }

    public Warehouse getWarehouse(Long id) {
        return warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", id));
    }

    private WarehouseDto.Response toResponse(Warehouse w) {
        return new WarehouseDto.Response(w.getId(), w.getCode(), w.getName(), w.getAddress(), w.isActive());
    }
}

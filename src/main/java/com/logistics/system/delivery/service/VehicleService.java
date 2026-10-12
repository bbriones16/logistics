package com.logistics.system.delivery.service;

import com.logistics.system.common.exception.BusinessException;
import com.logistics.system.common.exception.ResourceNotFoundException;
import com.logistics.system.delivery.domain.Vehicle;
import com.logistics.system.delivery.domain.VehicleStatus;
import com.logistics.system.delivery.dto.VehicleDto;
import com.logistics.system.delivery.repository.VehicleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class VehicleService {

    private static final Logger log = LoggerFactory.getLogger(VehicleService.class);

    private final VehicleRepository vehicleRepository;

    public VehicleService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    public List<VehicleDto.Response> findAll() {
        return vehicleRepository.findAll().stream().map(this::toResponse).toList();
    }

    public List<VehicleDto.Response> findAvailable() {
        return vehicleRepository.findByStatus(VehicleStatus.AVAILABLE).stream().map(this::toResponse).toList();
    }

    public VehicleDto.Response findById(Long id) {
        return toResponse(getVehicle(id));
    }

    @Transactional
    public VehicleDto.Response create(VehicleDto.CreateRequest request) {
        if (vehicleRepository.existsByPlateNumber(request.plateNumber())) {
            throw new BusinessException("VEHICLE_DUPLICATE_PLATE", "Vehicle with plate number already exists: " + request.plateNumber());
        }
        Vehicle vehicle = new Vehicle();
        vehicle.setPlateNumber(request.plateNumber());
        vehicle.setVehicleType(request.vehicleType());
        vehicle.setCapacityKg(request.capacityKg());
        Vehicle saved = vehicleRepository.save(vehicle);
        log.info("Vehicle created: id={}, plate={}", saved.getId(), saved.getPlateNumber());
        return toResponse(saved);
    }

    public Vehicle getVehicle(Long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", id));
    }

    private VehicleDto.Response toResponse(Vehicle v) {
        return new VehicleDto.Response(v.getId(), v.getPlateNumber(), v.getVehicleType(),
                v.getCapacityKg(), v.getStatus(), v.getCreatedAt());
    }
}

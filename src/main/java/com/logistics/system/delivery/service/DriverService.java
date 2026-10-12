package com.logistics.system.delivery.service;

import com.logistics.system.common.exception.BusinessException;
import com.logistics.system.common.exception.ResourceNotFoundException;
import com.logistics.system.delivery.domain.Driver;
import com.logistics.system.delivery.domain.DriverStatus;
import com.logistics.system.delivery.dto.DriverDto;
import com.logistics.system.delivery.repository.DriverRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class DriverService {

    private static final Logger log = LoggerFactory.getLogger(DriverService.class);

    private final DriverRepository driverRepository;

    public DriverService(DriverRepository driverRepository) {
        this.driverRepository = driverRepository;
    }

    public List<DriverDto.Response> findAll() {
        return driverRepository.findAll().stream().map(this::toResponse).toList();
    }

    public List<DriverDto.Response> findAvailable() {
        return driverRepository.findByStatus(DriverStatus.AVAILABLE).stream().map(this::toResponse).toList();
    }

    public DriverDto.Response findById(Long id) {
        return toResponse(getDriver(id));
    }

    @Transactional
    public DriverDto.Response create(DriverDto.CreateRequest request) {
        if (driverRepository.existsByLicenseNumber(request.licenseNumber())) {
            throw new BusinessException("DRIVER_DUPLICATE_LICENSE", "Driver with license number already exists: " + request.licenseNumber());
        }
        Driver driver = new Driver();
        driver.setName(request.name());
        driver.setLicenseNumber(request.licenseNumber());
        driver.setPhone(request.phone());
        Driver saved = driverRepository.save(driver);
        log.info("Driver created: id={}, license={}", saved.getId(), saved.getLicenseNumber());
        return toResponse(saved);
    }

    public Driver getDriver(Long id) {
        return driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver", id));
    }

    private DriverDto.Response toResponse(Driver d) {
        return new DriverDto.Response(d.getId(), d.getName(), d.getLicenseNumber(),
                d.getPhone(), d.getStatus(), d.getCreatedAt());
    }
}

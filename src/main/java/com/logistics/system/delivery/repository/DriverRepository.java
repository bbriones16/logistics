package com.logistics.system.delivery.repository;

import com.logistics.system.delivery.domain.Driver;
import com.logistics.system.delivery.domain.DriverStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DriverRepository extends JpaRepository<Driver, Long> {
    boolean existsByLicenseNumber(String licenseNumber);
    List<Driver> findByStatus(DriverStatus status);
}

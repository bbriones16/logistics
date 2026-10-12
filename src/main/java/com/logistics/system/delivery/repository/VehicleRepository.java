package com.logistics.system.delivery.repository;

import com.logistics.system.delivery.domain.Vehicle;
import com.logistics.system.delivery.domain.VehicleStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    boolean existsByPlateNumber(String plateNumber);
    List<Vehicle> findByStatus(VehicleStatus status);
}

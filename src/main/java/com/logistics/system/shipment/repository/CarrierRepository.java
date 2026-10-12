package com.logistics.system.shipment.repository;

import com.logistics.system.shipment.domain.Carrier;
import com.logistics.system.shipment.domain.ShipmentMedium;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CarrierRepository extends JpaRepository<Carrier, Long> {
    Optional<Carrier> findByCode(String code);
    boolean existsByCode(String code);
    List<Carrier> findByActiveTrue();
    List<Carrier> findByMediumAndActiveTrue(ShipmentMedium medium);
}

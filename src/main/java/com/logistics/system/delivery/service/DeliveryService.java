package com.logistics.system.delivery.service;

import com.logistics.system.common.event.DeliveryAssignedEvent;
import com.logistics.system.common.event.DeliveryCompletedEvent;
import com.logistics.system.common.event.DeliveryFailedEvent;
import com.logistics.system.common.exception.BusinessException;
import com.logistics.system.common.exception.ResourceNotFoundException;
import com.logistics.system.delivery.domain.*;
import com.logistics.system.delivery.dto.DeliveryDto;
import com.logistics.system.delivery.repository.DeliveryRepository;
import com.logistics.system.shipment.domain.Shipment;
import com.logistics.system.shipment.domain.ShipmentMedium;
import com.logistics.system.shipment.domain.ShipmentStatus;
import com.logistics.system.shipment.repository.ShipmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class DeliveryService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryService.class);

    private static final Map<DeliveryStatus, Set<DeliveryStatus>> VALID_TRANSITIONS = Map.of(
            DeliveryStatus.ASSIGNED,         EnumSet.of(DeliveryStatus.OUT_FOR_DELIVERY, DeliveryStatus.CANCELLED),
            DeliveryStatus.OUT_FOR_DELIVERY, EnumSet.of(DeliveryStatus.DELIVERED, DeliveryStatus.FAILED)
    );

    private final DeliveryRepository deliveryRepository;
    private final ShipmentRepository shipmentRepository;
    private final DriverService driverService;
    private final VehicleService vehicleService;
    private final ApplicationEventPublisher eventPublisher;

    public DeliveryService(DeliveryRepository deliveryRepository, ShipmentRepository shipmentRepository,
                           DriverService driverService, VehicleService vehicleService,
                           ApplicationEventPublisher eventPublisher) {
        this.deliveryRepository = deliveryRepository;
        this.shipmentRepository = shipmentRepository;
        this.driverService = driverService;
        this.vehicleService = vehicleService;
        this.eventPublisher = eventPublisher;
    }

    public Page<DeliveryDto.Response> findAll(Pageable pageable) {
        return deliveryRepository.findAll(pageable).map(this::toResponse);
    }

    public DeliveryDto.Response findById(Long id) {
        return toResponse(getDelivery(id));
    }

    @Transactional
    public DeliveryDto.Response create(DeliveryDto.CreateRequest request) {
        Shipment shipment = shipmentRepository.findById(request.shipmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", request.shipmentId()));

        if (!shipment.getCarrier().isCompanyOwned()) {
            throw new BusinessException("DELIVERY_THIRD_PARTY_CARRIER",
                    "Delivery assignment is only allowed for company-owned carrier shipments");
        }
        if (shipment.getMedium() != ShipmentMedium.LAND) {
            throw new BusinessException("DELIVERY_LAND_ONLY",
                    "Delivery assignment is only supported for LAND shipments in this version, current medium: " + shipment.getMedium());
        }
        if (shipment.getStatus() != ShipmentStatus.ARRIVED_AT_HUB) {
            throw new BusinessException("DELIVERY_INVALID_SHIPMENT_STATUS",
                    "Shipment must be in ARRIVED_AT_HUB status to assign a delivery, current status: " + shipment.getStatus());
        }
        if (deliveryRepository.existsByShipmentId(request.shipmentId())) {
            throw new BusinessException("DELIVERY_DUPLICATE", "A delivery already exists for shipment: " + request.shipmentId());
        }

        Driver driver = driverService.getDriver(request.driverId());
        if (driver.getStatus() != DriverStatus.AVAILABLE) {
            throw new BusinessException("DELIVERY_DRIVER_UNAVAILABLE",
                    "Driver is not available, current status: " + driver.getStatus());
        }

        Vehicle vehicle = vehicleService.getVehicle(request.vehicleId());
        if (vehicle.getStatus() != VehicleStatus.AVAILABLE) {
            throw new BusinessException("DELIVERY_VEHICLE_UNAVAILABLE",
                    "Vehicle is not available, current status: " + vehicle.getStatus());
        }

        Delivery delivery = new Delivery();
        delivery.setShipment(shipment);
        delivery.setDriver(driver);
        delivery.setVehicle(vehicle);
        delivery.setScheduledDate(request.scheduledDate());
        delivery.setNotes(request.notes());

        Delivery saved = deliveryRepository.save(delivery);
        log.info("Delivery created: id={}, shipmentId={}, driverId={}, vehicleId={}",
                saved.getId(), request.shipmentId(), request.driverId(), request.vehicleId());

        eventPublisher.publishEvent(new DeliveryAssignedEvent(saved.getId(), shipment.getId()));
        return toResponse(saved);
    }

    @Transactional
    public DeliveryDto.Response updateStatus(Long id, DeliveryDto.UpdateStatusRequest request) {
        Delivery delivery = getDelivery(id);
        DeliveryStatus current = delivery.getStatus();
        DeliveryStatus next = request.status();

        Set<DeliveryStatus> allowed = VALID_TRANSITIONS.getOrDefault(current, EnumSet.noneOf(DeliveryStatus.class));
        if (!allowed.contains(next)) {
            throw new BusinessException("DELIVERY_STATUS_ERROR",
                    "Cannot transition delivery from " + current + " to " + next);
        }

        if (next == DeliveryStatus.DELIVERED) {
            if (request.proofOfDelivery() == null || request.proofOfDelivery().isBlank()) {
                throw new BusinessException("DELIVERY_POD_REQUIRED", "Proof of delivery is required when marking delivery as DELIVERED");
            }
            eventPublisher.publishEvent(new DeliveryCompletedEvent(id, delivery.getShipment().getId(), request.proofOfDelivery()));
        } else if (next == DeliveryStatus.FAILED) {
            eventPublisher.publishEvent(new DeliveryFailedEvent(id, delivery.getShipment().getId(), "Delivery failed"));
        } else if (next == DeliveryStatus.CANCELLED) {
            eventPublisher.publishEvent(new DeliveryFailedEvent(id, delivery.getShipment().getId(), "Delivery cancelled"));
        }

        delivery.setStatus(next);
        Delivery saved = deliveryRepository.save(delivery);
        log.info("Delivery status updated: id={}, from={}, to={}", id, current, next);
        return toResponse(saved);
    }

    private Delivery getDelivery(Long id) {
        return deliveryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery", id));
    }

    private DeliveryDto.Response toResponse(Delivery d) {
        return new DeliveryDto.Response(
                d.getId(),
                d.getShipment().getId(),
                d.getDriver().getId(),
                d.getDriver().getName(),
                d.getVehicle().getId(),
                d.getVehicle().getPlateNumber(),
                d.getStatus(),
                d.getScheduledDate(),
                d.getNotes(),
                d.getCreatedAt()
        );
    }
}

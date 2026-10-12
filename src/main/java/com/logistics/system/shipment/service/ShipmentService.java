package com.logistics.system.shipment.service;

import com.logistics.system.common.event.ShipmentCreatedEvent;
import com.logistics.system.common.event.ShipmentStatusChangedEvent;
import com.logistics.system.common.exception.BusinessException;
import com.logistics.system.common.exception.ResourceNotFoundException;
import com.logistics.system.order.domain.OrderStatus;
import com.logistics.system.order.repository.OrderRepository;
import com.logistics.system.shipment.domain.*;
import com.logistics.system.shipment.dto.ShipmentDto;
import com.logistics.system.shipment.repository.ShipmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Transactional(readOnly = true)
public class ShipmentService {

    private static final Logger log = LoggerFactory.getLogger(ShipmentService.class);

    private static final Map<ShipmentStatus, Set<ShipmentStatus>> VALID_TRANSITIONS = Map.of(
            ShipmentStatus.CREATED,          EnumSet.of(ShipmentStatus.PROCESSING, ShipmentStatus.CANCELLED),
            ShipmentStatus.PROCESSING,       EnumSet.of(ShipmentStatus.READY_FOR_PICKUP, ShipmentStatus.CANCELLED),
            ShipmentStatus.READY_FOR_PICKUP, EnumSet.of(ShipmentStatus.PICKED_UP, ShipmentStatus.CANCELLED),
            ShipmentStatus.PICKED_UP,        EnumSet.of(ShipmentStatus.IN_TRANSIT),
            ShipmentStatus.IN_TRANSIT,       EnumSet.of(ShipmentStatus.ARRIVED_AT_HUB, ShipmentStatus.FAILED),
            ShipmentStatus.ARRIVED_AT_HUB,   EnumSet.of(ShipmentStatus.OUT_FOR_DELIVERY),
            ShipmentStatus.OUT_FOR_DELIVERY, EnumSet.of(ShipmentStatus.DELIVERED, ShipmentStatus.FAILED)
    );

    private static final AtomicLong trackingSequence = new AtomicLong(1);

    private final ShipmentRepository shipmentRepository;
    private final CarrierService carrierService;
    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ShipmentService(ShipmentRepository shipmentRepository, CarrierService carrierService,
                           OrderRepository orderRepository, ApplicationEventPublisher eventPublisher) {
        this.shipmentRepository = shipmentRepository;
        this.carrierService = carrierService;
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
    }

    public Page<ShipmentDto.Response> findAll(Pageable pageable) {
        return shipmentRepository.findAll(pageable).map(this::toResponse);
    }

    public ShipmentDto.Response findById(Long id) {
        return toResponse(getShipment(id));
    }

    public ShipmentDto.Response findByTrackingNumber(String trackingNumber) {
        return toResponse(shipmentRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment with tracking number " + trackingNumber, 0L)));
    }

    @Transactional
    public ShipmentDto.Response create(ShipmentDto.CreateRequest request) {
        Carrier carrier = carrierService.getCarrier(request.carrierId());
        Shipment shipment = new Shipment();
        shipment.setShipmentType(request.shipmentType());
        shipment.setCarrier(carrier);
        shipment.setMedium(request.medium());
        shipment.setShipToAddress(request.shipToAddress());
        shipment.setWeightKg(request.weightKg());
        shipment.setFreightCost(request.freightCost());
        shipment.setEstimatedDeliveryDate(request.estimatedDeliveryDate());
        shipment.setNotes(request.notes());
        shipment.setQuantity(request.quantity());

        if (request.shipmentType() == ShipmentType.FULFILLMENT) {
            validateFulfillmentShipment(request, shipment);
        } else {
            validateStandaloneShipment(request, shipment);
        }

        shipment.setTrackingNumber(resolveTrackingNumber(carrier, request.trackingNumber()));

        Shipment saved = shipmentRepository.save(shipment);
        log.info("Shipment created: id={}, type={}, trackingNumber={}", saved.getId(), saved.getShipmentType(), saved.getTrackingNumber());

        if (saved.getShipmentType() == ShipmentType.FULFILLMENT) {
            eventPublisher.publishEvent(new ShipmentCreatedEvent(saved.getId(), saved.getOrder().getId()));
        }
        eventPublisher.publishEvent(new ShipmentStatusChangedEvent(saved.getId(), saved.getStatus(), null, null));

        return toResponse(saved);
    }

    @Transactional
    public ShipmentDto.Response updateStatus(Long id, ShipmentDto.UpdateStatusRequest request) {
        Shipment shipment = getShipment(id);
        ShipmentStatus current = shipment.getStatus();
        ShipmentStatus next = request.status();

        Set<ShipmentStatus> allowed = VALID_TRANSITIONS.getOrDefault(current, EnumSet.noneOf(ShipmentStatus.class));
        if (!allowed.contains(next)) {
            throw new BusinessException("SHIPMENT_STATUS_ERROR",
                    "Cannot transition shipment from " + current + " to " + next);
        }

        if (next == ShipmentStatus.DELIVERED) {
            if (request.proofOfDelivery() == null || request.proofOfDelivery().isBlank()) {
                throw new BusinessException("SHIPMENT_POD_REQUIRED", "Proof of delivery is required when marking shipment as DELIVERED");
            }
            if (shipment.getCarrier().isCompanyOwned()) {
                throw new BusinessException("SHIPMENT_USE_DELIVERY",
                        "Use the Delivery module to mark DELIVERED for company-owned carrier shipments");
            }
            shipment.setProofOfDelivery(request.proofOfDelivery());
            shipment.setActualDeliveryDate(LocalDate.now());
        }

        if (next == ShipmentStatus.OUT_FOR_DELIVERY && shipment.getCarrier().isCompanyOwned()) {
            throw new BusinessException("SHIPMENT_USE_DELIVERY",
                    "Use the Delivery module to dispatch company-owned carrier shipments");
        }

        shipment.setStatus(next);
        Shipment saved = shipmentRepository.save(shipment);
        log.info("Shipment status updated: id={}, from={}, to={}", id, current, next);
        eventPublisher.publishEvent(new ShipmentStatusChangedEvent(saved.getId(), next, null, null));
        return toResponse(saved);
    }

    private void validateFulfillmentShipment(ShipmentDto.CreateRequest request, Shipment shipment) {
        if (request.orderId() == null) {
            throw new BusinessException("SHIPMENT_ORDER_REQUIRED", "orderId is required for FULFILLMENT shipments");
        }
        if (shipmentRepository.existsByOrderId(request.orderId())) {
            throw new BusinessException("SHIPMENT_DUPLICATE_ORDER", "A shipment already exists for order: " + request.orderId());
        }
        var order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", request.orderId()));
        if (order.getStatus() != OrderStatus.STOCK_RESERVED) {
            throw new BusinessException("SHIPMENT_INVALID_ORDER_STATUS",
                    "Order must be in STOCK_RESERVED status to create a shipment, current status: " + order.getStatus());
        }
        shipment.setOrder(order);
    }

    private void validateStandaloneShipment(ShipmentDto.CreateRequest request, Shipment shipment) {
        if (request.cargoDescription() == null || request.cargoDescription().isBlank()) {
            throw new BusinessException("SHIPMENT_CARGO_REQUIRED", "cargoDescription is required for STANDALONE shipments");
        }
        shipment.setCargoDescription(request.cargoDescription());
    }

    private String resolveTrackingNumber(Carrier carrier, String providedTrackingNumber) {
        if (carrier.isCompanyOwned()) {
            return generateTrackingNumber();
        }
        if (providedTrackingNumber == null || providedTrackingNumber.isBlank()) {
            throw new BusinessException("SHIPMENT_TRACKING_REQUIRED",
                    "trackingNumber is required for third-party carriers");
        }
        return providedTrackingNumber;
    }

    private String generateTrackingNumber() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return String.format("TRK-%s-%05d", date, trackingSequence.getAndIncrement());
    }

    private Shipment getShipment(Long id) {
        return shipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", id));
    }

    private ShipmentDto.Response toResponse(Shipment s) {
        return new ShipmentDto.Response(
                s.getId(),
                s.getShipmentType(),
                s.getOrder() != null ? s.getOrder().getId() : null,
                s.getCarrier().getId(),
                s.getCarrier().getName(),
                s.getMedium(),
                s.getTrackingNumber(),
                s.getStatus(),
                s.getCargoDescription(),
                s.getQuantity(),
                s.getWeightKg(),
                s.getFreightCost(),
                s.getShipToAddress(),
                s.getEstimatedDeliveryDate(),
                s.getActualDeliveryDate(),
                s.getProofOfDelivery(),
                s.getNotes(),
                s.getCreatedAt()
        );
    }
}

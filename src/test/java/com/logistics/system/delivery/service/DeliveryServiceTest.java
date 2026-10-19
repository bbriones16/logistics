package com.logistics.system.delivery.service;

import com.logistics.system.common.event.DeliveryAssignedEvent;
import com.logistics.system.common.event.DeliveryCompletedEvent;
import com.logistics.system.common.event.DeliveryFailedEvent;
import com.logistics.system.common.exception.BusinessException;
import com.logistics.system.common.exception.ResourceNotFoundException;
import com.logistics.system.delivery.domain.*;
import com.logistics.system.delivery.dto.DeliveryDto;
import com.logistics.system.delivery.repository.DeliveryRepository;
import com.logistics.system.shipment.domain.Carrier;
import com.logistics.system.shipment.domain.Shipment;
import com.logistics.system.shipment.domain.ShipmentStatus;
import com.logistics.system.shipment.repository.ShipmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {

    @Mock private DeliveryRepository deliveryRepository;
    @Mock private ShipmentRepository shipmentRepository;
    @Mock private DriverService driverService;
    @Mock private VehicleService vehicleService;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private DeliveryService deliveryService;

    private Shipment shipment;
    private Driver driver;
    private Vehicle vehicle;

    @BeforeEach
    void setUp() {
        Carrier carrier = new Carrier();
        carrier.setCompanyOwned(true);

        shipment = new Shipment();
        shipment.setCarrier(carrier);
        shipment.setStatus(ShipmentStatus.ARRIVED_AT_HUB);
        shipment.setMedium(com.logistics.system.shipment.domain.ShipmentMedium.LAND);

        driver = new Driver();
        driver.setName("John Driver");
        driver.setStatus(DriverStatus.AVAILABLE);

        vehicle = new Vehicle();
        vehicle.setPlateNumber("ABC-123");
        vehicle.setStatus(VehicleStatus.AVAILABLE);
    }

    @Test
    void create_validRequest_createsDeliveryAndPublishesEvent() {
        when(shipmentRepository.findById(1L)).thenReturn(Optional.of(shipment));
        when(deliveryRepository.existsByShipmentId(1L)).thenReturn(false);
        when(driverService.getDriver(1L)).thenReturn(driver);
        when(vehicleService.getVehicle(1L)).thenReturn(vehicle);
        when(deliveryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        deliveryService.create(new DeliveryDto.CreateRequest(1L, 1L, 1L, null, null, null));

        verify(deliveryRepository).save(any(Delivery.class));
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(DeliveryAssignedEvent.class);
    }

    @Test
    void create_thirdPartyCarrier_throwsBusinessException() {
        shipment.getCarrier().setCompanyOwned(false);
        when(shipmentRepository.findById(1L)).thenReturn(Optional.of(shipment));

        assertThatThrownBy(() -> deliveryService.create(new DeliveryDto.CreateRequest(1L, 1L, 1L, null, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("company-owned");
    }

    @Test
    void create_nonLandShipment_throwsBusinessException() {
        shipment.setMedium(com.logistics.system.shipment.domain.ShipmentMedium.SEA);
        when(shipmentRepository.findById(1L)).thenReturn(Optional.of(shipment));

        assertThatThrownBy(() -> deliveryService.create(new DeliveryDto.CreateRequest(1L, 1L, 1L, null, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("LAND");
    }

    @Test
    void create_shipmentNotArrivedAtHub_throwsBusinessException() {
        shipment.setStatus(ShipmentStatus.IN_TRANSIT);
        when(shipmentRepository.findById(1L)).thenReturn(Optional.of(shipment));

        assertThatThrownBy(() -> deliveryService.create(new DeliveryDto.CreateRequest(1L, 1L, 1L, null, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ARRIVED_AT_HUB");
    }

    @Test
    void create_duplicateDeliveryForShipment_throwsBusinessException() {
        when(shipmentRepository.findById(1L)).thenReturn(Optional.of(shipment));
        when(deliveryRepository.existsByShipmentId(1L)).thenReturn(true);

        assertThatThrownBy(() -> deliveryService.create(new DeliveryDto.CreateRequest(1L, 1L, 1L, null, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void create_driverNotAvailable_throwsBusinessException() {
        driver.setStatus(DriverStatus.ON_DELIVERY);
        when(shipmentRepository.findById(1L)).thenReturn(Optional.of(shipment));
        when(deliveryRepository.existsByShipmentId(1L)).thenReturn(false);
        when(driverService.getDriver(1L)).thenReturn(driver);

        assertThatThrownBy(() -> deliveryService.create(new DeliveryDto.CreateRequest(1L, 1L, 1L, null, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Driver is not available");
    }

    @Test
    void create_vehicleNotAvailable_throwsBusinessException() {
        vehicle.setStatus(VehicleStatus.IN_USE);
        when(shipmentRepository.findById(1L)).thenReturn(Optional.of(shipment));
        when(deliveryRepository.existsByShipmentId(1L)).thenReturn(false);
        when(driverService.getDriver(1L)).thenReturn(driver);
        when(vehicleService.getVehicle(1L)).thenReturn(vehicle);

        assertThatThrownBy(() -> deliveryService.create(new DeliveryDto.CreateRequest(1L, 1L, 1L, null, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Vehicle is not available");
    }

    @Test
    void create_nonExistingShipment_throwsResourceNotFoundException() {
        when(shipmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deliveryService.create(new DeliveryDto.CreateRequest(99L, 1L, 1L, null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateStatus_assignedToOutForDelivery_updatesStatus() {
        Delivery delivery = buildDelivery(DeliveryStatus.ASSIGNED);
        when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));
        when(deliveryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DeliveryDto.Response result = deliveryService.updateStatus(1L,
                new DeliveryDto.UpdateStatusRequest(DeliveryStatus.OUT_FOR_DELIVERY, null));

        assertThat(result.status()).isEqualTo(DeliveryStatus.OUT_FOR_DELIVERY);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void updateStatus_delivered_requiresProofOfDelivery() {
        Delivery delivery = buildDelivery(DeliveryStatus.OUT_FOR_DELIVERY);
        when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));

        assertThatThrownBy(() -> deliveryService.updateStatus(1L,
                new DeliveryDto.UpdateStatusRequest(DeliveryStatus.DELIVERED, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Proof of delivery is required");
    }

    @Test
    void updateStatus_delivered_publishesDeliveryCompletedEvent() {
        Delivery delivery = buildDelivery(DeliveryStatus.OUT_FOR_DELIVERY);
        when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));
        when(deliveryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        deliveryService.updateStatus(1L,
                new DeliveryDto.UpdateStatusRequest(DeliveryStatus.DELIVERED, "Signed by Jane"));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(DeliveryCompletedEvent.class);
        assertThat(((DeliveryCompletedEvent) captor.getValue()).proofOfDelivery()).isEqualTo("Signed by Jane");
    }

    @Test
    void updateStatus_failed_publishesDeliveryFailedEvent() {
        Delivery delivery = buildDelivery(DeliveryStatus.OUT_FOR_DELIVERY);
        when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));
        when(deliveryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        deliveryService.updateStatus(1L,
                new DeliveryDto.UpdateStatusRequest(DeliveryStatus.FAILED, null));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(DeliveryFailedEvent.class);
    }

    @Test
    void updateStatus_cancelled_publishesDeliveryFailedEvent() {
        Delivery delivery = buildDelivery(DeliveryStatus.ASSIGNED);
        when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));
        when(deliveryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        deliveryService.updateStatus(1L,
                new DeliveryDto.UpdateStatusRequest(DeliveryStatus.CANCELLED, null));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(DeliveryFailedEvent.class);
    }

    @Test
    void updateStatus_invalidTransition_throwsBusinessException() {
        Delivery delivery = buildDelivery(DeliveryStatus.DELIVERED);
        when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));

        assertThatThrownBy(() -> deliveryService.updateStatus(1L,
                new DeliveryDto.UpdateStatusRequest(DeliveryStatus.OUT_FOR_DELIVERY, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cannot transition");
    }

    @Test
    void findById_nonExisting_throwsResourceNotFoundException() {
        when(deliveryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deliveryService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private Delivery buildDelivery(DeliveryStatus status) {
        Delivery delivery = new Delivery();
        delivery.setShipment(shipment);
        delivery.setDriver(driver);
        delivery.setVehicle(vehicle);
        delivery.setStatus(status);
        return delivery;
    }
}

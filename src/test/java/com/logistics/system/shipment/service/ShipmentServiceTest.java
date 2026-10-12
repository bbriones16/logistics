package com.logistics.system.shipment.service;

import com.logistics.system.common.event.ShipmentCreatedEvent;
import com.logistics.system.common.exception.BusinessException;
import com.logistics.system.common.exception.ResourceNotFoundException;
import com.logistics.system.order.domain.Order;
import com.logistics.system.order.domain.OrderStatus;
import com.logistics.system.order.repository.OrderRepository;
import com.logistics.system.shipment.domain.*;
import com.logistics.system.shipment.dto.ShipmentDto;
import com.logistics.system.shipment.repository.ShipmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShipmentServiceTest {

    @Mock private ShipmentRepository shipmentRepository;
    @Mock private CarrierService carrierService;
    @Mock private OrderRepository orderRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ShipmentService shipmentService;

    private Carrier companyCarrier;
    private Carrier thirdPartyCarrier;
    private Order order;

    @BeforeEach
    void setUp() {
        companyCarrier = new Carrier();
        companyCarrier.setCode("OWN-01");
        companyCarrier.setName("Company Truck");
        companyCarrier.setMedium(ShipmentMedium.LAND);
        companyCarrier.setCompanyOwned(true);

        thirdPartyCarrier = new Carrier();
        thirdPartyCarrier.setCode("DHL-01");
        thirdPartyCarrier.setName("DHL Express");
        thirdPartyCarrier.setMedium(ShipmentMedium.AIR);
        thirdPartyCarrier.setCompanyOwned(false);

        order = new Order();
        order.setStatus(OrderStatus.STOCK_RESERVED);
    }

    @Test
    void create_fulfillmentWithCompanyCarrier_autoGeneratesTrackingNumber() {
        when(carrierService.getCarrier(1L)).thenReturn(companyCarrier);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(shipmentRepository.existsByOrderId(1L)).thenReturn(false);
        when(shipmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ShipmentDto.Response result = shipmentService.create(buildFulfillmentRequest(1L, null));

        assertThat(result.trackingNumber()).startsWith("TRK-");
        assertThat(result.shipmentType()).isEqualTo(ShipmentType.FULFILLMENT);
        verify(eventPublisher).publishEvent(any(ShipmentCreatedEvent.class));
    }

    @Test
    void create_fulfillmentWithThirdPartyCarrier_usesProvidedTrackingNumber() {
        when(carrierService.getCarrier(2L)).thenReturn(thirdPartyCarrier);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(shipmentRepository.existsByOrderId(1L)).thenReturn(false);
        when(shipmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ShipmentDto.Response result = shipmentService.create(buildFulfillmentRequest(2L, "DHL-123456"));

        assertThat(result.trackingNumber()).isEqualTo("DHL-123456");
    }

    @Test
    void create_fulfillmentWithThirdPartyCarrier_missingTrackingNumber_throwsBusinessException() {
        when(carrierService.getCarrier(2L)).thenReturn(thirdPartyCarrier);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(shipmentRepository.existsByOrderId(1L)).thenReturn(false);

        assertThatThrownBy(() -> shipmentService.create(buildFulfillmentRequest(2L, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("trackingNumber is required");
    }

    @Test
    void create_fulfillmentWithoutOrderId_throwsBusinessException() {
        when(carrierService.getCarrier(1L)).thenReturn(companyCarrier);

        assertThatThrownBy(() -> shipmentService.create(new ShipmentDto.CreateRequest(
                ShipmentType.FULFILLMENT, null, 1L, ShipmentMedium.LAND,
                null, null, null, null, null, "123 Main St", null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("orderId is required");
    }

    @Test
    void create_fulfillmentOrderNotStockReserved_throwsBusinessException() {
        order.setStatus(OrderStatus.CREATED);
        when(carrierService.getCarrier(1L)).thenReturn(companyCarrier);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(shipmentRepository.existsByOrderId(1L)).thenReturn(false);

        assertThatThrownBy(() -> shipmentService.create(buildFulfillmentRequest(1L, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("STOCK_RESERVED");
    }

    @Test
    void create_duplicateShipmentForOrder_throwsBusinessException() {
        when(carrierService.getCarrier(1L)).thenReturn(companyCarrier);
        when(shipmentRepository.existsByOrderId(1L)).thenReturn(true);

        assertThatThrownBy(() -> shipmentService.create(buildFulfillmentRequest(1L, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void create_standaloneWithoutCargoDescription_throwsBusinessException() {
        when(carrierService.getCarrier(1L)).thenReturn(companyCarrier);

        assertThatThrownBy(() -> shipmentService.create(new ShipmentDto.CreateRequest(
                ShipmentType.STANDALONE, null, 1L, ShipmentMedium.LAND,
                null, null, null, null, null, "123 Main St", null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cargoDescription is required");
    }

    @Test
    void create_standaloneShipment_noEventPublished() {
        when(carrierService.getCarrier(1L)).thenReturn(companyCarrier);
        when(shipmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        shipmentService.create(new ShipmentDto.CreateRequest(
                ShipmentType.STANDALONE, null, 1L, ShipmentMedium.LAND,
                null, "100 crates of fruits", 100, BigDecimal.valueOf(500), null, "Port of Manila", null, null));

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void updateStatus_validTransition_updatesStatus() {
        Shipment shipment = new Shipment();
        shipment.setStatus(ShipmentStatus.CREATED);
        shipment.setCarrier(companyCarrier);
        when(shipmentRepository.findById(1L)).thenReturn(Optional.of(shipment));
        when(shipmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ShipmentDto.Response result = shipmentService.updateStatus(1L,
                new ShipmentDto.UpdateStatusRequest(ShipmentStatus.PROCESSING, null));

        assertThat(result.status()).isEqualTo(ShipmentStatus.PROCESSING);
    }

    @Test
    void updateStatus_delivered_requiresProofOfDelivery() {
        Shipment shipment = new Shipment();
        shipment.setStatus(ShipmentStatus.OUT_FOR_DELIVERY);
        shipment.setCarrier(companyCarrier);
        when(shipmentRepository.findById(1L)).thenReturn(Optional.of(shipment));

        assertThatThrownBy(() -> shipmentService.updateStatus(1L,
                new ShipmentDto.UpdateStatusRequest(ShipmentStatus.DELIVERED, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Proof of delivery is required");
    }

    @Test
    void updateStatus_delivered_setsActualDeliveryDate() {
        Shipment shipment = new Shipment();
        shipment.setStatus(ShipmentStatus.OUT_FOR_DELIVERY);
        shipment.setCarrier(thirdPartyCarrier);  // third-party: manual DELIVERED allowed
        when(shipmentRepository.findById(1L)).thenReturn(Optional.of(shipment));
        when(shipmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ShipmentDto.Response result = shipmentService.updateStatus(1L,
                new ShipmentDto.UpdateStatusRequest(ShipmentStatus.DELIVERED, "Signed by John"));

        assertThat(result.status()).isEqualTo(ShipmentStatus.DELIVERED);
        assertThat(result.proofOfDelivery()).isEqualTo("Signed by John");
        assertThat(result.actualDeliveryDate()).isNotNull();
    }

    @Test
    void updateStatus_delivered_companyOwnedCarrier_throwsBusinessException() {
        Shipment shipment = new Shipment();
        shipment.setStatus(ShipmentStatus.OUT_FOR_DELIVERY);
        shipment.setCarrier(companyCarrier);
        when(shipmentRepository.findById(1L)).thenReturn(Optional.of(shipment));

        assertThatThrownBy(() -> shipmentService.updateStatus(1L,
                new ShipmentDto.UpdateStatusRequest(ShipmentStatus.DELIVERED, "Signed by John")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Delivery module");
    }

    @Test
    void updateStatus_outForDelivery_companyOwnedCarrier_throwsBusinessException() {
        Shipment shipment = new Shipment();
        shipment.setStatus(ShipmentStatus.ARRIVED_AT_HUB);
        shipment.setCarrier(companyCarrier);
        when(shipmentRepository.findById(1L)).thenReturn(Optional.of(shipment));

        assertThatThrownBy(() -> shipmentService.updateStatus(1L,
                new ShipmentDto.UpdateStatusRequest(ShipmentStatus.OUT_FOR_DELIVERY, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Delivery module");
    }

    @Test
    void updateStatus_invalidTransition_throwsBusinessException() {
        Shipment shipment = new Shipment();
        shipment.setStatus(ShipmentStatus.DELIVERED);
        shipment.setCarrier(companyCarrier);
        when(shipmentRepository.findById(1L)).thenReturn(Optional.of(shipment));

        assertThatThrownBy(() -> shipmentService.updateStatus(1L,
                new ShipmentDto.UpdateStatusRequest(ShipmentStatus.PROCESSING, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cannot transition");
    }

    @Test
    void findById_nonExisting_throwsResourceNotFoundException() {
        when(shipmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shipmentService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private ShipmentDto.CreateRequest buildFulfillmentRequest(Long carrierId, String trackingNumber) {
        return new ShipmentDto.CreateRequest(
                ShipmentType.FULFILLMENT, 1L, carrierId, ShipmentMedium.LAND,
                trackingNumber, null, null, BigDecimal.valueOf(10.5), null,
                "123 Main St", null, null);
    }
}

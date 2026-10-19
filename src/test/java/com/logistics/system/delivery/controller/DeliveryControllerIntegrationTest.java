package com.logistics.system.delivery.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.system.common.AuthTestHelper;
import com.logistics.system.common.BaseIntegrationTest;
import com.logistics.system.delivery.domain.VehicleType;
import com.logistics.system.delivery.dto.DeliveryDto;
import com.logistics.system.delivery.dto.DriverDto;
import com.logistics.system.delivery.dto.VehicleDto;
import com.logistics.system.inventory.dto.InventoryDto;
import com.logistics.system.inventory.dto.ProductDto;
import com.logistics.system.order.dto.CustomerDto;
import com.logistics.system.order.dto.OrderDto;
import com.logistics.system.delivery.domain.DeliveryStatus;
import com.logistics.system.shipment.domain.ShipmentMedium;
import com.logistics.system.shipment.domain.ShipmentStatus;
import com.logistics.system.shipment.domain.ShipmentType;
import com.logistics.system.shipment.dto.CarrierDto;
import com.logistics.system.shipment.dto.ShipmentDto;
import com.logistics.system.warehouse.dto.WarehouseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DeliveryControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private ObjectMapper objectMapper;

    private HttpHeaders authHeaders;
    private Long companyCarrierId;

    @BeforeEach
    void setUp() throws Exception {
        String token = AuthTestHelper.obtainToken(restTemplate, objectMapper,
                "delivery-it@example.com", "password123");
        authHeaders = new HttpHeaders();
        authHeaders.setBearerAuth(token);
        authHeaders.setContentType(MediaType.APPLICATION_JSON);

        companyCarrierId = createCarrier("OWN-DLV-" + UUID.randomUUID().toString().substring(0, 6), true);
    }

    @Test
    void createDriver_validRequest_returns201() throws Exception {
        DriverDto.CreateRequest request = new DriverDto.CreateRequest(
                "John Driver", "LIC-" + UUID.randomUUID().toString().substring(0, 8), "555-1234");

        ResponseEntity<String> response = restTemplate.exchange("/api/drivers", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("id").asLong()).isPositive();
        assertThat(body.get("status").asText()).isEqualTo("AVAILABLE");
    }

    @Test
    void createDriver_duplicateLicense_returns422() throws Exception {
        String license = "LIC-DUP-" + UUID.randomUUID().toString().substring(0, 6);
        DriverDto.CreateRequest request = new DriverDto.CreateRequest("Driver A", license, null);
        restTemplate.exchange("/api/drivers", HttpMethod.POST, new HttpEntity<>(request, authHeaders), String.class);

        ResponseEntity<String> response = restTemplate.exchange("/api/drivers", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void createVehicle_validRequest_returns201() throws Exception {
        VehicleDto.CreateRequest request = new VehicleDto.CreateRequest(
                "PLT-" + UUID.randomUUID().toString().substring(0, 6), VehicleType.VAN, BigDecimal.valueOf(500));

        ResponseEntity<String> response = restTemplate.exchange("/api/vehicles", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("status").asText()).isEqualTo("AVAILABLE");
    }

    @Test
    void createVehicle_duplicatePlate_returns422() throws Exception {
        String plate = "PLT-DUP-" + UUID.randomUUID().toString().substring(0, 4);
        VehicleDto.CreateRequest request = new VehicleDto.CreateRequest(plate, VehicleType.TRUCK, null);
        restTemplate.exchange("/api/vehicles", HttpMethod.POST, new HttpEntity<>(request, authHeaders), String.class);

        ResponseEntity<String> response = restTemplate.exchange("/api/vehicles", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void createDelivery_validRequest_returns201AndDriverVehicleSetToInUse() throws Exception {
        Long shipmentId = createShipmentAtArrivedAtHub();
        Long driverId = createDriver();
        Long vehicleId = createVehicle();

        ResponseEntity<String> response = restTemplate.exchange("/api/deliveries", HttpMethod.POST,
                new HttpEntity<>(new DeliveryDto.CreateRequest(shipmentId, driverId, vehicleId, null, null, null), authHeaders),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("status").asText()).isEqualTo("ASSIGNED");
        assertThat(body.get("shipmentId").asLong()).isEqualTo(shipmentId);

        // Shipment should now be OUT_FOR_DELIVERY
        Thread.sleep(200);
        JsonNode shipment = objectMapper.readTree(restTemplate.exchange(
                "/api/shipments/" + shipmentId, HttpMethod.GET, new HttpEntity<>(authHeaders), String.class).getBody());
        assertThat(shipment.get("status").asText()).isEqualTo("OUT_FOR_DELIVERY");

        // Driver and vehicle should be IN_USE / ON_DELIVERY
        JsonNode driver = objectMapper.readTree(restTemplate.exchange(
                "/api/drivers/" + driverId, HttpMethod.GET, new HttpEntity<>(authHeaders), String.class).getBody());
        assertThat(driver.get("status").asText()).isEqualTo("ON_DELIVERY");

        JsonNode vehicle = objectMapper.readTree(restTemplate.exchange(
                "/api/vehicles/" + vehicleId, HttpMethod.GET, new HttpEntity<>(authHeaders), String.class).getBody());
        assertThat(vehicle.get("status").asText()).isEqualTo("IN_USE");
    }

    @Test
    void createDelivery_thirdPartyCarrier_returns422() throws Exception {
        Long thirdPartyCarrierId = createCarrier("3PL-" + UUID.randomUUID().toString().substring(0, 6), false);
        Long shipmentId = createShipmentAtArrivedAtHub(thirdPartyCarrierId, "EXT-" + UUID.randomUUID().toString().substring(0, 8));
        Long driverId = createDriver();
        Long vehicleId = createVehicle();

        ResponseEntity<String> response = restTemplate.exchange("/api/deliveries", HttpMethod.POST,
                new HttpEntity<>(new DeliveryDto.CreateRequest(shipmentId, driverId, vehicleId, null, null, null), authHeaders),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void createDelivery_nonLandShipment_returns422() throws Exception {
        Long seaCarrierId = createCarrier("SEA-OWN-" + UUID.randomUUID().toString().substring(0, 6), true);
        Long shipmentId = createShipmentAtArrivedAtHub(seaCarrierId, null, ShipmentMedium.SEA);
        Long driverId = createDriver();
        Long vehicleId = createVehicle();

        ResponseEntity<String> response = restTemplate.exchange("/api/deliveries", HttpMethod.POST,
                new HttpEntity<>(new DeliveryDto.CreateRequest(shipmentId, driverId, vehicleId, null, null, null), authHeaders),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void createDelivery_duplicateForSameShipment_returns422() throws Exception {
        Long shipmentId = createShipmentAtArrivedAtHub();
        Long driverId1 = createDriver();
        Long vehicleId1 = createVehicle();
        restTemplate.exchange("/api/deliveries", HttpMethod.POST,
                new HttpEntity<>(new DeliveryDto.CreateRequest(shipmentId, driverId1, vehicleId1, null, null, null), authHeaders),
                String.class);

        Long driverId2 = createDriver();
        Long vehicleId2 = createVehicle();
        ResponseEntity<String> response = restTemplate.exchange("/api/deliveries", HttpMethod.POST,
                new HttpEntity<>(new DeliveryDto.CreateRequest(shipmentId, driverId2, vehicleId2, null, null, null), authHeaders),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void updateDeliveryStatus_delivered_updatesShipmentAndReleasesDriverVehicle() throws Exception {
        Long shipmentId = createShipmentAtArrivedAtHub();
        Long driverId = createDriver();
        Long vehicleId = createVehicle();

        Long deliveryId = objectMapper.readTree(restTemplate.exchange("/api/deliveries", HttpMethod.POST,
                new HttpEntity<>(new DeliveryDto.CreateRequest(shipmentId, driverId, vehicleId, null, null, null), authHeaders),
                String.class).getBody()).get("id").asLong();

        Thread.sleep(200);

        // ASSIGNED → OUT_FOR_DELIVERY
        restTemplate.exchange("/api/deliveries/" + deliveryId + "/status", HttpMethod.PUT,
                new HttpEntity<>(new DeliveryDto.UpdateStatusRequest(com.logistics.system.delivery.domain.DeliveryStatus.OUT_FOR_DELIVERY, null), authHeaders),
                String.class);

        // OUT_FOR_DELIVERY → DELIVERED
        ResponseEntity<String> response = restTemplate.exchange("/api/deliveries/" + deliveryId + "/status", HttpMethod.PUT,
                new HttpEntity<>(new DeliveryDto.UpdateStatusRequest(com.logistics.system.delivery.domain.DeliveryStatus.DELIVERED, "Signed by Jane"), authHeaders),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(objectMapper.readTree(response.getBody()).get("status").asText()).isEqualTo("DELIVERED");

        Thread.sleep(200);

        // Shipment should be DELIVERED with POD
        JsonNode shipment = objectMapper.readTree(restTemplate.exchange(
                "/api/shipments/" + shipmentId, HttpMethod.GET, new HttpEntity<>(authHeaders), String.class).getBody());
        assertThat(shipment.get("status").asText()).isEqualTo("DELIVERED");
        assertThat(shipment.get("proofOfDelivery").asText()).isEqualTo("Signed by Jane");
        assertThat(shipment.get("actualDeliveryDate").asText()).isNotBlank();

        // Driver and vehicle should be released back to AVAILABLE
        JsonNode driver = objectMapper.readTree(restTemplate.exchange(
                "/api/drivers/" + driverId, HttpMethod.GET, new HttpEntity<>(authHeaders), String.class).getBody());
        assertThat(driver.get("status").asText()).isEqualTo("AVAILABLE");

        JsonNode vehicle = objectMapper.readTree(restTemplate.exchange(
                "/api/vehicles/" + vehicleId, HttpMethod.GET, new HttpEntity<>(authHeaders), String.class).getBody());
        assertThat(vehicle.get("status").asText()).isEqualTo("AVAILABLE");
    }

    @Test
    void updateDeliveryStatus_delivered_missingPod_returns422() throws Exception {
        Long shipmentId = createShipmentAtArrivedAtHub();
        Long driverId = createDriver();
        Long vehicleId = createVehicle();

        Long deliveryId = objectMapper.readTree(restTemplate.exchange("/api/deliveries", HttpMethod.POST,
                new HttpEntity<>(new DeliveryDto.CreateRequest(shipmentId, driverId, vehicleId, null, null, null), authHeaders),
                String.class).getBody()).get("id").asLong();

        Thread.sleep(200);

        restTemplate.exchange("/api/deliveries/" + deliveryId + "/status", HttpMethod.PUT,
                new HttpEntity<>(new DeliveryDto.UpdateStatusRequest(com.logistics.system.delivery.domain.DeliveryStatus.OUT_FOR_DELIVERY, null), authHeaders),
                String.class);

        ResponseEntity<String> response = restTemplate.exchange("/api/deliveries/" + deliveryId + "/status", HttpMethod.PUT,
                new HttpEntity<>(new DeliveryDto.UpdateStatusRequest(com.logistics.system.delivery.domain.DeliveryStatus.DELIVERED, null), authHeaders),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void getAvailableDrivers_returns200() {
        ResponseEntity<String> response = restTemplate.exchange("/api/drivers/available", HttpMethod.GET,
                new HttpEntity<>(authHeaders), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getAvailableVehicles_returns200() {
        ResponseEntity<String> response = restTemplate.exchange("/api/vehicles/available", HttpMethod.GET,
                new HttpEntity<>(authHeaders), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getDelivery_nonExisting_returns404() {
        ResponseEntity<String> response = restTemplate.exchange("/api/deliveries/99999", HttpMethod.GET,
                new HttpEntity<>(authHeaders), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void unauthenticated_returns401() {
        assertThat(restTemplate.getForEntity("/api/deliveries", String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(restTemplate.getForEntity("/api/drivers", String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(restTemplate.getForEntity("/api/vehicles", String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // --- helpers ---

    private Long createDriver() throws Exception {
        DriverDto.CreateRequest request = new DriverDto.CreateRequest(
                "Driver " + UUID.randomUUID(), "LIC-" + UUID.randomUUID().toString().substring(0, 8), null);
        return objectMapper.readTree(restTemplate.exchange("/api/drivers", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders), String.class).getBody()).get("id").asLong();
    }

    private Long createVehicle() throws Exception {
        VehicleDto.CreateRequest request = new VehicleDto.CreateRequest(
                "PLT-" + UUID.randomUUID().toString().substring(0, 6), VehicleType.VAN, null);
        return objectMapper.readTree(restTemplate.exchange("/api/vehicles", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders), String.class).getBody()).get("id").asLong();
    }

    private Long createCarrier(String code, boolean companyOwned) throws Exception {
        CarrierDto.CreateRequest request = new CarrierDto.CreateRequest(
                code, "Carrier " + code, ShipmentMedium.LAND, companyOwned, null, null);
        return objectMapper.readTree(restTemplate.exchange("/api/carriers", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders), String.class).getBody()).get("id").asLong();
    }

    private Long createShipmentAtArrivedAtHub() throws Exception {
        return createShipmentAtArrivedAtHub(companyCarrierId, null, ShipmentMedium.LAND);
    }

    private Long createShipmentAtArrivedAtHub(Long carrierId, String trackingNumber) throws Exception {
        return createShipmentAtArrivedAtHub(carrierId, trackingNumber, ShipmentMedium.LAND);
    }

    private Long createShipmentAtArrivedAtHub(Long carrierId, String trackingNumber, ShipmentMedium medium) throws Exception {
        String sku = "SKU-DLV-" + UUID.randomUUID().toString().substring(0, 6);

        Long warehouseId = objectMapper.readTree(restTemplate.exchange("/api/warehouses", HttpMethod.POST,
                new HttpEntity<>(new WarehouseDto.CreateRequest("WH-DLV-" + UUID.randomUUID().toString().substring(0, 6), "WH", null), authHeaders),
                String.class).getBody()).get("id").asLong();

        Long productId = objectMapper.readTree(restTemplate.exchange("/api/products", HttpMethod.POST,
                new HttpEntity<>(new ProductDto.CreateRequest(sku, "Product", null, "EACH", 0), authHeaders),
                String.class).getBody()).get("id").asLong();

        restTemplate.exchange("/api/inventory/receive", HttpMethod.POST,
                new HttpEntity<>(new InventoryDto.ReceiveRequest(productId, warehouseId, 100, null), authHeaders), String.class);

        String email = "dlv-cust-" + UUID.randomUUID() + "@example.com";
        Long customerId = objectMapper.readTree(restTemplate.exchange("/api/customers", HttpMethod.POST,
                new HttpEntity<>(new CustomerDto.CreateRequest("Customer", email, null, null), authHeaders),
                String.class).getBody()).get("id").asLong();

        Long orderId = objectMapper.readTree(restTemplate.exchange("/api/orders", HttpMethod.POST,
                new HttpEntity<>(new OrderDto.CreateRequest(customerId,
                        List.of(new OrderDto.ItemRequest(sku, "Product", 1, BigDecimal.TEN)), null), authHeaders),
                String.class).getBody()).get("id").asLong();

        Thread.sleep(200);

        Long shipmentId = objectMapper.readTree(restTemplate.exchange("/api/shipments", HttpMethod.POST,
                new HttpEntity<>(new ShipmentDto.CreateRequest(ShipmentType.FULFILLMENT, orderId, carrierId,
                        medium, trackingNumber, null, null, null, null, "123 Delivery St", null, null), authHeaders),
                String.class).getBody()).get("id").asLong();

        Thread.sleep(200);

        // Drive shipment through to ARRIVED_AT_HUB
        ShipmentStatus[] path = {
                ShipmentStatus.PROCESSING, ShipmentStatus.READY_FOR_PICKUP,
                ShipmentStatus.PICKED_UP, ShipmentStatus.IN_TRANSIT, ShipmentStatus.ARRIVED_AT_HUB
        };
        for (ShipmentStatus s : path) {
            restTemplate.exchange("/api/shipments/" + shipmentId + "/status", HttpMethod.PUT,
                    new HttpEntity<>(new ShipmentDto.UpdateStatusRequest(s, null), authHeaders), String.class);
        }

        return shipmentId;
    }
}

package com.logistics.system.shipment.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.system.common.AuthTestHelper;
import com.logistics.system.common.BaseIntegrationTest;
import com.logistics.system.inventory.dto.InventoryDto;
import com.logistics.system.inventory.dto.ProductDto;
import com.logistics.system.order.dto.CustomerDto;
import com.logistics.system.order.dto.OrderDto;
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

class ShipmentControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private ObjectMapper objectMapper;

    private HttpHeaders authHeaders;
    private Long companyCarrierId;
    private Long thirdPartyCarrierId;

    @BeforeEach
    void setUp() throws Exception {
        String token = AuthTestHelper.obtainToken(restTemplate, objectMapper,
                "shipment-it@example.com", "password123");
        authHeaders = new HttpHeaders();
        authHeaders.setBearerAuth(token);
        authHeaders.setContentType(MediaType.APPLICATION_JSON);

        companyCarrierId = createCarrier("OWN-" + UUID.randomUUID().toString().substring(0, 6), true);
        thirdPartyCarrierId = createCarrier("DHL-" + UUID.randomUUID().toString().substring(0, 6), false);
    }

    @Test
    void createCarrier_validRequest_returns201() throws Exception {
        CarrierDto.CreateRequest request = new CarrierDto.CreateRequest(
                "CARR-" + UUID.randomUUID().toString().substring(0, 6),
                "Test Carrier", ShipmentMedium.SEA, false, "John", "555-0000");

        ResponseEntity<String> response = restTemplate.exchange("/api/carriers", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("id").asLong()).isPositive();
        assertThat(body.get("active").asBoolean()).isTrue();
    }

    @Test
    void createCarrier_duplicateCode_returns422() throws Exception {
        String code = "DUP-" + UUID.randomUUID().toString().substring(0, 6);
        CarrierDto.CreateRequest request = new CarrierDto.CreateRequest(code, "Carrier", ShipmentMedium.LAND, false, null, null);
        restTemplate.exchange("/api/carriers", HttpMethod.POST, new HttpEntity<>(request, authHeaders), String.class);

        ResponseEntity<String> response = restTemplate.exchange("/api/carriers", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void getCarriers_unauthenticated_returns401() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/carriers", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void createFulfillmentShipment_companyCarrier_autoGeneratesTrackingNumber() throws Exception {
        Long orderId = createStockReservedOrder();

        ShipmentDto.CreateRequest request = new ShipmentDto.CreateRequest(
                ShipmentType.FULFILLMENT, orderId, companyCarrierId, ShipmentMedium.LAND,
                null, null, null, BigDecimal.valueOf(20.5), BigDecimal.valueOf(150),
                "123 Delivery St", null, "Handle with care");

        ResponseEntity<String> response = restTemplate.exchange("/api/shipments", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("trackingNumber").asText()).startsWith("TRK-");
        assertThat(body.get("shipmentType").asText()).isEqualTo("FULFILLMENT");
        assertThat(body.get("orderId").asLong()).isEqualTo(orderId);
    }

    @Test
    void createFulfillmentShipment_orderTransitionsToReadyToShip() throws Exception {
        Long orderId = createStockReservedOrder();

        restTemplate.exchange("/api/shipments", HttpMethod.POST,
                new HttpEntity<>(new ShipmentDto.CreateRequest(
                        ShipmentType.FULFILLMENT, orderId, companyCarrierId, ShipmentMedium.LAND,
                        null, null, null, null, null, "123 Delivery St", null, null),
                        authHeaders), String.class);

        ResponseEntity<String> orderResponse = restTemplate.exchange(
                "/api/orders/" + orderId, HttpMethod.GET, new HttpEntity<>(authHeaders), String.class);
        assertThat(objectMapper.readTree(orderResponse.getBody()).get("status").asText())
                .isEqualTo("READY_TO_SHIP");
    }

    @Test
    void createFulfillmentShipment_thirdPartyCarrier_requiresTrackingNumber() throws Exception {
        Long orderId = createStockReservedOrder();

        ShipmentDto.CreateRequest request = new ShipmentDto.CreateRequest(
                ShipmentType.FULFILLMENT, orderId, thirdPartyCarrierId, ShipmentMedium.AIR,
                null, null, null, null, null, "123 Delivery St", null, null);

        ResponseEntity<String> response = restTemplate.exchange("/api/shipments", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void createFulfillmentShipment_thirdPartyCarrier_withTrackingNumber_returns201() throws Exception {
        Long orderId = createStockReservedOrder();

        ShipmentDto.CreateRequest request = new ShipmentDto.CreateRequest(
                ShipmentType.FULFILLMENT, orderId, thirdPartyCarrierId, ShipmentMedium.AIR,
                "DHL-" + UUID.randomUUID().toString().substring(0, 8),
                null, null, null, null, "123 Delivery St", null, null);

        ResponseEntity<String> response = restTemplate.exchange("/api/shipments", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(objectMapper.readTree(response.getBody()).get("trackingNumber").asText()).startsWith("DHL-");
    }

    @Test
    void createStandaloneShipment_noOrder_returns201() throws Exception {
        ShipmentDto.CreateRequest request = new ShipmentDto.CreateRequest(
                ShipmentType.STANDALONE, null, companyCarrierId, ShipmentMedium.SEA,
                null, "100 crates of fruits", 100, BigDecimal.valueOf(500), BigDecimal.valueOf(2000),
                "Port of Manila", null, "Fragile");

        ResponseEntity<String> response = restTemplate.exchange("/api/shipments", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("shipmentType").asText()).isEqualTo("STANDALONE");
        assertThat(body.get("orderId").isNull()).isTrue();
        assertThat(body.get("cargoDescription").asText()).isEqualTo("100 crates of fruits");
        assertThat(body.get("trackingNumber").asText()).startsWith("TRK-");
    }

    @Test
    void createStandaloneShipment_missingCargoDescription_returns422() {
        ShipmentDto.CreateRequest request = new ShipmentDto.CreateRequest(
                ShipmentType.STANDALONE, null, companyCarrierId, ShipmentMedium.LAND,
                null, null, null, null, null, "123 Delivery St", null, null);

        ResponseEntity<String> response = restTemplate.exchange("/api/shipments", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void updateStatus_validTransition_returns200() throws Exception {
        Long shipmentId = createStandaloneShipment();

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/shipments/" + shipmentId + "/status", HttpMethod.PUT,
                new HttpEntity<>(new ShipmentDto.UpdateStatusRequest(ShipmentStatus.PROCESSING, null), authHeaders),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(objectMapper.readTree(response.getBody()).get("status").asText()).isEqualTo("PROCESSING");
    }

    @Test
    void updateStatus_delivered_requiresProofOfDelivery_returns422() throws Exception {
        Long shipmentId = createShipmentAtStatus(ShipmentStatus.OUT_FOR_DELIVERY);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/shipments/" + shipmentId + "/status", HttpMethod.PUT,
                new HttpEntity<>(new ShipmentDto.UpdateStatusRequest(ShipmentStatus.DELIVERED, null), authHeaders),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void getByTrackingNumber_returns200() throws Exception {
        Long shipmentId = createStandaloneShipment();
        String trackingNumber = objectMapper.readTree(
                restTemplate.exchange("/api/shipments/" + shipmentId, HttpMethod.GET,
                        new HttpEntity<>(authHeaders), String.class).getBody())
                .get("trackingNumber").asText();

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/shipments/tracking/" + trackingNumber, HttpMethod.GET,
                new HttpEntity<>(authHeaders), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getById_nonExisting_returns404() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/shipments/99999", HttpMethod.GET, new HttpEntity<>(authHeaders), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateStatus_delivered_withProofOfDelivery_returns200() throws Exception {
        // Use third-party carrier standalone shipment — company-owned goes through Delivery module
        Long shipmentId = createThirdPartyStandaloneShipmentAtStatus(ShipmentStatus.OUT_FOR_DELIVERY);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/shipments/" + shipmentId + "/status", HttpMethod.PUT,
                new HttpEntity<>(new ShipmentDto.UpdateStatusRequest(ShipmentStatus.DELIVERED, "Signed by Jane"), authHeaders),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("status").asText()).isEqualTo("DELIVERED");
        assertThat(body.get("proofOfDelivery").asText()).isEqualTo("Signed by Jane");
        assertThat(body.get("actualDeliveryDate").asText()).isNotBlank();
    }

    @Test
    void updateStatus_cancelledShipment_returns422() throws Exception {
        Long shipmentId = createStandaloneShipment();
        restTemplate.exchange("/api/shipments/" + shipmentId + "/status", HttpMethod.PUT,
                new HttpEntity<>(new ShipmentDto.UpdateStatusRequest(ShipmentStatus.CANCELLED, null), authHeaders),
                String.class);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/shipments/" + shipmentId + "/status", HttpMethod.PUT,
                new HttpEntity<>(new ShipmentDto.UpdateStatusRequest(ShipmentStatus.PROCESSING, null), authHeaders),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void updateStatus_failedShipment_returns422() throws Exception {
        Long shipmentId = createShipmentAtStatus(ShipmentStatus.IN_TRANSIT);
        restTemplate.exchange("/api/shipments/" + shipmentId + "/status", HttpMethod.PUT,
                new HttpEntity<>(new ShipmentDto.UpdateStatusRequest(ShipmentStatus.FAILED, null), authHeaders),
                String.class);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/shipments/" + shipmentId + "/status", HttpMethod.PUT,
                new HttpEntity<>(new ShipmentDto.UpdateStatusRequest(ShipmentStatus.IN_TRANSIT, null), authHeaders),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void createFulfillmentShipment_duplicateOrderId_returns422() throws Exception {
        Long orderId = createStockReservedOrder();

        ShipmentDto.CreateRequest request = new ShipmentDto.CreateRequest(
                ShipmentType.FULFILLMENT, orderId, companyCarrierId, ShipmentMedium.LAND,
                null, null, null, null, null, "123 Delivery St", null, null);

        restTemplate.exchange("/api/shipments", HttpMethod.POST, new HttpEntity<>(request, authHeaders), String.class);

        ResponseEntity<String> response = restTemplate.exchange("/api/shipments", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void createFulfillmentShipment_nonExistingOrder_returns404() {
        ShipmentDto.CreateRequest request = new ShipmentDto.CreateRequest(
                ShipmentType.FULFILLMENT, 99999L, companyCarrierId, ShipmentMedium.LAND,
                null, null, null, null, null, "123 Delivery St", null, null);

        ResponseEntity<String> response = restTemplate.exchange("/api/shipments", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void createFulfillmentShipment_orderNotStockReserved_returns422() throws Exception {
        // Create an order that ends up FAILED (no inventory) — not STOCK_RESERVED
        String email = "shp-cust-norsvd-" + UUID.randomUUID() + "@example.com";
        Long customerId = objectMapper.readTree(restTemplate.exchange("/api/customers", HttpMethod.POST,
                new HttpEntity<>(new com.logistics.system.order.dto.CustomerDto.CreateRequest("Customer", email, null, null), authHeaders),
                String.class).getBody()).get("id").asLong();

        Long orderId = objectMapper.readTree(restTemplate.exchange("/api/orders", HttpMethod.POST,
                new HttpEntity<>(new com.logistics.system.order.dto.OrderDto.CreateRequest(customerId,
                        List.of(new com.logistics.system.order.dto.OrderDto.ItemRequest("SKU-NO-STOCK", "X", 1, BigDecimal.TEN)), null),
                        authHeaders), String.class).getBody()).get("id").asLong();

        Thread.sleep(200);

        ShipmentDto.CreateRequest request = new ShipmentDto.CreateRequest(
                ShipmentType.FULFILLMENT, orderId, companyCarrierId, ShipmentMedium.LAND,
                null, null, null, null, null, "123 Delivery St", null, null);

        ResponseEntity<String> response = restTemplate.exchange("/api/shipments", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    // --- helpers ---

    private Long createCarrier(String code, boolean companyOwned) throws Exception {
        CarrierDto.CreateRequest request = new CarrierDto.CreateRequest(
                code, "Carrier " + code, ShipmentMedium.LAND, companyOwned, null, null);
        ResponseEntity<String> response = restTemplate.exchange("/api/carriers", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders), String.class);
        return objectMapper.readTree(response.getBody()).get("id").asLong();
    }

    private Long createStockReservedOrder() throws Exception {
        String sku = "SKU-SHP-" + UUID.randomUUID().toString().substring(0, 6);

        WarehouseDto.CreateRequest wh = new WarehouseDto.CreateRequest("WH-SHP-" + UUID.randomUUID().toString().substring(0, 6), "WH", null);
        Long warehouseId = objectMapper.readTree(restTemplate.exchange("/api/warehouses", HttpMethod.POST,
                new HttpEntity<>(wh, authHeaders), String.class).getBody()).get("id").asLong();

        ProductDto.CreateRequest prod = new ProductDto.CreateRequest(sku, "Product", null, "EACH", 0);
        Long productId = objectMapper.readTree(restTemplate.exchange("/api/products", HttpMethod.POST,
                new HttpEntity<>(prod, authHeaders), String.class).getBody()).get("id").asLong();

        restTemplate.exchange("/api/inventory/receive", HttpMethod.POST,
                new HttpEntity<>(new InventoryDto.ReceiveRequest(productId, warehouseId, 100, null), authHeaders), String.class);

        String email = "shp-cust-" + UUID.randomUUID() + "@example.com";
        Long customerId = objectMapper.readTree(restTemplate.exchange("/api/customers", HttpMethod.POST,
                new HttpEntity<>(new CustomerDto.CreateRequest("Customer", email, null, null), authHeaders), String.class)
                .getBody()).get("id").asLong();

        ResponseEntity<String> orderResp = restTemplate.exchange("/api/orders", HttpMethod.POST,
                new HttpEntity<>(new OrderDto.CreateRequest(customerId,
                        List.of(new OrderDto.ItemRequest(sku, "Product", 5, BigDecimal.TEN)), null),
                        authHeaders), String.class);
        Long orderId = objectMapper.readTree(orderResp.getBody()).get("id").asLong();

        // wait for event to process
        Thread.sleep(200);

        return orderId;
    }

    private Long createStandaloneShipment() throws Exception {
        ShipmentDto.CreateRequest request = new ShipmentDto.CreateRequest(
                ShipmentType.STANDALONE, null, companyCarrierId, ShipmentMedium.LAND,
                null, "Test cargo", 10, null, null, "123 Test St", null, null);
        ResponseEntity<String> response = restTemplate.exchange("/api/shipments", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders), String.class);
        return objectMapper.readTree(response.getBody()).get("id").asLong();
    }

    private Long createShipmentAtStatus(ShipmentStatus targetStatus) throws Exception {
        Long shipmentId = createStandaloneShipment();
        ShipmentStatus[] path = {
                ShipmentStatus.PROCESSING, ShipmentStatus.READY_FOR_PICKUP,
                ShipmentStatus.PICKED_UP, ShipmentStatus.IN_TRANSIT,
                ShipmentStatus.ARRIVED_AT_HUB, ShipmentStatus.OUT_FOR_DELIVERY
        };
        for (ShipmentStatus s : path) {
            restTemplate.exchange("/api/shipments/" + shipmentId + "/status", HttpMethod.PUT,
                    new HttpEntity<>(new ShipmentDto.UpdateStatusRequest(s, null), authHeaders), String.class);
            if (s == targetStatus) break;
        }
        return shipmentId;
    }

    private Long createThirdPartyStandaloneShipmentAtStatus(ShipmentStatus targetStatus) throws Exception {
        ShipmentDto.CreateRequest request = new ShipmentDto.CreateRequest(
                ShipmentType.STANDALONE, null, thirdPartyCarrierId, ShipmentMedium.LAND,
                "EXT-" + UUID.randomUUID().toString().substring(0, 8),
                "Test cargo", 10, null, null, "123 Test St", null, null);
        Long shipmentId = objectMapper.readTree(restTemplate.exchange("/api/shipments", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders), String.class).getBody()).get("id").asLong();
        ShipmentStatus[] path = {
                ShipmentStatus.PROCESSING, ShipmentStatus.READY_FOR_PICKUP,
                ShipmentStatus.PICKED_UP, ShipmentStatus.IN_TRANSIT,
                ShipmentStatus.ARRIVED_AT_HUB, ShipmentStatus.OUT_FOR_DELIVERY
        };
        for (ShipmentStatus s : path) {
            restTemplate.exchange("/api/shipments/" + shipmentId + "/status", HttpMethod.PUT,
                    new HttpEntity<>(new ShipmentDto.UpdateStatusRequest(s, null), authHeaders), String.class);
            if (s == targetStatus) break;
        }
        return shipmentId;
    }
}

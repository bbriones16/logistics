package com.logistics.system.tracking.controller;

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

class TrackingControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private ObjectMapper objectMapper;

    private HttpHeaders authHeaders;
    private Long thirdPartyCarrierId;

    @BeforeEach
    void setUp() throws Exception {
        String token = AuthTestHelper.obtainToken(restTemplate, objectMapper,
                "tracking-it@example.com", "password123");
        authHeaders = new HttpHeaders();
        authHeaders.setBearerAuth(token);
        authHeaders.setContentType(MediaType.APPLICATION_JSON);

        thirdPartyCarrierId = createCarrier("3PL-TRK-" + UUID.randomUUID().toString().substring(0, 6), false);
    }

    @Test
    void getByTrackingNumber_public_noAuthRequired_returns200() throws Exception {
        String trackingNumber = createShipmentAndGetTrackingNumber();

        Thread.sleep(200);

        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/tracking/" + trackingNumber, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("trackingNumber").asText()).isEqualTo(trackingNumber);
        assertThat(body.get("currentStatus").asText()).isEqualTo("CREATED");
        assertThat(body.get("events").isArray()).isTrue();
        assertThat(body.get("events").size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void getByTrackingNumber_recordsEachStatusChange() throws Exception {
        String[] result = createShipmentAndAdvanceToInTransit();
        String trackingNumber = result[0];
        Long shipmentId = Long.parseLong(result[1]);

        Thread.sleep(200);

        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/tracking/" + trackingNumber, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("currentStatus").asText()).isEqualTo("IN_TRANSIT");
        // CREATED + PROCESSING + READY_FOR_PICKUP + PICKED_UP + IN_TRANSIT = 5 events
        assertThat(body.get("events").size()).isEqualTo(5);
    }

    @Test
    void getByTrackingNumber_unknownTrackingNumber_returns404() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/tracking/UNKNOWN-TRK-99999", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getByShipmentId_authenticated_returns200() throws Exception {
        String[] result = createShipmentAndAdvanceToInTransit();
        Long shipmentId = Long.parseLong(result[1]);

        Thread.sleep(200);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/shipments/" + shipmentId + "/tracking", HttpMethod.GET,
                new HttpEntity<>(authHeaders), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("shipmentId").asLong()).isEqualTo(shipmentId);
        assertThat(body.get("currentStatus").asText()).isEqualTo("IN_TRANSIT");
    }

    @Test
    void getByShipmentId_unauthenticated_returns401() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/shipments/1/tracking", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getByShipmentId_nonExisting_returns404() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/shipments/99999/tracking", HttpMethod.GET,
                new HttpEntity<>(authHeaders), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // --- helpers ---

    private String createShipmentAndGetTrackingNumber() throws Exception {
        return createShipmentAndAdvanceToInTransit(0)[0];
    }

    private String[] createShipmentAndAdvanceToInTransit() throws Exception {
        return createShipmentAndAdvanceToInTransit(4);
    }

    private String[] createShipmentAndAdvanceToInTransit(int steps) throws Exception {
        String sku = "SKU-TRK-" + UUID.randomUUID().toString().substring(0, 6);

        Long warehouseId = objectMapper.readTree(restTemplate.exchange("/api/warehouses", HttpMethod.POST,
                new HttpEntity<>(new WarehouseDto.CreateRequest("WH-TRK-" + UUID.randomUUID().toString().substring(0, 6), "WH", null), authHeaders),
                String.class).getBody()).get("id").asLong();

        Long productId = objectMapper.readTree(restTemplate.exchange("/api/products", HttpMethod.POST,
                new HttpEntity<>(new ProductDto.CreateRequest(sku, "Product", null, "EACH", 0), authHeaders),
                String.class).getBody()).get("id").asLong();

        restTemplate.exchange("/api/inventory/receive", HttpMethod.POST,
                new HttpEntity<>(new InventoryDto.ReceiveRequest(productId, warehouseId, 100, null), authHeaders), String.class);

        String email = "trk-cust-" + UUID.randomUUID() + "@example.com";
        Long customerId = objectMapper.readTree(restTemplate.exchange("/api/customers", HttpMethod.POST,
                new HttpEntity<>(new CustomerDto.CreateRequest("Customer", email, null, null), authHeaders),
                String.class).getBody()).get("id").asLong();

        Long orderId = objectMapper.readTree(restTemplate.exchange("/api/orders", HttpMethod.POST,
                new HttpEntity<>(new OrderDto.CreateRequest(customerId,
                        List.of(new OrderDto.ItemRequest(sku, "Product", 1, BigDecimal.TEN)), null), authHeaders),
                String.class).getBody()).get("id").asLong();

        Thread.sleep(200);

        String extTracking = "EXT-" + UUID.randomUUID().toString().substring(0, 8);
        JsonNode shipmentNode = objectMapper.readTree(restTemplate.exchange("/api/shipments", HttpMethod.POST,
                new HttpEntity<>(new ShipmentDto.CreateRequest(ShipmentType.FULFILLMENT, orderId, thirdPartyCarrierId,
                        ShipmentMedium.LAND, extTracking, null, null, null, null, "123 Track St", null, null), authHeaders),
                String.class).getBody());

        Long shipmentId = shipmentNode.get("id").asLong();
        String trackingNumber = shipmentNode.get("trackingNumber").asText();

        Thread.sleep(200);

        ShipmentStatus[] path = {
                ShipmentStatus.PROCESSING, ShipmentStatus.READY_FOR_PICKUP,
                ShipmentStatus.PICKED_UP, ShipmentStatus.IN_TRANSIT
        };
        for (int i = 0; i < steps; i++) {
            restTemplate.exchange("/api/shipments/" + shipmentId + "/status", HttpMethod.PUT,
                    new HttpEntity<>(new ShipmentDto.UpdateStatusRequest(path[i], null), authHeaders), String.class);
        }

        return new String[]{trackingNumber, String.valueOf(shipmentId)};
    }

    private Long createCarrier(String code, boolean companyOwned) throws Exception {
        CarrierDto.CreateRequest request = new CarrierDto.CreateRequest(
                code, "Carrier " + code, ShipmentMedium.LAND, companyOwned, null, null);
        return objectMapper.readTree(restTemplate.exchange("/api/carriers", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders), String.class).getBody()).get("id").asLong();
    }
}

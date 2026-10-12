package com.logistics.system.reporting.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.system.common.AuthTestHelper;
import com.logistics.system.common.BaseIntegrationTest;
import com.logistics.system.inventory.dto.InventoryDto;
import com.logistics.system.inventory.dto.ProductDto;
import com.logistics.system.order.dto.CustomerDto;
import com.logistics.system.order.dto.OrderDto;
import com.logistics.system.shipment.domain.ShipmentMedium;
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

class ReportingControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private ObjectMapper objectMapper;

    private HttpHeaders authHeaders;

    @BeforeEach
    void setUp() throws Exception {
        String token = AuthTestHelper.obtainToken(restTemplate, objectMapper,
                "reporting-it@example.com", "password123");
        authHeaders = new HttpHeaders();
        authHeaders.setBearerAuth(token);
        authHeaders.setContentType(MediaType.APPLICATION_JSON);
    }

    @Test
    void getOrderSummary_returns200WithExpectedStructure() throws Exception {
        createOrder();

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/reports/orders", HttpMethod.GET, new HttpEntity<>(authHeaders), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.has("total")).isTrue();
        assertThat(body.has("createdToday")).isTrue();
        assertThat(body.has("byStatus")).isTrue();
        assertThat(body.get("byStatus").has("CREATED")).isTrue();
        assertThat(body.get("byStatus").has("DELIVERED")).isTrue();
        assertThat(body.get("total").asLong()).isGreaterThanOrEqualTo(1L);
        assertThat(body.get("createdToday").asLong()).isGreaterThanOrEqualTo(1L);
    }

    @Test
    void getShipmentSummary_returns200WithExpectedStructure() throws Exception {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/reports/shipments", HttpMethod.GET, new HttpEntity<>(authHeaders), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.has("total")).isTrue();
        assertThat(body.has("byStatus")).isTrue();
        assertThat(body.get("byStatus").has("CREATED")).isTrue();
        assertThat(body.get("byStatus").has("IN_TRANSIT")).isTrue();
        assertThat(body.get("byStatus").has("DELIVERED")).isTrue();
    }

    @Test
    void getDeliverySummary_returns200WithExpectedStructure() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/reports/deliveries", HttpMethod.GET, new HttpEntity<>(authHeaders), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body;
        try {
            body = objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        assertThat(body.has("total")).isTrue();
        assertThat(body.has("scheduledToday")).isTrue();
        assertThat(body.has("byStatus")).isTrue();
        assertThat(body.get("byStatus").has("ASSIGNED")).isTrue();
        assertThat(body.get("byStatus").has("DELIVERED")).isTrue();
    }

    @Test
    void getInventorySummary_returns200WithExpectedStructure() throws Exception {
        createProductWithStock();

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/reports/inventory/summary", HttpMethod.GET, new HttpEntity<>(authHeaders), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.has("totalProducts")).isTrue();
        assertThat(body.has("totalWarehouses")).isTrue();
        assertThat(body.has("totalStockOnHand")).isTrue();
        assertThat(body.has("lowStockItemCount")).isTrue();
        assertThat(body.get("totalProducts").asLong()).isGreaterThanOrEqualTo(1L);
        assertThat(body.get("totalStockOnHand").asLong()).isGreaterThanOrEqualTo(100L);
    }

    @Test
    void getLowStockReport_returns200WithExpectedStructure() throws Exception {
        createProductWithLowStock();

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/reports/inventory/low-stock", HttpMethod.GET, new HttpEntity<>(authHeaders), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.has("count")).isTrue();
        assertThat(body.has("items")).isTrue();
        assertThat(body.get("items").isArray()).isTrue();
        assertThat(body.get("count").asLong()).isGreaterThanOrEqualTo(1L);
    }

    @Test
    void allReportEndpoints_unauthenticated_return401() {
        assertThat(restTemplate.getForEntity("/api/reports/orders", String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(restTemplate.getForEntity("/api/reports/shipments", String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(restTemplate.getForEntity("/api/reports/deliveries", String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(restTemplate.getForEntity("/api/reports/inventory/summary", String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(restTemplate.getForEntity("/api/reports/inventory/low-stock", String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // --- helpers ---

    private void createOrder() throws Exception {
        String sku = "SKU-RPT-" + UUID.randomUUID().toString().substring(0, 6);

        Long warehouseId = objectMapper.readTree(restTemplate.exchange("/api/warehouses", HttpMethod.POST,
                new HttpEntity<>(new WarehouseDto.CreateRequest("WH-RPT-" + UUID.randomUUID().toString().substring(0, 6), "WH", null), authHeaders),
                String.class).getBody()).get("id").asLong();

        Long productId = objectMapper.readTree(restTemplate.exchange("/api/products", HttpMethod.POST,
                new HttpEntity<>(new ProductDto.CreateRequest(sku, "Product", null, "EACH", 0), authHeaders),
                String.class).getBody()).get("id").asLong();

        restTemplate.exchange("/api/inventory/receive", HttpMethod.POST,
                new HttpEntity<>(new InventoryDto.ReceiveRequest(productId, warehouseId, 100, null), authHeaders), String.class);

        String email = "rpt-cust-" + UUID.randomUUID() + "@example.com";
        Long customerId = objectMapper.readTree(restTemplate.exchange("/api/customers", HttpMethod.POST,
                new HttpEntity<>(new CustomerDto.CreateRequest("Customer", email, null, null), authHeaders),
                String.class).getBody()).get("id").asLong();

        restTemplate.exchange("/api/orders", HttpMethod.POST,
                new HttpEntity<>(new OrderDto.CreateRequest(customerId,
                        List.of(new OrderDto.ItemRequest(sku, "Product", 1, BigDecimal.TEN)), null), authHeaders),
                String.class);
    }

    private void createProductWithStock() throws Exception {
        String sku = "SKU-RPT-STK-" + UUID.randomUUID().toString().substring(0, 6);

        Long warehouseId = objectMapper.readTree(restTemplate.exchange("/api/warehouses", HttpMethod.POST,
                new HttpEntity<>(new WarehouseDto.CreateRequest("WH-RPT-STK-" + UUID.randomUUID().toString().substring(0, 6), "WH", null), authHeaders),
                String.class).getBody()).get("id").asLong();

        Long productId = objectMapper.readTree(restTemplate.exchange("/api/products", HttpMethod.POST,
                new HttpEntity<>(new ProductDto.CreateRequest(sku, "Product", null, "EACH", 0), authHeaders),
                String.class).getBody()).get("id").asLong();

        restTemplate.exchange("/api/inventory/receive", HttpMethod.POST,
                new HttpEntity<>(new InventoryDto.ReceiveRequest(productId, warehouseId, 100, null), authHeaders), String.class);
    }

    private void createProductWithLowStock() throws Exception {
        String sku = "SKU-RPT-LOW-" + UUID.randomUUID().toString().substring(0, 6);

        Long warehouseId = objectMapper.readTree(restTemplate.exchange("/api/warehouses", HttpMethod.POST,
                new HttpEntity<>(new WarehouseDto.CreateRequest("WH-RPT-LOW-" + UUID.randomUUID().toString().substring(0, 6), "WH", null), authHeaders),
                String.class).getBody()).get("id").asLong();

        // low_stock_threshold = 50, but we only receive 5 → triggers low stock
        Long productId = objectMapper.readTree(restTemplate.exchange("/api/products", HttpMethod.POST,
                new HttpEntity<>(new ProductDto.CreateRequest(sku, "Low Stock Product", null, "EACH", 50), authHeaders),
                String.class).getBody()).get("id").asLong();

        restTemplate.exchange("/api/inventory/receive", HttpMethod.POST,
                new HttpEntity<>(new InventoryDto.ReceiveRequest(productId, warehouseId, 5, null), authHeaders), String.class);
    }
}

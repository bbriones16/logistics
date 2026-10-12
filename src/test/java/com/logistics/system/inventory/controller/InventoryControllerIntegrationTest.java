package com.logistics.system.inventory.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.system.common.AuthTestHelper;
import com.logistics.system.common.BaseIntegrationTest;
import com.logistics.system.inventory.dto.InventoryDto;
import com.logistics.system.inventory.dto.ProductDto;
import com.logistics.system.order.dto.CustomerDto;
import com.logistics.system.order.dto.OrderDto;
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

class InventoryControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private ObjectMapper objectMapper;

    private HttpHeaders authHeaders;
    private Long productId;
    private Long warehouseId;
    private String productSku;

    @BeforeEach
    void setUp() throws Exception {
        String token = AuthTestHelper.obtainToken(restTemplate, objectMapper,
                "inventory-it@example.com", "password123");
        authHeaders = new HttpHeaders();
        authHeaders.setBearerAuth(token);
        authHeaders.setContentType(MediaType.APPLICATION_JSON);

        productSku = "SKU-INV-" + UUID.randomUUID().toString().substring(0, 8);
        productId = createProduct(productSku);
        warehouseId = createWarehouse("WH-INV-" + UUID.randomUUID().toString().substring(0, 6));
    }

    @Test
    void receive_validRequest_returns200WithUpdatedStock() throws Exception {
        InventoryDto.ReceiveRequest request = new InventoryDto.ReceiveRequest(productId, warehouseId, 50, "Initial stock");

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/inventory/receive", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("quantityOnHand").asInt()).isEqualTo(50);
        assertThat(body.get("availableQuantity").asInt()).isEqualTo(50);
    }

    @Test
    void receive_nonExistingProduct_returns404() {
        InventoryDto.ReceiveRequest request = new InventoryDto.ReceiveRequest(99999L, warehouseId, 10, null);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/inventory/receive", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getByWarehouse_returns200() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/inventory/warehouse/" + warehouseId, HttpMethod.GET,
                new HttpEntity<>(authHeaders), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getByProduct_returns200() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/inventory/product/" + productId, HttpMethod.GET,
                new HttpEntity<>(authHeaders), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getLowStock_returns200() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/inventory/low-stock", HttpMethod.GET,
                new HttpEntity<>(authHeaders), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void pick_insufficientReserved_returns422() {
        receiveStock(productId, warehouseId, 50);

        InventoryDto.PickRequest request = new InventoryDto.PickRequest(productId, warehouseId, 10, 1L);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/inventory/pick", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void orderCreated_eventTriggersStockReservation() throws Exception {
        receiveStock(productId, warehouseId, 100);

        Long customerId = createCustomer();
        ResponseEntity<String> orderResponse = restTemplate.exchange("/api/orders", HttpMethod.POST,
                new HttpEntity<>(new OrderDto.CreateRequest(
                        customerId,
                        List.of(new OrderDto.ItemRequest(productSku, "Product A", 5, BigDecimal.valueOf(10.00))),
                        "Event test order"
                ), authHeaders), String.class);

        Long orderId = objectMapper.readTree(orderResponse.getBody()).get("id").asLong();

        ResponseEntity<String> stockResponse = restTemplate.exchange(
                "/api/inventory/product/" + productId, HttpMethod.GET,
                new HttpEntity<>(authHeaders), String.class);
        JsonNode stocks = objectMapper.readTree(stockResponse.getBody());
        assertThat(stocks.get(0).get("quantityReserved").asInt()).isEqualTo(5);
        assertThat(stocks.get(0).get("availableQuantity").asInt()).isEqualTo(95);

        ResponseEntity<String> orderFetch = restTemplate.exchange(
                "/api/orders/" + orderId, HttpMethod.GET, new HttpEntity<>(authHeaders), String.class);
        assertThat(objectMapper.readTree(orderFetch.getBody()).get("status").asText())
                .isEqualTo("STOCK_RESERVED");
    }

    @Test
    void orderCreated_multipleItems_secondSkuNoStock_noPartialReservation() throws Exception {
        // SKU with stock
        receiveStock(productId, warehouseId, 100);

        // Second SKU with no stock
        String sku2 = "SKU-INV2-" + UUID.randomUUID().toString().substring(0, 8);
        createProduct(sku2);

        Long customerId = createCustomer();
        ResponseEntity<String> orderResponse = restTemplate.exchange("/api/orders", HttpMethod.POST,
                new HttpEntity<>(new OrderDto.CreateRequest(
                        customerId,
                        List.of(
                                new OrderDto.ItemRequest(productSku, "Product A", 5, BigDecimal.valueOf(10.00)),
                                new OrderDto.ItemRequest(sku2, "Product B", 3, BigDecimal.valueOf(10.00))
                        ),
                        "Partial stock order"
                ), authHeaders), String.class);

        Long orderId = objectMapper.readTree(orderResponse.getBody()).get("id").asLong();

        // Order must be FAILED — not partially reserved
        ResponseEntity<String> orderFetch = restTemplate.exchange(
                "/api/orders/" + orderId, HttpMethod.GET, new HttpEntity<>(authHeaders), String.class);
        assertThat(objectMapper.readTree(orderFetch.getBody()).get("status").asText()).isEqualTo("FAILED");

        // SKU-001 stock must be untouched — no partial reservation
        JsonNode stocks = objectMapper.readTree(restTemplate.exchange(
                "/api/inventory/product/" + productId, HttpMethod.GET,
                new HttpEntity<>(authHeaders), String.class).getBody());
        assertThat(stocks.get(0).get("quantityReserved").asInt()).isEqualTo(0);
        assertThat(stocks.get(0).get("availableQuantity").asInt()).isEqualTo(100);
    }

    @Test
    void orderCreated_insufficientStock_orderTransitionsToFailed() throws Exception {
        Long customerId = createCustomer();
        ResponseEntity<String> orderResponse = restTemplate.exchange("/api/orders", HttpMethod.POST,
                new HttpEntity<>(new OrderDto.CreateRequest(
                        customerId,
                        List.of(new OrderDto.ItemRequest(productSku, "Product A", 5, BigDecimal.valueOf(10.00))),
                        "No stock order"
                ), authHeaders), String.class);

        Long orderId = objectMapper.readTree(orderResponse.getBody()).get("id").asLong();

        ResponseEntity<String> orderFetch = restTemplate.exchange(
                "/api/orders/" + orderId, HttpMethod.GET, new HttpEntity<>(authHeaders), String.class);
        JsonNode order = objectMapper.readTree(orderFetch.getBody());
        assertThat(order.get("status").asText()).isEqualTo("FAILED");
        assertThat(order.get("failureReason").asText()).contains(productSku);
    }

    private Long createProduct(String sku) throws Exception {
        ProductDto.CreateRequest request = new ProductDto.CreateRequest(sku, "Test Product", null, "EACH", 5);
        ResponseEntity<String> response = restTemplate.exchange("/api/products", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders), String.class);
        return objectMapper.readTree(response.getBody()).get("id").asLong();
    }

    private Long createWarehouse(String code) throws Exception {
        WarehouseDto.CreateRequest request = new WarehouseDto.CreateRequest(code, "Test Warehouse", null);
        ResponseEntity<String> response = restTemplate.exchange("/api/warehouses", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders), String.class);
        return objectMapper.readTree(response.getBody()).get("id").asLong();
    }

    private void receiveStock(Long productId, Long warehouseId, int qty) {
        restTemplate.exchange("/api/inventory/receive", HttpMethod.POST,
                new HttpEntity<>(new InventoryDto.ReceiveRequest(productId, warehouseId, qty, null), authHeaders),
                String.class);
    }

    private Long createCustomer() throws Exception {
        String email = "inv-customer-" + UUID.randomUUID() + "@example.com";
        CustomerDto.CreateRequest request = new CustomerDto.CreateRequest("Test Customer", email, null, null);
        ResponseEntity<String> response = restTemplate.exchange("/api/customers", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders), String.class);
        return objectMapper.readTree(response.getBody()).get("id").asLong();
    }
}

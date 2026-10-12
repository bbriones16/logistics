package com.logistics.system.inventory.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.system.common.AuthTestHelper;
import com.logistics.system.common.BaseIntegrationTest;
import com.logistics.system.inventory.dto.ProductDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProductControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private ObjectMapper objectMapper;

    private HttpHeaders authHeaders;

    @BeforeEach
    void setUp() throws Exception {
        String token = AuthTestHelper.obtainToken(restTemplate, objectMapper,
                "product-it@example.com", "password123");
        authHeaders = new HttpHeaders();
        authHeaders.setBearerAuth(token);
        authHeaders.setContentType(MediaType.APPLICATION_JSON);
    }

    @Test
    void getAll_authenticated_returns200() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/products", HttpMethod.GET, new HttpEntity<>(authHeaders), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getAll_unauthenticated_returns401() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/products", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        ProductDto.CreateRequest request = new ProductDto.CreateRequest(
                "SKU-" + UUID.randomUUID(), "Test Product", "A test product", "EACH", 5);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/products", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("id").asLong()).isPositive();
        assertThat(body.get("active").asBoolean()).isTrue();
    }

    @Test
    void create_duplicateSku_returns422() throws Exception {
        String sku = "SKU-DUP-" + UUID.randomUUID();
        ProductDto.CreateRequest request = new ProductDto.CreateRequest(sku, "Product", null, "EACH", 0);

        restTemplate.exchange("/api/products", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders), String.class);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/products", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void getById_nonExisting_returns404() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/products/99999", HttpMethod.GET, new HttpEntity<>(authHeaders), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}

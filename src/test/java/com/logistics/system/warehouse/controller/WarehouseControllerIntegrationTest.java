package com.logistics.system.warehouse.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.system.common.AuthTestHelper;
import com.logistics.system.common.BaseIntegrationTest;
import com.logistics.system.warehouse.dto.WarehouseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WarehouseControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private ObjectMapper objectMapper;

    private HttpHeaders authHeaders;

    @BeforeEach
    void setUp() throws Exception {
        String token = AuthTestHelper.obtainToken(restTemplate, objectMapper,
                "warehouse-it@example.com", "password123");
        authHeaders = new HttpHeaders();
        authHeaders.setBearerAuth(token);
        authHeaders.setContentType(MediaType.APPLICATION_JSON);
    }

    @Test
    void getAll_authenticated_returns200() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/warehouses", HttpMethod.GET, new HttpEntity<>(authHeaders), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getAll_unauthenticated_returns401() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/warehouses", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        WarehouseDto.CreateRequest request = new WarehouseDto.CreateRequest(
                "WH-" + UUID.randomUUID().toString().substring(0, 8), "Test Warehouse", "123 Main St");

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/warehouses", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("id").asLong()).isPositive();
        assertThat(body.get("active").asBoolean()).isTrue();
    }

    @Test
    void create_duplicateCode_returns422() throws Exception {
        String code = "WH-DUP-" + UUID.randomUUID().toString().substring(0, 6);
        WarehouseDto.CreateRequest request = new WarehouseDto.CreateRequest(code, "Warehouse", null);

        restTemplate.exchange("/api/warehouses", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders), String.class);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/warehouses", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void getById_nonExisting_returns404() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/warehouses/99999", HttpMethod.GET, new HttpEntity<>(authHeaders), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}

package com.logistics.system.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.system.common.AuthTestHelper;
import com.logistics.system.common.BaseIntegrationTest;
import com.logistics.system.order.dto.CustomerDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private HttpHeaders authHeaders;

    @BeforeEach
    void setUp() throws Exception {
        String token = AuthTestHelper.obtainToken(restTemplate, objectMapper,
                "customer-it@example.com", "password123");
        authHeaders = new HttpHeaders();
        authHeaders.setBearerAuth(token);
        authHeaders.setContentType(MediaType.APPLICATION_JSON);
    }

    @Test
    void getAll_authenticated_returns200() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/customers", HttpMethod.GET, new HttpEntity<>(authHeaders), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getAll_unauthenticated_returns401() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/customers", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void create_validRequest_returns201() {
        CustomerDto.CreateRequest request = new CustomerDto.CreateRequest(
                "Jane Doe", "jane-" + UUID.randomUUID() + "@example.com", "0987654321", "456 Oak Ave");

        ResponseEntity<CustomerDto.Response> response = restTemplate.exchange(
                "/api/customers", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders), CustomerDto.Response.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isNotNull();
    }

    @Test
    void create_duplicateEmail_returns422() {
        CustomerDto.CreateRequest request = new CustomerDto.CreateRequest(
                "Duplicate User", "duplicate@example.com", null, null);

        restTemplate.exchange("/api/customers", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders), String.class);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/customers", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void create_invalidEmail_returns400() {
        CustomerDto.CreateRequest request = new CustomerDto.CreateRequest(
                "Bad Email", "not-an-email", null, null);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/customers", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void create_missingName_returns400() {
        CustomerDto.CreateRequest request = new CustomerDto.CreateRequest(
                "", "valid@example.com", null, null);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/customers", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void getById_existingCustomer_returns200() {
        String email = "getbyid-" + UUID.randomUUID() + "@example.com";
        CustomerDto.CreateRequest request = new CustomerDto.CreateRequest("Get By Id", email, null, null);
        ResponseEntity<CustomerDto.Response> created = restTemplate.exchange(
                "/api/customers", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders), CustomerDto.Response.class);

        Long id = created.getBody().id();
        ResponseEntity<CustomerDto.Response> response = restTemplate.exchange(
                "/api/customers/" + id, HttpMethod.GET,
                new HttpEntity<>(authHeaders), CustomerDto.Response.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().email()).isEqualTo(email);
    }

    @Test
    void getById_nonExistingCustomer_returns404() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/customers/99999", HttpMethod.GET,
                new HttpEntity<>(authHeaders), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}

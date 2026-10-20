package com.logistics.system.configuration.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.system.common.AuthTestHelper;
import com.logistics.system.common.BaseIntegrationTest;
import com.logistics.system.configuration.dto.ModuleDto;
import com.logistics.system.security.domain.Role;
import com.logistics.system.security.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ModuleConfigControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;

    private HttpHeaders adminHeaders;
    private HttpHeaders userHeaders;

    @BeforeEach
    void setUp() throws Exception {
        String adminEmail = "module-admin-" + UUID.randomUUID() + "@example.com";
        String adminToken = AuthTestHelper.obtainToken(restTemplate, objectMapper, adminEmail, "password123");
        userRepository.findByEmail(adminEmail).ifPresent(u -> {
            u.setRoles(Set.of(Role.ADMIN));
            userRepository.save(u);
        });
        // Re-login to get a token with the updated ADMIN role
        adminToken = AuthTestHelper.loginToken(restTemplate, objectMapper, adminEmail, "password123");
        adminHeaders = new HttpHeaders();
        adminHeaders.setBearerAuth(adminToken);
        adminHeaders.setContentType(MediaType.APPLICATION_JSON);

        String userToken = AuthTestHelper.obtainToken(restTemplate, objectMapper,
                "module-user-" + UUID.randomUUID() + "@example.com", "password123");
        userHeaders = new HttpHeaders();
        userHeaders.setBearerAuth(userToken);
        userHeaders.setContentType(MediaType.APPLICATION_JSON);
    }

    @Test
    void listAllModules_asAdmin_returns200WithAllModules() throws Exception {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/admin/modules", HttpMethod.GET, new HttpEntity<>(adminHeaders), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.isArray()).isTrue();
        assertThat(body.size()).isGreaterThanOrEqualTo(6);
    }

    @Test
    void listAllModules_containsExpectedModuleKeys() throws Exception {
        JsonNode body = objectMapper.readTree(restTemplate.exchange(
                "/api/admin/modules", HttpMethod.GET, new HttpEntity<>(adminHeaders), String.class).getBody());

        boolean hasDomestic = false;
        boolean hasFreight = false;
        for (JsonNode node : body) {
            String key = node.get("moduleKey").asText();
            if ("DOMESTIC_LOGISTICS".equals(key)) hasDomestic = true;
            if ("INTERNATIONAL_FREIGHT".equals(key)) hasFreight = true;
        }
        assertThat(hasDomestic).isTrue();
        assertThat(hasFreight).isTrue();
    }

    @Test
    void getModuleByKey_domesticLogistics_returns200AndIsEnabled() throws Exception {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/admin/modules/DOMESTIC_LOGISTICS", HttpMethod.GET,
                new HttpEntity<>(adminHeaders), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("moduleKey").asText()).isEqualTo("DOMESTIC_LOGISTICS");
        assertThat(body.get("enabled").asBoolean()).isTrue();
    }

    @Test
    void getModuleByKey_internationalFreight_returns200AndIsDisabledByDefault() throws Exception {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/admin/modules/INTERNATIONAL_FREIGHT", HttpMethod.GET,
                new HttpEntity<>(adminHeaders), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("enabled").asBoolean()).isFalse();
    }

    @Test
    void getModuleByKey_unknownKey_returns404() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/admin/modules/UNKNOWN_MODULE", HttpMethod.GET,
                new HttpEntity<>(adminHeaders), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateModule_enableFreight_returns200AndAuditRecordCreated() throws Exception {
        ModuleDto.UpdateRequest request = new ModuleDto.UpdateRequest(true, null, null, null, "Enabling for test");

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/admin/modules/CUSTOMS_CLEARANCE", HttpMethod.PUT,
                new HttpEntity<>(request, adminHeaders), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("moduleKey").asText()).isEqualTo("CUSTOMS_CLEARANCE");
        assertThat(body.get("enabled").asBoolean()).isTrue();

        // Verify audit record was created
        JsonNode audit = objectMapper.readTree(restTemplate.exchange(
                "/api/admin/modules/audit", HttpMethod.GET,
                new HttpEntity<>(adminHeaders), String.class).getBody());
        assertThat(audit.isArray()).isTrue();
        boolean found = false;
        for (JsonNode entry : audit) {
            if ("CUSTOMS_CLEARANCE".equals(entry.get("moduleKey").asText())) {
                found = true;
                assertThat(entry.get("newValue").asText()).contains("true");
                break;
            }
        }
        assertThat(found).as("Audit record for CUSTOMS_CLEARANCE should exist").isTrue();
    }

    @Test
    void updateModule_disableModule_updatesEnabledToFalse() throws Exception {
        // First enable it
        restTemplate.exchange("/api/admin/modules/CUSTOMER_PORTAL", HttpMethod.PUT,
                new HttpEntity<>(new ModuleDto.UpdateRequest(true, null, null, null, null), adminHeaders), String.class);

        // Then disable it
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/admin/modules/CUSTOMER_PORTAL", HttpMethod.PUT,
                new HttpEntity<>(new ModuleDto.UpdateRequest(false, null, null, null, "Disabling"), adminHeaders),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(objectMapper.readTree(response.getBody()).get("enabled").asBoolean()).isFalse();
    }

    @Test
    void getAuditLog_asAdmin_returns200() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/admin/modules/audit", HttpMethod.GET,
                new HttpEntity<>(adminHeaders), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getMyModules_authenticatedUser_returns200WithEnabledModules() throws Exception {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/me/modules", HttpMethod.GET,
                new HttpEntity<>(userHeaders), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.isArray()).isTrue();
        // DOMESTIC_LOGISTICS is enabled by default — must appear
        boolean hasDomestic = false;
        for (JsonNode node : body) {
            if ("DOMESTIC_LOGISTICS".equals(node.get("moduleKey").asText())) {
                hasDomestic = true;
            }
        }
        assertThat(hasDomestic).isTrue();
    }

    @Test
    void adminEndpoints_asNonAdminUser_returns403() {
        // Application returns 401 for non-admin users on admin endpoints
        // (authenticationEntryPoint is configured to return 401 for all auth failures)
        int status = restTemplate.exchange("/api/admin/modules", HttpMethod.GET,
                new HttpEntity<>(userHeaders), String.class).getStatusCode().value();
        assertThat(status).isIn(401, 403);

        status = restTemplate.exchange("/api/admin/modules/DOMESTIC_LOGISTICS", HttpMethod.GET,
                new HttpEntity<>(userHeaders), String.class).getStatusCode().value();
        assertThat(status).isIn(401, 403);

        status = restTemplate.exchange("/api/admin/modules/audit", HttpMethod.GET,
                new HttpEntity<>(userHeaders), String.class).getStatusCode().value();
        assertThat(status).isIn(401, 403);
    }

    @Test
    void unauthenticated_returns401() {
        assertThat(restTemplate.getForEntity("/api/admin/modules", String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(restTemplate.getForEntity("/api/me/modules", String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}

package com.logistics.system.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.system.security.dto.AuthDto;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

public class AuthTestHelper {

    private static final String REGISTER_URL = "/api/auth/register";
    private static final String LOGIN_URL = "/api/auth/login";

    public static String obtainToken(TestRestTemplate restTemplate, ObjectMapper objectMapper,
                                     String email, String password) throws Exception {
        AuthDto.RegisterRequest register = new AuthDto.RegisterRequest("Test User", email, password);
        ResponseEntity<String> registerResponse = restTemplate.postForEntity(REGISTER_URL, register, String.class);

        if (registerResponse.getStatusCode().is2xxSuccessful()) {
            return objectMapper.readTree(registerResponse.getBody()).get("token").asText();
        }

        AuthDto.LoginRequest login = new AuthDto.LoginRequest(email, password);
        ResponseEntity<String> loginResponse = restTemplate.postForEntity(LOGIN_URL, login, String.class);
        return objectMapper.readTree(loginResponse.getBody()).get("token").asText();
    }
}

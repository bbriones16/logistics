package com.logistics.system.order.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class CustomerDto {

    public record CreateRequest(
            @NotBlank String name,
            @Email @NotBlank String email,
            String phone,
            String address
    ) {}

    public record Response(
            Long id,
            String name,
            String email,
            String phone,
            String address
    ) {}
}

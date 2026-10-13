package com.portfolio.helpdesk.auth.api;

import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public final class AuthDtos {
    private AuthDtos() {
    }

    public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
    ) {
    }

    public record UserResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        List<String> roles
    ) {
    }

    public record LoginResponse(
        String token,
        String tokenType,
        UserResponse user
    ) {
    }
}

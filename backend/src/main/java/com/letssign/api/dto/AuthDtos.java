package com.letssign.api.dto;

import jakarta.validation.constraints.NotBlank;

public final class AuthDtos {
    private AuthDtos() {}

    public record LoginRequest(
            @NotBlank(message = "Informe o e-mail.") String email,
            @NotBlank(message = "Informe a senha.") String password) {}

    public record LoginResponse(String token, String tokenType, long expiresIn, UserResponse user) {}
}

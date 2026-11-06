package com.letssign.api.service;

import com.letssign.api.domain.User;
import com.letssign.api.dto.AuthDtos.LoginResponse;
import com.letssign.api.dto.UserResponse;
import com.letssign.api.error.ApiException;
import com.letssign.api.repository.UserRepository;
import com.letssign.api.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    public LoginResponse login(String email, String password) {
        User u = users.findByEmailIgnoreCase(email.trim())
                .filter(x -> encoder.matches(password, x.passwordHash))
                .orElseThrow(() -> ApiException.unauthorized("E-mail ou senha inválidos."));
        return new LoginResponse(jwt.issue(u), "Bearer", jwt.ttlSeconds(), UserResponse.of(u));
    }

    public UserResponse me(Long id) {
        return users.findById(id).map(UserResponse::of)
                .orElseThrow(() -> ApiException.unauthorized("Usuário não existe mais."));
    }
}

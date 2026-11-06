package com.letssign.api.web;

import com.letssign.api.dto.AuthDtos.LoginRequest;
import com.letssign.api.dto.AuthDtos.LoginResponse;
import com.letssign.api.dto.UserResponse;
import com.letssign.api.security.AuthUser;
import com.letssign.api.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;

    public AuthController(AuthService auth) { this.auth = auth; }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        return auth.login(req.email(), req.password());
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AuthUser user) {
        return auth.me(user.id());
    }
}

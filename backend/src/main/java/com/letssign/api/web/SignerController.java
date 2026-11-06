package com.letssign.api.web;

import com.letssign.api.dto.SignerDtos.CreateSignerRequest;
import com.letssign.api.dto.SignerDtos.UpdateSignerRequest;
import com.letssign.api.dto.UserResponse;
import com.letssign.api.security.AuthUser;
import com.letssign.api.service.SignerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Cadastro de signatários pelo Admin (rota inteira exige ROLE_ADMIN no SecurityConfig). */
@RestController
@RequestMapping("/api/signers")
public class SignerController {
    private final SignerService signers;

    public SignerController(SignerService signers) { this.signers = signers; }

    @GetMapping
    public List<UserResponse> list(@AuthenticationPrincipal AuthUser admin) {
        return signers.list(admin.id());
    }

    @GetMapping("/{id}")
    public UserResponse get(@AuthenticationPrincipal AuthUser admin, @PathVariable Long id) {
        return signers.get(admin.id(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@AuthenticationPrincipal AuthUser admin, @Valid @RequestBody CreateSignerRequest req) {
        return signers.create(admin.id(), req);
    }

    @PutMapping("/{id}")
    public UserResponse update(@AuthenticationPrincipal AuthUser admin, @PathVariable Long id,
                               @Valid @RequestBody UpdateSignerRequest req) {
        return signers.update(admin.id(), id, req);
    }
}

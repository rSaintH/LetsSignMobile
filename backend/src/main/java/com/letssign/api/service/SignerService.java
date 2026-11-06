package com.letssign.api.service;

import com.letssign.api.domain.User;
import com.letssign.api.domain.UserRole;
import com.letssign.api.dto.SignerDtos.CreateSignerRequest;
import com.letssign.api.dto.SignerDtos.UpdateSignerRequest;
import com.letssign.api.dto.UserResponse;
import com.letssign.api.error.ApiException;
import com.letssign.api.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SignerService {
    private final UserRepository users;
    private final PasswordEncoder encoder;

    public SignerService(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    public List<UserResponse> list(Long adminId) {
        return users.findByRoleAndCreatedByAdminIdOrderByFullNameAsc(UserRole.SIGNER, adminId)
                .stream().map(UserResponse::of).toList();
    }

    public UserResponse get(Long adminId, Long signerId) {
        return UserResponse.of(owned(adminId, signerId));
    }

    @Transactional
    public UserResponse create(Long adminId, CreateSignerRequest r) {
        if (users.findByEmailIgnoreCase(r.email().trim()).isPresent()) {
            throw ApiException.conflict("Já existe um usuário com este e-mail.");
        }
        User u = new User();
        u.fullName = r.fullName().trim();
        u.email = r.email().trim();
        u.phone = r.phone().trim();
        u.passwordHash = encoder.encode(r.password());
        u.role = UserRole.SIGNER;
        u.createdByAdminId = adminId;
        return UserResponse.of(users.save(u));
    }

    @Transactional
    public UserResponse update(Long adminId, Long signerId, UpdateSignerRequest r) {
        User u = owned(adminId, signerId);
        users.findByEmailIgnoreCase(r.email().trim())
                .filter(other -> !other.id.equals(signerId))
                .ifPresent(other -> { throw ApiException.conflict("Já existe outro usuário com este e-mail."); });
        u.fullName = r.fullName().trim();
        u.email = r.email().trim();
        u.phone = r.phone().trim();
        if (r.newPassword() != null && !r.newPassword().isBlank()) u.passwordHash = encoder.encode(r.newPassword());
        u.updatedAt = System.currentTimeMillis();
        return UserResponse.of(users.save(u));
    }

    /** Admin só enxerga signatários que ele cadastrou (404 para os demais, sem vazar existência). */
    User owned(Long adminId, Long signerId) {
        return users.findById(signerId)
                .filter(u -> u.role == UserRole.SIGNER && adminId.equals(u.createdByAdminId))
                .orElseThrow(() -> ApiException.notFound("Signatário não encontrado."));
    }
}

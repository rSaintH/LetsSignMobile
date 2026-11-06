package com.letssign.api.security;

import com.letssign.api.domain.UserRole;

/** Principal autenticado (extraído do JWT). */
public record AuthUser(Long id, String email, UserRole role) {
    public boolean isAdmin() { return role == UserRole.ADMIN; }
}

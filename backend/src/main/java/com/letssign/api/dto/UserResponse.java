package com.letssign.api.dto;

import com.letssign.api.domain.User;
import com.letssign.api.domain.UserRole;

public record UserResponse(Long id, String fullName, String email, String phone, UserRole role,
                           Long createdByAdminId, long createdAt, long updatedAt) {
    public static UserResponse of(User u) {
        return new UserResponse(u.id, u.fullName, u.email, u.phone, u.role, u.createdByAdminId, u.createdAt, u.updatedAt);
    }
}

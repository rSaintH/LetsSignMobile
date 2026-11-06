package com.letssign.api.repository;

import com.letssign.api.domain.User;
import com.letssign.api.domain.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);
    List<User> findByRoleAndCreatedByAdminIdOrderByFullNameAsc(UserRole role, Long adminId);
}

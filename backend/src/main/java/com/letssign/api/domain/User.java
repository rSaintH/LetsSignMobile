package com.letssign.api.domain;

import jakarta.persistence.*;

/** Admin ou Signatário. Datas em epoch millis, igual ao Room do app. */
@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(nullable = false) public String fullName;
    @Column(nullable = false) public String email;
    @Column(nullable = false) public String phone;
    @Column(nullable = false) public String passwordHash;
    @Enumerated(EnumType.STRING) @Column(nullable = false) public UserRole role;
    public Long createdByAdminId;
    public long createdAt = System.currentTimeMillis();
    public long updatedAt = System.currentTimeMillis();
}

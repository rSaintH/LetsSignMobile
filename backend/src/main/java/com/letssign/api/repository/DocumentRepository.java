package com.letssign.api.repository;

import com.letssign.api.domain.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByOwnerAdminIdOrderByCreatedAtDesc(Long ownerAdminId);
}

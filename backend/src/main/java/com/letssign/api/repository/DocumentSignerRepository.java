package com.letssign.api.repository;

import com.letssign.api.domain.DocumentSigner;
import com.letssign.api.domain.SignatureStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentSignerRepository extends JpaRepository<DocumentSigner, Long> {
    List<DocumentSigner> findByDocumentIdOrderByIdAsc(Long documentId);
    List<DocumentSigner> findBySignerId(Long signerId);
    Optional<DocumentSigner> findByDocumentIdAndSignerId(Long documentId, Long signerId);
    long countByDocumentIdAndStatusNot(Long documentId, SignatureStatus status);
    void deleteByDocumentId(Long documentId);
}

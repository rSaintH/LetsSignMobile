package com.letssign.api.repository;

import com.letssign.api.domain.SignatureField;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SignatureFieldRepository extends JpaRepository<SignatureField, Long> {
    List<SignatureField> findByDocumentIdOrderByPageAscIdAsc(Long documentId);
    List<SignatureField> findByDocumentSignerIdOrderByPageAscIdAsc(Long documentSignerId);
    void deleteByDocumentId(Long documentId);
}

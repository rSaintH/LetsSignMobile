package com.letssign.api.repository;

import com.letssign.api.domain.DocumentEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentEventRepository extends JpaRepository<DocumentEvent, Long> {
    List<DocumentEvent> findByDocumentIdOrderByTimestampAscIdAsc(Long documentId);
}

package com.letssign.api.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "document_events")
public class DocumentEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(nullable = false) public Long documentId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) public DocumentEventType type;
    @Column(nullable = false, length = 1000) public String description;
    public long timestamp = System.currentTimeMillis();

    public static DocumentEvent of(Long documentId, DocumentEventType type, String description) {
        DocumentEvent e = new DocumentEvent();
        e.documentId = documentId;
        e.type = type;
        e.description = description;
        return e;
    }
}

package com.letssign.api.domain;

import jakarta.persistence.*;

/**
 * Documento enviado para assinatura. O PDF fica em disco
 * (storage/documents/{id}/original.pdf e signed.pdf); aqui só metadados.
 */
@Entity
@Table(name = "documents")
public class Document {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(nullable = false) public String title;
    @Column(nullable = false) public String originalFileName;
    public int pageCount;
    @Column(nullable = false) public Long ownerAdminId;
    @Column(nullable = false, length = 2000) public String inviteMessage = "";
    @Enumerated(EnumType.STRING) @Column(nullable = false) public DocumentStatus status = DocumentStatus.DRAFT;
    public int version = 1;
    public Long parentDocumentId;
    public long createdAt = System.currentTimeMillis();
    public Long sentAt;
    public Long completedAt;

    public boolean isEditable() {
        return status == DocumentStatus.DRAFT || status == DocumentStatus.SENDING_FAILED;
    }
}

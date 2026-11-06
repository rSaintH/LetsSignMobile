package com.letssign.api.domain;

import jakarta.persistence.*;

/** Participação de um signatário num documento (N:N com classificação e status). */
@Entity
@Table(name = "document_signers",
        uniqueConstraints = @UniqueConstraint(columnNames = {"documentId", "signerId"}))
public class DocumentSigner {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(nullable = false) public Long documentId;
    @Column(nullable = false) public Long signerId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) public SignerClassification classification;
    @Enumerated(EnumType.STRING) @Column(nullable = false) public SignatureStatus status = SignatureStatus.PENDING;
    public Long signedAt;
}

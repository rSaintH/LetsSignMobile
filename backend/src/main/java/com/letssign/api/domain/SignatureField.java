package com.letssign.api.domain;

import jakarta.persistence.*;

/** Campo de assinatura. Posição e tamanho em RATIOS 0..1 da página (origem topo-esquerda). */
@Entity
@Table(name = "signature_fields")
public class SignatureField {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(nullable = false) public Long documentId;
    @Column(nullable = false) public Long documentSignerId;
    public int page;
    public float xRatio;
    public float yRatio;
    public float widthRatio;
    public float heightRatio;
    public String signedImageFile;
}

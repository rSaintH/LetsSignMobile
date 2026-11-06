package com.letssign.api.dto;

import com.letssign.api.domain.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;
import java.util.Map;

public final class DocumentDtos {
    private DocumentDtos() {}

    public record DocumentResponse(Long id, String title, String originalFileName, int pageCount, Long ownerAdminId,
                                   String inviteMessage, DocumentStatus status, int version, Long parentDocumentId,
                                   long createdAt, Long sentAt, Long completedAt) {
        public static DocumentResponse of(Document d) {
            return new DocumentResponse(d.id, d.title, d.originalFileName, d.pageCount, d.ownerAdminId,
                    d.inviteMessage, d.status, d.version, d.parentDocumentId, d.createdAt, d.sentAt, d.completedAt);
        }
    }

    public record DocumentSignerResponse(Long id, Long documentId, Long signerId, String signerName, String signerEmail,
                                         SignerClassification classification, SignatureStatus status, Long signedAt) {}

    public record SignatureFieldResponse(Long id, Long documentId, Long documentSignerId, Long signerId, int page,
                                         float xRatio, float yRatio, float widthRatio, float heightRatio, boolean signed) {}

    public record DocumentEventResponse(Long id, Long documentId, DocumentEventType type, String description, long timestamp) {
        public static DocumentEventResponse of(DocumentEvent e) {
            return new DocumentEventResponse(e.id, e.documentId, e.type, e.description, e.timestamp);
        }
    }

    public record DocumentDetailResponse(DocumentResponse document, List<DocumentSignerResponse> signers,
                                         List<SignatureFieldResponse> fields, List<DocumentEventResponse> events) {}

    /** Item da caixa do signatário: sua participação + o documento. */
    public record SignerInboxItem(Long documentSignerId, SignerClassification classification, SignatureStatus status,
                                  Long signedAt, int myFieldCount, DocumentResponse document) {}

    public record SignerSelection(
            @NotNull(message = "signerId é obrigatório.") Long signerId,
            @NotNull(message = "classification é obrigatória.") SignerClassification classification) {}

    public record SetSignersRequest(@NotNull @Valid List<SignerSelection> signers) {}

    public record FieldInput(
            @NotNull(message = "signerId é obrigatório.") Long signerId,
            @Min(value = 0, message = "page inválida.") int page,
            @DecimalMin(value = "0.0", message = "xRatio deve estar entre 0 e 1.") @DecimalMax(value = "1.0", message = "xRatio deve estar entre 0 e 1.") float xRatio,
            @DecimalMin(value = "0.0", message = "yRatio deve estar entre 0 e 1.") @DecimalMax(value = "1.0", message = "yRatio deve estar entre 0 e 1.") float yRatio,
            @DecimalMin(value = "0.05", message = "widthRatio deve estar entre 0.05 e 1.") @DecimalMax(value = "1.0", message = "widthRatio deve estar entre 0.05 e 1.") float widthRatio,
            @DecimalMin(value = "0.02", message = "heightRatio deve estar entre 0.02 e 1.") @DecimalMax(value = "1.0", message = "heightRatio deve estar entre 0.02 e 1.") float heightRatio) {}

    public record SetFieldsRequest(@NotNull @Valid List<FieldInput> fields) {}

    public record InviteMessageRequest(@NotNull @Size(max = 2000) String message) {}

    /** fieldId -> PNG em base64 (aceita prefixo data:image/png;base64,). */
    public record SignRequest(@NotNull(message = "Envie as assinaturas.") Map<Long, String> signatures) {}
}

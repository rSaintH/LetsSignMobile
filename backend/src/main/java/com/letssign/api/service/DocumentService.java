package com.letssign.api.service;

import com.letssign.api.domain.*;
import com.letssign.api.dto.DocumentDtos.*;
import com.letssign.api.error.ApiException;
import com.letssign.api.repository.*;
import com.letssign.api.security.AuthUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Regras de negócio espelhando o DocumentRepositoryImpl do app:
 * - envio exige >= 1 signatário e >= 1 campo; envio é idempotente;
 * - depois de enviado, alterações só via nova versão (aqui é 409, no app era convenção);
 * - documento fica COMPLETED quando todos os signatários assinam;
 * - admin só vê os próprios documentos; signatário só vê os não-rascunho em que participa.
 */
@Service
public class DocumentService {
    private static final long MAX_SIGNATURE_BYTES = 2 * 1024 * 1024;

    private final DocumentRepository documents;
    private final DocumentSignerRepository docSigners;
    private final SignatureFieldRepository fields;
    private final DocumentEventRepository events;
    private final UserRepository users;
    private final SignerService signerService;
    private final StorageService storage;
    private final PdfService pdf;

    public DocumentService(DocumentRepository documents, DocumentSignerRepository docSigners,
                           SignatureFieldRepository fields, DocumentEventRepository events, UserRepository users,
                           SignerService signerService, StorageService storage, PdfService pdf) {
        this.documents = documents;
        this.docSigners = docSigners;
        this.fields = fields;
        this.events = events;
        this.users = users;
        this.signerService = signerService;
        this.storage = storage;
        this.pdf = pdf;
    }

    // ------------------------------------------------------------------ admin

    @Transactional
    public DocumentDetailResponse create(AuthUser admin, String title, String inviteMessage, MultipartFile file) {
        if (title == null || title.isBlank()) throw ApiException.badRequest("Informe o título do documento.");
        if (file == null || file.isEmpty()) throw ApiException.badRequest("Selecione um arquivo PDF.");
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw ApiException.badRequest("Não foi possível ler o arquivo enviado.");
        }
        int pages = pdf.pageCount(bytes);

        Document d = new Document();
        d.title = title.trim();
        d.originalFileName = Optional.ofNullable(file.getOriginalFilename()).filter(s -> !s.isBlank()).orElse("documento.pdf");
        d.pageCount = pages;
        d.ownerAdminId = admin.id();
        d.inviteMessage = inviteMessage == null ? "" : inviteMessage;
        documents.save(d);
        storage.write(storage.original(d.id), bytes);
        events.save(DocumentEvent.of(d.id, DocumentEventType.CREATED, "Documento criado como rascunho."));
        return detail(d);
    }

    public List<DocumentResponse> listForAdmin(AuthUser admin) {
        return documents.findByOwnerAdminIdOrderByCreatedAtDesc(admin.id()).stream().map(DocumentResponse::of).toList();
    }

    @Transactional
    public DocumentDetailResponse setSigners(AuthUser admin, Long id, SetSignersRequest req) {
        Document d = ownedEditable(admin, id);
        Set<Long> seen = new HashSet<>();
        for (SignerSelection s : req.signers()) {
            if (!seen.add(s.signerId())) throw ApiException.badRequest("Signatário repetido na lista.");
            signerService.owned(admin.id(), s.signerId());
        }
        // trocar a lista de participantes invalida os campos (cada campo pertence a um participante)
        fields.deleteByDocumentId(id);
        docSigners.deleteByDocumentId(id);
        docSigners.flush();
        for (SignerSelection s : req.signers()) {
            DocumentSigner ds = new DocumentSigner();
            ds.documentId = id;
            ds.signerId = s.signerId();
            ds.classification = s.classification();
            docSigners.save(ds);
        }
        return detail(d);
    }

    @Transactional
    public DocumentDetailResponse setFields(AuthUser admin, Long id, SetFieldsRequest req) {
        Document d = ownedEditable(admin, id);
        Map<Long, DocumentSigner> bySigner = docSigners.findByDocumentIdOrderByIdAsc(id).stream()
                .collect(Collectors.toMap(ds -> ds.signerId, Function.identity()));
        List<SignatureField> toSave = new ArrayList<>();
        for (FieldInput in : req.fields()) {
            DocumentSigner ds = bySigner.get(in.signerId());
            if (ds == null) throw ApiException.badRequest("Signatário " + in.signerId() + " não participa deste documento.");
            if (in.page() >= d.pageCount) throw ApiException.badRequest("Página " + in.page() + " não existe (o PDF tem " + d.pageCount + ").");
            SignatureField f = new SignatureField();
            f.documentId = id;
            f.documentSignerId = ds.id;
            f.page = in.page();
            f.widthRatio = in.widthRatio();
            f.heightRatio = in.heightRatio();
            // mesmo clamp do app: o campo nunca sai da página
            f.xRatio = Math.max(0f, Math.min(in.xRatio(), 1f - f.widthRatio));
            f.yRatio = Math.max(0f, Math.min(in.yRatio(), 1f - f.heightRatio));
            toSave.add(f);
        }
        fields.deleteByDocumentId(id);
        fields.flush();
        fields.saveAll(toSave);
        return detail(d);
    }

    @Transactional
    public DocumentDetailResponse setInviteMessage(AuthUser admin, Long id, String message) {
        Document d = ownedEditable(admin, id);
        d.inviteMessage = message;
        documents.save(d);
        return detail(d);
    }

    @Transactional
    public DocumentDetailResponse send(AuthUser admin, Long id) {
        Document d = owned(admin, id);
        if (d.status == DocumentStatus.SENT) return detail(d); // idempotente
        if (!d.isEditable()) throw ApiException.conflict("Documento já está em assinatura ou concluído.");

        List<DocumentSigner> signers = docSigners.findByDocumentIdOrderByIdAsc(id);
        if (signers.isEmpty()) throw ApiException.badRequest("O documento precisa ter ao menos 1 signatário para ser enviado.");
        if (fields.findByDocumentIdOrderByPageAscIdAsc(id).isEmpty())
            throw ApiException.badRequest("Posicione ao menos um campo de assinatura antes de enviar.");
        if (!Files.exists(storage.original(id))) {
            d.status = DocumentStatus.SENDING_FAILED;
            documents.save(d);
            events.save(DocumentEvent.of(id, DocumentEventType.SEND_FAILED, "Arquivo PDF do documento não foi encontrado."));
            throw ApiException.conflict("Arquivo PDF do documento não foi encontrado.");
        }
        d.status = DocumentStatus.SENT;
        d.sentAt = System.currentTimeMillis();
        documents.save(d);
        events.save(DocumentEvent.of(id, DocumentEventType.SENT, "Documento enviado para " + signers.size() + " signatário(s)."));
        return detail(d);
    }

    @Transactional
    public DocumentDetailResponse newVersion(AuthUser admin, Long id) {
        Document o = owned(admin, id);
        Document n = new Document();
        n.title = o.title;
        n.originalFileName = o.originalFileName;
        n.pageCount = o.pageCount;
        n.ownerAdminId = o.ownerAdminId;
        n.inviteMessage = o.inviteMessage;
        n.version = o.version + 1;
        n.parentDocumentId = o.id;
        documents.save(n);
        // nova versão parte do PDF original (sem as assinaturas da versão anterior)
        storage.copy(storage.original(o.id), storage.original(n.id));

        Map<Long, Long> idMap = new HashMap<>();
        for (DocumentSigner ds : docSigners.findByDocumentIdOrderByIdAsc(o.id)) {
            DocumentSigner c = new DocumentSigner();
            c.documentId = n.id;
            c.signerId = ds.signerId;
            c.classification = ds.classification;
            idMap.put(ds.id, docSigners.save(c).id);
        }
        for (SignatureField f : fields.findByDocumentIdOrderByPageAscIdAsc(o.id)) {
            SignatureField c = new SignatureField();
            c.documentId = n.id;
            c.documentSignerId = idMap.get(f.documentSignerId);
            c.page = f.page;
            c.xRatio = f.xRatio;
            c.yRatio = f.yRatio;
            c.widthRatio = f.widthRatio;
            c.heightRatio = f.heightRatio;
            fields.save(c);
        }
        events.save(DocumentEvent.of(n.id, DocumentEventType.NEW_VERSION,
                "Nova versão (v" + n.version + ") criada a partir do documento #" + o.id + "."));
        return detail(n);
    }

    // ------------------------------------------------------------------ leitura (admin dono ou signatário)

    public DocumentDetailResponse get(AuthUser user, Long id) {
        return detail(visible(user, id));
    }

    public Path file(AuthUser user, Long id, boolean original) {
        Document d = visible(user, id);
        Path p = original ? storage.original(d.id) : storage.current(d.id);
        if (!Files.exists(p)) throw ApiException.notFound("Arquivo PDF do documento não foi encontrado.");
        return p;
    }

    // ------------------------------------------------------------------ signatário

    public List<SignerInboxItem> inbox(AuthUser signer) {
        List<SignerInboxItem> out = new ArrayList<>();
        for (DocumentSigner ds : docSigners.findBySignerId(signer.id())) {
            documents.findById(ds.documentId)
                    .filter(d -> d.status != DocumentStatus.DRAFT)
                    .ifPresent(d -> out.add(new SignerInboxItem(ds.id, ds.classification, ds.status, ds.signedAt,
                            fields.findByDocumentSignerIdOrderByPageAscIdAsc(ds.id).size(), DocumentResponse.of(d))));
        }
        out.sort(Comparator.comparing((SignerInboxItem i) -> i.status() == SignatureStatus.SIGNED)
                .thenComparing(i -> -Optional.ofNullable(i.document().sentAt()).orElse(0L)));
        return out;
    }

    @Transactional
    public synchronized DocumentDetailResponse sign(AuthUser signer, Long id, SignRequest req) {
        Document d = documents.findById(id).orElseThrow(() -> ApiException.notFound("Documento não encontrado."));
        DocumentSigner ds = docSigners.findByDocumentIdAndSignerId(id, signer.id())
                .filter(x -> d.status != DocumentStatus.DRAFT)
                .orElseThrow(() -> ApiException.notFound("Documento não encontrado."));
        if (ds.status == SignatureStatus.SIGNED) throw ApiException.conflict("Você já assinou este documento.");
        if (d.status != DocumentStatus.SENT && d.status != DocumentStatus.PARTIALLY_SIGNED)
            throw ApiException.conflict("Este documento não está disponível para assinatura.");

        List<SignatureField> mine = fields.findByDocumentSignerIdOrderByPageAscIdAsc(ds.id);
        Set<Long> mineIds = mine.stream().map(f -> f.id).collect(Collectors.toSet());
        for (Long fid : req.signatures().keySet()) {
            if (!mineIds.contains(fid)) throw ApiException.badRequest("Campo " + fid + " não pertence a você.");
        }
        if (!req.signatures().keySet().containsAll(mineIds))
            throw ApiException.badRequest("Assine todos os campos destacados antes de concluir.");

        Map<Long, BufferedImage> images = new HashMap<>();
        Map<Long, byte[]> raw = new HashMap<>();
        for (var e : req.signatures().entrySet()) {
            byte[] png = decodeImage(e.getValue());
            raw.put(e.getKey(), png);
            images.put(e.getKey(), readImage(png));
        }

        byte[] stamped = pdf.stamp(storage.current(id), mine, images);
        storage.write(storage.signed(id), stamped);
        long now = System.currentTimeMillis();
        for (SignatureField f : mine) {
            Path img = storage.signatureImage(id, f.id);
            storage.write(img, raw.get(f.id));
            f.signedImageFile = img.getFileName().toString();
            fields.save(f);
        }
        ds.status = SignatureStatus.SIGNED;
        ds.signedAt = now;
        docSigners.save(ds);
        String name = users.findById(signer.id()).map(u -> u.fullName).orElse("Signatário");
        events.save(DocumentEvent.of(id, DocumentEventType.SIGNER_SIGNED, name + " concluiu a assinatura."));

        docSigners.flush();
        if (docSigners.countByDocumentIdAndStatusNot(id, SignatureStatus.SIGNED) == 0) {
            d.status = DocumentStatus.COMPLETED;
            d.completedAt = now;
            events.save(DocumentEvent.of(id, DocumentEventType.COMPLETED,
                    "Todos os signatários concluíram a assinatura. Documento concluído."));
        } else {
            d.status = DocumentStatus.PARTIALLY_SIGNED;
        }
        documents.save(d);
        return detail(d);
    }

    // ------------------------------------------------------------------ helpers

    private Document owned(AuthUser admin, Long id) {
        return documents.findById(id).filter(d -> d.ownerAdminId.equals(admin.id()))
                .orElseThrow(() -> ApiException.notFound("Documento não encontrado."));
    }

    private Document ownedEditable(AuthUser admin, Long id) {
        Document d = owned(admin, id);
        if (!d.isEditable())
            throw ApiException.conflict("Documento já enviado não pode ser alterado. Crie uma nova versão.");
        return d;
    }

    private Document visible(AuthUser user, Long id) {
        Document d = documents.findById(id).orElseThrow(() -> ApiException.notFound("Documento não encontrado."));
        if (user.isAdmin() && d.ownerAdminId.equals(user.id())) return d;
        if (!user.isAdmin() && d.status != DocumentStatus.DRAFT
                && docSigners.findByDocumentIdAndSignerId(id, user.id()).isPresent()) return d;
        throw ApiException.notFound("Documento não encontrado.");
    }

    private static byte[] decodeImage(String b64) {
        if (b64 == null || b64.isBlank()) throw ApiException.badRequest("Assinatura vazia.");
        String s = b64.trim();
        int comma = s.indexOf(',');
        if (s.startsWith("data:") && comma > 0) s = s.substring(comma + 1);
        byte[] bytes;
        try {
            bytes = Base64.getMimeDecoder().decode(s);
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("Assinatura não está em base64 válido.");
        }
        if (bytes.length > MAX_SIGNATURE_BYTES) throw ApiException.badRequest("Imagem de assinatura grande demais (máx. 2 MB).");
        return bytes;
    }

    private static BufferedImage readImage(byte[] bytes) {
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
            if (img == null) throw ApiException.badRequest("Imagem de assinatura inválida (use PNG ou JPEG).");
            return img;
        } catch (IOException e) {
            throw ApiException.badRequest("Imagem de assinatura inválida (use PNG ou JPEG).");
        }
    }

    private DocumentDetailResponse detail(Document d) {
        List<DocumentSigner> signers = docSigners.findByDocumentIdOrderByIdAsc(d.id);
        Map<Long, User> userById = users.findAllById(signers.stream().map(s -> s.signerId).toList()).stream()
                .collect(Collectors.toMap(u -> u.id, Function.identity()));
        Map<Long, Long> signerIdByDs = signers.stream().collect(Collectors.toMap(s -> s.id, s -> s.signerId));
        var signerDtos = signers.stream().map(s -> {
            User u = userById.get(s.signerId);
            return new DocumentSignerResponse(s.id, s.documentId, s.signerId, u == null ? null : u.fullName,
                    u == null ? null : u.email, s.classification, s.status, s.signedAt);
        }).toList();
        var fieldDtos = fields.findByDocumentIdOrderByPageAscIdAsc(d.id).stream()
                .map(f -> new SignatureFieldResponse(f.id, f.documentId, f.documentSignerId,
                        signerIdByDs.get(f.documentSignerId), f.page, f.xRatio, f.yRatio, f.widthRatio, f.heightRatio,
                        f.signedImageFile != null))
                .toList();
        var eventDtos = events.findByDocumentIdOrderByTimestampAscIdAsc(d.id).stream()
                .map(DocumentEventResponse::of).toList();
        return new DocumentDetailResponse(DocumentResponse.of(d), signerDtos, fieldDtos, eventDtos);
    }
}

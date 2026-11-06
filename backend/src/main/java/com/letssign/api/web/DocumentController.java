package com.letssign.api.web;

import com.letssign.api.dto.DocumentDtos.*;
import com.letssign.api.security.AuthUser;
import com.letssign.api.service.DocumentService;
import jakarta.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api")
public class DocumentController {
    private final DocumentService docs;

    public DocumentController(DocumentService docs) { this.docs = docs; }

    // ---------------------------------------------------------------- admin

    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public DocumentDetailResponse create(@AuthenticationPrincipal AuthUser admin,
                                         @RequestParam String title,
                                         @RequestParam(required = false, defaultValue = "") String inviteMessage,
                                         @RequestPart("file") MultipartFile file) {
        return docs.create(admin, title, inviteMessage, file);
    }

    @GetMapping("/documents")
    @PreAuthorize("hasRole('ADMIN')")
    public List<DocumentResponse> list(@AuthenticationPrincipal AuthUser admin) {
        return docs.listForAdmin(admin);
    }

    @PutMapping("/documents/{id}/signers")
    @PreAuthorize("hasRole('ADMIN')")
    public DocumentDetailResponse setSigners(@AuthenticationPrincipal AuthUser admin, @PathVariable Long id,
                                             @Valid @RequestBody SetSignersRequest req) {
        return docs.setSigners(admin, id, req);
    }

    @PutMapping("/documents/{id}/fields")
    @PreAuthorize("hasRole('ADMIN')")
    public DocumentDetailResponse setFields(@AuthenticationPrincipal AuthUser admin, @PathVariable Long id,
                                            @Valid @RequestBody SetFieldsRequest req) {
        return docs.setFields(admin, id, req);
    }

    @PutMapping("/documents/{id}/invite-message")
    @PreAuthorize("hasRole('ADMIN')")
    public DocumentDetailResponse setInvite(@AuthenticationPrincipal AuthUser admin, @PathVariable Long id,
                                            @Valid @RequestBody InviteMessageRequest req) {
        return docs.setInviteMessage(admin, id, req.message());
    }

    @PostMapping("/documents/{id}/send")
    @PreAuthorize("hasRole('ADMIN')")
    public DocumentDetailResponse send(@AuthenticationPrincipal AuthUser admin, @PathVariable Long id) {
        return docs.send(admin, id);
    }

    @PostMapping("/documents/{id}/versions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public DocumentDetailResponse newVersion(@AuthenticationPrincipal AuthUser admin, @PathVariable Long id) {
        return docs.newVersion(admin, id);
    }

    // ---------------------------------------------------------------- admin dono ou signatário

    @GetMapping("/documents/{id}")
    public DocumentDetailResponse get(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return docs.get(user, id);
    }

    /** PDF atual (com as assinaturas já carimbadas). ?original=true devolve o PDF sem assinaturas. */
    @GetMapping("/documents/{id}/file")
    public ResponseEntity<Resource> file(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                                         @RequestParam(defaultValue = "false") boolean original) {
        Path p = docs.file(user, id, original);
        String name = docs.get(user, id).document().originalFileName();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(name, StandardCharsets.UTF_8).build().toString())
                .body(new FileSystemResource(p));
    }

    // ---------------------------------------------------------------- signatário

    @GetMapping("/me/documents")
    public List<SignerInboxItem> inbox(@AuthenticationPrincipal AuthUser signer) {
        return docs.inbox(signer);
    }

    @PostMapping("/documents/{id}/sign")
    @PreAuthorize("hasRole('SIGNER')")
    public DocumentDetailResponse sign(@AuthenticationPrincipal AuthUser signer, @PathVariable Long id,
                                       @Valid @RequestBody SignRequest req) {
        return docs.sign(signer, id, req);
    }
}

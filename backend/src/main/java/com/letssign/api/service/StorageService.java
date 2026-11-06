package com.letssign.api.service;

import com.letssign.api.config.LetsSignProperties;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Layout em disco: {storageDir}/documents/{id}/original.pdf | signed.pdf | signatures/field-{n}.png */
@Service
public class StorageService {
    private final Path root;

    public StorageService(LetsSignProperties props) {
        this.root = Path.of(props.storageDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root.resolve("documents"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Path docDir(Long documentId) {
        return root.resolve("documents").resolve(String.valueOf(documentId));
    }

    public Path original(Long documentId) { return docDir(documentId).resolve("original.pdf"); }

    public Path signed(Long documentId) { return docDir(documentId).resolve("signed.pdf"); }

    /** PDF "atual": o carimbado se já houver assinatura, senão o original. */
    public Path current(Long documentId) {
        Path s = signed(documentId);
        return Files.exists(s) ? s : original(documentId);
    }

    public Path signatureImage(Long documentId, Long fieldId) {
        return docDir(documentId).resolve("signatures").resolve("field-" + fieldId + ".png");
    }

    public void write(Path target, byte[] bytes) {
        try {
            Files.createDirectories(target.getParent());
            Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
            Files.write(tmp, bytes);
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void copy(Path from, Path to) {
        try {
            Files.createDirectories(to.getParent());
            Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}

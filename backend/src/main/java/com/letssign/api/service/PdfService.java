package com.letssign.api.service;

import com.letssign.api.domain.SignatureField;
import com.letssign.api.error.ApiException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Service
public class PdfService {

    /** Valida o PDF e devolve o número de páginas (mesma mensagem de erro do app). */
    public int pageCount(byte[] pdf) {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            if (doc.isEncrypted()) throw ApiException.badRequest("PDF protegido por senha não é suportado.");
            int n = doc.getNumberOfPages();
            if (n < 1) throw ApiException.badRequest("O PDF não tem páginas.");
            return n;
        } catch (IOException e) {
            throw ApiException.badRequest("Arquivo inválido, corrompido ou não suportado. Selecione outro PDF.");
        }
    }

    /**
     * Carimba as imagens nos campos. Mesma conversão do PdfStampingUtil do app:
     * ratios com origem topo-esquerda -> coordenadas PDF com origem inferior-esquerda,
     * relativos ao mediaBox da página.
     */
    public byte[] stamp(Path source, List<SignatureField> fields, Map<Long, BufferedImage> imagesByFieldId) {
        try (PDDocument doc = Loader.loadPDF(source.toFile())) {
            for (SignatureField f : fields) {
                BufferedImage img = imagesByFieldId.get(f.id);
                if (img == null || f.page < 0 || f.page >= doc.getNumberOfPages()) continue;
                PDPage page = doc.getPage(f.page);
                PDRectangle box = page.getMediaBox();
                float x = box.getLowerLeftX() + f.xRatio * box.getWidth();
                float w = f.widthRatio * box.getWidth();
                float h = f.heightRatio * box.getHeight();
                float y = box.getLowerLeftY() + box.getHeight() - f.yRatio * box.getHeight() - h;
                PDImageXObject pdImage = LosslessFactory.createFromImage(doc, img);
                try (PDPageContentStream cs = new PDPageContentStream(doc, page,
                        PDPageContentStream.AppendMode.APPEND, true, true)) {
                    cs.drawImage(pdImage, x, y, w, h);
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}

package com.letssign.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Fluxo ponta a ponta: admin cadastra, cria, posiciona, envia; signatários assinam; PDF sai carimbado. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DocumentFlowIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void fluxoCompletoDeAssinatura() throws Exception {
        String admin = login("admin@letssign.com", "admin123");

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        long ana = createSigner(admin, "Ana Contratante", "ana-" + suffix + "@x.com");
        long bob = createSigner(admin, "Bob Contratado", "bob-" + suffix + "@x.com");

        // e-mail duplicado -> 409
        mvc.perform(auth(post("/api/signers"), admin).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("fullName", "X", "email", "ana-" + suffix + "@x.com",
                                "phone", "11999999999", "password", "segredo1"))))
                .andExpect(status().isConflict());

        // PDF inválido -> 400 com a mesma mensagem do app
        mvc.perform(auth(multipart("/api/documents").file(new MockMultipartFile("file", "x.pdf", "application/pdf",
                        "nao sou pdf".getBytes())).param("title", "Ruim"), admin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Arquivo inválido, corrompido ou não suportado. Selecione outro PDF."));

        // cria documento de 2 páginas
        JsonNode created = body(mvc.perform(auth(multipart("/api/documents")
                        .file(new MockMultipartFile("file", "contrato.pdf", "application/pdf", pdf(2)))
                        .param("title", "Contrato de teste").param("inviteMessage", "Assine, por favor"), admin))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.document.status").value("DRAFT"))
                .andExpect(jsonPath("$.document.pageCount").value(2)));
        long docId = created.at("/document/id").asLong();

        // enviar sem signatários -> 400
        mvc.perform(auth(post("/api/documents/" + docId + "/send"), admin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("O documento precisa ter ao menos 1 signatário para ser enviado."));

        putJson(admin, "/api/documents/" + docId + "/signers", Map.of("signers", new Object[]{
                Map.of("signerId", ana, "classification", "CONTRATANTE"),
                Map.of("signerId", bob, "classification", "CONTRATADO")}))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.signers.length()").value(2));

        // página inexistente -> 400
        putJson(admin, "/api/documents/" + docId + "/fields", Map.of("fields", new Object[]{
                field(ana, 5, 0.1, 0.1)})).andExpect(status().isBadRequest());

        // x=0.95 com largura 0.3 é clampado para 0.7 (campo não sai da página)
        JsonNode withFields = body(putJson(admin, "/api/documents/" + docId + "/fields", Map.of("fields", new Object[]{
                field(ana, 0, 0.95, 0.80), field(bob, 1, 0.10, 0.10)}))
                .andExpect(status().isOk()));
        assertThat(withFields.at("/fields/0/xRatio").floatValue()).isEqualTo(0.7f, org.assertj.core.data.Offset.offset(1e-5f));
        long anaField = withFields.at("/fields/0/id").asLong();
        long bobField = withFields.at("/fields/1/id").asLong();

        String anaTok = login("ana-" + suffix + "@x.com", "segredo1");
        String bobTok = login("bob-" + suffix + "@x.com", "segredo1");

        // rascunho não aparece pro signatário
        mvc.perform(auth(get("/api/me/documents"), anaTok)).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(auth(get("/api/documents/" + docId), anaTok)).andExpect(status().isNotFound());
        // signatário não acessa rotas de admin
        mvc.perform(auth(get("/api/signers"), anaTok)).andExpect(status().isForbidden());

        mvc.perform(auth(post("/api/documents/" + docId + "/send"), admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.document.status").value("SENT"));
        // reenvio idempotente: continua SENT e não duplica evento
        mvc.perform(auth(post("/api/documents/" + docId + "/send"), admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.events.length()").value(2));
        // depois de enviado não edita
        putJson(admin, "/api/documents/" + docId + "/fields", Map.of("fields", new Object[]{}))
                .andExpect(status().isConflict());

        mvc.perform(auth(get("/api/me/documents"), anaTok))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].myFieldCount").value(1))
                .andExpect(jsonPath("$[0].status").value("PENDING"));

        // Ana tenta assinar o campo do Bob -> 400
        signJson(anaTok, docId, Map.of(String.valueOf(bobField), png())).andExpect(status().isBadRequest());
        // Ana sem assinar todos -> 400
        signJson(anaTok, docId, Map.of()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Assine todos os campos destacados antes de concluir."));
        // admin não assina
        signJson(admin, docId, Map.of(String.valueOf(anaField), png())).andExpect(status().isForbidden());

        signJson(anaTok, docId, Map.of(String.valueOf(anaField), "data:image/png;base64," + png()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.document.status").value("PARTIALLY_SIGNED"));
        signJson(anaTok, docId, Map.of(String.valueOf(anaField), png())).andExpect(status().isConflict());

        signJson(bobTok, docId, Map.of(String.valueOf(bobField), png()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.document.status").value("COMPLETED"))
                .andExpect(jsonPath("$.document.completedAt").isNumber())
                .andExpect(jsonPath("$.events[-1].type").value("COMPLETED"));

        // PDF final tem 1 imagem carimbada em cada página
        byte[] finalPdf = mvc.perform(auth(get("/api/documents/" + docId + "/file"), admin))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn().getResponse().getContentAsByteArray();
        try (PDDocument d = Loader.loadPDF(finalPdf)) {
            assertThat(d.getNumberOfPages()).isEqualTo(2);
            for (PDPage p : d.getPages()) assertThat(images(p)).isEqualTo(1);
        }
        byte[] orig = mvc.perform(auth(get("/api/documents/" + docId + "/file?original=true"), anaTok))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        try (PDDocument d = Loader.loadPDF(orig)) {
            assertThat(images(d.getPage(0))).isZero();
        }

        // nova versão: rascunho v2, mesmos participantes/campos, pendentes, PDF sem assinatura
        mvc.perform(auth(post("/api/documents/" + docId + "/versions"), admin))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.document.version").value(2))
                .andExpect(jsonPath("$.document.parentDocumentId").value(docId))
                .andExpect(jsonPath("$.document.status").value("DRAFT"))
                .andExpect(jsonPath("$.signers[0].status").value("PENDING"))
                .andExpect(jsonPath("$.fields.length()").value(2))
                .andExpect(jsonPath("$.fields[0].signed").value(false));
    }

    @Test
    void semTokenOuTokenRuimDa401() throws Exception {
        mvc.perform(get("/api/documents")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists());
        mvc.perform(get("/api/documents").header("Authorization", "Bearer lixo")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@letssign.com\",\"password\":\"errada\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("E-mail ou senha inválidos."));
        mvc.perform(get("/api/health")).andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ helpers

    private static Map<String, Object> field(long signer, int page, double x, double y) {
        return Map.of("signerId", signer, "page", page, "xRatio", x, "yRatio", y, "widthRatio", 0.3, "heightRatio", 0.08);
    }

    private String login(String email, String pw) throws Exception {
        return body(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email, "password", pw))))
                .andExpect(status().isOk())).get("token").asText();
    }

    private long createSigner(String tok, String name, String email) throws Exception {
        return body(mvc.perform(auth(post("/api/signers"), tok).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("fullName", name, "email", email,
                        "phone", "11999999999", "password", "segredo1"))))
                .andExpect(status().isCreated())).get("id").asLong();
    }

    private org.springframework.test.web.servlet.ResultActions putJson(String tok, String url, Object b) throws Exception {
        return mvc.perform(auth(put(url), tok).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(b)));
    }

    private org.springframework.test.web.servlet.ResultActions signJson(String tok, long doc, Map<String, String> sigs) throws Exception {
        return mvc.perform(auth(post("/api/documents/" + doc + "/sign"), tok).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("signatures", sigs))));
    }

    private static <T extends MockHttpServletRequestBuilder> T auth(T b, String tok) {
        b.header("Authorization", "Bearer " + tok);
        return b;
    }

    private JsonNode body(org.springframework.test.web.servlet.ResultActions r) throws Exception {
        return json.readTree(r.andReturn().getResponse().getContentAsString());
    }

    private static byte[] pdf(int pages) throws Exception {
        try (PDDocument d = new PDDocument()) {
            for (int i = 0; i < pages; i++) d.addPage(new PDPage(PDRectangle.A4));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            d.save(out);
            return out.toByteArray();
        }
    }

    private static String png() throws Exception {
        BufferedImage img = new BufferedImage(300, 80, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.BLUE);
        g.setStroke(new BasicStroke(4));
        g.drawLine(10, 60, 290, 20);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return Base64.getEncoder().encodeToString(out.toByteArray());
    }

    private static int images(PDPage p) throws Exception {
        if (p.getResources() == null) return 0;
        int n = 0;
        for (var name : p.getResources().getXObjectNames()) {
            if (p.getResources().getXObject(name) instanceof PDImageXObject) n++;
        }
        return n;
    }
}

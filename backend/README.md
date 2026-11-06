# LetsSign Backend (Spring Boot)

API REST de teste para o app LetsSign Mobile. Espelha as entidades e regras do
app (Room) para que, na Sprint 2, `UserRepository`/`DocumentRepository` possam
ganhar uma implementação Retrofit sem mexer nas telas.

Stack: Java 17, Spring Boot 3.3, Spring Security + JWT (HS256), Spring Data JPA,
H2 em arquivo, PDFBox 3 (carimbo das assinaturas), springdoc (Swagger).

## Rodar

```
cd backend
./mvnw spring-boot:run          # ou: mvn spring-boot:run
# Swagger: http://localhost:8088/swagger-ui.html
```

Variáveis (opcionais): `PORT` (8088), `LETSSIGN_DATA` (./data — banco H2 + PDFs),
`LETSSIGN_JWT_SECRET` (32+ chars), `LETSSIGN_JWT_TTL` (minutos, 720),
`LETSSIGN_ADMIN_EMAIL` / `LETSSIGN_ADMIN_PASSWORD` (admin@letssign.com / admin123,
iguais ao app), `H2_CONSOLE=true` para liberar `/h2-console`.

Testes: `./mvnw test` (fluxo ponta a ponta em `DocumentFlowIntegrationTest`).

Emulador Android acessa o host em `http://10.0.2.2:8088`.

## Endpoints

Todos (exceto login/health/swagger) exigem `Authorization: Bearer <token>`.
Datas são epoch millis (igual ao app). Erros: `{status, error, message, fields, timestamp}`.

| Método | Rota | Perfil | O quê |
|---|---|---|---|
| POST | /api/auth/login | — | `{email,password}` → `{token, expiresIn, user}` |
| GET | /api/auth/me | qualquer | usuário logado |
| GET | /api/health | — | status |
| GET | /api/signers | ADMIN | signatários do admin |
| POST | /api/signers | ADMIN | `{fullName,email,phone,password}` |
| GET/PUT | /api/signers/{id} | ADMIN | ver / editar (`newPassword` opcional) |
| POST | /api/documents | ADMIN | multipart: `file` (PDF), `title`, `inviteMessage` |
| GET | /api/documents | ADMIN | documentos do admin |
| PUT | /api/documents/{id}/signers | ADMIN | `{signers:[{signerId,classification}]}` (zera campos) |
| PUT | /api/documents/{id}/fields | ADMIN | `{fields:[{signerId,page,xRatio,yRatio,widthRatio,heightRatio}]}` |
| PUT | /api/documents/{id}/invite-message | ADMIN | `{message}` |
| POST | /api/documents/{id}/send | ADMIN | envia (idempotente) |
| POST | /api/documents/{id}/versions | ADMIN | nova versão (rascunho v+1) |
| GET | /api/documents/{id} | dono/signatário | documento + signatários + campos + eventos |
| GET | /api/documents/{id}/file | dono/signatário | PDF atual; `?original=true` sem assinaturas |
| GET | /api/me/documents | SIGNER | pendentes e assinados (sem rascunhos) |
| POST | /api/documents/{id}/sign | SIGNER | `{signatures:{"<fieldId>":"<png base64>"}}` |

## Regras (mesmas do app, mais estritas onde o app só tinha convenção)

- Envio exige ≥1 signatário e ≥1 campo; reenviar um SENT não duplica nada.
- Signatários/campos/mensagem só mudam em DRAFT ou SENDING_FAILED; depois disso → 409, crie nova versão.
- Campos em ratios 0..1 (origem topo-esquerda), clampados para não sair da página;
  `page` precisa existir no PDF.
- Assinar exige enviar exatamente os campos do próprio signatário; cada um só assina uma vez.
  Todos assinaram → COMPLETED; senão PARTIALLY_SIGNED.
- Assinaturas são carimbadas no PDF com a mesma conversão do `PdfStampingUtil` do app.
  O original fica preservado; nova versão parte do original.
- Admin só enxerga os próprios signatários/documentos; signatário só vê documentos
  não-rascunho em que participa (o resto é 404).

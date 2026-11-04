# LetsSign Mobile — Sprint 1

Aplicativo Android nativo (Kotlin + Jetpack Compose) desenvolvido para o
Challenge LetsSign (FIAP), contemplando os dois fluxos da Sprint 1:

1. **Admin**: cadastra signatários, cria um fluxo de documento a partir de um
   PDF existente, seleciona/classifica signatários, posiciona campos de
   assinatura, revisa e envia, e acompanha o status do documento.
2. **Signatário**: visualiza documentos pendentes/assinados, assina (desenho
   manuscrito) cada campo destacado no PDF e conclui, atualizando o status do
   registro.

## Stack técnica

| Camada            | Tecnologia                                                   |
|-------------------|---------------------------------------------------------------|
| Linguagem/UI      | Kotlin + Jetpack Compose (Material 3) + Navigation-Compose     |
| Persistência      | Room (SQLite) — estruturado atrás de interfaces de repositório |
| Arquivos PDF      | Sistema de arquivos do app (`filesDir/documents`) + metadados no Room |
| Leitura de PDF    | `android.graphics.pdf.PdfRenderer` (renderização de páginas)   |
| Edição de PDF     | `PDFBox-Android` (carimbar a assinatura manuscrita no PDF)      |
| Senhas            | BCrypt (`jbcrypt`)                                              |

## Arquitetura

```
data/
  local/            entidades Room, DAOs, AppDatabase, Converters
  repository/        UserRepository / DocumentRepository (interfaces)
                      + implementações Room (*Impl)
  storage/            PdfFileStorage, PdfPageRenderer, PdfStampingUtil
di/                  AppContainer (injeção de dependência manual)
session/             SessionManager (usuário autenticado)
navigation/          Routes + NavGraph (Navigation-Compose)
ui/
  auth/               Login
  admin/
    signers/          Cadastro/edição de signatários
    documents/         Lista + detalhe/acompanhamento de documentos
    newdocument/        Wizard de criação (título/PDF → signatários →
                         posicionamento → revisão/envio)
  signer/              Lista de documentos do signatário + tela de assinatura
  common/              Componentes reutilizáveis (PdfPageView, StatusChip...)
```

As telas dependem apenas das **interfaces** `UserRepository` e
`DocumentRepository`. Na Sprint 2, essas interfaces poderão ganhar uma nova
implementação que consome a API REST do backend, sem necessidade de alterar
nenhuma tela/ViewModel — apenas trocar o que é instanciado em `AppContainer`.

## Persistência de documentos PDF

Optamos pela estratégia de **armazenamento em sistema de arquivos do
device**: o PDF selecionado é copiado para `context.filesDir/documents/`, e o
Room guarda apenas `filePath`, `originalFileName`, `pageCount`, `status` etc.
(ver `DocumentEntity`). Essa escolha favorece desempenho ao manipular PDFs
grandes com o PdfRenderer/PDFBox.

## Posicionamento de assinaturas (requisito técnico)

Cada campo (`SignatureFieldEntity`) guarda `page`, `xRatio`, `yRatio`,
`widthRatio`, `heightRatio` — todos como frações (0f..1f) do tamanho da
página do PDF, e não em pixels de tela. Isso garante que a posição seja
reaberta corretamente em qualquer dispositivo/tamanho de tela.

## Regras de negócio implementadas

- Documento precisa de ao menos 1 signatário e 1 campo de assinatura para ser
  enviado (`DocumentRepositoryImpl.send`).
- Cada campo está sempre associado a um `DocumentSignerEntity` (participante).
- Campos são sempre posicionados/clampados dentro dos limites da página
  (0..1 - largura/altura do campo).
- Envio é idempotente: reenviar um documento com falha atualiza o mesmo
  registro, sem duplicar solicitações (`DocumentStatus.SENDING_FAILED` →
  `SENT`).
- Alterações após o envio exigem nova versão: `createNewVersion` clona o
  documento (PDF, signatários e campos) com `version + 1` e
  `parentDocumentId`, mantendo o documento original imutável.
- Documento só fica `COMPLETED` quando todos os `DocumentSignerEntity`
  mudarem para `SIGNED` (`DocumentRepositoryImpl.submitSignature`).
- Isolamento de dados: Admin só vê documentos de `ownerAdminId` = seu id;
  Signatário só vê documentos com status diferente de `DRAFT` nos quais foi
  incluído como signatário.

## Como executar

1. Abra a pasta `LetsSignMobile` no Android Studio (Koala ou mais recente).
   O Android Studio detecta o `gradle-wrapper.properties` (Gradle 8.7) e
   baixa/gera automaticamente o `gradle-wrapper.jar` no primeiro sync caso
   ele não esteja presente no pacote .zip.
2. Aguarde a sincronização do Gradle (dependências baixadas automaticamente:
   Compose, Room, Navigation, PDFBox-Android, jBCrypt).
3. Rode no emulador ou dispositivo físico com Android 7.0 (API 24) ou
   superior.
4. Login padrão do Admin (semeado automaticamente na primeira execução):
   - **E-mail:** `admin@letssign.com`
   - **Senha:** `admin123`
5. Cadastre signatários pelo Admin (aba "Signatários") e use as credenciais
   definidas (e-mail/senha) para logar como Signatário.

## Gerando o APK

```
./gradlew assembleDebug
```

O APK fica em `app/build/outputs/apk/debug/app-debug.apk`.

## Próximos passos (Sprint 2)

- Substituir `UserRepositoryImpl`/`DocumentRepositoryImpl` por
  implementações que consomem API REST (Retrofit) sem alterar as telas.
- Disparo de notificação (e-mail/push) com deeplink `letssign://document/...`
  (scheme já registrado no `AndroidManifest.xml`).

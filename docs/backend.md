# Backend and integration reference

[← Back to README](../README.md)

## Backend — Java 21

cd backend
./mvnw test

## Frontend — Node 20+

cd frontend
npm ci
npm run build

````

To actually start it, the backend reads its configuration from **environment
variables** (Spring does not load `.env`; see [Environment variables](#environment-variables)),
Postgres starts with `docker compose up -d` in `backend/`, and the frontend reads
`frontend/.env` (copy it from `frontend/.env.example`).

---

## Frontend

## Stack

| Layer | Technology |
|-------|-----------|
| Runtime | React 19 |
| Authentication | Keycloak via `oidc-client-ts` + `react-oidc-context` |
| Container | Docker Compose (node:20) |

## Requirements

- Node 20+
- Environment variables (`.env`):
  - `VITE_API_BASE_URL` — backend API base URL
  - `VITE_OIDC_AUTHORITY` — Keycloak realm URL
  - `VITE_OIDC_CLIENT_ID` — OIDC client ID
  - `VITE_OIDC_REDIRECT_URI` — post-login redirect URI

```bash
docker compose up   # starts the dev server in a node:20 container
````

Exposed port: `5173`. Proxy configured in `vite.config.js`:

- `/deploy-api` → `http://host.docker.internal:8081`

## Structure

```
src/
├── App.jsx                  # Routes, providers, global logic
├── main.jsx                 # Entry point (wraps the OIDC provider)
├── auth/
│   └── oidcConfig.js        # Keycloak/OIDC configuration
├── assets/icons/            # SVG icons as React components
├── components/
│   ├── Shell.jsx            # Main layout (Rail, TopBar, Sidebar)
│   ├── Queue.jsx            # Ticket list (inbox)
│   ├── Detail.jsx           # Ticket detail + DetailAside
│   ├── DeployPage.jsx       # Deploy interface
│   ├── AdminPanel.jsx       # Admin settings panel
│   ├── MetricsPage.jsx      # SLA metrics dashboard
│   ├── JiraPanel.jsx        # Jira integration panel
│   ├── LinkAtlassianPage.jsx # Atlassian OAuth linking screen (conditional)
│   ├── SecondaryLayout.jsx  # Secondary layout wrapper
│   ├── Skeleton.jsx         # Loading placeholders
│   ├── Tooltip.jsx          # Tooltip component
│   ├── Tour.jsx             # Guided onboarding tour
│   ├── Rail.jsx             # Vertical side navigation
│   ├── Icons.jsx            # Icon wrapper
│   └── data.jsx             # Static/mock data
├── constants/
│   └── colors.js            # Color palette per status
├── context/
│   └── StatusColorsContext.js # Status color context
├── hooks/
│   ├── useConversations.js  # Fetches Jira cards + pagination
│   ├── useFeedback.js       # UI feedback state
│   ├── useJiraCard.js       # Fetches a card + transitions
│   ├── useOutsideClick.js   # Detects clicks outside an element
│   └── useToast.jsx         # Toast notification system
├── services/
│   ├── api.js               # HTTP layer (fetch + bearer auth)
│   └── metricsSettings.js   # Metrics settings
├── styles/                  # Global CSS (variables, utilities, badges)
└── utils/
    └── string.js            # String utilities
```

## Routes

| Path       | Component   | Description                                                         |
| ---------- | ----------- | ------------------------------------------------------------------- |
| `/`        | App (Queue) | Ticket inbox — shows `LinkAtlassianPage` if Atlassian is not linked |
| `/deploy`  | DeployPage  | Deploy interface                                                    |
| `/admin`   | AdminPanel  | Admin settings                                                      |
| `/metrics` | MetricsPage | SLA dashboard                                                       |

## Consumed API endpoints

| Group         | Endpoints                                                                                                                                                                                                                                                                                                                                                     |
| ------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Cards (Jira)  | `GET /card/search`, `GET /card/:key`, `PATCH /card/:key`, `GET /card/:key/transitions`, `POST /card/:key/comment`, `GET /card/sprints`, `GET /card/statuses`, `GET /card/priorities`, `GET /card/users/assignable/default`, `GET /card/counts`, `GET /card/attachment/:id/content`, `PUT /card/:key/star`, `DELETE /card/:key/star`, `GET /card/starred/keys` |
| Email         | `GET /email/by-ticket/:id`, `GET /email/by-ticket/:id/thread-messages`, `POST /email/send` (multipart)                                                                                                                                                                                                                                                        |
| Conversations | `GET /conversations?ticketId=...`, `GET /conversations/ticket/:id/last-interaction`                                                                                                                                                                                                                                                                           |
| Admin         | `GET /admin/config/public`, `GET /admin/config`, `PUT /admin/config/:key`                                                                                                                                                                                                                                                                                     |
| Auth          | `GET /auth/atlassian/status`, `GET /auth/atlassian/initiate`                                                                                                                                                                                                                                                                                                  |
| Metrics       | `GET /metrics?sprintId=...`                                                                                                                                                                                                                                                                                                                                   |

## Authentication

OIDC/Keycloak. The token is injected into every request through the `Authorization: Bearer <token>` header. The special `X-Atlassian-Reauth: required` response header triggers Atlassian re-authentication.

---

## Backend

## Architecture

The project follows **Hexagonal Architecture** (Ports and Adapters):

```
src/main/java/com/example/EVIM/
├── domain/
│   ├── model/          ← Domain entities (POJOs with Lombok)
│   ├── port/out/       ← Outbound interfaces (contracts)
│   ├── service/        ← Domain services (HtmlSanitizer, TextNormalizer,
│   │                     PortalEmailThreadParser facade, SenderClassification)
│   └── parsing/        ← Thread-parsing subsystem (cohesive components)
│       ├── HeaderDetector        ← detects author/date headers
│       ├── DateRecognizer         ← parses PT/EN/ES/ISO dates
│       ├── BoilerplateFilter      ← boilerplate rules (with BoilerplateRule)
│       ├── HtmlToPlainTextConverter ← HTML→text (pre-strips hidden preheaders,
│       │                            injected sanitizer, Proofpoint v3 unwrap)
│       ├── UrlUnwrapper           ← undoes Proofpoint/SafeLinks wrappers
│       ├── SignatureStripper      ← removes trailing signature blocks
│       │                            (closing + name + phone + e-mail)
│       └── ThreadSplitter         ← message-splitting state machine
├── application/        ← Use cases, orchestration, DTOs
│   ├── auth/           ← Atlassian credentials context
│   ├── config/         ← Application configuration
│   ├── dto/            ← Data Transfer Objects (Java records)
│   └── metrics/        ← Parser observability (zero-dep)
│       ├── ParserMetrics          ← AtomicLong counters (success/fallback/failed/boilerplateByRule)
│       └── ParseDiagnostic        ← diagnostic record emitted per parse
├── adapter/
│   ├── in/
│   │   ├── web/        ← REST controllers
│   │   ├── scheduler/  ← Scheduled jobs (@Scheduled)
│   │   └── filter/     ← HTTP filters (AtlassianCredentialsFilter)
│   └── out/
│       ├── graph/      ← Microsoft Graph API (fetch/send e-mails)
│       ├── jira/       ← Jira Cloud REST API (client, mapper, util)
│       └── persistence/← JPA (PostgreSQL)
└── config/             ← Security, Jackson, CORS
```

**Dependency flow:** `adapter/in → application → domain ← adapter/out`

Inbound adapters (controllers, schedulers) call the application services, which operate on the domain through ports. Outbound adapters implement the ports defined in the domain.

---

## Technologies

| Layer      | Technology                                                                |
| ---------- | ------------------------------------------------------------------------- |
| Runtime    | Java 21 (Amazon Corretto), Spring Boot                                    |
| APIs       | Spring Web (REST)                                                         |
| Security   | Spring Security + OAuth2 Resource Server (JWT via Keycloak)               |
| E-mail     | Microsoft Graph SDK v6.6.0 + `java.net.http.HttpClient` (paginated fetch) |
| Credential | Azure Identity (ClientSecretCredential) with OkHttp                       |
| Jira       | REST API v3 (per-user OAuth2 + Basic Auth fallback)                       |
| Database   | PostgreSQL                                                                |

---

## Configuration

### Prerequisites

- Java 21 (Amazon Corretto 21 recommended)
- Docker (for PostgreSQL)
- Keycloak (OIDC provider)
- An app registration in Azure AD / Graph API (admin only)
- An app registration in the Atlassian Developer Console (see the scopes below)

### application.properties

```properties
## Database
spring.datasource.url=jdbc:postgresql://localhost:5433/Manager_DB
spring.datasource.username=${DB_USERNAME:evim}
spring.datasource.password=${DB_PASSWORD:evim}
spring.jpa.hibernate.ddl-auto=update

### OIDC (Keycloak)
spring.security.oauth2.resourceserver.jwt.issuer-uri=${OIDC_ISSUER_URI}

### Atlassian OAuth2
atlassian.client-id=${ATLASSIAN_CLIENT_ID}
atlassian.client-secret=${ATLASSIAN_CLIENT_SECRET}
atlassian.redirect-uri=${ATLASSIAN_REDIRECT_URI}
atlassian.frontend-url=${ATLASSIAN_FRONTEND_URL}

### Jira (service account)
jira.base-url=${jira_base_url}
jira.username=${jira_username}
jira.api-token=${jira_api_token}
jira.project-key=${jira_project_key}

### Microsoft Graph API
graph.tenant-id=${GRAPH_TENANT_ID}
graph.client-id=${GRAPH_CLIENT_ID}
graph.client-secret=${GRAPH_CLIENT_SECRET}
graph.mailbox-user-id=${GRAPH_MAILBOX_USER_ID}
graph.folder-id=${GRAPH_FOLDER_ID}
graph.mock.enabled=true
graph.poll.fixed-delay-ms=60000
```

---

## Database

### Email

| Field            | Type               | Description                                                    |
| ---------------- | ------------------ | -------------------------------------------------------------- |
| `id`             | Long               | Auto-generated PK                                              |
| `sender`         | String             | Sender address                                                 |
| `receiver`       | String             | First recipient address                                        |
| `subject`        | String             | Subject                                                        |
| `body`           | String (TEXT)      | HTML body of the e-mail                                        |
| `received`       | LocalDateTime      | Received date/time (UTC)                                       |
| `status`         | Status (enum)      | Open, UnderReview, PendingCustomer, CustomerValidation, Closed |
| `messageId`      | String (UNIQUE)    | Message ID in the Graph API                                    |
| `conversationId` | Long (FK)          | Associated conversation                                        |
| `attachments`    | List\<Attachment\> | E-mail attachments                                             |

### Conversation

| Field             | Type          | Description                                        |
| ----------------- | ------------- | -------------------------------------------------- |
| `id`              | Long          | Auto-generated PK                                  |
| `ticketId`        | Integer       | Vendor ticket number (extracted from subject/body) |
| `subject`         | String        | Normalized subject                                 |
| `status`          | Status        | Conversation status                                |
| `priority`        | Priority      | High, Medium, Low                                  |
| `lastUpdate`      | LocalDateTime | Last update (last e-mail received)                 |
| `pendingResponse` | Boolean       | `true` = waiting for the vendor's reply            |
| `externalBoardId` | String        | Jira issue key (e.g. `EVIM-123`)                   |

### ThreadMessage

| Field            | Type          | Description                           |
| ---------------- | ------------- | ------------------------------------- |
| `id`             | Long          | PK                                    |
| `emailId`        | Long (FK)     | Source e-mail (portal)                |
| `conversationId` | Long (FK)     | Associated conversation               |
| `author`         | String        | Message author's name                 |
| `company`        | String        | Author's company                      |
| `sentAt`         | LocalDateTime | Sent date/time                        |
| `body`           | String        | Body of the individual message        |
| `ordinal`        | Integer       | Order in the thread (0 = most recent) |

### Attachment

| Field          | Type      | Description          |
| -------------- | --------- | -------------------- |
| `id`           | Long      | PK                   |
| `fileName`     | String    | File name            |
| `contentType`  | String    | MIME type            |
| `contentBytes` | byte[]    | Binary content       |
| `emailId`      | Long (FK) | E-mail it belongs to |

### SlaOwnershipLog

| Field       | Type          | Description                   |
| ----------- | ------------- | ----------------------------- |
| `id`        | Long          | PK                            |
| `jiraKey`   | String        | Jira issue key                |
| `ownerType` | OwnerType     | VENDOR or INTERNAL            |
| `startAt`   | LocalDateTime | Start of the ownership period |
| `endAt`     | LocalDateTime | End (null = active period)    |

---

## Application flows

### 1. E-mail ingestion

Ingestion is the system's main flow. It fetches unread e-mails from the shared mailbox, in the configured folder (`graph.folder-id`), and processes them.

**Trigger:** `EmailPollingScheduler` runs every 60 seconds (`fixedDelay`), or manually through `POST /email/ingest`.

**Concurrency guard:** an `AtomicBoolean` in `EmailIngestionService` prevents the scheduler and a manual trigger from running at the same time.

```
EmailPollingScheduler (@Scheduled, 60s)
  │
  ▼
EmailIngestionService.ingestNewEmails()
  │
  ├─ Acquires the lock (AtomicBoolean.compareAndSet)
  │
  ├─ EmailFetchPort.fetchNewEmails(pageConsumer)
  │   │
  │   │  ┌─────────────────────────────────────────────┐
  │   └─▶│ GraphMailFetchAdapter                       │
  │      │                                             │
  │      │ 1. Gets an access token via                 │
  │      │    ClientSecretCredential                   │
  │      │                                             │
  │      │ 2. Direct HTTP GET (java.net.http):         │
  │      │    /users/{userId}/mailFolders/{folderId}/  │
  │      │    messages?$filter=isRead eq false         │
  │      │    &$top=50                                 │
  │      │                                             │
  │      │ 3. For each page of 50 e-mails:             │
  │      │    - Maps JSON → Email via                  │
  │      │      GraphEmailMapper.fromJsonNode()        │
  │      │    - If hasAttachments: fetches via SDK     │
  │      │    - Hands the page to the consumer         │
  │      │                                             │
  │      │ 4. Follows @odata.nextLink (validating      │
  │      │    the graph.microsoft.com domain)          │
  │      │                                             │
  │      │ 5. Returns the number of fetched e-mails    │
  │      └─────────────────────────────────────────────┘
  │
  ├─ For each e-mail in the page:
  │   │
  │   ├─ EmailProcessingService.processSingleEmail(email)
  │   │   (each e-mail in its own @Transactional)
  │   │
  │   ├─ If processed successfully:
  │   │   └─ emailFetchPort.markAsRead(messageId)
  │   │      (PATCH via Graph SDK — outside the transaction)
  │   │
  │   ├─ If duplicate: increments the skipped counter
  │   │
  │   └─ If error: increments the counter, logs, continues
  │
  ├─ Logs progress after each page
  │
  └─ Releases the lock (finally)
```

**Architectural decision:** `markAsRead` runs **outside** the JPA transaction. If the e-mail is saved but `markAsRead` fails, the e-mail is fetched again on the next poll — but deduplication by `messageId` (UNIQUE) prevents reprocessing. This guarantees that no e-mail is lost.

---

### 2. Processing a single e-mail

`EmailProcessingService.processSingleEmail()` — annotated with `@Transactional` (one e-mail = one transaction).

```
processSingleEmail(email)
  │
  ├─ 1. DEDUPLICATION
  │   └─ findByMessageId(email.messageId)
  │      If it already exists → return false (skip)
  │
  ├─ 2. CONVERSATION RESOLUTION
  │   │
  │   ├─ Normalizes the subject:
  │   │   Removes Re: / Fwd: / Fw: / AW: / ENC: prefixes
  │   │   recursively (e.g. "Re: Fwd: Re: Problem" → "Problem")
  │   │
  │   ├─ Looks up existing conversations with a similar subject:
  │   │   SELECT DISTINCT conversation_id
  │   │   FROM tb_email
  │   │   WHERE LOWER(subject) LIKE '%problem%'
  │   │   ORDER BY conversation_id ASC
  │   │
  │   ├─ If found → uses the existing conversation (lowest ID)
  │   └─ If not → creates a new conversation (status=Open, pendingResponse=true)
  │
  ├─ 3. VENDOR TICKET EXTRACTION
  │   │
  │   │  Tries to extract the 8-digit ticket number:
  │   │
  │   ├─ Subject: regex \b(\d{8})\b
  │   │   e.g. "Problem ticket 12345678" → 12345678
  │   │
  │   ├─ Body (HTML stripped): regex \((\d{8})\)
  │   │   e.g. "Regarding request (12345678)" → 12345678
  │   │
  │   └─ Body: regex solicita[çc][aã]o\s+n.*(\d{8})
  │       e.g. "Solicitação nº 12345678" → 12345678
  │
  │   If found and the conversation has no ticketId:
  │   → conversation.ticketId = Integer(ticket)
  │
  ├─ 4. UPDATES THE CONVERSATION
  │   ├─ lastUpdate = email.received (or now)
  │   ├─ pendingResponse = !isInternalSender(email.sender)
  │   │   (uses SenderClassificationService — checks the domain
  │   │    against the 'sla.internal.domain' setting)
  │   └─ save(conversation)
  │
  ├─ 5. PERSISTS THE E-MAIL + ATTACHMENTS
  │   ├─ email.conversationId = conversation.id
  │   ├─ Detaches attachments from the e-mail (avoids cascade)
  │   ├─ save(email) → gets email.id
  │   └─ For each attachment:
  │       attachment.emailId = email.id
  │       save(attachment)
  │
  ├─ 6. THREAD MESSAGE PARSING
  │   └─ (see section 3 below)
  │
  └─ 7. SLA TRACKING
      ├─ If the sender is a vendor (external):
      │   AND the conversation is linked to Jira:
      │   → slaMetricsService.onVendorEmailReceived(jiraKey)
      │     (VENDOR → INTERNAL transition)
      └─ return true
```

---

### 3. Thread message parsing

E-mails from support portals (e.g. a vendor's help-desk portal) arrive as a single HTML document containing the whole conversation history. `PortalEmailThreadParser` (facade) delegates to a pipeline of components in `domain/parsing/`.

#### Parser architecture

```
PortalEmailThreadParser (facade, static API)
  │
  ├─ HtmlToPlainTextConverter
  │   ├─ Receives a UnaryOperator<String> sanitizer (functional injection)
  │   ├─ Receives a Predicate<String> isProperName
  │   ├─ Pre-pass: removeHiddenElements() — strips <div style="display:none">,
  │   │   visibility:hidden, opacity:0, max-height:0, font-size:1px and the
  │   │   HTML5 `hidden` attribute. Required BEFORE the OWASP sanitizer, which
  │   │   would remove the forbidden CSS values and leave the element orphaned.
  │   ├─ Decodes double escaping (&lt;a&gt; → <a>) when the body arrived
  │   │   entity-encoded
  │   ├─ Applies sanitizer → JSoup DOM walk → markdown links → img-alt promotion
  │   └─ Returns plain text
  │
  ├─ UrlUnwrapper (utility)
  │   ├─ Undoes Proofpoint v3 (`urldefense.com/v3/__<url>__;...`)
  │   ├─ Undoes Microsoft SafeLinks (`*.safelinks.protection.outlook.com`)
  │   └─ Applied by HtmlToPlainTextConverter when rewriting <a href>
  │
  ├─ ThreadSplitter (state machine)
  │   ├─ normalizeLines() → removes zero-width chars, collapses whitespace
  │   ├─ Main loop:
  │   │   ├─ Try 1: two-line header (AUTHOR_LINE + date within 5 lines)
  │   │   └─ Try 2: inline header
  │   ├─ collectBody() until the next header
  │   ├─ cleanOrphanHeaders() (uses dateRecognizer.isExactlyDate)
  │   └─ Post-process: SignatureStripper.strip() on each emitted body
  │
  ├─ HeaderDetector
  │   ├─ AUTHOR_LINE pattern (Unicode-aware)
  │   ├─ isProperName() — multi-word capitalized name
  │   ├─ tryInlineHeader() — e-mail + name + date on the same line
  │   ├─ findDateLineWithin() — looks for a date in the next N lines
  │   └─ startsWithUpper() — handles accents/BMP+
  │
  ├─ DateRecognizer
  │   ├─ Combined DATE_LINE pattern (PT/EN/ES/ISO/numeric)
  │   ├─ parseDate() → LocalDateTime or null
  │   ├─ containsDate() — find()
  │   └─ isExactlyDate() — matches() (full line)
  │
  ├─ SignatureStripper
  │   ├─ Bottom-up heuristic: classifies lines as SIGNATURE (e-mail,
  │   │   phone, URL, ALL CAPS name, department, slogan) / WEAK_SIGNATURE
  │   │   (title-case name) / DELIMITER (Atte., Atenciosamente, separator)
  │   │   / BLANK / CONTENT
  │   ├─ Only cuts if there is at least one strong signal (e-mail/phone/URL/
  │   │   closing) and the surviving body still has real prose
  │   ├─ Attachments live on the Email aggregate — never touched
  │   └─ isEffectivelyEmpty(body) — true when the body is just a greeting +
  │       signature; used by the ThreadMessageBuilder fallback to
  │       suppress phantom interactions (portal auto-acks)
  │
  └─ BoilerplateFilter
      ├─ BoilerplateRule record (name, pattern, minLineLength, priority)
      │   └─ implements Comparable (priority desc → minLineLength desc → pattern length desc)
      ├─ 50+ default rules (vendor portal, CSAT/NPS survey, [EXTERNAL]/
      │   [EXTERNO] banners, Proofpoint, disclaimers, closings)
      ├─ The `vendor-portal.solicitacao-atualizada` rule requires a digit (ticket no.)
      │   so it does not swallow legitimate prose such as "Sua solicitação foi
      │   recebida em nossa fila" ("your request was received in our queue")
      ├─ The `vendor-portal.csat-score-line` rule matches both bare digits (0..10)
      │   and the markdown-link form "[N](https://surveys...)" that each
      │   score takes after HTML→text
      ├─ isBoilerplate() / matchedRule()
      ├─ Consumer<String> onRuleMatched (callback for metrics)
      └─ stripFromText(text, isProperName) — strip + trailing signature trim
```

#### ParseResult

```java
public record ParseResult(List<ParsedMessage> messages, String plainText) {}
```

`parseDetailed(html)` returns both, avoiding a double parse on the fallback path.

#### Persistence orchestration (ThreadMessageBuilder)

```
ThreadMessageBuilder.buildAndSave(savedEmail, conversation)
  │
  ├─ PortalEmailThreadParser.parseDetailed(email.body)
  │   → ParseResult { messages, plainText }
  │
  ├─ If messages is empty → fallback:
  │   ├─ stripBoilerplate(plainText) + SignatureStripper.strip()
  │   ├─ If the body ends up empty → ParseDiagnostic.failed("blank-after-strip")
  │   ├─ If the body is effectively empty (just greeting + signature) →
  │   │   ParseDiagnostic.failed("phantom-empty") — suppresses portal
  │   │   auto-acks that would surface as "Olá Lucas," + signature
  │   └─ Otherwise saves it as a single message using senderDisplayName/Address
  │
  ├─ Checks whether this e-mail is the most recent in the conversation
  │
  ├─ If it is the most recent (or the only one):
  │   ├─ Removes the conversation's previous thread messages
  │   └─ Saves the new thread messages (ordinal = 0..N)
  │
  └─ Emits ParseDiagnostic → ParserMetrics
```

#### Batch reparse (ThreadReparseService)

```
POST /admin/reparse-threads        → reparses ALL e-mails
POST /admin/reparse-threads        → reparse scoped by payload:
  body { "conversationIds":[…], "emailIds":[…] }
  │
  ├─ Resolves the scope (conversationIds expand to emailIds via the repository)
  ├─ Runs on a virtual thread (async, does not block the request)
  ├─ Chunked (200 e-mails per chunk) with @Transactional per chunk
  ├─ Reparse clears the e-mail's stale thread messages before rewriting
  ├─ Status: GET /admin/reparse-threads/status →
  │   { running, total, processed, progress, newlyParsed,
  │     failedEmails, fallbackEmails, status, finishedAt }
  └─ Cancellable: DELETE /admin/reparse-threads
```

Example (scoped by conversation):

```bash
curl -X POST http://localhost:8080/admin/reparse-threads \
  -H "Content-Type: application/json" \
  -d '{"conversationIds":[28]}'
curl http://localhost:8080/admin/reparse-threads/status
```

#### Observability (ParserMetrics — zero-dep)

AtomicLong counters exposed at `GET /admin/parser-metrics`:

- `success` / `fallback` / `failed` / `dateUnparsed`
- `messagesEmitted` (total sub-messages parsed)
- `successByDomain` / `fallbackByDomain` (per-sender-domain breakdown)
- `boilerplateByRule` (per-rule hit counts via the BoilerplateFilter callback)

Reset with `DELETE /admin/parser-metrics`.

**Why replace instead of accumulate?** The vendor portal's e-mails carry the full history. Every new e-mail (interaction) brings all previous messages plus the new ones. Keeping both would cause duplication.

---

### 4. Sending e-mails

```
POST /email/send
  Body: { ticketId, to, subject, body, replicateToJira }
  Auth: JWT (Keycloak)
  │
  ▼
EmailSendService.send(dto, jwtAuth)
  │
  ├─ 1. Extracts the user's name from the JWT ("name" or "email" claim)
  │
  ├─ 2. Appends an HTML signature:
  │     <hr><small><strong>{name}</strong> via E-VIM</small>
  │
  ├─ 3. Sends through the Graph API:
  │   │
  │   └─ GraphMailSendAdapter.sendOnBehalf()
  │       POST /users/{mailboxUserId}/sendMail
  │       {
  │         message: {
  │           from: { name: "João via ", address: mailboxUserId },
  │           to: [{ address: dto.to }],
  │           subject: dto.subject,
  │           body: { contentType: "HTML", content: bodyWithSignature }
  │         },
  │         saveToSentItems: true
  │       }
  │
  ├─ 4. Saves a record of the sent e-mail:
  │     Email { sender=displayName, receiver=dto.to, ... }
  │     Links it to the conversation through the ticketId
  │
  └─ 5. If replicateToJira = true:
        ├─ Strips HTML from the body → plain text
        ├─ Builds the comment in ADF (Atlassian Document Format)
        └─ ProviderCommentPort.addComment(issueKey, adfJson)
           POST /rest/api/3/issue/{issueKey}/comment
```

---

### 5. Conversation lifecycle

```
CREATION (automatic during ingestion)
  │
  │  E-mail arrives → normalizes the subject → no existing conversation found
  │  → Creates Conversation { status=Open, pendingResponse=true }
  │
  ▼
UPDATE (on every new e-mail)
  │
  │  Each received e-mail updates:
  │  - lastUpdate = e-mail date
  │  - pendingResponse = true (external sender)
  │                      false (internal sender)
  │
  ▼
JIRA LINKING
  │
  │  PATCH /conversations/{id}/link-jira
  │  Body: "EVIM-123"
  │
  │  ConversationService.updateExternalBoardId():
  │  1. conversation.externalBoardId = "EVIM-123"
  │  2. save(conversation)
  │  3. slaMetricsService.onJiraLinked(jiraKey, pendingVendor)
  │     → Starts SLA tracking
  │
  ▼
QUERIES
  │
  ├─ GET /conversations → paginated list with filters (status, ticketId)
  ├─ GET /conversations/by-provider-assignee/{accountId} → by Jira assignee
  ├─ GET /conversations/by-provider-unassigned → unassigned
  └─ GET /conversations/ticket/{ticketId}/last-interaction → last e-mail
```

---

### 6. Jira integration

The integration uses **two authentication mechanisms**:

1. **Per-user OAuth2** — individual token bound through `AtlassianCredentialsFilter` (thread-local). Used when the user is authenticated and has a valid Atlassian token.
2. **Basic Auth (service account)** — fallback when there is no OAuth token. Configured with `jira.username` + `jira.api-token`.

```
ProviderApiClient (centralized HTTP client)
  │
  ├─ If CredentialsContext has an OAuth token → uses a Bearer token
  └─ Otherwise → uses Basic Auth (service account)
```

#### Jira operations/endpoints

| Operation           | Jira endpoint                              | Service                    |
| ------------------- | ------------------------------------------ | -------------------------- |
| Get issue           | `GET /rest/api/3/issue/{key}`              | ProviderIssueService       |
| Search issues (JQL) | `POST /rest/api/3/search/jql`              | ProviderIssueService       |
| Update issue        | `PUT /rest/api/3/issue/{key}`              | ProviderIssueUpdateService |
| Status transition   | `POST /rest/api/3/issue/{key}/transitions` | ProviderIssueUpdateService |
| List transitions    | `GET /rest/api/3/issue/{key}/transitions`  | ProviderTransitionService  |
| Add comment         | `POST /rest/api/3/issue/{key}/comment`     | ProviderCommentService     |
| Assignable users    | `GET /rest/api/3/user/assignable/search`   | ProviderUserService        |
| Current user        | `GET /rest/api/3/myself`                   | ProviderUserService        |

#### JQL construction

```
Base: project = {projectKey}

Optional filters:
  + AND status = "{status}"
  + AND assignee = "{accountId}"
  + AND assignee = EMPTY
  + AND {vendorTicketField} ~ "{ticketId}"
  + AND sprint = {sprintId}

ORDER BY created DESC
```

#### Issue PATCH

`PATCH /card/{issueKey}` supports partial updates:

```json
{
  "summary": "New title",
  "vendorTicket": "12345678",
  "priorityId": "3",
  "assigneeAccountId": "5f7...",
  "transitionId": "31"
}
```

`null` fields are ignored. `assigneeAccountId = ""` removes the assignee.

---

### 7. Atlassian OAuth2 flow

```
┌──────────┐     ┌──────────────┐     ┌─────────────────┐
│ Frontend │     │   Backend    │     │   Atlassian     │
└────┬─────┘     └──────┬───────┘     └────────┬────────┘
     │                  │                      │
     │ GET /auth/       │                      │
     │ atlassian/       │                      │
     │ initiate         │                      │
     ├─────────────────▶│                      │
     │                  │                      │
     │  { redirectUrl } │                      │
     │◀─────────────────┤                      │
     │                  │                      │
     │ window.location  │                      │
     │ = redirectUrl    │                      │
     ├──────────────────┼─────────────────────▶│
     │                  │                      │
     │                  │   GET /callback?     │
     │                  │   code=xxx&state=yyy │
     │                  │◀─────────────────────┤
     │                  │                      │
     │                  │  POST /oauth/token   │
     │                  │  (exchange code)     │
     │                  ├─────────────────────▶│
     │                  │                      │
     │                  │  { access_token,     │
     │                  │    refresh_token }   │
     │                  │◀─────────────────────┤
     │                  │                      │
     │                  │  GET /accessible-    │
     │                  │  resources           │
     │                  ├─────────────────────▶│
     │                  │                      │
     │                  │  [{ id: cloudId }]   │
     │                  │◀─────────────────────┤
     │                  │                      │
     │                  │  Saves token in DB:  │
     │                  │  keycloakSub →       │
     │                  │  { token, cloudId }  │
     │                  │                      │
     │ 302 Redirect     │                      │
     │ → frontend?      │                      │
     │   atlassian=     │                      │
     │   linked         │                      │
     │◀─────────────────┤                      │
     │                  │                      │
```

**Automatic refresh:** `AtlassianCredentialsFilter` checks the token's validity on every request. If it is expired (with a 5-minute margin), it tries to refresh. If the refresh fails, it responds `401` with the `X-Atlassian-Reauth: required` header — the frontend detects it and asks the user to link again.

**Requested scopes:**

```
read:jira-work
write:jira-work
read:jira-user
read:board-scope:jira-software
read:sprint:jira-software
read:issue:jira-software
offline_access (for refresh tokens)
```

---

### 8. SLA metrics

The system tracks who is responsible for each ticket: the vendor (VENDOR) or the internal team (INTERNAL). This makes it possible to compute response times and bottlenecks.

```
                   Vendor e-mail arrives
                   onVendorEmailReceived()
    ┌──────────┐ ─────────────────────────────▶ ┌──────────┐
    │  VENDOR  │                                │ INTERNAL │
    │ (waiting │ ◀───────────────────────────── │  (our    │
    │ on reply)│      Reply e-mail sent         │  turn to │
    └──────────┘      onInternalReply()         │   act)   │
         │                                      └──────────┘
         │                                           │
         │          Issue resolved in Jira           │
         │          onIssueResolved()                │
         └──────────────▶ CLOSED ◀───────────────────┘
```

#### Events that trigger transitions

| Event                        | Transition                     | Triggered by                                                |
| ---------------------------- | ------------------------------ | ----------------------------------------------------------- |
| `onJiraLinked(key, pending)` | → VENDOR or INTERNAL (initial) | `ConversationService` when linking Jira                     |
| `onVendorEmailReceived(key)` | VENDOR → INTERNAL              | `EmailProcessingService` when processing an external e-mail |
| `onInternalReply(key)`       | INTERNAL → VENDOR              | `EmailSendService` when sending a reply to the vendor       |
| `onIssueResolved(key)`       | Any → CLOSED                   | `SlaPollingScheduler` when it detects a "done" issue        |

#### Computed metrics

| Metric                 | Calculation                                                 |
| ---------------------- | ----------------------------------------------------------- |
| `vendorTimeSeconds`    | Sum of every period with ownerType=VENDOR                   |
| `firstResponseSeconds` | Duration of the first VENDOR period (until the first reply) |
| `pingPongScore`        | Total number of state transitions                           |
| `currentOwner`         | Owner of the active log (endAt=null), or "CLOSED"           |
| `trackingSince`        | Date of the first log                                       |

**Endpoint:** `GET /metrics?sprintId={optional}` — returns per-ticket metrics + an aggregated summary.

---

### 9. Issue resolution polling

`SlaPollingScheduler` runs every 10 minutes and checks whether Jira issues were resolved:

```
SlaPollingScheduler.checkResolvedIssues()
  │
  ├─ Gets every active jiraKey from SlaMetricsService
  │
  └─ For each key:
      ├─ GET /rest/api/3/issue/{key}
      ├─ Extracts status.name
      ├─ If status ∈ {"done", "closed", "resolved", "concluído"}:
      │   └─ slaMetricsService.onIssueResolved(key)
      │      (closes the active ownership)
      └─ On error → logs and continues
```

---

## Security

### Authentication

- **JWT** via Keycloak (OAuth2 Resource Server)
- Every request (except public endpoints) requires the `Authorization: Bearer {token}` header
- The token is validated against the configured `issuer-uri`

### Atlassian credentials

- Per-user OAuth2 tokens, stored in the database and bound to the `keycloakSub`
- Automatic refresh in the filter (5 minutes before expiry)
- Falls back to the service account (Basic Auth) when there is no OAuth token

---

## API — Endpoints

### E-mails (`/email`)

| Method | Path                                          | Description               |
| ------ | --------------------------------------------- | ------------------------- |
| GET    | `/email`                                      | Paginated e-mail list     |
| GET    | `/email/search?subject=`                      | Search by subject         |
| GET    | `/email/conversation/{id}`                    | E-mails of a conversation |
| GET    | `/email/by-ticket/{ticketId}`                 | E-mails by vendor ticket  |
| GET    | `/email/by-ticket/{ticketId}/thread-messages` | Parsed thread messages    |
| POST   | `/email/ingest`                               | Manual ingestion trigger  |
| POST   | `/email/send`                                 | Sends an e-mail           |
| GET    | `/email/{emailId}/attachments`                | Lists attachments         |
| GET    | `/email/attachments/{id}/download`            | Downloads an attachment   |

### Conversations (`/conversations`)

| Method | Path                                                | Description                                    |
| ------ | --------------------------------------------------- | ---------------------------------------------- |
| GET    | `/conversations`                                    | Paginated list with filters (status, ticketId) |
| PATCH  | `/conversations/{id}/link-jira`                     | Links to a Jira issue                          |
| GET    | `/conversations/by-provider-assignee/{accountId}`   | By Jira assignee                               |
| GET    | `/conversations/by-provider-unassigned`             | Unassigned                                     |
| GET    | `/conversations/ticket/{ticketId}/last-interaction` | Last interaction                               |

### Jira (`/card`)

| Method | Path                                      | Description                    |
| ------ | ----------------------------------------- | ------------------------------ |
| GET    | `/card/{issueKey}`                        | Issue data                     |
| GET    | `/card/{issueKey}/raw`                    | Raw Jira JSON                  |
| POST   | `/card/{issueKey}/comment`                | Adds a comment                 |
| GET    | `/card/{issueKey}/transitions`            | Available transitions          |
| PATCH  | `/card/{issueKey}`                        | Updates an issue               |
| GET    | `/card/users/assignable/project/{key}`    | Assignable users               |
| GET    | `/card/users/assignable/default`          | Users of the default project   |
| GET    | `/card/sprints`                           | Active sprints                 |
| GET    | `/card/statuses`                          | Project statuses               |
| GET    | `/card/priorities`                        | Priorities                     |
| GET    | `/card/search`                            | Filtered search                |
| GET    | `/card/attachment/{attachmentId}/content` | Jira attachment download proxy |
| GET    | `/card/starred`                           | User's starred issues          |
| GET    | `/card/starred/keys`                      | Starred issue keys only        |
| PUT    | `/card/{issueKey}/star`                   | Stars an issue                 |
| DELETE | `/card/{issueKey}/star`                   | Unstars an issue               |

### Metrics (`/metrics`)

| Method | Path                 | Description |
| ------ | -------------------- | ----------- |
| GET    | `/metrics?sprintId=` | SLA metrics |

### Admin (`/admin`)

| Method | Path                            | Description                                                                                       |
| ------ | ------------------------------- | ------------------------------------------------------------------------------------------------- |
| GET    | `/admin/config`                 | All settings                                                                                      |
| POST   | `/admin/config`                 | Creates a setting                                                                                 |
| GET    | `/admin/config/public`          | Public settings (no auth)                                                                         |
| PUT    | `/admin/config/{key}`           | Updates a setting                                                                                 |
| DELETE | `/admin/config/{id}`            | Removes a setting                                                                                 |
| POST   | `/admin/ingest`                 | Manual e-mail ingestion trigger                                                                   |
| GET    | `/admin/parser-metrics`         | Parser counter snapshot                                                                           |
| DELETE | `/admin/parser-metrics`         | Resets the counters                                                                               |
| POST   | `/admin/reparse-threads`        | Starts an async reparse (optional body `{conversationIds:[…], emailIds:[…]}` for scope)           |
| DELETE | `/admin/reparse-threads`        | Cancels the running reparse                                                                       |
| GET    | `/admin/reparse-threads/status` | Progress (running, processed, total, progress, newlyParsed, failedEmails, fallbackEmails, status) |

### Atlassian OAuth (`/auth/atlassian`)

| Method | Path                       | Description           |
| ------ | -------------------------- | --------------------- |
| GET    | `/auth/atlassian/initiate` | Starts the OAuth flow |
| GET    | `/auth/atlassian/callback` | Callback (public)     |
| GET    | `/auth/atlassian/status`   | Linking status        |
| DELETE | `/auth/atlassian`          | Unlinks the account   |

---

## Environment variables

| Variable                      | Required | Description                                                             |
| ----------------------------- | :------: | ----------------------------------------------------------------------- |
| `DB_USERNAME` / `DB_PASSWORD` |    No    | PostgreSQL credentials (default: `evim` / `evim`)                       |
| `GRAPH_MOCK_ENABLED`          |    No    | `true` switches Microsoft Graph off (file default: `true`)              |
| `OIDC_ISSUER_URI`             |   Yes    | Keycloak realm URL                                                      |
| `GRAPH_TENANT_ID`             |   Yes    | Azure AD tenant ID                                                      |
| `GRAPH_CLIENT_ID`             |   Yes    | Azure AD app registration client ID                                     |
| `GRAPH_CLIENT_SECRET`         |   Yes    | Azure AD app registration secret                                        |
| `GRAPH_MAILBOX_USER_ID`       |   Yes    | ID or UPN of the shared mailbox                                         |
| `GRAPH_FOLDER_ID`             |   Yes    | ID of the folder to watch (Inbox, etc.)                                 |
| `ATLASSIAN_CLIENT_ID`         |   Yes    | Atlassian OAuth2 client ID                                              |
| `ATLASSIAN_CLIENT_SECRET`     |   Yes    | Atlassian OAuth2 client secret                                          |
| `ATLASSIAN_REDIRECT_URI`      |    No    | Callback URL (default: `http://localhost:8080/auth/atlassian/callback`) |
| `ATLASSIAN_FRONTEND_URL`      |    No    | Frontend URL (default: `http://localhost:5173`)                         |
| `jira_base_url`               |   Yes*   | Jira base URL (service account)                                         |
| `jira_username`               |   Yes*   | Jira user (service account)                                             |
| `jira_api_token`              |   Yes*   | Jira API token (service account)                                        |
| `jira_project_key`            |   Yes*   | Jira project key (e.g. `EVIM`)                                          |

\* Jira is required if per-user OAuth2 is not used.

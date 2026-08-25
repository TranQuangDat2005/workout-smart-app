  
**Technical Design Specification**

***\[Project Name\] (\[CODE\]) \- Report 4***

| Project Name | \[Project Name\] |
| :---- | :---- |
| **Architecture Style** | \[Monolith / Microservices / Serverless / Event-Driven / Hybrid\] |
| **SRS Reference** | SRS v\[X.X\] |
| **Screen Design Ref** | ScreenDesignSpec v\[X.X\] |
| **TDS Version** | v1.0.0 |
| **Date Created** | \[DD/MM/YYYY\] |
| **Last Updated** | \[DD/MM/YYYY\] |
| **Author(s)** | \[Tech Lead / Senior Dev\] |
| **Reviewer(s)** | \[Name / Role\] |
| **Status** | Draft / In Review / Approved |

# **How to Use This Template**

| Template conventions *Colour-coded guidance blocks:*   *BLUE  box \= Guidance — explains what to write in this section*   *TEAL  box \= ARCH — what changes per architecture style*   *RED   box \= REQUIRED — must not be skipped regardless of architecture*   *GREEN box \= Example — illustrative content; replace with your own*   *PURPLE box \= Diagram placeholder — insert diagram here*   *BROWN box \= Code / DDL / pseudocode block*   *GRAY  box \= Reference to companion document* *Workflow:*   *1\. Fill Parts 1–3 before Sprint 1 coding begins*   *2\. Fill Parts 4–6 rolling — one section per feature sprint*   *3\. Parts 7–9 once during Sprint 0, refine as project progresses*   *4\. DELETE all guidance boxes before distributing the completed TDS* *Companion documents (do not duplicate content from these):*   *SRS v\[X.X\]                — WHAT the system must do*   *UI/UX Specification v\[X.X\] — HOW it looks and how users interact*   *\[ProjectName\]\_RTW.xlsx    — BR Register, NFR Tracker, Traceability*   *OpenAPI/Swagger file       — machine-readable REST API contract* |
| :---- |

# **Document Change History**

| Version | Date | Changes | Author |
| ----- | ----- | ----- | ----- |
| v1.0.0 | \[DD/MM/YYYY\] | Initial TDS baseline | \[Author\] |

# **Part 1 — System Architecture**

| Guidance — Part 1 *Answers: What does the system look like structurally, and why was this architecture chosen?* *Complete this part before any coding begins.* *Decisions made here (architecture style, tech stack, deployment) are expensive to reverse.* |
| :---- |

## **1.1  Architecture Overview & Decision Rationale**

| Guidance — 1.1 *Describe the chosen architecture style and explain WHY it was chosen.* *Do not just name the style — justify it against NFR targets, team size, timeline, constraints.* *Reference the NFR IDs from the SRS that drove key architecture decisions.* |
| :---- |

**Architecture Style: \[Monolith / Microservices / Serverless / Event-Driven / Hybrid\]**

*\[2–4 paragraphs describing the high-level structure and rationale.\]*

| Decision | Choice | Rationale | NFR / Constraint Driving It |
| ----- | ----- | ----- | ----- |
| Architecture style | \[e.g. Layered Monolith\] | \[Reason\] | \[e.g. team size 4, 6-month timeline\] |
| Primary language | \[e.g. Java 21\] | \[Reason\] | \[e.g. team expertise\] |
| Database type | \[e.g. PostgreSQL\] | \[Reason\] | \[e.g. NFR-C01 — 5-yr retention, ACID\] |
| Auth approach | \[e.g. Session / JWT\] | \[Reason\] | \[e.g. NFR-SEC01, user type\] |
| \[Add decisions\] |  |  |  |

| ARCH — Architecture-specific additions *Monolith: Describe the layering (Presentation → Service → Repository) and how boundaries are enforced.* *Microservices: Add service decomposition rationale — DDD bounded contexts, team ownership, scaling.* *Serverless: Describe the function grouping strategy and the event/trigger model.* *Event-Driven: Describe the domain event model and whether event sourcing or CQRS is applied.* |
| :---- |

## **1.2  System / Component Diagram**

| Guidance — 1.2 *Runtime view — shows what components exist when the system is running and how they communicate.* *Different from Package Diagram (which shows source code structure).* *Minimum content: all runtime components, communication paths with protocol labels,* *deployment boundaries (same process / different server / cloud service).* |
| :---- |

| Diagram Placeholder — System Component Diagram *INSERT HERE: Component Diagram showing runtime components and communication.* *Tool: draw.io  |  File: \[ProjectName\]\_ComponentDiagram.drawio* *Export PNG and embed at full content width.* |
| :---- |

| Component | Type | Responsibility | Technology |
| ----- | ----- | ----- | ----- |
| \[e.g. Web Frontend\] | \[UI / Service / DB / Queue / Cache / External\] | \[What it does\] | \[Tech\] |
| \[e.g. Application Server\] |  |  |  |
| \[e.g. Primary Database\] |  |  |  |
| \[Add rows\] |  |  |  |

| ARCH — Architecture-specific additions *Microservices: Add API Gateway box. Show service-to-service communication (sync vs async).* *Serverless: Show function groups, event triggers, and cloud managed services (S3, SQS, etc.).* *Event-Driven: Show message broker as central component. Label topics/queues on arrows.* |
| :---- |

## **1.3  Package / Module Diagram**

| Guidance — 1.3 *Source code structure view — shows how code is organised into packages, modules, or layers.* *This is the diagram developers reference most frequently during implementation.* *Rules: dependencies flow in ONE direction (no circular); each package has a single responsibility;* *the diagram must match the actual folder/package structure in the repository.* |
| :---- |

| REQUIRED for Monolith *Without package boundaries, monolith code degrades into a 'big ball of mud' over time.* *This diagram is the primary architectural guard — define it before Sprint 1\.* |
| :---- |

| Diagram Placeholder — Package / Module Diagram *INSERT HERE: Package Diagram showing source code structure and allowed dependency directions.* *Tool: draw.io  |  File: \[ProjectName\]\_PackageDiagram.drawio* |
| :---- |

| Package / Module | Layer | Responsibility | Allowed Dependencies |
| ----- | ----- | ----- | ----- |
| \`\[com.project.controller\]\` | Presentation | Handle HTTP requests; delegate to service | Service layer only |
| \`\[com.project.service\]\` | Business Logic | Implement use cases; enforce BRs | Repository, Domain model |
| \`\[com.project.repository\]\` | Data Access | DB queries via ORM/JDBC | Domain model only |
| \`\[com.project.domain\]\` | Domain Model | Entity classes, value objects, enums | None (no dependencies) |
| \`\[com.project.config\]\` | Configuration | Spring config, security config, beans | Any layer (cross-cutting) |
| \`\[com.project.integration\]\` | Ext. Integration | Clients for external APIs | Called by Service layer only |
| \[Add packages\] |  |  |  |

| ARCH — Architecture-specific variations *Monolith: Use Java packages / .NET namespaces. Enforce boundaries via ArchUnit tests.* *Microservices: This diagram shows the internal structure of ONE service. Repeat per service.* *Serverless: Show function groups and shared layers (Lambda Layers / shared libraries).* *Event-Driven: Show consumer / producer packages separately. Include event schema package.* |
| :---- |

## **1.4  Technology Stack**

| REQUIRED *List every technology choice with version and justification.* *Vague entries like 'standard libraries' are not acceptable.* *Include: language, framework, database, cache, queue, test tools, build tools, deployment.* |
| :---- |

| Layer | Technology | Version | Justification |
| ----- | ----- | ----- | ----- |
| Language | \[e.g. Java\] | \[e.g. 21 LTS\] | \[e.g. Team expertise; LTS stability\] |
| Web Framework | \[e.g. Spring Boot\] | \[e.g. 3.2.x\] |  |
| ORM / Data Access | \[e.g. Spring Data JPA\] | \[e.g. 3.x\] |  |
| Primary Database | \[e.g. PostgreSQL\] | \[e.g. 16\] | \[e.g. ACID; NFR-C01 retention\] |
| Cache | \[e.g. Redis / None\] |  |  |
| Message Queue | \[e.g. RabbitMQ / None\] |  |  |
| Frontend | \[e.g. Thymeleaf / React\] |  |  |
| Authentication | \[e.g. Spring Security — Session / JWT\] |  |  |
| Build Tool | \[e.g. Maven / Gradle\] |  |  |
| Test Framework | \[e.g. JUnit 5 \+ Mockito\] |  |  |
| Migration Tool | \[e.g. Flyway / Liquibase\] |  |  |
| Containerisation | \[e.g. Docker\] |  |  |
| CI/CD | \[e.g. GitHub Actions\] |  |  |
| Monitoring | \[e.g. Prometheus \+ Grafana\] |  |  |
| Logging | \[e.g. SLF4J \+ Logback / ELK\] |  |  |

## **1.5  Deployment Architecture**

| Guidance — 1.5 *Describe where and how the system runs in each environment.* *Show: servers/containers, load balancer, CDN, external services, network boundaries.* *Include environment matrix (dev / staging / prod) with differences noted.* |
| :---- |

| Diagram Placeholder — Deployment Architecture *INSERT HERE: Deployment diagram showing infrastructure, containers, and environments.* *Tool: draw.io  |  File: \[ProjectName\]\_DeploymentDiagram.drawio* |
| :---- |

| Aspect | Development | Staging | Production |
| ----- | ----- | ----- | ----- |
| Infrastructure | \[Local Docker Compose\] | \[Cloud provider\] | \[Cloud provider\] |
| Database | \[Local / Shared dev DB\] | \[Dedicated instance\] | \[Managed service \+ replica\] |
| Scaling | Single instance | Single instance | \[Auto-scaling / manual\] |
| External APIs | \[Sandbox / mock\] | \[Sandbox\] | \[Live\] |
| Monitoring | Minimal | Full | Full \+ alerting |

# **Part 2 — Interface Specification**

| Guidance — Part 2 *Answers: How do users and external systems interact with this application?* *'Interface' is architecture-neutral — covers SSR pages, REST APIs, events, internal service contracts.* *IMPORTANT: Start with Section 2.1 Interface Inventory — classify every feature first.* *Screen layout and interaction design are NOT documented here.*   *→ See: UI/UX Specification v\[X.X\] for wireframes, screen states, and interaction flows.* |
| :---- |

## **2.1  Interface Inventory & Classification**

| Guidance — 2.1 *For every SRS feature (FT-xx), decide what type of interface it requires.* *Interface types:*   *SSR Page        — server renders full HTML (Thymeleaf, JSP, Razor)*   *REST API        — JSON over HTTP; AJAX, mobile clients, webhooks, external systems*   *GraphQL         — query-based API (if applicable)*   *Internal Service — Java/C\# interface called within same process (monolith only)*   *Event / Message — async via queue or event bus*   *Scheduled Job   — no user-facing interface; triggered by time*   *Webhook Receiver — inbound HTTP call from external system* |
| :---- |

| Feature (FT-xx) | Feature Name | Interface Type | Reason for Choice |
| ----- | ----- | ----- | ----- |
| FT-01 | \[Feature name\] | \[Interface type\] | \[Why SSR vs API vs event vs job\] |
| FT-02 |  |  |  |
| \[Add rows\] |  |  |  |

## **2.2  Authentication & Session / Token Design**

| Guidance — 2.2 *Implementation-level detail for authentication — not policy (policy is in SRS NFR-SEC).* *Choose: Session-based (monolith with SSR), JWT (stateless REST / SPA / mobile), OAuth2+OIDC.* *Show the full authentication sequence as a diagram or numbered steps.* |
| :---- |

| Diagram Placeholder — Authentication Flow *INSERT HERE: Sequence diagram showing the full login → token/session → request → validate flow.* *Tool: draw.io  |  File: \[ProjectName\]\_AuthFlow.drawio* |
| :---- |

### **Session-Based Auth (fill if applicable)**

| Aspect | Design Decision |
| ----- | ----- |
| Session store | \[In-memory (single node only) / Redis (multi-node)\] |
| Session timeout | \[e.g. 30 min inactivity, 8 hr absolute\] |
| Session ID location | \[HTTP-only cookie — name: JSESSIONID\] |
| Concurrent sessions | \[Allow all / limit to 1 / configurable per role\] |
| Session fixation prevention | \[Invalidate and reissue on login — Spring Security default\] |
| CSRF protection | \[Synchronizer token pattern — enabled by default in Spring Security\] |

### **JWT-Based Auth (fill if applicable)**

| Aspect | Design Decision |
| ----- | ----- |
| Token format | \[JWT — HS256 / RS256\] |
| Access token expiry | \[e.g. 15 minutes\] |
| Refresh token expiry | \[e.g. 7 days\] |
| Token storage (client) | \[HttpOnly cookie / localStorage — note security tradeoff\] |
| Token blacklist strategy | \[Redis key store for invalidated tokens\] |
| Claims included | \[\`sub\`, \`role\`, \`iat\`, \`exp\` — list all\] |

## **2.3  Server-Side Pages (SSR)**

| Guidance — 2.3 *Fill only for features delivered as server-rendered pages (SSR Page from 2.1).* *For each page: URL pattern, HTTP method, Controller\#method, template, model attributes, auth roles.* *URL pattern: use path parameters for IDs — /orders/{orderId} not /getOrder?id=xxx* *Skip this section entirely if the application has no SSR pages (pure SPA or API-only).* |
| :---- |

| URL Pattern | Method | Controller\#Method | Template | Model Attributes | Auth — Roles |
| ----- | ----- | ----- | ----- | ----- | ----- |
| /\[resource\] | GET | \[Controller\#method\] | \[template-name\] | \[attr: Type\] | \[Roles\] |
| /\[resource\]/{id} | GET |  |  |  |  |
| /\[resource\]/{id}/\[action\] | POST |  |  |  |  |
| \[Add rows\] |  |  |  |  |  |

## **2.4  REST API Endpoints**

| Guidance — 2.4 *Fill for features delivered as REST APIs (REST API or Webhook Receiver type from 2.1).* *For each endpoint: HTTP method \+ URL, request (headers/params/body), response (all status codes).* *For large APIs (10+ endpoints), maintain full spec in OpenAPI/Swagger YAML; reference here.* *REQUIRED: every error response must reference an error code from the Error Code Registry (Part 8.1).* |
| :---- |

### **API Conventions**

| Convention | Decision |
| ----- | ----- |
| Base URL | \`/api/v\[N\]/\` |
| Versioning strategy | \[URL path \`/v1/\` / Header \`API-Version: 1\`\] |
| Date/time format | \[ISO 8601 UTC — \`2024-01-15T08:30:00Z\`\] |
| Pagination | \[Cursor-based / Offset — \`?page=0\&size=20\`\] |
| Error response format | \`{"error\_code": "...", "message": "...", "field": "..."}\` |
| Correlation ID header | \`X-Correlation-ID\` — required on all requests |
| OpenAPI spec file | \[ProjectName\]\_OpenAPI.yaml |

### **\[HTTP METHOD\] /api/v1/\[resource\] — \[Description\]**

| Guidance — Endpoint block *Repeat this block for each REST endpoint. Include:*   *Purpose (1 sentence), Related SRS Feature (FT-xx), Authorization (roles / API key)*   *Request: headers, path params, query params, request body with field constraints*   *Responses: ALL possible HTTP status codes with response body structure* |
| :---- |

| Attribute | Value |
| ----- | ----- |
| Purpose | \[One sentence describing what this endpoint does\] |
| Related SRS Feature | \[FT-xx\] |
| Authorization | \[Role(s) / API key / Public\] |

| Request format Headers:   Authorization: Bearer {token}   \[if JWT\]   Content-Type: application/json   X-Correlation-ID: {uuid}        \[required\] Path Parameters:   {param}: \[type\] — \[description\] Query Parameters:   \[param\]: \[type\] — \[description\] — default: \[value\] Request Body:   {     "\[field\]": "\[type\]",    // \[constraint: required / max 255 chars / etc.\]     "\[field\]": "\[type\]"   } |
| :---- |

| HTTP Status | Condition | Response Body |
| ----- | ----- | ----- |
| 200 OK | \[Success condition\] | { "\[field\]": "\[type\]", ... } |
| 201 Created | \[Created condition\] | { "\[field\]": "\[type\]", ... } |
| 400 Bad Request | \[Validation failure\] | { "error\_code": "ERR\_xxx", "message": "...", "field": "..." } |
| 401 Unauthorized | Invalid / missing token | { "error\_code": "AUTH\_001", "message": "..." } |
| 403 Forbidden | Insufficient role | { "error\_code": "AUTH\_002", "message": "..." } |
| 404 Not Found | Resource not found | { "error\_code": "ERR\_xxx", "message": "..." } |
| 409 Conflict | BR violation (e.g. BR-04) | { "error\_code": "ERR\_xxx", "message": "..." } |
| 422 Unprocessable | DC violation (e.g. DC-02) | { "error\_code": "ERR\_xxx", "message": "..." } |
| 500 Server Error | Unexpected error | { "error\_code": "SYS\_001", "message": "Contact support" } |

*\[Repeat the endpoint block above for each REST API endpoint\]*

## **2.5  Internal Service Interfaces**

| Guidance — 2.5 *REQUIRED for Monolith. Optional for other architectures.* *In a monolith, modules communicate via direct method calls — these are the contracts between layers,* *equivalent to API contracts in microservices.* *Document key service interfaces so different developers can work on different layers independently.* |
| :---- |

| Internal Service Interface Template (Java) // \[ServiceName\] — one-sentence responsibility public interface \[ServiceName\] {     /\*\*      \* \[Method description — what it does, when to call it\]      \* @param \[param\]  \[description \+ constraints\]      \* @return \[return type \+ description\]      \* @throws \[ExceptionType\]  \[when thrown — reference BR-xx or DC-xx\]      \*/     \[ReturnType\] \[methodName\](\[ParamType\] \[param\]);     // Add all key public methods } |
| :---- |

| ARCH — Event-Driven / Microservices: replace 2.5 with Event/Message Schema Catalog *List all events/messages with: event name, producer, consumer(s), payload schema,* *ordering guarantee (none / per-key / global), retry policy.* |
| :---- |

# **Part 3 — Data Model**

| Guidance — Part 3 *Answers: How is data structured, stored, and managed at the physical level?* *This is the technical implementation of the data requirements in SRS Part 4\.* *Mapping: SRS 4.1 (Entities) → TDS 3.1 ERD; SRS 4.2 (Constraints) → TDS 3.2 DDL;*          *SRS 4.3 (Retention) → TDS 3.5 Migration; SRS 4.4 (State Transitions) → TDS 3.2 \+ 6.1* |
| :---- |

## **3.1  Physical Entity Relationship Diagram (ERD)**

| Guidance — 3.1 *Physical ERD — includes all tables, columns with data types, PKs, FKs, indexes.* *Different from the conceptual ERD in SRS Part 4.1 (which shows business entities only).* *Use crow's foot notation. Show: all tables including junction tables, column names and types,* *PK (bold/underlined), FK (arrow), index indicators, and cardinality (1:1, 1:N, M:N).* |
| :---- |

| Diagram Placeholder — Physical ERD *INSERT HERE: Physical ERD with all tables, columns, types, PKs, FKs, and indexes.* *Conceptual ERD for business stakeholders: SRS Part 4.1* *Physical ERD for dev/DBA: here (TDS Part 3.1)* *Tool: draw.io  |  File: \[ProjectName\]\_PhysicalERD.drawio* |
| :---- |

## **3.2  Database Schema (DDL)**

| Guidance — 3.2 *Provide full CREATE TABLE statements. Naming: snake\_case for tables and columns.* *Every table must have: primary key, created\_at timestamp, appropriate constraints.* *All CHECK constraints should reference the SRS DC-xx that requires them.* *This DDL is the source of truth — migration scripts should be generated from or validated against it.* |
| :---- |

| DDL Template — \[table\_name\] \-- \[TABLE NAME\] — \[one-line description\] \-- SRS Reference: \[Entity from SRS 4.1\], \[DC-xx constraints\] CREATE TYPE \[enum\_type\] AS ENUM ('val1', 'val2', 'val3');  \-- if PostgreSQL CREATE TABLE \[table\_name\] (     \[pk\_column\]      \[TYPE\]          PRIMARY KEY,     \[column\_name\]    \[TYPE\]          NOT NULL,     \[column\_name\]    \[TYPE\]          NOT NULL DEFAULT \[value\],     \[fk\_column\]      \[TYPE\]          NOT NULL                      REFERENCES \[ref\_table\](\[ref\_col\]) ON DELETE RESTRICT,     \[amount\_col\]     DECIMAL(12,2)   NOT NULL CHECK (\[col\] \>= 0),  \-- DC-xx     \[enum\_col\]       \[ENUM\_TYPE\]     NOT NULL DEFAULT 'value',     created\_at       TIMESTAMPTZ     NOT NULL DEFAULT NOW(),     updated\_at       TIMESTAMPTZ     NOT NULL DEFAULT NOW(),     UNIQUE (\[col1\], \[col2\])  \-- BR-xx: \[reason\] ); \-- Indexes — justify each one with the query pattern it supports CREATE INDEX idx\_\[table\]\_\[col\] ON \[table\_name\](\[col\]);  \-- \[query pattern\] \-- \[Repeat for each table\] |
| :---- |

## **3.3  ORM / ODM Entity Mapping**

| Guidance — 3.3 *Document how the DB schema maps to application-layer entity classes.* *Prevents common JPA/Hibernate pitfalls: wrong fetch type (causing N+1 queries),* *missing cascade settings (orphan records), incorrect column naming.* *Skip if the application uses raw JDBC/SQL without an ORM.* |
| :---- |

| ORM Entity Template (JPA / Spring) @Entity @Table(name \= "\[table\_name\]") public class \[EntityClass\] {     @Id     @Column(name \= "\[pk\_column\]", length \= \[len\])     private \[Type\] \[fieldName\];     @Column(name \= "\[column\]", nullable \= false, length \= \[len\])     private \[Type\] \[fieldName\];     @Enumerated(EnumType.STRING)     @Column(name \= "\[enum\_col\]", nullable \= false)     private \[EnumType\] \[fieldName\];     // One-to-Many: specify fetch type explicitly — never leave as default     @OneToMany(mappedBy \= "\[field\]", fetch \= FetchType.LAZY,                cascade \= CascadeType.ALL, orphanRemoval \= true)     private List\<\[ChildEntity\]\> \[children\] \= new ArrayList\<\>();     // Many-to-One     @ManyToOne(fetch \= FetchType.LAZY)     @JoinColumn(name \= "\[fk\_column\]", nullable \= false)     private \[ParentEntity\] \[parent\]; } |
| :---- |

| Entity | Fetch Strategy | Cascade | Reason |
| ----- | ----- | ----- | ----- |
| \[Entity\] → \[Child\] | LAZY | ALL | \[e.g. children loaded on-demand to avoid N+1\] |
| \[Entity\] → \[Parent\] | LAZY | NONE | \[e.g. parent has independent lifecycle\] |
| \[Add rows\] |  |  |  |

## **3.4  Indexing & Query Strategy**

| Guidance — 3.4 *For each index, document the query pattern it supports and the cardinality estimate.* *Flag any queries that risk N+1 and how they are addressed (join fetch, batch fetch, etc.).* |
| :---- |

| Index | Table | Column(s) | Query Pattern | Cardinality | Notes |
| ----- | ----- | ----- | ----- | ----- | ----- |
| idx\_\[name\] | \[table\] | \[col\] | \[e.g. filter by status in queue\] | High / Medium / Low |  |
| \[Add rows\] |  |  |  |  |  |

| Query | N+1 Risk | Mitigation |
| ----- | ----- | ----- |
| \[e.g. Order list with items\] | N+1 on OrderItems | \[e.g. @EntityGraph or JOIN FETCH\] |
| \[Add rows\] |  |  |

## **3.5  Data Migration Strategy**

| Aspect | Decision |
| ----- | ----- |
| Migration tool | \[Flyway / Liquibase / Manual\] |
| Migration file location | \[src/main/resources/db/migration/\] |
| Naming convention | \[V{version}\_\_{description}.sql — e.g. V1.0.0\_\_create\_orders\_table.sql\] |
| Rollback strategy | \[Flyway Pro undo scripts / Manual rollback scripts / Blue-green deploy\] |
| Run on startup | \[Yes — auto-migrate / No — manual trigger\] |
| Validation | \[Flyway validates checksum on every startup\] |

# **Part 4 — Integration & Communication Design**

| Guidance — Part 4 *Answers: How does this system communicate with external systems and internal components?* *Cover both synchronous (HTTP, direct call) and asynchronous (queue, events) communication.* *For each integration: endpoint, auth method, payload format, error handling, retry policy.* |
| :---- |

## **4.1  External System Integration Map**

| Guidance — 4.1 *One row per external system. At-a-glance reference — details in 4.2–4.4.* |
| :---- |

| External System | Direction | Protocol | Auth Method | Sync/Async | Error Handling | SRS Ref |
| ----- | ----- | ----- | ----- | ----- | ----- | ----- |
| \[e.g. Payment Gateway\] | Outbound \+ Inbound webhook | HTTPS/REST | \[API key / OAuth2\] | Both | \[Retry N times; alert\] | FT-xx |
| \[e.g. Courier API\] | Outbound \+ Inbound webhook | HTTPS/REST | \[API key\] | Both | \[Manual fallback\] | FT-xx |
| \[Add rows\] |  |  |  |  |  |  |

## **4.2  Webhook Handling Design**

| Aspect | Design Decision |
| ----- | ----- |
| Signature verification | \[e.g. HMAC-SHA256 of request body using shared secret from env var\] |
| Signature header | \[e.g. \`X-Signature-256: sha256={hash}\`\] |
| Idempotency key | \[e.g. event\_id in payload — deduplicated via Redis SETNX or DB unique constraint\] |
| Response strategy | \[Respond HTTP 200 immediately; process in background thread / queue\] |
| Processing timeout | \[e.g. background job must complete within 30 seconds\] |
| Retry on failure | \[e.g. 3 retries with exponential backoff: 1s, 5s, 25s\] |
| Dead letter handling | \[e.g. after 3 failures: log to dead\_letter\_events table; alert Admin\] |

## **4.3  Synchronous External API Integration**

| Guidance — 4.3 *Repeat this block per external API. Include sequence diagram.* |
| :---- |

### **\[External System Name\] — \[Integration Purpose\]**

| Aspect | Detail |
| ----- | ----- |
| Base URL (production) | \[https://api.system.com/v1\] |
| Base URL (sandbox) | \[https://sandbox.system.com/v1\] |
| Authentication | \[e.g. API key in header X-API-Key from env var EXTERNAL\_API\_KEY\] |
| Triggered by | \[FT-xx — e.g. barcode scan triggers courier pick-up request\] |
| Timeout | \[e.g. 10 seconds — maps to BV-04b in SRS\] |
| Retry policy | \[e.g. 2 retries, 2s delay, only on 5xx or network timeout\] |
| Circuit breaker | \[e.g. open after 5 failures in 60s; half-open after 30s\] |
| Fallback | \[e.g. show manual tracking number entry — AC-14\] |

| Diagram Placeholder — \[ExternalSystem\] Sequence Diagram *INSERT HERE: Sequence diagram showing interaction with this external system.* *Tool: draw.io  |  File: \[ProjectName\]\_\[ExternalSystem\]\_SequenceDiagram.drawio* |
| :---- |

## **4.4  Asynchronous Communication Design**

| Guidance — 4.4 *Fill if the application uses message queues, event buses, or async job processing.* *Skip if the application is fully synchronous.* *ARCH — Required for: Microservices, Event-Driven, and Monolith using queues for background processing.* |
| :---- |

| Aspect | Decision |
| ----- | ----- |
| Message broker | \[RabbitMQ / Kafka / AWS SQS / None\] |
| Message format | \[JSON / Avro / Protobuf\] |
| Delivery guarantee | \[At-most-once / At-least-once / Exactly-once\] |
| Ordering guarantee | \[None / Per-partition key\] |
| Consumer ACK | \[Auto-ack / Manual ack after processing\] |
| Dead letter queue | \[e.g. DLQ after 3 failed attempts; alert on DLQ depth \> 0\] |

| Event / Queue Name | Producer | Consumer(s) | Trigger | Payload Summary |
| ----- | ----- | ----- | ----- | ----- |
| \[e.g. order.confirmed\] | \[Component\] | \[Component\] | \[Payment confirmed\] | { order\_id, customer\_id, items\[\] } |
| \[Add rows\] |  |  |  |  |

## **4.5  Architecture-Specific Communication Patterns**

### **Monolith — Transaction Boundary Design**

| Guidance — Monolith Transaction Boundaries *REQUIRED for monolith. Document @Transactional scope for each key use case.* *Wrong transaction scope causes: data inconsistency (too narrow) or lock contention (too broad).* *Rule: sending notifications and firing events must be OUTSIDE the transaction — after commit.* |
| :---- |

| Use Case / Method | Transaction Scope | Operations INSIDE Tx | Operations OUTSIDE Tx (why) |
| ----- | ----- | ----- | ----- |
| \[ServiceMethod\] | @Transactional | \[stock decrement \+ status update \+ create notification record\] | \[send actual notification — failure must not rollback order\] |
| \[Add rows\] |  |  |  |

### **Microservices — Distributed Transaction / Saga Design**

| ARCH — Microservices only *Pattern choice: Choreography Saga (event-driven) or Orchestration Saga (central coordinator).* |
| :---- |

| Step | Service | Action | Compensating Action (on failure) |
| ----- | ----- | ----- | ----- |
| 1 | \[Service\] | \[Action\] | \[Rollback action\] |
| 2 | \[Service\] | \[Action\] | \[Rollback action\] |
| \[Add rows\] |  |  |  |

# **Part 5 — Security Design**

| Guidance — Part 5 *Translates security NFRs (NFR-SEC01–SEC05) into concrete implementation decisions.* *Each section should answer: how exactly is this implemented in code?* |
| :---- |

## **5.1  Authentication Flow**

*→ Documented in Part 2.2. See: Authentication & Session / Token Design.*

## **5.2  Authorization & RBAC Implementation**

| Aspect | Decision |
| ----- | ----- |
| RBAC implementation | \[e.g. Spring Security @PreAuthorize / Custom @Secured / Filter chain\] |
| Role storage | \[e.g. User table \`role\` column / JWT claim / Separate role-permission table\] |
| Role hierarchy | \[e.g. ADMIN \> MANAGER \> WAREHOUSE \> CUSTOMER\] |
| Permission check layer | \[e.g. Controller for URL; Service for data-level (own records)\] |
| Failed auth response | \[HTTP 403 with AUTH\_002 — no role information disclosed\] |

## **5.3  Data Protection**

| Data Category | At Rest | In Transit | Notes |
| ----- | ----- | ----- | ----- |
| All traffic | N/A | TLS 1.2+ (NFR-SEC01) | HTTP → HTTPS redirect |
| Passwords | bcrypt, cost \>= 12 (NFR-SEC02) | TLS | Never logged |
| PII (name, email, phone) | \[Encrypted at DB / App / Not encrypted — note risk\] | TLS | Deletion per NFR-C02 |
| Payment data | Not stored (NFR-SEC03) | TLS | Delegated to payment gateway |
| Session tokens | HttpOnly cookie | TLS | SameSite=Strict |
| API keys (external) | Environment variables only | TLS | Never in source code or logs |

## **5.4  Input Validation Strategy**

| Validation Type | Where Applied | Library / Mechanism | Example |
| ----- | ----- | ----- | ----- |
| Bean validation | Controller (request DTO) | \[JSR-380 @Valid, @NotNull, @Size\] | @NotBlank String name |
| Business rule validation | Service layer | Custom validators | quantity \>= 1 && \<= 10 (BR-01) |
| SQL injection prevention | Repository layer | \[JPA parameterised / PreparedStatement\] | Never string concatenation |
| XSS prevention | Presentation layer | \[Thymeleaf auto-escaping / CSP header\] |  |
| File upload (if applicable) | Controller | \[File type whitelist, size limit\] |  |

## **5.5  Security Headers & CORS Policy**

| Header | Value | Reason |
| ----- | ----- | ----- |
| \`Strict-Transport-Security\` | \`max-age=31536000; includeSubDomains\` | Force HTTPS |
| \`X-Content-Type-Options\` | \`nosniff\` | Prevent MIME sniffing |
| \`X-Frame-Options\` | \`DENY\` | Prevent clickjacking |
| \`Content-Security-Policy\` | \[Define per environment\] | XSS mitigation |
| \`Referrer-Policy\` | \`strict-origin-when-cross-origin\` |  |

| Environment | Allowed Origins | Allowed Methods | Allow Credentials |
| ----- | ----- | ----- | ----- |
| Development | \* or http://localhost:\[port\] | GET, POST, PUT, DELETE | true |
| Staging | https://staging.\[domain\] | GET, POST, PUT, DELETE | true |
| Production | https://\[domain\] | GET, POST, PUT, DELETE | true |

# **Part 6 — Key Algorithms & Business Logic**

| Guidance — Part 6 *Translates SRS state transition table, business rules, and ACs into implementation-level design.* *Dev should be able to implement from this section without re-reading the SRS.* *Write pseudocode, patterns, and decision tables — not production code.* |
| :---- |

## **6.1  State Machine Implementation**

| Guidance — 6.1 *Translate SRS Part 4.4 State Transition Table into a code-level design.* *Pattern choices: plain conditionals (simple), State Pattern (moderate), Spring Statemachine (complex).* *Document: pattern chosen, guard condition implementation, invalid transition rejection.* |
| :---- |

| Aspect | Decision |
| ----- | ----- |
| Pattern | \[Plain conditionals / State Pattern / Spring Statemachine / Other\] |
| Reason | \[Why this pattern fits the complexity level\] |
| Invalid transition response | \[HTTP 400 \+ error\_code from Error Code Registry \+ log attempt\] |

| State Machine Pseudocode function transitionStatus(entity, event):   allowed \= TRANSITION\_MAP\[entity.currentStatus\]   if event not in allowed:     throw InvalidStateTransitionException(from, event)  // → HTTP 400   guard \= allowed\[event\].guard   if not guard.evaluate(entity, context):     throw GuardConditionNotMetException(guard.reason)   entity.status \= allowed\[event\].toStatus   for action in allowed\[event\].actions:     action.execute(entity)  // stock decrement, notify, etc. TRANSITION\_MAP \= {   'StateA': {     'EVENT\_1': { to: 'StateB', guard: \[condition\], actions: \[action1, action2\] },     'EVENT\_2': { to: 'Cancelled', guard: always, actions: \[rollback\] }   },   // \[Add all states from SRS Part 4.4\] } |
| :---- |

## **6.2  Concurrency & Consistency Handling**

| Operation | Race Condition Risk | Strategy | Implementation |
| ----- | ----- | ----- | ----- |
| \[e.g. Stock decrement\] | Two orders → negative qty (DC-02) | \[Optimistic locking / SELECT FOR UPDATE\] | \[e.g. @Version field; retry on OptimisticLockException\] |
| \[e.g. Processing lock\] | Two users click Start simultaneously | \[DB-level lock / Redis distributed lock\] | \[e.g. UPDATE SET locked\_by=? WHERE locked\_by IS NULL\] |
| \[Add rows\] |  |  |  |

## **6.3  Transaction Boundary Design**

| REQUIRED for Monolith — see also Part 4.5 *General rule: one @Transactional per use case service method.* *Operations that must NOT be inside the transaction (run after commit):*   *\- Sending external notifications (failure must not rollback the business operation)*   *\- Calling external APIs (network failures must not rollback DB changes)*   *\- Publishing events / messages (event should only fire after state is durable in DB)* |
| :---- |

| Transaction Boundary Example @Transactional  // ← transaction boundary opens function confirmOrder(orderId, payment):   validateWebhookSignature(payment)        // inside   order \= loadOrder(orderId)              // inside   validateTransition(order, CONFIRMED)    // inside   decrementStock(order.items)             // inside — MUST rollback with order   order.status \= CONFIRMED                // inside   notifRecord \= createNotification(...)   // inside — DB record created // COMMIT happens here ↑ // After commit — outside transaction: sendActualNotification(notifRecord)       // outside — failure ≠ order rollback publishEvent(OrderConfirmedEvent)         // outside — fires after state is durable |
| :---- |

## **6.4  Scheduled Jobs & Background Tasks**

| Job Name | Schedule (cron) | Description | Idempotency | Failure Handling |
| ----- | ----- | ----- | ----- | ----- |
| \[JobName\] | \[0 0 \*/6 \* \* \*\] | \[What it does\] | \[e.g. processes PENDING records; safe to re-run\] | \[Log \+ alert; retry next cycle\] |
| \[Add rows\] |  |  |  |  |

## **6.5  Notification Dispatch Design**

| Guidance — 6.5 *Document how notifications are routed to the correct channel.* *Include: channel routing logic, template management, retry policy, suppression rules (e.g. BR-05).* |
| :---- |

| Notification Dispatch Pseudocode function dispatchNotification(notificationRecord):   channel \= resolveChannel(notificationRecord.order.sourceChannel)   // facebook → Facebook Messenger API   // zalo     → Zalo OA API   // shopee   → Shopee messaging API   // website  → Email   template \= loadTemplate(notificationRecord.type, channel)   message  \= template.render(notificationRecord.data)   result   \= channel.send(notificationRecord.customerId, message)   if result.success:     notificationRecord.status \= SENT     notificationRecord.sentAt  \= now()   else:     scheduleRetry(notificationRecord, attempt \+ 1\)     // Max 3 retries; after that: status \= FAILED, alert Admin |
| :---- |

# **Part 7 — Performance & Scalability Design**

| Guidance — Part 7 *Translates NFR-Pxx and NFR-Sxx from the SRS into concrete technical strategies.* *Every design decision here should reference the NFR ID it satisfies.* |
| :---- |

## **7.1  Caching Strategy**

| What is Cached | Cache Key Pattern | TTL | Invalidation Trigger | NFR Satisfied |
| ----- | ----- | ----- | ----- | ----- |
| \[e.g. Product/SKU details\] | product:{sku\_id} | \[e.g. 5 min\] | \[On product update\] | NFR-Pxx |
| \[e.g. User roles\] | user:roles:{user\_id} | \[e.g. 15 min\] | \[On role change\] | NFR-SECxx |
| \[Add rows\] |  |  |  |  |

*Cache implementation: \[Redis / Caffeine (in-process) / None\]*

## **7.2  Database Query Optimization**

| Query Scenario | Volume Estimate | Optimization Applied | Index Used |
| ----- | ----- | ----- | ----- |
| \[e.g. Order inbox list\] | \[e.g. 100–200 orders/page\] | \[Pagination \+ index on status+created\_at\] | idx\_orders\_status |
| \[Add rows\] |  |  |  |

*Pagination strategy: \[Cursor-based (large datasets) / Offset (small datasets)\] — reason: \[justify\]*

## **7.3  Scaling Approach**

| NFR Target | Current Approach | Scaling Mechanism | Constraint |
| ----- | ----- | ----- | ----- |
| NFR-S01: \>= 1,000 concurrent users | \[Single / Multiple instances\] | \[Horizontal scaling — stateless \+ shared session store\] | \[DB connection pool limit\] |
| NFR-S02: No code changes for scaling | \[Stateless design\] | \[Load balancer \+ multiple app instances\] | \[Shared session store required\] |
| \[Add rows\] |  |  |  |

## **7.4  Load Testing Plan**

| NFR | Test Type | Tool | VUs | Ramp-up | Duration | Pass Threshold |
| ----- | ----- | ----- | ----- | ----- | ----- | ----- |
| NFR-P01 | Load test | k6 | 500 | 60s | 5 min | p95 \< 2s, error \< 1% |
| NFR-S01 | Stress test | k6 | 1000 | 120s | 10 min | No degradation, error \< 1% |
| \[Add rows\] |  |  |  |  |  |  |

# **Part 8 — Error Handling & Observability**

| Guidance — Part 8 *Defines the operational contract — how failures are detected, communicated, and investigated.* *The Error Code Registry (8.1) must be defined early and shared with frontend teams.* *Format: \[DOMAIN\]\_\[NUMBER\] — e.g. AUTH\_001, ORDER\_001, STOCK\_001, SYS\_001* |
| :---- |

## **8.1  Error Code Registry**

| REQUIRED *Define all error codes before implementation begins.* *Every REST API error response in Part 2.4 must reference a code from this registry.* *Format: \[DOMAIN\]\_\[NUMBER\] — e.g. AUTH\_001, ORDER\_001, STOCK\_001, SYS\_001* |
| :---- |

| Error Code | HTTP Status | User-Facing Message | Internal Trigger | SRS Reference |
| ----- | ----- | ----- | ----- | ----- |
| AUTH\_001 | 401 | Session expired. Please log in again. | Invalid/missing token | NFR-SEC01 |
| AUTH\_002 | 403 | You don't have permission to perform this action. | RBAC check failed | NFR-SEC05 |
| SYS\_001 | 500 | An unexpected error occurred. Please contact support. | Unhandled exception | — |
| \[DOMAIN\]\_001 | \[4xx/5xx\] | \[Plain language — no technical detail\] | \[Code trigger\] | \[BR-xx / DC-xx\] |
| \[Add rows\] |  |  |  |  |

## **8.2  Logging Standard**

| Field | Type | Description | Example |
| ----- | ----- | ----- | ----- |
| \`timestamp\` | ISO 8601 UTC | When the event occurred | \`2024-03-18T08:30:00.123Z\` |
| \`level\` | ENUM | ERROR / WARN / INFO / DEBUG | \`INFO\` |
| \`service\` | String | Application / service name | \`shopeasy-app\` |
| \`correlation\_id\` | UUID | End-to-end request trace ID (NFR-M02) | \`a1b2c3d4-...\` |
| \`user\_id\` | String | Authenticated user (if applicable) | \`usr-001\` |
| \`message\` | String | Human-readable event description | \`Order confirmed\` |
| \[context fields\] |  | Feature-specific context | \`order\_id: ORD-...\` |

| Level | When to Use | Examples |
| ----- | ----- | ----- |
| ERROR | Unexpected failures requiring investigation | Unhandled exception, DB connection loss |
| WARN | Expected but noteworthy failures | Webhook signature mismatch, retry attempt |
| INFO | Significant business events | Order confirmed, user login, scheduled job done |
| DEBUG | Detailed trace for development (disabled in prod) | SQL queries, method entry/exit |

| Never log *Passwords, payment card data, full PII without masking, auth tokens.* *Any secret or credential — even temporarily. Rotate immediately if leaked.* |
| :---- |

## **8.3  Monitoring & Alerting**

| Metric | Source | Alert Threshold | Alert Channel | NFR Reference |
| ----- | ----- | ----- | ----- | ----- |
| HTTP p95 response time | APM / Load balancer | \> 2s sustained 5 min | \[Slack / PagerDuty\] | NFR-P01 |
| HTTP error rate (5xx) | APM | \> 1% sustained 5 min | \[Alert channel\] | NFR-A01 |
| DB connection pool usage | DB metrics | \> 80% | \[Alert channel\] | NFR-S01 |
| Scheduled job failure | Application log | Any failure | \[Alert channel\] | NFR-A04 |
| Dead letter queue depth | Queue metrics | \> 0 | \[Alert channel\] | NFR-A04 |
| \[Add rows\] |  |  |  |  |

## **8.4  Distributed Tracing**

| Aspect | Decision |
| ----- | ----- |
| Tracing tool | \[Zipkin / Jaeger / OpenTelemetry / Datadog APM\] |
| Trace ID propagation | \[X-Correlation-ID header; set at entry point if absent\] |
| Span creation | \[Automatic for HTTP \+ DB; manual for external API calls\] |
| Sampling rate | \[100% in dev/staging; 10% in prod\] |
| Log correlation | \[Correlation ID in every log entry — ties logs to traces\] |

# **Part 9 — Development Guidelines**

| Guidance — Part 9 *Ensures consistency across the development team.* *More critical for larger teams or projects with junior developers.* *For very small teams (1–2 senior devs): simplify to structure, naming, and testing essentials.* |
| :---- |

## **9.1  Project & Package Structure**

| Guidance — 9.1 *Document the actual folder/package structure that corresponds to the Package Diagram (Part 1.3).* *Every developer should be able to determine where a new class belongs without asking.* |
| :---- |

| Project Structure Template \[project-root\]/ ├── src/ │   ├── main/ │   │   ├── java/ │   │   │   └── \[com.company.project\]/ │   │   │       ├── \[module1\]/ │   │   │       │   ├── controller/    \# HTTP handlers │   │   │       │   ├── service/       \# Business logic interfaces \+ impl │   │   │       │   ├── repository/    \# Data access interfaces │   │   │       │   ├── domain/        \# Entities, value objects, enums │   │   │       │   └── dto/           \# Request/Response transfer objects │   │   │       ├── \[module2\]/ │   │   │       ├── integration/       \# External API clients │   │   │       ├── config/            \# Spring config, security config │   │   │       └── exception/         \# Custom exception classes │   │   └── resources/ │   │       ├── db/migration/          \# Flyway migration scripts │   │       ├── templates/             \# Thymeleaf templates (if SSR) │   │       ├── application.yml │   │       ├── application-dev.yml │   │       └── application-prod.yml │   └── test/ │       └── java/  \[mirror main structure\] ├── docker-compose.yml └── pom.xml / build.gradle |
| :---- |

## **9.2  Coding Standards & Conventions**

| Convention | Rule | Example |
| ----- | ----- | ----- |
| Class naming | PascalCase, descriptive noun | OrderConfirmationService |
| Method naming | camelCase, verb phrase | confirmOrder(), findByStatus() |
| Constants | SCREAMING\_SNAKE\_CASE | MAX\_PENDING\_ORDERS \= 3 |
| DB column naming | snake\_case | order\_id, created\_at |
| REST URL naming | Plural nouns, lowercase, hyphens | /api/v1/orders/{orderId} |
| Error messages | Plain language, no stack traces to users | 'Invalid quantity' not NPE message |
| TODO/FIXME | Must include ticket reference | // TODO: ORD-42 — handle lock expiry |
| Magic numbers | Named constants only | MAX\_RETRY\_ATTEMPTS \= 3 not \`\> 3\` |

### **Code Review Checklist (minimum required before merge)**

* No business logic in controllers (belongs in service layer)

* No raw SQL string concatenation (use parameterised queries)

* All new endpoints have unit tests \+ at least 1 integration test

* Error responses use Error Code Registry codes (Part 8.1)

* No credentials / secrets in source code

* Transaction boundaries match Part 4.5 / Part 6.3 design

* New BR implementation references the BR-xx ID in a comment

## **9.3  Testing Strategy**

| Test Type | Scope | Tool | Coverage Target | Where Runs |
| ----- | ----- | ----- | ----- | ----- |
| Unit test | Service layer methods | \[JUnit 5 \+ Mockito\] | \>= 80% line coverage on service layer | Local \+ CI |
| Integration test | Controller → DB | \[Spring Boot Test \+ MockMvc\] | All AC-xx covered | CI |
| State transition test | All valid \+ invalid transitions (SRS 4.4) | \[JUnit 5\] | 100% rows covered | CI |
| API contract test | REST endpoints match OpenAPI spec | \[Spring Cloud Contract / Pact\] | All endpoints | CI |
| Load test | NFR-Pxx targets | \[k6\] | NFR-P01, P02, P03 | Pre-release |
| Security test | OWASP Top 10, NFR-SEC | \[OWASP ZAP / manual\] | NFR-SEC01–05 | Pre-release |

### **Test Naming Convention**

| Test naming: \[MethodName\]\_\[Scenario\]\_\[ExpectedOutcome\] // Positive test confirmOrder\_whenPaymentConfirmed\_shouldDecrementStockAndSendNotification() // Negative tests (NAC) confirmOrder\_whenOrderAlreadyCancelled\_shouldThrowStaleOrderException()   // NAC-02a createOrder\_whenFourthPendingOrder\_shouldReturnConflict409()              // NAC-02b, BR-04 // State transition test transition\_fromShippedToProcessing\_shouldRejectWithHttp400()              // Invalid transition |
| :---- |

## **9.4  CI/CD Pipeline Design**

| Pipeline Stages \[Push to branch\]       ↓ 1\. Build & Compile         — fail fast on compile error       ↓ 2\. Unit Tests              — fail on test failure or coverage \< threshold       ↓ 3\. Code Quality Gates      — SonarQube / Checkstyle (fail on critical issues)       ↓ 4\. Integration Tests       — fail on test failure       ↓ 5\. Build Docker Image      — tag with commit SHA       ↓ 6\. Deploy to Staging       — blue-green or rolling (NFR-M03)       ↓ 7\. Smoke Tests             — basic health check on staging       ↓ \[Manual approval gate for production\]       ↓ 8\. Deploy to Production    — blue-green or rolling       ↓ 9\. Post-deploy Smoke Test |
| :---- |

| Aspect | Decision |
| ----- | ----- |
| CI tool | \[GitHub Actions / GitLab CI / Jenkins\] |
| Branch strategy | \[Git Flow / Trunk-based\] |
| PR requirement | \[\>= 1 reviewer approval \+ all checks pass\] |
| Deployment strategy | \[Blue-green / Rolling update — NFR-M03: zero-downtime\] |
| Rollback trigger | \[Smoke test failure / Error rate spike \> 5% in 5 min post-deploy\] |
| Rollback mechanism | \[Redeploy previous Docker image tag\] |

# **References & Diagram Index**

## **Referenced Documents**

| Document | Version | Location | Purpose |
| ----- | ----- | ----- | ----- |
| Software Requirements Specification (SRS) | v\[X.X\] | \[Link / Path\] | Requirements baseline — WHAT the system must do |
| UI/UX Specification | v\[X.X\] | \[Link / Path\] | Screen designs and interaction flows |
| \[ProjectName\]\_RTW.xlsx | v\[X.X\] | \[Link / Path\] | Traceability workbook (BR Register, NFR Tracker, etc.) |
| OpenAPI Specification | v\[X.X\] | \[filename\].yaml | Machine-readable REST API contract |

## **Diagram Index**

| Guidance — Diagram Index *List every diagram referenced in this TDS. For each: type, section referenced, and draw.io filename.* *When the diagram is created: export as PNG and embed at the placeholder location in the relevant section.* *Store source draw.io files alongside this document for future editing.* |
| :---- |

| Diagram | Type | Referenced in | Source File |
| ----- | ----- | ----- | ----- |
| Component Diagram | Runtime / Component | Part 1.2 | \[ProjectName\]\_ComponentDiagram.drawio |
| Package / Module Diagram | Source Code Structure | Part 1.3 | \[ProjectName\]\_PackageDiagram.drawio |
| Deployment Architecture | Infrastructure | Part 1.5 | \[ProjectName\]\_DeploymentDiagram.drawio |
| Authentication Flow | Sequence | Part 2.2 | \[ProjectName\]\_AuthFlow.drawio |
| Physical ERD | Entity Relationship | Part 3.1 | \[ProjectName\]\_PhysicalERD.drawio |
| \[External System\] Integration | Sequence | Part 4.3 | \[ProjectName\]\_\[System\]\_SequenceDiagram.drawio |
| \[Entity\] State Machine | State Machine | Part 6.1 | \[ProjectName\]\_\[Entity\]StateMachine.drawio |
| \[Add rows\] |  |  |  |

*— End of TDS Template —*
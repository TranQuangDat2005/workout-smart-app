  
**Software Requirements Specification**

***\[Project Name\] (\[CODE\]) \- Report 3***

| Project Name | \[Project Name\] |
| :---- | :---- |
| **Vision & Scope Ref** | Vision & Scope Document v\[X.X\] |
| **RTW Ref** | \[ProjectName\]\_RTW.xlsx (Requirements Traceability Workbook) |
| **SRS Version** | v1.0.0 |
| **Date Created** | \[DD/MM/YYYY\] |
| **Last Updated** | \[DD/MM/YYYY\] |
| **Author(s)** | \[Name / Role\] |
| **Reviewer(s)** | \[Name / Role\] |
| **Status** | Draft / In Review / Approved |

# **How to Use This Template**

| Template conventions *This template defines the structure and writing rules for an SRS that uses three* *complementary specification methods:*   *• Scenario / Narrative  — Part 2: real-world business flows*   *• Feature Description   — Part 3: precise system behaviour*   *• Non-Functional Requirements — Part 6: quality constraints*   *• Data Requirements — Part 4: entities, constraints, state transitions* *Colour coding in this template:*   *BLUE box   \= Guidance — explains what to write in this section*   *GREEN box  \= Sample placeholder — shows format of example content*   *PURPLE box \= Diagram placeholder — where to insert a diagram*   *GRAY box   \= Excel reference — content maintained in RTW.xlsx*   *RED box    \= Negative AC (Acceptance Criteria) guidance*   *AMBER box  \= Boundary Value guidance* *Fields shown as \[Italic gray text\] are fill-in placeholders — replace with real content.* *Delete all blue Guidance boxes before distributing the completed SRS.* *Companion files to create alongside this SRS:*   *• Vision & Scope Document (use the V\&S Template)*   *• \[ProjectName\]\_RTW.xlsx (use the RTW Template)* |
| :---- |

# **Document Change History**

| Guidance *Record every version change here. After the first approved baseline, add a row for* *every subsequent change. Include the section affected, not just a generic description.* |
| :---- |

| Version | Date | Changes | Author |
| ----- | ----- | ----- | ----- |
| v1.0.0 | \[DD/MM/YYYY\] | Initial SRS baseline | \[Author\] |

# **Part 0 — Document Overview**

| Guidance — Part 0 *Describe how this SRS fits into the broader documentation ecosystem.* *List companion documents and what NOT to duplicate from each.* *This section is read first — it sets expectations for scope and cross-references.* |
| :---- |

*\[Describe how this SRS complements the Vision & Scope Document and the RTW workbook.\]*

| What is NOT in this SRS — see companion documents *\[List the sections/content that live in V\&S and RTW.xlsx, not here.\]* *Example: V\&S Sections 1–5 (background, gap analysis, proposed solution, exclusions)* *Example: RTW.xlsx Sheet 2–9 (UC List, Traceability, Permission Matrix, Data Dictionary,*   *BR Register, NFR Tracker, Open Issues, Change Log)* |
| :---- |

# **Part 1 — System Overview**

## **1.1  System Objective & Business Context**

| Guidance — 1.1 *Do NOT repeat the full product background from V\&S here.* *Copy only the one-sentence system objective from V\&S Section 3\.* *Add a reference to V\&S for full context.* *Rule: if this section grows beyond 3 lines, you are duplicating V\&S content — trim it.* |
| :---- |

*\[Reference to V\&S Document vX.X — Sections 1, 2, 3\]*

*\[One-sentence system objective copied from V\&S Section 3\]*

## **1.2  Technical Scope**

| Guidance — 1.2 *Business scope (what to build / not build) is already in V\&S Section 4\.* *This section covers TECHNICAL scope only:*   *• Modules the system contains*   *• External systems and APIs integrated with*   *• Tech stack (framework, DB, hosting) — at a high level*   *• Data boundary: whose data does the system own?*   *• What is technically excluded (and why — with V\&S reference)* *Follow with the Context Diagram placeholder to visualise the boundary.* |
| :---- |

| Dimension | In Technical Scope | Notes |
| ----- | ----- | ----- |
| Modules | \[List main modules\] |  |
| Integrations | \[External APIs and systems\] | \[Direction: read-only / read-write\] |
| Tech Stack | \[Framework / DB / hosting\] |  |
| Data Boundary | \[Entities owned by this system\] | \[What is NOT owned\] |
| Excluded (Tech) | \[What is delegated to third parties\] | \[Reference V\&S LI-xx\] |

| Diagram Placeholder — System Context Diagram *INSERT HERE: Context Diagram showing the system and all external entities.* *Show data flows in (solid arrows) and out (dashed arrows) for each entity.* *Recommended tool: draw.io  |  Export PNG and embed at full content width.* |
| :---- |

## **1.3  System Roles**

| Guidance — 1.3 *List roles for requirement authoring and access-control specification only.* *Full stakeholder analysis (interests, concerns): V\&S Section 1.3.* *Full Permission Matrix (Role × Feature × CRUD action): RTW.xlsx Sheet 4\.* *Full Use Case List: RTW.xlsx Sheet 2\.* *This section has three parts:*   *(a) Roles table — role name, access level, primary feature areas*   *(b) Excel reference box — Permission Matrix in RTW.xlsx*   *(c) Excel reference box \+ diagram placeholder — UC List \+ Use Case Diagram* |
| :---- |

| Role | Access Level | Primary Feature Areas |
| ----- | ----- | ----- |
| \[Role 1\] | \[Public / Internal / API only\] | \[Feature areas\] |
| \[Role 2\] |  |  |
| \[Add rows as needed\] |  |  |

| Permission Matrix → \[ProjectName\]\_RTW.xlsx Sheet 4 *Detailed Role x Feature x Action permission matrix (F/E/R/O/— per action).* *Update RTW.xlsx Sheet 4 when roles or feature access rules change.* |
| :---- |

| Use Case List → \[ProjectName\]\_RTW.xlsx Sheet 2 *Full Use Case List with: UC ID, Name, Primary Actor, Secondary Actor(s),* *Pre/Postcondition, Related FT-xx, Related SC-xx, Priority, Status.* *To add/update/deprecate a UC: edit RTW.xlsx Sheet 2 directly.* *Then update the Use Case Diagram below.* |
| :---- |

| Diagram Placeholder — Use Case Diagram (System Overview) *INSERT HERE: One high-level Use Case Diagram — all actors \+ grouped use cases.* *This is a SUMMARY diagram. Do NOT draw one diagram per individual use case.* *Actors: internal roles on left, external systems on right.* *Group use cases by module inside the system boundary rectangle.* *Use UML notation: stick figures, ovals, lines, \<\<include\>\> / \<\<extend\>\> where applicable.* *For individual UC detail: RTW.xlsx Sheet 2 \+ Scenarios in Part 2\.* *Tool: draw.io  |  Export PNG and embed at full content width.* |
| :---- |

## **1.4  Glossary**

| Guidance — 1.4 *Define SRS-specific technical and domain terms that appear in Scenarios,* *Feature Descriptions, and Acceptance Criteria.* *Do NOT repeat general business terms already defined in V\&S.* *Always include: AC-xx, NAC-xx, BV-xx conventions.* |
| :---- |

| Term | Definition | Used in |
| ----- | ----- | ----- |
| \[Key domain term\] | \[Plain-language definition\] | \[FT-xx, SC-xx\] |
| AC-xx | Acceptance Criterion — a positive, testable condition for feature completion | All FT-xx |
| NAC-xx | Negative Acceptance Criterion — failure/rejection/boundary-breach test condition | All FT-xx |
| BV-xx | Boundary Value note — defines valid/invalid ranges for a specific constraint | All FT-xx |
| BR-xx | Business Rule ID — full definition in RTW.xlsx Sheet 6 | All FT-xx |

## **1.5  Feature Traceability Bridge (V\&S → SRS)**

| Guidance — 1.5 *Full traceability (FE-xx → FT-xx → UC → Scenario → Data → NFR → BR → Test → Sprint)* *is maintained in RTW.xlsx Sheet 3\.* *This section contains only a quick-reference summary table (FE → FT mapping).* *Every V\&S Feature (FE-xx) must have a corresponding SRS Feature (FT-xx).* *Any FE-xx without an FT-xx is unspecified — flag as Pending.* |
| :---- |

| Full traceability matrix → \[ProjectName\]\_RTW.xlsx Sheet 3 *Maintained in: RTW.xlsx Sheet 3: Feature Traceability Matrix* |
| :---- |

| V\&S Feature (FE-xx) | Feature Name in V\&S | Maps to SRS (FT-xx) | Status |
| ----- | ----- | ----- | ----- |
| FE-01 | \[Feature name\] | FT-01 | Specified / Pending |
| FE-02 | \[Feature name\] | \[FT-xx\] | Pending |
| \[Add rows\] |  |  |  |

# **Part 2 — Scenario / Narrative**

| Guidance — Part 2 *Scenarios describe real-world business flows in natural language.* *Reviewed and approved by stakeholders BEFORE Feature Descriptions are written.* *Rules for writing good scenarios:*   *• Use a specific persona name (not just a role label) — makes flows tangible*   *• Include context: device, time of day, prior state*   *• Keep to 1–3 paragraphs per scenario*   *• Always include 1–2 most important exception / alternate flows*   *• Do NOT describe UI elements or implementation details*   *• Do NOT list features — describe what a real person does and experiences* *Diagram: add Activity or Sequence Diagram after narratives with complex branching* *or cross-system interactions (see placeholder below each scenario).* *Close Part 2 with a Scenario List table summarising all scenarios.* |
| :---- |

## **SC-01 — \[Scenario Name\]**

| Guidance — Scenario header table *Fill in: Business Flow (verb phrase), Primary Actor (the role driving the flow),* *Pre-condition (system/data state required before flow starts),* *Post-condition (guaranteed state after successful completion),* *Related Feature (V\&S FE-xx → SRS FT-xx), Related UC (RTW.xlsx Sheet 2).* |
| :---- |

| Business Flow | \[Verb phrase describing the end-to-end action\] |
| :---- | :---- |
| **Primary Actor** | \[Role name\] |
| **Pre-condition** | \[What must be true before this flow can start\] |
| **Post-condition** | \[Guaranteed system state after successful completion\] |
| **Related Feature** | \[FE-xx (V\&S)\] → \[FT-xx (SRS)\] |
| **Related UC** | \[UC-xx (RTW.xlsx Sheet 2)\] |

**Narrative:**

*\[Write 1–3 paragraphs. Name your persona. Include context. Describe what happens, not how it is implemented. Use past/present tense consistently.\]*

**Key Exception / Alternate Flows:**

| Guidance — exception flows *List 1–2 most important failure or alternate paths.* *Format: \[Trigger\]: \[What the system does\] — \[Outcome for the user\].* *Do not attempt to cover every edge case here — that belongs in NAC-xx in Part 3\.* |
| :---- |

| Diagram Placeholder — \[SC-xx Diagram Type\] *INSERT HERE: \[Activity Diagram / Sequence Diagram\] for this scenario.* *Activity Diagram: for flows with multiple decision branches.* *Sequence Diagram: for flows with significant cross-system interactions.* *Tool: draw.io  |  File: \[ProjectName\]\_\[SC-xx\]\_\[DiagramType\].drawio* |
| :---- |

## **SC-02 — \[Next Scenario\]**

*(Repeat SC structure above for each scenario. Aim for 4–8 scenarios covering the most important business flows.)*

## **Scenario List**

| Guidance — Scenario List *Summarise all scenarios in one table for quick navigation.* *Every Feature Description in Part 3 must link to at least one scenario.* |
| :---- |

| ID | Scenario Name | Primary Actor | Maps to Feature(s) | Priority |
| ----- | ----- | ----- | ----- | ----- |
| SC-01 | \[Name\] | \[Actor\] | \[FT-xx\] | High/Medium/Low |
| SC-02 | \[Name\] | \[Actor\] | \[FT-xx\] |  |
| SC-0N | \[Add rows as needed\] |  |  |  |

# **Part 3 — Feature Description**

| Guidance — Part 3 convention *Feature Descriptions specify WHAT the system does — written after scenarios are approved.* *Each FT-xx maps to at least one V\&S feature (FE-xx) via the traceability bridge (Part 1.5).* *Every FT-xx contains five sub-sections:*   *1\. Header table (source feature, scenario, UC, summary)*   *2\. System Behaviour (what the system does — use active system language \+ specific numbers)*   *3\. Applicable Business Rules (BR IDs only — full definitions in RTW.xlsx Sheet 6\)*   *4\. Positive Acceptance Criteria (AC-xx) — happy path, verifiable conditions*   *5\. Negative Acceptance Criteria (NAC-xx) — failure, rejection, invalid inputs*   *6\. Boundary Value Notes (BV-xx) — valid/invalid ranges with test points* *Business Rule convention (Approach B — master in RTW.xlsx):*   *List BR IDs only in each FT-xx. Full rule text is in RTW.xlsx Sheet 6\.*   *This eliminates duplication and ensures a single point of update.* *Writing rules:*   *• 'The system automatically...' / 'The system displays...' / 'The system rejects...'*   *• Specific numbers always: '15 minutes', 'within 90 seconds', 'maximum 10 units'*   *• Never vague: 'quickly', 'soon', 'appropriate', 'standard'*   *• Do NOT describe UI layout or technical implementation* |
| :---- |

## **FT-01 — \[Feature Name\]**

| Source Feature (V\&S) | \[FE-xx\] |
| :---- | :---- |
| **Related Scenario** | \[SC-xx\] |
| **Related UC** | \[UC-xx (RTW.xlsx Sheet 2)\] |
| **Summary** | \[One sentence: what the system does for whom, and what value it delivers.\] |

**System Behaviour:**

| Guidance — System Behaviour *Describe the system's behaviour in operational terms. Structure as:*   *Trigger → Processing → Output* *Include: polling/push mechanism, timing (every X seconds), data normalisation,*   *display rules, sorting, and any automatic actions.* *Use specific numbers. Reference BR IDs for any constraint that has a policy basis.* |
| :---- |

*\[Describe system behaviour here. 2–4 paragraphs. Active voice. Specific numbers.\]*

**Applicable Business Rules:**

*BR-xx, BR-yy  →  See \[ProjectName\]\_RTW.xlsx Sheet 6 for full definitions.*

**Positive Acceptance Criteria (AC):**

| Guidance — Positive AC *Write 2–5 conditions that must be TRUE for this feature to be considered done.* *Each AC must be independently testable — a QA engineer should be able to write* *a test case from each AC without asking questions.* *Format: 'AC-xx: When \[condition\], the system \[does X\] within \[Y time / Z count\].'* |
| :---- |

> * **AC-01:** \[When X happens, the system does Y. Specific and measurable.\]

> * **AC-02:** \[Another verifiable positive condition.\]

| Negative Acceptance Criteria (NAC) — FT-01 *Write 3–5 failure / rejection scenarios that the system must handle correctly.* *For each NAC, specify:*   *• The invalid input, forbidden action, or failure condition*   *• The exact system response (HTTP status code, error message text, or behaviour)*   *• Any side effects that must NOT occur (no record created, no stock changed, etc.)*   *• Any audit/logging requirement* *Common NAC categories to cover:*   *• Invalid input: malformed payload, missing required field, wrong data type*   *• Boundary breach: value outside valid range (reference BV-xx)*   *• Unauthorised access: role without permission attempts an action → 403*   *• Race condition: two users attempting the same action simultaneously*   *• Stale / duplicate: event received for already-closed or already-processed state*   *• Timer expiry: action attempted after a time window has closed* *Replace this guidance box with actual NAC-xx entries when writing the real SRS.* |
| :---- |

| Boundary Value Notes (BV) — FT-01 *For each numeric constraint in this feature, define:*   *• Lower boundary (minimum valid value)*   *• Upper boundary (maximum valid value)*   *• Default value*   *• Invalid values just outside boundaries*   *• Explicit test points for QA (parameterised test inputs)* *Format:*   *BV-xx  \[Constraint name\]:*     *Valid: \[min\] to \[max\]  |  Default: \[value\]*     *Invalid: \[below min\], \[above max\]*     *Test points: \[value1 (label)\], \[value2 (label)\], \[boundary\], \[boundary+1 (breach)\]* *Replace this guidance box with actual BV-xx entries when writing the real SRS.* |
| :---- |

| Diagram Placeholder — \[FT-01 Diagram if needed\] *INSERT HERE only if this feature requires a visual to clarify integration or flow.* *Not every feature needs its own diagram — use sparingly.* *Candidates: integration flow (for API-heavy features), state machine (if complex state).* |
| :---- |

## **FT-02 to FT-0N — \[Additional Features\]**

*(Repeat the FT structure above for each feature. Add as many FT-xx sections as needed, one per feature.)*

| ID | Feature Name | Source (V\&S) | Scenario | Status |
| ----- | ----- | ----- | ----- | ----- |
| FT-01 | \[Name\] | FE-xx | SC-xx | Specified / Draft |
| FT-02 | \[Name\] | FE-xx | SC-xx |  |
| FT-0N | \[Add rows\] |  |  |  |

# **Part 4 — Data Requirements**

| Guidance — Part 4 *Four sub-sections:*   *4.1 Core Entities & Relationships — entity summary (not full ERD)*   *4.2 Data Constraints & Integrity Rules — system-enforced rules (distinct from BRs)*   *4.3 Data Retention & Ownership — how long, who owns, who can delete*   *4.4 State Transition Table — for any entity with a multi-step lifecycle* *What belongs in RTW.xlsx Sheet 5 (not here):*   *Full attribute-level detail: data type, nullable, unique, default, validation rule* *What belongs in the TDS (not here):*   *Physical schema, indexes, migration scripts, ORM mappings* |
| :---- |

## **4.1  Core Entities & Relationships**

| Guidance — 4.1 *List each entity, its purpose in one sentence, and its key relationships.* *Use the notation: Entity A \--\< Entity B (one-to-many), A \-- B (one-to-one).* *Full attribute definitions are in RTW.xlsx Sheet 5\.* *Place the ERD diagram placeholder below — insert a conceptual ERD (not physical schema).* |
| :---- |

| Diagram Placeholder — Entity Relationship Diagram (ERD) *INSERT HERE: Conceptual ERD showing core entities and their relationships.* *Use crow's foot notation. Show cardinality and key foreign keys.* *This is a conceptual ERD for requirement purposes — physical ERD belongs in TDS.* *Tool: draw.io  |  File: \[ProjectName\]\_ERD.drawio* |
| :---- |

| Entity | Purpose (one sentence) | Key Relationships | Full Attributes |
| ----- | ----- | ----- | ----- |
| \[Entity 1\] | \[What it represents\] | \[Relationships to other entities\] | RTW.xlsx Sheet 5 |
| \[Entity 2\] |  |  | RTW.xlsx Sheet 5 |
| \[Add rows\] |  |  | RTW.xlsx Sheet 5 |

## **4.2  Data Constraints & Integrity Rules**

| Guidance — 4.2 *List constraints that affect system behaviour at the requirement level.* *These are data integrity rules, NOT business policy rules (those are in RTW.xlsx Sheet 6).* *Integrity rules include: immutability, uniqueness, referential integrity, atomic operations,*   *valid value ranges enforced at the data layer.* *Each DC-xx should reference the feature (FT-xx) or state transition (Part 4.4) it supports.* |
| :---- |

| ID | Constraint | Entities Affected | Violation Behaviour |
| ----- | ----- | ----- | ----- |
| DC-01 | \[Constraint description\] | \[Entity name(s)\] | \[What system does when violated\] |
| DC-02 | \[Add rows as needed\] |  |  |

## **4.3  Data Retention & Ownership Policy**

| Guidance — 4.3 *For each data type, specify: how long it must be kept, who owns it, who can delete it,* *and the regulatory or policy basis for these decisions.* *This drives: automated purge jobs, data deletion request handling (GDPR/local law),*   *and audit log immutability requirements.* |
| :---- |

| Data Type | Retention Period | Owner | Deletion Authority | Basis |
| ----- | ----- | ----- | ----- | ----- |
| \[Data type\] | \[Duration / condition\] | \[Role / entity\] | \[Who / when\] | \[Law / policy\] |
| \[Add rows\] |  |  |  |  |

## **4.4  \[Entity\] State Transition Table**

| Guidance — 4.4 State Transition Table *Create one State Transition Table for each entity with a significant lifecycle* *(e.g. Order, Ticket, Request, Task).* *The table has two parts:*   *(a) Valid Transitions — every legal state change with trigger, guard, and system action*   *(b) Invalid Transitions — explicit list of forbidden transitions that must be rejected* *Each row in (a) should correspond to 1–2 positive test cases.* *Each row in (b) should correspond to 1 negative test case (NAC).* *Columns for Valid Transitions:*   *From State | Trigger Event | Guard Condition | To State | System Action* *Columns for Invalid Transitions:*   *From State | Attempted Transition | Why Invalid | System Response* *Close this section with a QA guidance note listing the key edge cases to test.* *Add a Sequence/State Machine diagram placeholder.* |
| :---- |

**Valid State Transitions:**

| From State | Trigger Event | Guard Condition | To State | System Action |
| ----- | ----- | ----- | ----- | ----- |
| \[Initial state\] | \[Event that fires the transition\] | \[Condition that must be true\] | \[New state\] | \[What system does automatically\] |
| \[State\] | \[Trigger\] | \[Guard\] | \[State\] | \[Action\] |
| \[Add rows\] |  |  |  |  |

**Invalid Transitions (must be rejected — DC-01):**

| From State | Attempted Transition | Why Invalid | System Response |
| ----- | ----- | ----- | ----- |
| \[State\] | \[Forbidden action\] | \[Reason: backward / terminal / skip\] | HTTP 4xx \+ error message |
| \[Add rows\] |  |  |  |

| Diagram Placeholder — \[Entity\] State Machine Diagram *INSERT HERE: State Machine / State Diagram for this entity's lifecycle.* *Label each transition arrow with the trigger event.* *Show terminal states (no outgoing transitions) clearly.* *Tool: draw.io  |  File: \[ProjectName\]\_\[Entity\]StateMachine.drawio* |
| :---- |

# **Part 5 — Business Rules**

| Guidance — Part 5 *All Business Rules are maintained exclusively in RTW.xlsx Sheet 6 (master).* *This Part contains only an Excel reference box and a quick-reference ID list.* *Each FT-xx in Part 3 lists only the applicable BR IDs — never the full text.* *This eliminates duplication and ensures a single update point.* |
| :---- |

| Business Rules Register → \[ProjectName\]\_RTW.xlsx Sheet 6 *All Business Rules are maintained in: \[ProjectName\]\_RTW.xlsx → Sheet 6* *Each FT-xx in Part 3 lists applicable BR IDs only.* *Full rule text, trigger conditions, exception handling, source: RTW.xlsx Sheet 6\.* *Current BRs: \[List BR-01 through BR-xx with one-line description each\]* |
| :---- |

# **Part 6 — Non-Functional Requirements (NFR)**

| Guidance — Part 6 *NFR definitions belong here. Test method, tool, measured result, and status* *are tracked in RTW.xlsx Sheet 7: NFR Tracker.* *Rules for writing good NFRs:*   *• Measurable: '\< 2 seconds' not 'must be fast'*   *• Conditional: state the load / context explicitly*   *• Achievable: based on real tech stack and budget*   *• Categorised: group by dimension (Performance, Security, etc.)* *Standard NFR categories (add/remove as needed for your project):*   *6.1 Performance   6.2 Scalability   6.3 Availability & Reliability*   *6.4 Security      6.5 Usability     6.6 Maintainability   6.7 Compliance* |
| :---- |

## **6.1  Performance**

| ID | Requirement | Condition | Priority |
| ----- | ----- | ----- | ----- |
| NFR-P01 | \[Page / operation\] loads in \< \[X\] seconds | \[Load condition: N users, network type\] | High / Medium / Low |
| NFR-P0N | \[Add rows\] |  |  |

## **6.2  Scalability**

| ID | Requirement | Notes |
| ----- | ----- | ----- |
| NFR-S01 | System supports \>= \[N\] concurrent users without degradation | Verified by load test |
| NFR-S0N | \[Add rows\] |  |

## **6.3  Availability & Reliability**

| ID | Requirement | Notes |
| ----- | ----- | ----- |
| NFR-A01 | Uptime \>= \[X\]% per month | Excludes announced maintenance |
| NFR-A0N | \[Add rows\] |  |

## **6.4  Security**

| ID | Requirement | Notes |
| ----- | ----- | ----- |
| NFR-SEC01 | All traffic over HTTPS / TLS 1.2+ | HTTP redirects to HTTPS |
| NFR-SEC0N | \[Add rows\] |  |

## **6.5  Usability**

| ID | Requirement | Notes |
| ----- | ----- | ----- |
| NFR-U01 | \[Usability requirement\] | \[Test method or device scope\] |
| NFR-U0N | \[Add rows\] |  |

## **6.6  Maintainability**

| ID | Requirement | Notes |
| ----- | ----- | ----- |
| NFR-M01 | \[Maintainability requirement\] |  |
| NFR-M0N | \[Add rows\] |  |

## **6.7  Compliance**

| Guidance — Compliance NFRs *List legal and regulatory requirements relevant to your project context.* *Common examples: data retention law, personal data protection, financial reporting.* *Always state the specific regulation or policy — not just 'comply with the law'.* |
| :---- |

| ID | Requirement | Regulatory Basis |
| ----- | ----- | ----- |
| NFR-C01 | \[Compliance requirement\] | \[Specific law / regulation / policy\] |
| NFR-C0N | \[Add rows\] |  |

# **Appendices**

## **Appendix A — RTW.xlsx Reference**

| All tracking artefacts → \[ProjectName\]\_RTW.xlsx *Sheet 1: Overview Dashboard     Sheet 2: Use Case List* *Sheet 3: Feature Traceability   Sheet 4: Permission Matrix* *Sheet 5: Data Dictionary        Sheet 6: Business Rules Register* *Sheet 7: NFR Tracker            Sheet 8: Open Issues Log* *Sheet 9: Change Log* |
| :---- |

## **Appendix B — Diagram Index**

| Guidance — Diagram Index *List every diagram referenced in this SRS.* *For each: diagram type, where it is referenced, and the source draw.io filename.* *When the diagram is created, export as PNG and embed at the placeholder location.* |
| :---- |

| Diagram | Type | Referenced in | File Name |
| ----- | ----- | ----- | ----- |
| \[Diagram name\] | \[Type: Context / UC / ERD / State Machine / Activity / Sequence\] | \[Part X.Y\] | \[ProjectName\]\_\[Name\].drawio |
| \[Add rows\] |  |  |  |

*— End of Template —*
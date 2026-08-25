**Functional Design Specification**

***\[Project Name\] (\[CODE\]) \- Report 3***

| Document Title | *\[Project Name\] — Functional Design Specification* |
| :---- | :---- |
| **Version** | v\[X.Y\] |
| **Date** | *\[DD/MM/YYYY\]* |
| **Status** | Draft / In Review / Approved |
| **Author(s)** | *\[Name / Role\]* |
| **Reviewer(s)** | *\[Name / Role\]* |
| **SRS Reference** | *\[ProjectName\]\_SRS\_v\[X.X\]* |
| **TDS Reference** | *\[ProjectName\]\_TDS\_v\[X.X\]* |
| **RTW Reference** | *\[ProjectName\]\_RTW.xlsx — Sheet 10 & 11* |

# **Version History**

| Version | Date | Author | Description |
| ----- | ----- | ----- | ----- |
| v0.1 | *\[DD/MM/YYYY\]* | *\[Name\]* | Initial draft |
| v\[X.Y\] | *\[DD/MM/YYYY\]* | *\[Name\]* | *\[Description of changes\]* |

# **PART 1 – OVERVIEW**

## **1.1  Screen Navigation Flow**

*Diagram showing all screens, navigation paths, role-based entry points, and public screens (no auth). Attached file: \[path/to/nav\_flow.drawio\]*

\[Paste navigation flow diagram here\]

Example Mermaid:  
flowchart TD  
    LOGIN\["Login"\] \--\> |role=Staff|   QUEUE\["Order Queue"\]  
    LOGIN          \--\> |role=Manager| DASH\["Dashboard"\]  
    QUEUE \--\> DETAIL\["Order Detail"\] \--\> SHIP\["Scan & Ship"\]  
    PUBLIC\["Public Tracking · no auth"\]

## **1.2  Job Schedule & Dependencies**

*Diagram showing each job's schedule, inter-job dependencies, and which external systems trigger which jobs. Attached file: \[path/to/job\_schedule.drawio\]*

\[Paste Job Schedule diagram here\]

Example:  
┌─────────────────────────────────────────────────────┐  
│  JOB-01  Channel Polling      ←── every 30s         │  
│  JOB-02  Payment Timeout      ←── every 60s         │  
│  JOB-03  Low-Stock Detection  ←── every 5 min       │  
│  JOB-04  Auto-Complete Order  ←── every 10 min      │  
│  JOB-05  Notification Retry   ←── on RabbitMQ event │  
└─────────────────────────────────────────────────────┘

# **PART 2 – SCREEN SPECIFICATIONS**

*One section per screen (2.1, 2.2, ...). Copy the full 2.x section for each screen. Attach wireframe/mockup to each section where available.*

## **2.x  \[Screen Name\]**

### **2.x.1  General Information**

*RTW ref: Sheet 10 — look up by Screen Name (URL · Type · Role(s) · FT · UC · API · Auth · Status)*

| Screen Name | *\[Screen name — must be unique across the entire system\]* |
| :---- | :---- |
| **Purpose** | *\[1–2 sentences: allows \[actor\] to do what, in order to achieve what goal — written from the user's perspective\]* |
| **Mockup / Wireframe** | *\[link\] / \[TBD\]* |

#### **Navigation Context**

| Navigates from | Condition / Trigger |
| ----- | ----- |
| *\[Screen name\]* | *\[What the user did / what event led to this screen\]* |

| Navigates to | Condition / Trigger |
| ----- | ----- |
| *\[Screen name\]* | *\[When / under what condition\]* |

### **2.x.2  Entry & Exit Conditions**

#### **Preconditions & Guard Redirects**

*Conditions that must be true before the screen renders. If not met → redirect immediately, do not render.*

| \# | Condition check | If met | If not met → Action |
| :---: | ----- | ----- | ----- |
| **PC-01** | *\[Example: Valid JWT token\]* | *Continue rendering* | *Redirect → Login; toast: "Please log in to continue"* |
| **PC-02** | *\[Example: User has required role\]* | *Continue rendering* | *Redirect → /403; toast: "You do not have permission"* |
| **PC-03** | *\[Example: Resource ID in URL exists\]* | *Continue rendering* | *Redirect → \[previous screen\]; toast: "\[Resource\] not found"* |
| **PC-0x** | *\[Add more as needed\]* |  |  |

#### **Entry Triggers**

*All events that bring the user to this screen.*

| \# | Trigger | From screen / Source | Parameters passed |
| :---: | ----- | ----- | ----- |
| **ET-01** | *User clicks \[button/link name\]* | *\[Source screen name\]* | *\`\[param1, param2\]\`* |
| **ET-02** | *Redirect after \[action\] succeeds* | *\[Source screen name\]* | *\`\[param\]\`* |
| **ET-03** | *Direct URL / notification link* | *Browser* | *Query: \`\[?param=value\]\`* |

#### **Exit Points**

*All ways the user leaves this screen, including timeouts and system-triggered redirects.*

| \# | Event | Condition | Navigates to | Notes |
| :---: | ----- | ----- | ----- | ----- |
| **EX-01** | *User clicks \[Submit\]* | *Validation passes* | *\[Target screen\]* | *Toast: "\[Success message\]"* |
| **EX-02** | *User clicks \[Cancel / Back\]* | *Any* | *\[Target screen\]* | *Unsaved changes discarded* |
| **EX-03** | *\[Business rule violation\]* | *\[Condition\]* | *Stay on screen* | *Error toast: "\[Message\]"* |
| **EX-04** | *JWT expired, silent refresh failed* | *Token expires* | *Login* |  |

### **2.x.3  UI Components**

*Component code: \[ScreenName\]-\[TYPE\]-\[NN\] — e.g. OrderDetail-BTN-01, OrderDetail-TXT-01. State: specify condition for Active / Disabled / Hidden / Read-only.*

| Code | Component name | Type | Validation / Constraint | Notes |
| ----- | ----- | ----- | ----- | ----- |
| **\[ScreenName\]-TXT-01** | *\[Field name\]* | *Text Input* | *Required; Max \[N\] chars; Error: "\[Message\]"* | *Placeholder: "\[...\]"* |
| **\[ScreenName\]-SEL-01** | *\[Dropdown name\]* | *Select* | *Required; Options: \[val1, val2\]* | *Default: \[value\]* |
| **\[ScreenName\]-BTN-01** | *\[Primary button\]* | *Button (Primary)* | *—* | *Triggers: \[action\]; Disabled when \[condition\]* |
| **\[ScreenName\]-BTN-02** | *\[Secondary button\]* | *Button (Secondary)* | *—* | *Triggers: \[action\]* |
| **\[ScreenName\]-BTN-03** | *\[Destructive button\]* | *Button (Danger)* | *Requires confirm dialog* | *Visible when \[condition\]* |
| **\[ScreenName\]-TBL-01** | *\[Table name\]* | *Table* | *\[N\] rows/page; Sortable: \[col\]* | *Empty state: "\[Message\]"* |
| **\[ScreenName\]-BDG-01** | *\[Badge name\]* | *Badge* | *Values: \[val1→color1, val2→color2\]* |  |

### **2.x.4  API Calls & Data Flow**

*All API calls from this screen: page load, form submit, button actions. Include loading state in the Notes / Success column.*

| \# | Trigger (UI event) | API ID | Method \+ Path | Request | Success → UI action | Error → UI action |
| ----- | ----- | ----- | ----- | ----- | ----- | ----- |
| **1** | *Page load (mount)* | *API-xx* | *\`GET /\[path\]\`* | *Query: \`\[params\]\`* | *Render \[component\] · loading: skeleton* | *401 → Login; 500 → error banner* |
| **2** | *User clicks \[button\]* | *API-xx* | *\`POST /\[path\]\`* | *\`{field1, field2}\`* | *Toast "\[Success\]" · navigate \[target\] · loading: disable \+ spinner* | *4xx \[CODE\] → toast "\[Error\]"* |
| **3** | *User clicks \[button\]* | *API-xx* | *\`PUT /\[path\]/:id\`* | *\`{field}\`* | *Re-fetch / optimistic update · loading: inline spinner* | *422 → highlight field \+ error* |

### **2.x.5  Interaction Specifications**

*Describe complex interaction logic not captured in the tables above: conditional display rules, real-time updates, debounced search, etc. Add or remove subsections to match the screen.*

#### **\[Interaction name — e.g. Return button conditional display\]**

IF \[condition A\] AND \[condition B\]  
THEN   \[action / what is shown\]  
ELSE   \[action / what is shown\]  
END IF

#### **\[Interaction name — e.g. Real-time countdown\]**

\[Describe logic\]

### **2.x.6  Messages & Display Content**

*List all messages on this screen. The Content column is the exact UI copy — never expose technical error codes to users; always suggest a next action.*

| Code | Type | Trigger condition | Content (UI copy) | Action / Duration |
| ----- | ----- | ----- | ----- | ----- |
| **MSG-01** | *Success Toast* | *Form submitted successfully* | *"\[Action\] completed successfully."* | *Auto-dismiss 3s* |
| **MSG-02** | *Error Toast* | *API returns \[ERROR\_CODE\]* | *"\[User-friendly description\]. \[Suggested next action\]."* | *Auto-dismiss 5s* |
| **MSG-03** | *Warning Toast* | *\[Condition\]* | *"\[Warning message\]"* | *Auto-dismiss 5s* |
| **MSG-04** | *Confirm Modal* | *User clicks \[destructive button\]* | *Title: "\[Action\]?" · Body: "\[Explain the consequence\]"* | *Buttons: "\[Confirm\]" (Danger) \+ "\[Cancel\]"* |
| **MSG-05** | *Alert Banner* | *\[Business condition\]* | *"\[Notification \+ action link\]"* | *Persistent / Dismissible* |
| **MSG-06** | *Empty State* | *No data in list/table* | *Title: "\[No X yet\]" · Description: "\[How to get started\]"* | *CTA: "\[Create new\]" if applicable* |
| **MSG-07** | *Inline Error* | *Field validation fails* | *"\[Specific error message for this field\]"* | *Below field, red colour* |
| **MSG-0x** | *\[Add as needed\]* |  |  |  |

### **2.x.7  Business Rules**

*List only the BR/BV rules that apply directly to this screen. Do not copy the full rule — reference the code and describe UI behaviour when violated.*

| BR/BV Code | Short description | UI behaviour when violated |
| ----- | ----- | ----- |
| **BR-xx** | *\[Rule name\]* | *Button disabled \+ tooltip: "\[Explanation\]" OR Toast MSG-xx* |
| **BV-xx** | *\[Boundary name\]* | *Inline error MSG-07: "\[Message\]"* |
| **NFR-xx** | *\[NFR name\]* | *Show loading indicator; on timeout → Alert banner MSG-05* |

**PART 3 – BACKGROUND JOB SPECIFICATIONS**

*One section per background job (3.1, 3.2, ...). Copy the full 3.x section for each job. Covers: @Scheduled jobs, @RabbitListener consumers, webhook receivers.*

## **3.x  \[JOB-xx\] Job Name**

### **3.x.1  General Information**

*RTW ref: Sheet 10 — Row JOB-xx (Name · Trigger type · Schedule/Event · FT · UC · BR/BV · Idempotent · Lock)*

| Code / Name | *JOB-xx — \[Job name\]* |
| :---- | :---- |
| **Class / Bean** | *com.\[package\].\[ClassName\]* |
| **Purpose** | *\[1–2 sentences: what this job does and why it must run automatically rather than being user-triggered\]* |
| **SLA (max runtime)** | *\< \[N\]s for \[N\] records under normal load. Alert if exceeded.* |

#### **Business Context**

*Explain in 3–5 sentences: (1) Why does this job exist? (2) What happens if it does not run? (3) Who is affected?*

**Example:** *"When a customer initiates payment, the order moves to AwaitingPayment and waits for confirmation from the payment gateway. If payment is not confirmed within 900 seconds (BV-02a), the order must be cancelled automatically to free the pending slot (BR-04) and restore the reserved stock (DC-02). Without this job, stalled orders permanently occupy slots and block customers from placing new orders."*

*\[Enter the business context for this job here\]*

### **3.x.2  Detailed Specification**

#### **Processing Flow**

STEP 1: Acquire lock  
  \- \[Lock key, TTL, action if lock cannot be acquired\]

STEP 2: Query data  
  \- \[Table, WHERE condition, batch size, action if result is empty\]

STEP 3: Process each record  
  FOR EACH record:  
    3a: \[Idempotency check / guard condition\]  
    3b: \[Main action — update/insert, transactional or not\]  
    3c: \[Side effects — publish event, send notification\]  
    3d: \[Per-record error handling — skip or rollback\]

STEP 4: Log result  
  \- \[processed=N, skipped=M, failed=K, durationMs=X\]

STEP 5: Release lock

#### **Input / Output Data**

**Input:**

| Source | Data | Filter condition | Notes |
| ----- | ----- | ----- | ----- |
| **Table \`\[name\]\`** | \`\[fields\]\` | \`\[WHERE clause\]\` | Batch size: \[N\] |
| **Queue \`\[name\]\`** | \`\[EventObject\]\` | Message type: \`\[type\]\` | Deserialise to \`\[Class\]\` |

**Output:**

| Type | Destination | Content | Condition |
| ----- | ----- | ----- | ----- |
| **DB Update** | Table \`\[name\]\` | \`SET \[field\]=\[value\]\` | When \[condition\] |
| **Event Publish** | Exchange \`\[name\]\` → Queue \`\[name\]\` | \`\[EventName\]\` {payload} | AFTER\_COMMIT |
| **Notification** | \[Channel\] | "\[Message content\]" | When \[condition\] |

#### **Idempotency Guarantee**

*Describe the mechanism that ensures repeated executions do not produce duplicate side effects. Choose one pattern; remove the others.*

\[Chosen pattern — describe the concrete implementation\]

Common patterns:  
  A. Idempotency key table: INSERT processed\_events(event\_id) in the same transaction  
  B. Status-based guard: only process if status \== \[expected\]; state machine rejects if done  
  C. Distributed lock per record: ShedLock key \= "\[job\]-\[recordId\]-\[date\]"  
  D. Upsert: INSERT ... ON CONFLICT DO NOTHING

**Verification:** *\[Describe how to test — send the same event twice, verify no duplicate in DB\] · L2 ref: \[test-id\]*

#### **Business Rules**

| Code | Rule | Applied at step | If violated |
| ----- | ----- | ----- | ----- |
| **BR-xx** | \[Name\] | Step \[N\] | \[Skip / Rollback / Alert\] |
| **BV-xx** | \[Name\] | Step \[N\] | \[Describe behaviour\] |

#### **Error Handling & Retry Policy**

*Distinguish: Transient (retry) — Permanent (skip \+ alert) — Critical (DLQ \+ alert).*

| Type | Exception / Condition | Action | Retry? | Max attempts |
| ----- | ----- | ----- | ----- | ----- |
| **Transient** | *Network timeout, DB deadlock* | *Log WARN; retry with backoff* | *Yes* | *\[N\]* |
| **Permanent** | *Validation error, business rule violation* | *Log ERROR; skip record; alert* | *No* | *—* |
| **Critical** | *Unhandled exception* | *Log ERROR \+ stack trace; DLQ; alert admin* | *No* | *—* |

**Retry policy:** *Max \[N\] attempts · Backoff: \[N\]ms initial, ×\[N\] multiplier, max \[N\]ms*

**DLQ (if event-driven):** *Queue \[name\].dlq · TTL \[N\] days · Alert when depth \> 0*

#### **SLA / Performance Expectation**

| Metric | Target | Alert when | How to measure |
| ----- | ----- | ----- | ----- |
| **Runtime per execution (normal)** | \< \[N\]s | \> \[N\]s | Log timing / Zipkin |
| **Runtime per execution (peak)** | \< \[N\]s | \> \[N\]s | Load test |
| **Records per execution** | ≤ \[N\] (batch size) | — | Log output |
| **DB query time** | \< \[N\]ms | \> \[N\]ms | Slow query log |

**Required index:** *\[table\](\[columns\]) for the query in Step 2*

**Batch strategy:** *\[N\] records/run · \[cursor-based / offset\] pagination*

#### **Logging & Alerting**

**Required log events:**

| Event | Level | Required fields |
| ----- | ----- | ----- |
| **Job started** | *INFO* | *\`job runId batchSize\`* |
| **Job completed** | *INFO* | *\`processed skipped failed durationMs\`* |
| **Job skipped (lock held)** | *DEBUG* | *\`reason=lock\_exists\`* |
| **Record processed** | *INFO* | *\`entityType entityId action newStatus\`* |
| **Record skipped** | *DEBUG* | *\`entityId reason\`* |
| **Record error** | *WARN/ERROR* | *\`entityId error attempt\` (+ stack trace at ERROR)* |

**Log format:** *\[TIMESTAMP\] \[LEVEL\] \[job-name\] \[runId\] \[message\] \[k=v pairs\]*

**Alert rules:**

| Alert | Condition | Severity |
| ----- | ----- | ----- |
| *\`\[Job\]SlowExecution\`* | *runtime \> \[N\]s* | *Warning* |
| *\`\[Job\]Failed\`* | *failed\_total \> 0 within \[N\] min* | *Critical* |
| *\`\[Job\]NotRunning\`* | *no execution within 2 × interval* | *Critical* |
| *\`\[Job\]HighFailureRate\`* | *failed/processed \> \[N\]%* | *Warning* |
| *\`DLQDepth\[Job\]\`* | *dlq\_messages \> 0* | *Warning* |

*This document is version-controlled alongside the codebase. Any change to screen logic or job behaviour must update this FDS before (or in parallel with) the code change. The FDS version must stay aligned with the corresponding SRS version.*
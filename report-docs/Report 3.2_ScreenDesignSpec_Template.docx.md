  
**Screen Design Specification**

***\[Project Name\] (\[CODE\]) \- Report 3***

| Project Name | \[Project Name\] |
| :---- | :---- |
| **SRS Reference** | SRS v\[X.X\] |
| **TDS Reference** | TDS v\[X.X\] |
| **Spec Version** | v1.0.0 |
| **Date Created** | \[DD/MM/YYYY\] |
| **Last Updated** | \[DD/MM/YYYY\] |
| **Author(s)** | \[UX Designer / Product Owner — Name\] |
| **Reviewer(s)** | \[Tech Lead, Product Owner, Key Stakeholder\] |
| **Status** | Draft / In Review / Approved |
| **Design File** | \[Figma / draw.io — link\] |

# **Purpose & Scope**

| What this document covers *Design language (colours, typography, grid, icons)* *Information architecture (site map, navigation, role access)* *Screen flows (end-to-end user journeys with decision branches)* *Screen specifications (layout, states, key interactions, mockup references per screen)* *Responsive rules (breakpoints and global layout adaptation)* |
| :---- |

| What this document does NOT cover (defined in other documents) *Component-level implementation detail, ARIA labels, keyboard navigation  →  TDS / Frontend Spec* *WCAG contrast ratios, accessibility audit  →  Accessibility Review* *Motion and animation timing  →  Frontend Spec* *Detailed acceptance criteria and business rules  →  SRS (AC-xx, NAC-xx)* |
| :---- |

| Companion documents *SRS v\[X.X\]                     — requirements, user roles, scenarios, acceptance criteria* *TDS v\[X.X\]                     — technical implementation (Part 2.3 SSR Pages maps to screens here)* *\[ProjectName\]\_RTW.xlsx Sheet 2  — Use Case List* *\[ProjectName\]\_RTW.xlsx Sheet 4  — Permission Matrix* *\[Design file link\]              — Figma / draw.io source; all mockups live here* *Who writes this: UX Designer \+ Product Owner* *When: after SRS approved; mockups approved before frontend implementation begins* *Guidance block colours used in this template:*   *BLUE  box \= Guidance — explains what to write (delete before distributing)*   *TEAL  box \= Scope / purpose information*   *GREEN box \= Example — illustrative content to replace*   *RED   box \= Required — must not be skipped*   *AMBER box \= Pattern — reusable rule applied across all screens*   *PURPLE box \= Diagram / mockup placeholder*   *GRAY  box \= Reference note* |
| :---- |

# **Document Change History**

| Version | Date | Changes | Author |
| ----- | ----- | ----- | ----- |
| v1.0.0 | \[DD/MM/YYYY\] | Initial baseline | \[Author\] |

# **Part 1 — Design Language**

| Guidance — Part 1 *Establishes the visual rules applied consistently across every screen.* *Define these BEFORE designing any individual screen — they are the foundation.* *Changing foundations after screens are drawn is expensive.* |
| :---- |

## **1.1  Design Principles**

| Guidance — 1.1 *Write 3–5 principles specific to this product — not generic design advice.* *Format: Principle name — one sentence describing what it means in practice.* *Good principles describe real tensions or priorities unique to this product and its users.* |
| :---- |

| \# | Principle | What It Means in Practice |
| ----- | ----- | ----- |
| 1 | \[Name\] | \[One specific sentence. e.g. Speed over decoration — warehouse staff process 50+ orders per shift; every unnecessary click costs real time.\] |
| 2 | \[Name\] | \[One specific sentence.\] |
| 3 | \[Name\] | \[One specific sentence.\] |
| 4 | \[Name\] | \[One specific sentence.\] |
| 5 | \[Name\] | \[One specific sentence.\] |

| Example principles *1  Status always visible       Every screen shows the current state of the key entity without an extra click.* *2  Errors prevent, not punish  Validate inline before submission. Tell the user what is wrong and how to fix it. Never clear a form on error.* *3  Trust the user's intent     Confirmations only for irreversible actions — do not ask 'are you sure?' for reversible operations.* *4  Progressive disclosure      Show the most important information first. Secondary details are one click away.* *5  Speed for operational screens  Internal/staff screens strip decoration. Customer-facing screens allow more visual investment.* |
| :---- |

## **1.2  Colour Palette**

| Guidance — 1.2 *Use semantic names — not 'blue-500' but 'Primary'. Each colour: hex \+ where used.* *Note: WCAG contrast ratios are verified separately in the Accessibility Review.* |
| :---- |

### **Brand / Primary Colours**

| Name | Hex | Usage |
| ----- | ----- | ----- |
| Primary | \`\#\[hex\]\` | \[Primary buttons, active nav, key links, focus rings\] |
| Primary Dark | \`\#\[hex\]\` | \[Hover state on Primary; high-emphasis text on light background\] |
| Primary Light | \`\#\[hex\]\` | \[Selected row background, active nav background, tag fill\] |
| Primary XLight | \`\#\[hex\]\` | \[Subtle row hover, light section backgrounds\] |

### **Semantic / Feedback Colours**

| Name | Hex | Usage |
| ----- | ----- | ----- |
| Success | \`\#\[hex\]\` | \[Success states, positive status badges, confirmed indicators\] |
| Success Light | \`\#\[hex\]\` | \[Success banner background, success badge fill\] |
| Warning | \`\#\[hex\]\` | \[Warning banners, at-risk states, pending indicators\] |
| Warning Light | \`\#\[hex\]\` | \[Warning banner background, warning badge fill\] |
| Error | \`\#\[hex\]\` | \[Error banners, validation messages, destructive buttons\] |
| Error Light | \`\#\[hex\]\` | \[Error banner background, error badge fill\] |
| Info | \`\#\[hex\]\` | \[Informational banners, neutral status indicators\] |
| Info Light | \`\#\[hex\]\` | \[Info banner background, info badge fill\] |

### **Neutral / Text / Surface Colours**

| Name | Hex | Usage |
| ----- | ----- | ----- |
| Text Primary | \`\#\[hex\]\` | \[Body text, headings, labels, high-importance values\] |
| Text Secondary | \`\#\[hex\]\` | \[Supporting labels, helper text, timestamps, de-emphasised values\] |
| Text Disabled | \`\#\[hex\]\` | \[Disabled field labels and placeholder values\] |
| Border | \`\#\[hex\]\` | \[Input borders, table lines, dividers\] |
| Border Strong | \`\#\[hex\]\` | \[Card outlines, section separators\] |
| Surface 1 | \`\#\[hex\]\` | \[Page background (lightest)\] |
| Surface 2 | \`\#\[hex\]\` | \[Card background, alternate table rows\] |
| Surface 3 | \`\#\[hex\]\` | \[Sidebar, modal overlay tint, header bar\] |
| White | \`\#FFFFFF\` | \[Button labels on dark fills, card inner content area\] |

## **1.3  Typography**

| Guidance — 1.3 *Every text element in the UI maps to one of these styles.* *Include: size, weight, line height, and primary usage.* |
| :---- |

**Font family:  Primary: \[Font Name\] (\[Google Fonts / Self-hosted / System stack\])**

Fallback: \[Font\], \[Font\], sans-serif

Monospace (IDs, codes, tracking numbers): \[Mono Font\], monospace

| Style | Size | Weight | Line Height | Used For |
| ----- | ----- | ----- | ----- | ----- |
| \`heading-1\` | \[e.g. 32px\] | \[e.g. 700\] | \[e.g. 1.2\] | \[Page titles\] |
| \`heading-2\` | \[e.g. 24px\] | \[e.g. 700\] | \[e.g. 1.3\] | \[Section headings, modal titles\] |
| \`heading-3\` | \[e.g. 20px\] | \[e.g. 600\] | \[e.g. 1.4\] | \[Card titles, subsection headings\] |
| \`heading-4\` | \[e.g. 16px\] | \[e.g. 600\] | \[e.g. 1.4\] | \[Form section labels\] |
| \`body-lg\` | \[e.g. 16px\] | \[e.g. 400\] | \[e.g. 1.6\] | \[Primary descriptive text\] |
| \`body-md\` | \[e.g. 14px\] | \[e.g. 400\] | \[e.g. 1.6\] | \[Default body, table cells, form inputs\] |
| \`body-sm\` | \[e.g. 12px\] | \[e.g. 400\] | \[e.g. 1.5\] | \[Helper text, timestamps, captions\] |
| \`label\` | \[e.g. 12px\] | \[e.g. 500\] | \[e.g. 1.4\] | \[Form field labels, table column headers\] |
| \`code\` | \[e.g. 13px\] | \[e.g. 400\] | \[e.g. 1.5\] | \[Order IDs, SKU codes, tracking numbers\] |
| \`button\` | \[e.g. 14px\] | \[e.g. 600\] | \[e.g. 1.0\] | \[All button labels\] |
| \`badge\` | \[e.g. 11px\] | \[e.g. 600\] | \[e.g. 1.2\] | \[Status badges, notification counts\] |

## **1.4  Layout Grid & Spacing**

**Spacing base unit: \[4px / 8px\]**

| Token | Value | Primary Usage |
| ----- | ----- | ----- |
| \`xs\` | \[e.g. 4px\] | \[Icon-to-label gap, badge inner padding\] |
| \`sm\` | \[e.g. 8px\] | \[Between closely related elements\] |
| \`md\` | \[e.g. 16px\] | \[Default padding inside cards and panels\] |
| \`lg\` | \[e.g. 24px\] | \[Between form groups, between sections\] |
| \`xl\` | \[e.g. 32px\] | \[Card-to-card gap, major section separation\] |
| \`2xl\` | \[e.g. 48px\] | \[Page section gap, empty state padding\] |

| Breakpoint | Width Range | Columns | Gutter | Page Margin |
| ----- | ----- | ----- | ----- | ----- |
| Mobile | \< 768px | 4 | 16px | 16px |
| Tablet | 768–1024px | 8 | 24px | 24px |
| Desktop | 1025–1440px | 12 | 24px | 32px |
| Wide | \> 1440px | 12 | 32px | auto (content max 1200px) |

| Layout Zone | Width |
| ----- | ----- |
| Left sidebar (desktop) | \[e.g. 240px — fixed\] |
| Main content area | \[e.g. 100% of remaining; max 1200px\] |
| Narrow forms (login, etc.) | \[e.g. max 480px, centred\] |
| Modal — small | \[e.g. 400px\] |
| Modal — medium | \[e.g. 600px\] |
| Modal — large | \[e.g. 800px\] |

## **1.5  Iconography**

| Guidance — 1.5 *Name the icon library and define semantic rules for the most important icons.* *Consistency of meaning matters more than visual style.* |
| :---- |

**Icon library:  \[e.g. Heroicons / Lucide / Phosphor / Custom SVG — link\]**

Sizes:  16px (inline/small)  ·  20px (default/button)  ·  24px (standalone/large)

| Icon | Meaning | Must NOT be used for |
| ----- | ----- | ----- |
| \[e.g. ✕ / X-mark\] | Close, dismiss, remove from list | Delete permanently |
| \[e.g. 🗑 Trash\] | Delete permanently | Remove from list |
| \[e.g. ✎ Pencil\] | Edit / modify | Create new |
| \[e.g. ＋ Plus\] | Create new, add | Expand / collapse |
| \[e.g. ⌄ Chevron down\] | Expand / collapse, dropdown | Navigate back |
| \[e.g. ← Arrow left\] | Navigate back | Expand / collapse |
| \[e.g. ⚠ Warning\] | Caution / side-effect warning | Blocking error |
| \[e.g. ✓ Check circle\] | Completed, confirmed, success | Selectable option (use radio/checkbox) |
| \[Add all semantic icons\] |  |  |

# **Part 2 — Information Architecture**

| Guidance — Part 2 *Defines the structure of the product — what pages exist, how they are organised,* *and how users navigate between them.* *Complete this BEFORE designing any individual screen.* |
| :---- |

## **2.1  User Roles & Navigation Access**

| Guidance — 2.1 *Summarise what each role sees at the navigation level.* *Full CRUD-level access is in RTW.xlsx Sheet 4 (Permission Matrix).* *This answers: what menu items are visible, and where does each role land after login?* |
| :---- |

| Role | Visible Navigation Sections | Landing Page After Login |
| ----- | ----- | ----- |
| \[Role 1\] | \[List top-level sections visible to this role\] | \[Page name\] |
| \[Role 2\] |  |  |
| \[Role 3\] |  |  |
| \[Add rows\] |  |  |

| Example *Customer         →  My Orders, Track Order, Support              →  My Orders* *Warehouse Staff  →  Order Queue, Scan & Ship                     →  Order Queue* *Shop Manager     →  Dashboard, Orders, Inventory, Reports         →  Dashboard* *Admin            →  All sections \+ User Management \+ Channels     →  Dashboard* |
| :---- |

## **2.2  Site Map**

| Required — every page must appear in the Site Map *Format per line: \[P-ID\] Page Name (Roles)  \[nav level\]* *Nav levels:*   *PRIMARY    \= in main navigation*   *SUB        \= in a sub-section or tab within a section*   *DEEP       \= reached only by clicking through another page (not in nav)* |
| :---- |

| Site Map template — replace with your actual page hierarchy \[Root\] │ ├── \[P-00\] Login / Sign In                           (Public)                   \[DEEP\] │ ├── \[P-01\] \[Section Name\]                            (\[Roles\])                  \[PRIMARY\] │   ├── \[P-01a\] \[Page Name\]                          (\[Roles\])                  \[PRIMARY under section\] │   └── \[P-01b\] \[Page Name\]                          (\[Roles\])                  \[DEEP — from P-01a\] │ ├── \[P-02\] \[Section Name\]                            (\[Roles\])                  \[PRIMARY\] │   ├── \[P-02a\] \[Page Name\]                          (\[Roles\])                  \[PRIMARY under section\] │   ├── \[P-02b\] \[Page Name\]                          (\[Roles\])                  \[DEEP — from P-02a\] │   └── \[P-02c\] \[Page Name\]                          (\[Roles\])                  \[DEEP — from P-02b\] │ └── \[P-0N\] \[Add all sections and pages\] |
| :---- |

## **2.3  Navigation Patterns**

| Guidance — 2.3 *Describe the navigation type and behaviour — not pixel-level specs.* *Pixel specs go in the design file.* |
| :---- |

### **Primary Navigation**

| Attribute | Specification |
| ----- | ----- |
| Pattern | \[e.g. Fixed left sidebar / Top navbar / Bottom tab bar\] |
| Desktop width | \[e.g. 240px fixed\] |
| Tablet behaviour | \[e.g. Collapses to icon-only (64px wide)\] |
| Mobile behaviour | \[e.g. Hidden; hamburger icon opens full-width overlay drawer\] |
| Active state | \[e.g. Primary Light background \+ Primary text \+ 3px left border\] |
| Badge / count | \[e.g. Right-aligned count bubble for sections with pending items\] |

### **In-Page Navigation (Tabs)**

*Used on:  \[e.g. Order Detail — tabs: Overview / Timeline / Notes\]*

| Attribute | Specification |
| ----- | ----- |
| Style | \[e.g. Underline tabs — 2px Primary bottom border on active tab\] |
| Position | \[e.g. Below page heading, above content area\] |
| Mobile | \[e.g. Horizontal scroll if tabs overflow screen width\] |

### **Breadcrumbs**

*Used on:  \[e.g. All pages deeper than level 1 — e.g. Order Detail, SKU Detail\]*

| Attribute | Specification |
| ----- | ----- |
| Format | Home  \>  Section  \>  Current Page |
| Current page | \[e.g. Plain text (not a link), Text Primary colour\] |
| Mobile | \[e.g. Show only previous level \+ current, e.g.  ← Orders\] |

# **Part 3 — Screen Flows**

| Guidance — Part 3 *Documents end-to-end user journeys — how users move between screens to complete a goal.* *Each flow covers one significant user task from entry point to completion,* *including the most important error and alternate paths.* *Rules for a good screen flow:*   *• Start from a specific entry point (not 'from anywhere')*   *• Show the happy path top-to-bottom*   *• Branch for key decision points and error paths*   *• Reference Page IDs (P-xx) from Section 2.2*   *• Map to the SRS Scenario (SC-xx) it implements*   *• Note SRS boundary conditions (BV-xx) or negative cases (NAC-xx) that affect the flow* *Aim for one flow per major user goal — typically 4–8 flows for a product of moderate size.* *Simple single-page interactions do not need a flow.* |
| :---- |

## **Flow \[F-01\] — \[Flow Name\]**

| Related SRS Scenario | \[SC-xx\] |
| :---- | :---- |
| **Primary Actor** | \[Role\] |
| **Entry Point** | \[Page name / trigger — e.g. 'User opens product detail page'\] |
| **Success End State** | \[What has been achieved — e.g. 'Order confirmed; customer receives email'\] |
| **Design File** | \[Figma / draw.io — frame '\[F-01\] Flow Name'\] |

### **Flow Diagram**

| Guidance — flow diagram *Use the ASCII diagram below for the Word document.* *Maintain the full visual version in the design file (Figma / draw.io).* *Show: entry point → happy path steps → branches for key error / alternate paths → end state.* |
| :---- |

| Flow Diagram — \[F-01\] \[Entry Point: Page name (P-xx)\]          │          ▼ \[Step 1: Action / Screen (P-xx)\]          │          ├──── \[Branch: e.g. 'Item out of stock'\]          │              │          │              ▼          │     \[Alternate path: error screen / inline message\]          │              │          │              ▼          │     \[Resolution: redirect / retry option\]          │          ▼ \[Step 2: Next screen (P-xx)\]          │          ├──── \[Branch: e.g. 'Action fails / timeout'\]          │              │          │              ▼          │     \[Error handling: inline error / retry screen\]          │     \[User options: retry / cancel / contact support\]          │          ▼ \[Success / Confirmation screen (P-xx)\]          │          ▼ \[End state: describe what was achieved\] |
| :---- |

### **Key Decision Points**

| Branch | Condition | Outcome |
| ----- | ----- | ----- |
| \[Branch 1\] | \[e.g. Item goes out of stock during checkout\] | \[e.g. Block order; show error; suggest alternatives — NAC-01c\] |
| \[Branch 2\] | \[e.g. Payment gateway returns error\] | \[e.g. Show retry option; preserve all form input — NAC-02a\] |
| \[Branch 3\] | \[e.g. Session timeout / 15-min payment timeout\] | \[e.g. Auto-cancel; notify customer — SRS BV-02a\] |
| \[Add rows for each branch\] |  |  |

## **Flow \[F-02\] — \[Flow Name\]**

| Related SRS Scenario | \[SC-xx\] |
| :---- | :---- |
| **Primary Actor** | \[Role\] |
| **Entry Point** | \[Page / trigger\] |
| **Success End State** | \[Outcome\] |
| **Design File** | \[Link\] |

### **Flow Diagram**

| Flow Diagram — \[F-02\] \[Entry Point (P-xx)\]          │          ▼ \[Step 1 (P-xx)\]          │          ▼ \[Step 2 (P-xx)\]          │          ▼ \[End State\] |
| :---- |

### **Key Decision Points**

| Branch | Condition | Outcome |
| ----- | ----- | ----- |
| \[Branch 1\] |  |  |
| \[Add rows\] |  |  |

| Add more Flow sections (F-03, F-04 ...) as needed *Recommended flows for a typical product:*   *F-01  Main customer action (e.g. place order)           → SC-01*   *F-02  Main staff action (e.g. process and ship order)   → SC-02*   *F-03  Tracking / status check                           → SC-03*   *F-04  Return / refund or complaint                      → SC-04*   *F-05  Management action (e.g. review dashboard)         → SC-05*   *F-06  Admin / configuration action* |
| :---- |

# **Part 4 — Screen Specifications**

| Guidance — Part 4 *One section per screen (matching Page IDs from Section 2.2).* *Provides sufficient detail to produce and review the mockup.* *Each screen section includes:*   *1\. Screen metadata (ID, role, SRS reference, design file link)*   *2\. Screen states (every variant that must be designed)*   *3\. Layout description (content zones — structure, not visual styling)*   *4\. Key interactions (3–5 most important actions — not exhaustive)*   *5\. Mockup / wireframe placeholder* *What NOT to include at this level (covered in TDS / Frontend Spec):*   *Pixel-perfect measurements, full component state tables, ARIA attributes,*   *exhaustive interaction tables (those go in SRS AC-xx / NAC-xx).* *Every screen in the Site Map (2.2) needs a section here.* *Screen order should roughly follow SRS feature priority and implementation sprint order.* |
| :---- |

## **4.0 — Shared Layout (Authenticated Shell)**

| Guidance — 4.0 *Document the page shell shared by all authenticated screens ONCE here,* *so you do not repeat it in every screen section.* |
| :---- |

*Design file:  \[Figma / draw.io — frame 'Shared Layout — Authenticated Shell'\]*

| Shell layout structure ┌───────────────────────────────────────────────────────────┐ │ \[Optional: Top bar — logo, search, user menu, notifs\]     │ ├────────────┬──────────────────────────────────────────────┤ │            │  \[Page Content Area\]                         │ │ \[Primary   │                                              │ │  Navigation│  \[Page heading \+ breadcrumbs (if deep)\]      │ │  Sidebar\]  │                                              │ │            │  \[Page-specific content\]                     │ │  \[Width:   │                                              │ │   see 2.3\] │  \[Content padding: lg on all sides\]          │ └────────────┴──────────────────────────────────────────────┘ |
| :---- |

| Nav Item | Icon | Visible to Roles | Shows Badge? |
| ----- | ----- | ----- | ----- |
| \[e.g. Dashboard\] | \[icon-name\] | \[Roles\] | No |
| \[e.g. Orders\] | \[icon-name\] | \[Roles\] | Yes — unread order count |
| \[e.g. Inventory\] | \[icon-name\] | \[Roles\] | Yes — low-stock alert count |
| \[e.g. Reports\] | \[icon-name\] | \[Roles\] | No |
| \[e.g. Admin\] | \[icon-name\] | Admin only | No |
| \[Add all nav items\] |  |  |  |

## **4.\[N\] — \[Screen Name\]**

| Copy this block for every screen — replace N with the Page ID from 2.2 *Fill in all subsections. Write 'N/A' if a subsection does not apply — do not delete it.* |
| :---- |

| Page ID | \[P-xx\] |
| :---- | :---- |
| **Screen name** | \[e.g. Order Inbox\] |
| **Design file** | \[Figma / draw.io — frame '\[Screen Name\]'\] |
| **SRS Feature** | \[FT-xx\] |
| **SRS Scenario** | \[SC-xx\] |
| **Roles with access** | \[List — from RTW.xlsx Sheet 4 Permission Matrix\] |

### **Screen States**

| Guidance — Screen States *Every screen that loads data needs ALL applicable states designed.* *Never leave a state undocumented — developers will invent something inconsistent.* |
| :---- |

| State | When It Appears | Design File Frame |
| ----- | ----- | ----- |
| Loading | Data is being fetched | \[Frame link\] |
| Empty | No data (first use or all filtered out) | \[Frame link\] |
| Populated — default | Normal data present | \[Frame link\] |
| Error | Data fetch failed or action failed | \[Frame link\] |
| \[Other, e.g. Filtered\] | \[Condition\] | \[Frame link\] |

### **Layout Description**

| Guidance — Layout *Describe content zones and their arrangement. Use ASCII art for structure.* *Describe STRUCTURE, not visual styling (styling is in the design file).* |
| :---- |

| Layout zones — replace with your actual screen structure ┌──────────────────────────────────────────────────────┐ │  \[Zone A: Page heading \+ primary action button(s)\]   │ │  'Page Title'                    \[Primary Button\]    │ ├──────────────────────────────────────────────────────┤ │  \[Zone B: Filters / search / controls (if any)\]      │ │  \[Filter A\]  \[Filter B\]  \[Search\]      \[Clear all\]   │ ├──────────────────────────────────────────────────────┤ │  \[Zone C: Main content area\]                         │ │  Describe: table / card grid / form / detail view    │ │  If table: list column names                         │ │  If cards: describe card content                     │ │  If form: describe fields and sections               │ ├──────────────────────────────────────────────────────┤ │  \[Zone D: Footer actions / pagination (if any)\]      │ └──────────────────────────────────────────────────────┘ |
| :---- |

**Zone descriptions:**

* Zone A:  \[Page heading text. What primary action button(s) appear here and for which roles.\]

* Zone B:  \[Filter/search controls available. Default values.\]

* Zone C:  \[Main content description — columns if table, card content if grid, fields if form.\]

* Zone D:  \[Pagination type, or footer actions if any.\]

### **Key Interactions**

| Guidance — Key Interactions *List the 3–5 most important user actions on this screen.* *Focus on navigation decisions and state changes — not exhaustive interaction lists.* *Full acceptance criteria are in SRS AC-xx / NAC-xx.* |
| :---- |

| User Action | What Happens | Notes |
| ----- | ----- | ----- |
| \[e.g. Click a table row\] | \[Navigate to P-xx detail page\] | \[SRS AC-xx\] |
| \[e.g. Click primary action button\] | \[Open modal / navigate to P-xx / trigger action\] |  |
| \[e.g. Apply a filter\] | \[Table reloads with filtered results; URL updated\] |  |
| \[e.g. Destructive action\] | \[Confirmation dialog; on confirm: action \+ success toast\] | \[SRS NAC-xx\] |
| \[e.g. Error state retry\] | \[Reload / retry the failed data fetch\] |  |

### **Mockup**

| Mockup / Wireframe Placeholder *INSERT HERE: Mockup or wireframe image for the default (populated) state.* *Design file: \[Figma / draw.io — frame '\[Screen Name\] — Populated state'\]* *Additional states: \[Frame link — '\[Screen Name\] — All states'\]* |
| :---- |

*(Repeat section 4.\[N\] for each screen in the Site Map — see Appendix A.1 Screen Index)*

# **Part 5 — Responsive Rules**

| Guidance — Part 5 *Global responsive rules applied to every screen.* *Exceptions for specific screens are noted in their Part 4 section.* *Detailed component-level responsive behaviour is in the TDS / Frontend Spec.* |
| :---- |

## **5.1  Navigation**

| Breakpoint | Behaviour |
| ----- | ----- |
| Desktop (≥ 1025px) | \[e.g. Full sidebar — 240px; all icons and labels visible\] |
| Tablet (768–1024px) | \[e.g. Collapsed sidebar — icon-only (64px wide); hover shows label tooltip\] |
| Mobile (\< 768px) | \[e.g. Sidebar hidden; hamburger opens full-width overlay drawer\] |

## **5.2  Content Layout**

| Breakpoint | Layout Behaviour |
| ----- | ----- |
| Desktop | \[e.g. Multi-column card grids (2–3 col); full-width tables; side-by-side short fields\] |
| Tablet | \[e.g. 2-column card grids; tables may require horizontal scroll for many columns\] |
| Mobile | \[e.g. Single column throughout; all form fields full-width\] |

## **5.3  Data Tables**

| Two patterns — choose one per table and note the choice in the Part 4 screen section *Pattern A — Horizontal scroll:*   *Table keeps all columns; the container scrolls horizontally.*   *Recommend sticky first column (ID or name) for tables with 5+ columns.*   *Use when: all columns are important and comparable at a glance.* *Pattern B — Card reflow:*   *Each table row becomes a card on mobile. Card shows the 3–4 most important fields.*   *Remaining fields accessible via expand or detail page.*   *Use when: rows represent entities (orders, users, SKUs) that have a natural detail page.* |
| :---- |

## **5.4  Typography Scaling**

| Breakpoint | Heading Adjustment |
| ----- | ----- |
| Desktop | Full type scale as defined in Part 1.3 |
| Tablet | \`heading-1\` reduces to \`heading-2\` size; other styles unchanged |
| Mobile | \`heading-1\` → \[e.g. 24px\]; \`heading-2\` → \[e.g. 20px\]; body sizes unchanged |

## **5.5  Touch Targets (Mobile)**

| Required if the product is used on mobile *Minimum touch target: 44 × 44 px (iOS HIG / Android guidelines / WCAG 2.5.5).* *The visual element can be smaller; padding expands the tap area.* |
| :---- |

| Element | Visual Size | Minimum Touch Target |
| ----- | ----- | ----- |
| Primary buttons | \[design spec\] | 44px height minimum |
| Icon-only buttons | \[e.g. 24px icon\] | 44 × 44px tap area via padding |
| Checkboxes / radio | \[e.g. 18px\] | 44 × 44px tap area |
| Navigation items (drawer) | \[variable\] | 48px height minimum |
| Table row (mobile card) | \[variable\] | Full row tappable; 48px min height |

# **Appendix A — Screen Index & Design File**

## **A.1  Screen Index**

| Guidance — Screen Index *Update this table as screens are designed and approved.* *Every row in the Site Map (2.2) should appear here.* *Status values:  Draft  →  In Review  →  Approved  →  Implemented* |
| :---- |

| Page ID | Screen Name | SRS Feature | Flow(s) | Spec Section | Design Frame | Status |
| ----- | ----- | ----- | ----- | ----- | ----- | ----- |
| P-00 | Login | — | — | 4.N | \[Frame link\] | Draft |
| P-01 | \[Screen\] | \[FT-xx\] | F-xx | 4.N | \[Frame link\] |  |
| P-02a | \[Screen\] | \[FT-xx\] | F-xx | 4.N | \[Frame link\] |  |
| P-02b | \[Screen\] | \[FT-xx\] | F-xx | 4.N | \[Frame link\] |  |
| \[Add all screens from Site Map\] |  |  |  |  |  |  |

## **A.2  Design File Structure**

| Guidance — Design File *Document the folder/frame organisation so all team members can find what they need.* |
| :---- |

| Recommended design file structure (Figma / draw.io) \[Design File\] │ ├── 🎨 Foundations │   ├── Colour Palette │   ├── Typography │   ├── Grid & Spacing │   └── Icons │ ├── 🔄 Flows │   ├── \[F-01\] \[Flow Name\] │   ├── \[F-02\] \[Flow Name\] │   └── \[Add all flows\] │ ├── 🖥️ Screens — Desktop │   ├── \[Shared Layout Shell\] │   ├── \[P-00\] Login │   ├── \[P-01\] Dashboard │   ├── \[Section\]/ │   │   ├── \[P-xx\] \[Screen\] — Loading │   │   ├── \[P-xx\] \[Screen\] — Empty │   │   ├── \[P-xx\] \[Screen\] — Populated (default) │   │   ├── \[P-xx\] \[Screen\] — Error │   │   └── \[P-xx\] \[Screen\] — \[Other state if needed\] │   └── \[Add all sections\] │ └── 📱 Screens — Mobile     └── \[Mirror structure of Desktop\] |
| :---- |

## **A.3  Glossary**

| Term | Definition |
| ----- | ----- |
| P-xx | Page ID — used in the Site Map and Part 4 for screen traceability |
| F-xx | Flow ID — references a Screen Flow in Part 3 |
| FT-xx | SRS Feature ID |
| SC-xx | SRS Scenario ID |
| AC-xx | SRS Acceptance Criterion (positive condition) |
| NAC-xx | SRS Negative Acceptance Criterion (failure or boundary condition) |
| BV-xx | SRS Boundary Value note — valid/invalid ranges |
| Screen state | A distinct visual variant of a screen (loading, empty, populated, error, etc.) |
| Happy path | The main flow where everything goes as expected |
| Design file | Figma / draw.io source; the visual source of truth for all mockups |
| Mockup | Hi-fi design showing final visual appearance; created from this spec |
| Wireframe | Lo-fi layout sketch showing structure only; typically precedes the mockup |

*— End of Template —*
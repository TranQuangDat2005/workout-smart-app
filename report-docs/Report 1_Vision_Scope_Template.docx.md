  
**Vision & Scope Document**

***\[Project Name\] (\[CODE\]) \- Report 1***

| Project Name | \[Your Project Name\] |
| :---- | :---- |
| **Version** | v1.0.0 |
| **Date Created** | \[DD/MM/YYYY\] |
| **Last Updated** | \[DD/MM/YYYY\] |
| **Author(s)** | \[Author Name / Tên tác giả\] |
| **Reviewer(s)** | \[Reviewer Name / Tên người review\] |
| **Status** | Draft / In Review / Approved |

# **Document Change History**

| Version | Date | Changes | Author |
| ----- | ----- | ----- | ----- |
| v1.0.0 | \[DD/MM/YYYY\] | Initial creation | \[Author\] |

# **1\. Product Background**

| Purpose of This Section *Describe the current situation that makes this product necessary. Focus on:*   *• Who is affected and what pain they experience (with concrete numbers if possible)*   *• The cost or consequence of NOT solving this problem*   *• The context that makes this the right time to build the solution* *Writing tip: Tell a story — describe a real person's typical day and where the friction occurs.* *Avoid: listing features, discussing technology, or repeating the problem statement.* |
| :---- |

| Example (Online E-Commerce System): Small and medium-sized retailers in Vietnam currently manage their sales through a combination of Facebook Messenger, Zalo, and manual spreadsheets. A typical shop owner spends an average of 3–4 hours per day manually copying orders from chat messages into Excel, calculating totals, and texting customers with payment and delivery updates. During peak sales periods such as major promotions (11/11, 12/12), this process becomes unmanageable: orders are missed, customers receive incorrect totals, and inventory counts fall out of sync with actual stock. A survey of 50 shop owners in Ho Chi Minh City found that 68% had experienced at least one order loss per week due to messages being buried in chat threads, and 42% reported stock discrepancies causing customer complaints. The total estimated revenue loss per shop averages VND 15–20 million per month — entirely attributable to the absence of a centralised order management system. |
| :---- |

# **2\. Existing Solutions**

| Purpose of This Section *Analyse solutions that currently exist (commercial products, open-source tools, manual workarounds).* *The goal is NOT to list their features — it is to show why none of them fully solves the problem.* *Each entry must end with a GAP statement: what it cannot do in the context of YOUR project.* *Close the section with a summary paragraph explaining the unmet need that justifies your product.* *Structure for each solution:  Name → Brief description → Strengths → Weaknesses → GAP* |
| :---- |

## **2.1  \[Solution Name 1\]**

*Write 2–3 sentences describing what this solution is, who made it, and how it is used.*

| Attribute | Description |
| ----- | ----- |
| Website / Link | \[URL or N/A\] |
| Target Users | \[Who uses it\] |
| Core Features | \[Key features relevant to your problem\] |
| Strengths | \[What it does well\] |
| Weaknesses | \[Where it falls short\] |
| GAP for this project | \[Why it still does not solve YOUR specific problem\] |

| Example — Shopee Seller Centre: Shopee Seller Centre is the official order management portal provided by Shopee for merchants selling within its marketplace ecosystem. Attribute Description Website / Link https://seller.shopee.vn Target Users Merchants registered on the Shopee marketplace Core Features Order tracking, inventory sync, automated shipping labels, customer messaging Strengths Tightly integrated with Shopee logistics; low barrier to entry; mobile app available Weaknesses Locked to the Shopee ecosystem; cannot manage orders from other channels                             (Facebook, Zalo, TikTok Shop); no custom reporting; limited stock management GAP for this project Shop owners who sell across multiple channels (Facebook Live, Zalo, website)                   still need separate tools for non-Shopee orders. There is no unified view of                   all orders and no way to correlate stock across channels — the core pain                   point described in Section 1 remains entirely unaddressed.  |
| :---- |

## **2.2  \[Solution Name 2\]**

*(Repeat the structure above for each additional existing solution. Aim for 2–4 solutions total.)*

| Section Summary — Unmet Need *Close this section with 2–3 sentences summarising why none of the above solutions fully* *addresses the problem. This paragraph directly justifies the existence of your proposed product.* *Example: 'While marketplace platforms (Shopee, Lazada) provide order management within their* *own ecosystems, and spreadsheet tools offer flexibility, no existing solution unifies orders* *from all sales channels into a single view with real-time stock synchronisation. This gap* *represents the core opportunity addressed by the proposed ShopEasy system.'* |
| :---- |

# **3\. Proposed Solution**

| Purpose of This Section *Describe WHAT the product will do and WHY it is the right approach — not HOW it will be built.* *Answer three questions in this section:*   *1\. Which specific gaps (from Section 2\) does this product close?*   *2\. Who benefits and in what concrete way?*   *3\. Why is this the right time / right approach?* *Keep it to 2–4 paragraphs. Avoid technology details and do not list features here.* |
| :---- |

| Example (Online E-Commerce System): ShopEasy is a web-based order management platform designed specifically for Vietnamese small and medium-sized retailers who sell across multiple channels simultaneously. Unlike marketplace tools that lock merchants into a single platform, ShopEasy aggregates orders from Facebook, Zalo, Shopee, and a shop's own website into a single, unified dashboard — eliminating the manual copy-paste workflow that currently costs shop owners 3–4 hours per day. For shop owners, the immediate benefit is time reclaimed: orders are captured automatically, stock levels update in real time across all channels, and customers receive automated confirmations without manual intervention. For shop staff, picking and packing become guided tasks rather than error-prone memory exercises. The net result is fewer lost orders, fewer stock discrepancies, and a meaningful reduction in monthly revenue leakage. The timing is appropriate: smartphone penetration and social commerce have grown rapidly in Vietnam, but the tooling available to small retailers has not kept pace. ShopEasy bridges this gap with a low-cost, mobile-friendly solution that requires no technical expertise to operate — directly addressing the constraints identified in the gap analysis above. |
| :---- |

# **4\. Project Scope & Limitations**

| Purpose of This Section *Define precisely what is and is not included in this product. This section manages stakeholder* *expectations and serves as the baseline for evaluating future change requests.* *Rule: every feature listed here must be traceable back to a gap identified in Section 2\.* *If a feature cannot be linked to a gap, it should not be in scope.* |
| :---- |

## **4.1  Major Features**

*List the major features of the product. Each feature should have an ID, a name, a brief description, and a reference to the gap it addresses.*

| ID | Feature Name | Description | Addresses Gap |
| ----- | ----- | ----- | ----- |
| FE-01 | \[Feature Name\] | \[1–2 sentence description\] | \[Gap Ref\] |
| FE-02 | \[Feature Name\] | \[1–2 sentence description\] | \[Gap Ref\] |
| FE-03 | \[Feature Name\] | \[1–2 sentence description\] | \[Gap Ref\] |
| FE-04 | \[Feature Name\] | \[1–2 sentence description\] | \[Gap Ref\] |
| FE-05 | \[Feature Name\] | \[1–2 sentence description\] | \[Gap Ref\] |

| Example (Online E-Commerce System): ID Feature Name Description Addressed Gap FE-01 Unified Order Inbox Automatically aggregates orders from Facebook, Zalo, Shopee, and the shop's own website into one dashboard. GAP-01 FE-02 Real-Time Inventory Sync Updates stock levels across all connected sales channels instantly when an order is confirmed or cancelled. GAP-01 FE-03 Automated Customer Notify    Sends order confirmation, payment reminder, and shipping status messages to customers without manual intervention. GAP-02 FE-04 Order Processing Workflow    Guides warehouse staff through picking, packing, and courier handoff with a step-by-step mobile interface. GAP-02 FE-05 Sales & Revenue Reports Provides daily and monthly sales summaries, best-selling products, and channel-level performance breakdowns. GAP-03  |
| :---- |

## **4.2  Limitations & Exclusions**

*List what stakeholders might expect to be included but will NOT be in this version. For each exclusion, state the reason.*

| ID | Excluded Item | Reason |
| ----- | ----- | ----- |
| LI-01 | \[What is excluded\] | \[Reason for exclusion\] |
| LI-02 | \[What is excluded\] | \[Reason for exclusion\] |
| LI-03 | \[What is excluded\] | \[Reason for exclusion\] |

| Example (Online E-Commerce System): ID Excluded Item Reason LI-01 Native mobile app (iOS/Android) A responsive web app covers mobile use cases in v1; native apps are deferred to v2 based on user feedback. LI-02 Payment gateway integration The system records payment status but does not process payments directly; this is handled by third-party tools. LI-03 AI-powered demand forecasting Predictive analytics require historical data that does not yet exist; deferred until 6 months post-launch. LI-04 Multi-warehouse management v1 supports a single warehouse location. Multi-location                                         support is planned for v2 after validating core workflows. LI-05 Fraud detection / chargeback The system is not responsible for verifying payment handling                           authenticity or managing disputed transactions.  |
| :---- |

# **5\. Expected Contributions & Next Steps**

| Purpose of This Section *Describe the VALUE delivered after the product is complete — not the problem (that was Section 1).* *Organise contributions into three dimensions:*   *• For the business / organisation: operational and financial impact*   *• For end users: experience and capability improvements*   *• For the field / future research: what this work enables next* *Avoid repeating the problem statement. Every sentence here should begin with an outcome, not a feature.* |
| :---- |

## **5.1  Business & Operational Impact**

*\[Describe measurable operational improvements for the organisation or business owner.\]*

| Example: By automating order capture and stock synchronisation across all sales channels, ShopEasy is expected to reclaim 3–4 hours of daily manual work per shop, translating to approximately VND 10–15 million in recovered revenue per month through reduced order loss and fewer stock discrepancies. The unified dashboard reduces the need for additional headcount during peak promotion periods, directly lowering operational costs. |
| :---- |

## **5.2  User Experience Impact**

*\[Describe how the product improves the daily experience of its direct users.\]*

| Example: Shop owners gain a single source of truth for all orders and stock levels, reducing the cognitive load of monitoring multiple chat apps simultaneously. Customers benefit from faster, consistent communication — automated confirmations replace the current practice of manual replies that can take hours during busy periods. |
| :---- |

## **5.3  Planned Next Steps After Project Completion**

*\[Describe what comes next — future versions, research directions, or scale-up plans.\]*

| Example: Following successful v1 deployment and validation with pilot shops, the planned next steps include: (i) native iOS and Android applications for warehouse staff mobility, (ii) direct payment gateway integration to close the end-to-end transaction loop, (iii) AI-driven demand forecasting leveraging accumulated order history, and (iv) multi-warehouse support for shops operating multiple fulfilment locations. |
| :---- |

*— End of Document —*
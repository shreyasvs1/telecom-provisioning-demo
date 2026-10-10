# telecom-provisioning-demo

A small Spring Boot learning sandbox that mirrors a telecom provisioning
pipeline. An order comes in, is validated, enriched, turned into work specs,
grouped into dispatchable work orders, and saved. The business rules are plain
Java stand-ins for IBM ODM; the pipeline's "external" calls are fake and
deterministic.

State as of 10 October 2026. Owner: Shreyas V S.

## Commands

```bash
mvn spring-boot:run                # start on http://localhost:8080
mvn -q test                        # 29 tests, all Java, no internet needed
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081   # a second instance
```

- Java is not on the shell PATH, but Maven finds its own JDK (Homebrew
  OpenJDK 27; the project targets Java 17). Run Java through `mvn`.
- Port 8080 is usually taken by an instance Shreyas has left running. Do not
  stop or restart it without asking: the database is in memory, so a restart
  wipes every saved order. Use port 8081 for a throwaway test instance and
  stop it afterwards.
- Thymeleaf templates and static files are only picked up on restart.

## How the code is organised

Everything is under `com.example.provisioning`.

| Package | Role |
|---|---|
| `controller` | `ProvisioningController`: JSON endpoint `POST /api/provisioning/process` |
| `web` | `OrderFormController`, `OrderForm`: the browser order entry screen (`/`, `/orders/new`, `POST /orders`) |
| `service` | `ProvisioningOrchestrator` runs the pipeline; `OrderIdGenerator` makes IDs for screen orders |
| `validation` | `RequestValidator` (step 2). `VALID_REQUEST_TYPES` and `VALID_SERVICE_CODES` are public so the form offers exactly those values |
| `enrichment` | `LocationEnrichmentClient`, `ServiceEnrichmentClient` (step 3, fake, no network) |
| `catalog` | `WorkSpecCatalogService` reading `work_spec_catalog` (step 4) |
| `rules` | `HierarchyRulesEngine` (step 5) |
| `persistence` | `ProvisioningRecordService` and the record entities: every request and its work orders |

Other files that matter:

- `src/main/resources/templates/`: `layout.html` (shared layout), `index.html`, `order-form.html`.
- `src/main/resources/static/js/`: `address-lookup.js` (calls Nominatim), `address-form.js` (Find address button).
- `src/main/resources/schema.sql`, `data.sql`: H2 schema and catalog seed, reloaded on every start. Hibernate does not manage the schema.
- `.claude/skills/`: three project skills (see below).
- `docs/architecture-flow.drawio`: component diagram with links to the Confluence pages. It does not yet show the address lookup. The script that generated it is not in the repo, so edit the file directly or in draw.io.

Both entry points end in `ProvisioningOrchestrator.process`, so validation,
enrichment, catalog, dispatch and persistence are identical for JSON and
screen orders. The screen does not call the JSON endpoint.

## What has been built

| Work | Jira | Where |
|---|---|---|
| Pipeline and JSON endpoint | (original) | `controller`, `service`, `validation`, `enrichment`, `catalog`, `rules` |
| Saving every request and its work orders | (original) | `persistence`; tables `provisioning_request`, `work_order`, `work_order_spec` |
| Order entry screen: New order form, generated order ID, cancel | KAN-5, Done | `web`, templates, `OrderIdGenerator`, sequence `order_id_seq` |
| Find address lookup on the form | KAN-14, KAN-15, KAN-16, Done | `static/js/`, `order-form.html` |
| Skills for catalog summary, work order summary, Jira stories | none | `.claude/skills/` |

Not built yet: KAN-4 (dashboard with status filter), KAN-6 (field-by-field
validation messages), KAN-7 (full result screen). Feature KAN-3 (testing of the
intake form) is still empty. The home page `/` is a placeholder for the
dashboard, and the form's handling of rejected orders is a stopgap that KAN-6
and KAN-7 will replace.

## Decisions already made

Do not reopen these without being asked.

**Order entry screen (KAN-2, KAN-5)**

- Thymeleaf, server-rendered, served by the same Spring Boot app.
- The operator never types an order ID. The app generates `ORD-<number>` from
  the `order_id_seq` sequence (starts at 1001) and skips any ID already saved,
  because JSON callers bring their own IDs.
- The JSON endpoint is unchanged and still requires the caller's `orderId`.
- Cancel is a plain link to `/`. Nothing is sent, so nothing is saved.
- Sessions are cookie-only (`server.servlet.session.tracking-modes: cookie`)
  to keep `;jsessionid=` out of URLs.
- The dashboard (KAN-4) will show all orders by default with a filter on status.

**Find address (KAN-14)**

- It is a button, not suggest-as-you-type. The public Nominatim usage policy
  forbids auto-complete and allows at most 1 request per second.
- The public service `nominatim.openstreetmap.org` is acceptable.
- The browser calls Nominatim directly. The server never does, and there is no
  proxy or cache.
- The search text is all four address fields together. No separate search box.
- Matches are limited to the United States and Canada.
- State is filled as a short code (`IL`, `ON`).
- Choosing a match replaces all four fields; a field the match lacks is cleared.
- OpenStreetMap attribution sits in the bottom right corner.
- The label reads "Zip / Postal Code". The field is still named `zip` in the
  form post, the JSON, the rules and the database. Keep the JSON structure as is.
- The JavaScript is tested by hand. Do not add JavaScript test tooling.

**Documentation**

- Confluence does not document the optional Oracle setup; Shreyas had it
  removed. The `oracle` profile, `schema-oracle.sql` and the commented-out
  `ojdbc11` dependency still exist in the repo and README. The phrase "Oracle
  compatibility mode" stays, because it describes the H2 configuration.

## Working conventions

- **Branch:** work on `shreyas-wip`. Do not create feature branches or commit
  to `main` unless asked. `main` is behind: it has none of the order entry
  screen, skills or address lookup.
- **Commit and push only when asked.** Shreyas usually reviews first, then
  says to commit, then separately to push.
- **A finished story usually means four updates,** each on request: code,
  README, Jira status, Confluence.
- **Jira stories:** use the `provisioning-jira-story-writer` skill. Title is
  `<Stage> - <description>` with Stage one of Intake, Validation, Enrichment,
  Catalog, Hierarchy. Always include an impact analysis across all five stages
  and surface ambiguity as Open Questions; record answers in a Decisions table.
- **Do not assume answers to open questions.** Shreyas answers them inline in
  Jira ("Shreyas: ..."), then asks for the issue to be rewritten around them.

## Jira

Site `shreyasvs.atlassian.net`, project `KAN`, epic KAN-1 "Order Entry User
Interface".

- **Hierarchy limit:** Feature and Story are at the same level, so a Story
  cannot sit under a Feature. Stories go under epic KAN-1, with a "relates to"
  link to feature KAN-2 where relevant.
- **Test cases** are Subtasks under their story, titled `[Test] ACn: ...`
  (see KAN-8 to KAN-13 under KAN-5). There is no Test issue type.
- **Statuses:** Idea, Approved, In Progress, Testing, Done. "Approved" is
  reached with the transition named "Approval".
- Test results are recorded as a comment on each test case.

## Confluence

Parent page: `telecom-provisioning-demo`, page ID 65700, in Shreyas's personal
space (space ID 131074).

| Page | ID | Covers |
|---|---|---|
| Architecture and request flow | 327688 | Packages, entry points, external integrations, request flow |
| Order entry screen | 65836 | The form, Find address, order IDs, what is built so far |
| API reference | 327708 | The JSON endpoint and sample requests |
| Business rules | 131409 | Validation, enrichment, catalog, dispatch, order ID generation |
| Data model and persistence | 65730 | Connecting to H2, tables, `order_id_seq`, status lifecycle |
| Running and testing locally | 65751 | Start, browser steps, tests, manual test table, troubleshooting |

- An update replaces the whole page body, so read the page first. Shreyas edits
  pages directly, so check whether a page changed since the last edit.
- The Architecture page holds a **draw.io macro that Shreyas maintains**. Edit
  that page in HTML format and copy the macro element back unchanged. The other
  pages can be edited as Markdown.
- Confluence has no built-in flowchart rendering and this connection cannot
  upload attachments. A table laid out as a diagram was rejected; Shreyas
  wants real draw.io diagrams, imported by hand from the `.drawio` file.

## Skills in this repo

| Skill | Use it for | Note |
|---|---|---|
| `provisioning-catalog-summarizer` | Summarising the work spec catalog | Parses `data.sql` |
| `provisioning-work-orders-summarizer` | Counts by status and details of saved orders and work orders | Needs the app running; reads the live H2 database through `/h2-console` with SELECTs only |
| `provisioning-jira-story-writer` | Drafting Jira stories | Format described above |

## Things that are easy to get wrong

- **The form page must not contain the text `orderId`.**
  `OrderFormControllerTest.newOrderPage_hasNoOrderIdField` asserts this on the
  whole rendered page, including comments and scripts referenced inline.
- **Work orders are not in `data.sql`.** They exist only in the running app's
  memory. Work orders have no status of their own; the status belongs to the
  order (`RECEIVED`, `COMPLETED`, `REJECTED`, `FAILED`).
- **The H2 database cannot be reached by an external tool.** Only the app's own
  H2 console can see it (`jdbc:h2:mem:provisioning`, user `sa`, no password).
- **Nominatim often has no house number.** A match can replace "123 Main St"
  with "North Main Street". This follows the replace-all-fields decision; it was
  flagged to Shreyas and left as is.
- **Calls to the real Nominatim service** must be few, identified with a
  User-Agent or Referer, and at least a second apart.
- **Behaviours that look like bugs but are current design:** a fiber address
  never gets `INSTALL_DROP`; a TV-only order at a copper address completes with
  zero work orders; service tier, bundled flag and request type do not change
  the output; a rejected screen order still uses up an order ID.
- **Canadian postal codes work in the pipeline unchanged,** because the
  location rule reads only the last character of the zip.

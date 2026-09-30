# Telecom Provisioning Demo (Learning Sandbox)

A small, fake-but-realistic Spring Boot app that mirrors the shape of a
telecom provisioning pipeline, so you can practice connecting Claude to a
codebase and asking it to explain code and business logic — without needing
access to your real work repo.

## How this maps to your real system

| Real system step | This project |
|---|---|
| 1. Accept incoming JSON | `ProvisioningController` (`POST /api/provisioning/process`) |
| 2. Validate via IBM ODM rules | `RequestValidator` — plain Java rules standing in for ODM decision tables |
| 3. Enrich via external APIs (location, services) | `LocationEnrichmentClient`, `ServiceEnrichmentClient` — fake, deterministic "external calls" |
| 4. Work spec catalog lookup | `WorkSpecCatalogService` + `WorkSpecCatalogEntry`/`WorkSpecCatalogRepository` — Oracle-backed (H2 locally) |
| 5. Hierarchy / dispatch rules via ODM | `HierarchyRulesEngine` — plain Java rules standing in for a second ODM rule set |

`ProvisioningOrchestrator` wires all five steps together in order — that's
the single best file to point Claude at when you want it to "trace the whole
flow."

## Running it locally (no Oracle install needed)

This defaults to an in-memory H2 database running in Oracle-compatibility
mode, so you can run it with just a JDK and Maven — no Oracle instance
required.

**Prerequisites:** JDK 17+, Maven (or use the Maven wrapper if you add one).

```bash
mvn spring-boot:run
```

Then, in another terminal, try the sample requests:

```bash
curl -X POST http://localhost:8080/api/provisioning/process \
  -H "Content-Type: application/json" \
  -d @sample-requests/valid-single-dispatch.json

curl -X POST http://localhost:8080/api/provisioning/process \
  -H "Content-Type: application/json" \
  -d @sample-requests/valid-multi-dispatch.json

curl -X POST http://localhost:8080/api/provisioning/process \
  -H "Content-Type: application/json" \
  -d @sample-requests/invalid-missing-fields.json

curl -X POST http://localhost:8080/api/provisioning/process \
  -H "Content-Type: application/json" \
  -d @sample-requests/invalid-not-serviceable.json
```

You can also browse the H2 console at `http://localhost:8080/h2-console`
(JDBC URL: `jdbc:h2:mem:provisioning`, user `sa`, no password) to see the
`work_spec_catalog` table that step 4 queries.

### Using a real Oracle instance instead

If you want the full experience with real Oracle:
1. Run Oracle XE locally in Docker (search "oracle-xe docker" for an image).
2. Uncomment the `ojdbc11` dependency in `pom.xml`.
3. Run `src/main/resources/schema-oracle.sql` and then the inserts from
   `data.sql` against your Oracle instance.
4. Start the app with `-Dspring.profiles.active=oracle` and fill in real
   credentials in `application.yml`.

## Using this to practice with Claude

### Step 1: Push it to your own GitHub

```bash
cd telecom-provisioning-demo
git init
git add .
git commit -m "Initial commit: telecom provisioning demo"
```
Create a new (private, if you like) repo on github.com, then:
```bash
git remote add origin https://github.com/<your-username>/telecom-provisioning-demo.git
git branch -M main
git push -u origin main
```

### Step 2: Connect it to Claude

Pick either or both:

**A. Claude Project + GitHub sync (no terminal needed after this)**
1. In claude.ai, go to Settings → Connectors and connect GitHub if you
   haven't already, then create a new Project.
2. Use the Project's file/GitHub sync option to point at your new repo.
3. Try asking it things like:
   - *"Walk me through what happens, file by file, when a request for INTERNET and PHONE over fiber comes in."*
   - *"If I wanted to add a rule that TV requires an existing drop, which files would need to change?"*
   - *"Explain RequestValidator as if I were a new BA on the team."*

**B. Claude Code (if you have it installed)**
1. `cd` into the cloned repo.
2. Run `claude` and ask it to trace logic live, e.g.:
   *"Trace the full call path from ProvisioningController to the final response for a multi-service request, and tell me exactly where the number of dispatchable work orders gets decided."*

### Step 3: Practice the BA-style use cases from earlier

Some things worth trying once it's connected:
- Ask it to explain what would happen if you changed a rule in
  `RequestValidator` or `HierarchyRulesEngine`, *before* you make the change,
  to practice writing an impact-analysis section for a Jira story.
- Ask it to generate additional sample JSON test cases beyond the ones
  included here (e.g., an UPGRADE request, or a request with an unknown
  service code) and predict what should happen to each.
- Ask it to draft a plain-language "business rule summary" of
  `HierarchyRulesEngine`, the way you'd want an ODM rule set explained to a
  non-technical stakeholder.

## A note on realism

The validation and hierarchy rules here are deliberately simple Java
`if`-statements, not a real rules engine — the goal is to practice the
*conversation* with Claude about code and business logic, not to reproduce
IBM ODM itself. The file-level comments call out explicitly where each piece
stands in for a real-system component, so the analogy stays clear as you
explore.

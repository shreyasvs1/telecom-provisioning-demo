---
name: provisioning-jira-story-writer
description: Use this skill when drafting a Jira story or acceptance criteria for a change to the telecom provisioning pipeline (JSON intake, validation, enrichment, work spec catalog, or hierarchy/dispatch rules). Trigger on requests like "write a story for...", "write a feature for...", "draft acceptance criteria for...", or "turn this into a Jira ticket".
---

# Provisioning Pipeline Story Writer

When drafting a Jira story for this project, follow this format exactly.

## Story structure

1. **Title**: `[Stage] - short description`, where Stage is one of:
   Intake, Validation, Enrichment, Catalog, Hierarchy.
   Example: `Validation - Reject requests missing serviceAddress.zip`

2. **Description**: 2-4 sentences. State the current behavior, the
   desired behavior, and which pipeline stage(s) are affected. Always
   name the actual class involved (e.g. `RequestValidator`,
   `HierarchyRulesEngine`) if you can identify it from the code.

3. **Acceptance Criteria**: Given/When/Then format, minimum 2 scenarios:
   - One happy-path scenario
   - One edge case or failure scenario

4. **Benefit Hypothesis**: Explain in no more than 4-5 sentences, what are the tangible benefits of implementing this Story.   

5. **Impact Analysis** (always include this section): a bullet list of
   which of the 5 pipeline stages are touched by this change, and why.
   If a stage is NOT affected, say so explicitly rather than omitting it
   - this is what lets a reviewer trust the analysis is complete.

6. **Open Questions**: anything ambiguous that needs a decision before
   dev work starts. Never silently assume an answer to an ambiguous
   requirement - surface it here instead.

## Tone

Write for a BA audience first, developer audience second: lead with the
business behavior, not the implementation. Avoid describing *how* the
code should change unless specifically asked - that's the dev's call
during refinement.
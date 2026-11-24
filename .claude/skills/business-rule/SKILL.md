---
name: business-rule
description: How a business rule of SchaffBar goes through backend and portal - what is decided before any code (the contract between backend and portal, rule id and text, where it lives, when it is checked, exception and code, error text, portal behaviour, devices), then the README row, the backend check, its exception and tests, the translation and the explanation in the portal, the docs and the check in the browser; also how a rule is changed or removed, and how a rule enforced only in the portal moves to the backend. Use when adding, changing or removing a business rule, when writing the spec of a rule, when a rule "enforced only in the portal" moves to the backend, or when reviewing a change that touches a rule.
---

# A business rule from the README to the portal

A business rule is a condition every change has to respect – "an RFID reader assigned to a tool cannot be deleted"
(R-5), "only an instructor of the tool certifies for it" (TC-8). Each rule has an id (`R-5`: the letters of its
aggregate, a number) and a row in the `README.md` of its aggregate. A format of one field (a valid email address,
C-6) is a rule too, but it lives in Bean Validation and needs only part of this.

This skill is the order of the work and the decisions behind it; how each part is built is in the skills it names:

| Part | Skill (path from the repo root) |
| --- | --- |
| where a rule lives, the exception and its code, the check in entity or service | `aggregate` – `schaffbar-backend/.claude/skills/aggregate/SKILL.md` |
| a check with a second aggregate, the use case | `use-case` – `schaffbar-backend/.claude/skills/use-case/SKILL.md` |
| the error answer (`409` with `code`), endpoints and the Bruno collection, devices | `rest-api` – `schaffbar-backend/.claude/skills/rest-api/SKILL.md` |
| the backend tests on each level | `backend-testing` – `schaffbar-backend/.claude/skills/backend-testing/SKILL.md` |
| the translation of the code, the API layer | `api` – `schaffbar-portal/.claude/skills/api/SKILL.md` |
| the rule in the portal's feature model or store | `feature` – `schaffbar-portal/.claude/skills/feature/SKILL.md` |
| the explanation on the page: hint, danger zone, form error | `component`, `dialog`, `design` – `schaffbar-portal/.claude/skills/…` |
| the portal specs, the check in the browser | `testing`, `verify-ui` – `schaffbar-portal/.claude/skills/…` |

Project rules: `CLAUDE.md` (glossary, languages, git), `schaffbar-backend/CLAUDE.md`, `schaffbar-portal/CLAUDE.md`.

## 1. The contract – decided before any code

Backend and portal are built against the same decisions. They are written down first – in the spec of the task,
section "Vertrag" of `docs/specs/<name>.md` (template `docs/specs/vorlage.md`) – and
**every open point is a question for Piotr – never guessed**; the important decisions of the project were exactly
these (`docs/entscheidungen.md`). The examples show the kind of answer – they are not the decisions for R-6.

| Decision | What to write down | Example (R-6, reader names unique) |
| --- | --- | --- |
| id and rule | the next free id of the aggregate, the rule as one English sentence | `R-6` – "The name of an RFID reader is unique" |
| the exact condition | what counts as equal, what is excluded | ignoring case? a reader without a name? |
| when it is checked | which actions (create, update, a type change, a device) | on update of the name – a new reader registers itself without one |
| existing data | data that already breaks it: stays, gets cleaned, or blocks the start (a migration) | two readers with the same name today? |
| where it lives | the row of "Where a rule lives" (skill `aggregate`) | other rows of the same aggregate → the service command |
| exception and code | the class in `shared/exception/` and its `CODE` – a new one or an existing one that fits | `RfidReaderNameAlreadyUsedException`, `RFID_READER_NAME_ALREADY_USED`, `409` |
| error text | German and English for `shared.errors.<CODE>`: what blocks, what to do – no "du" or "Sie" | „Der Name wird schon von einem anderen Lesegerät verwendet. Bitte einen anderen Namen wählen.“ |
| API change | none, or a new field the portal needs to explain the rule before the action | none – the portal has the list of readers |
| portal behaviour | where the portal explains it before the action (disabled button with hint, danger zone, form error) or only shows the error of the backend | form error in „Lesegerät bearbeiten“ against the loaded list |
| devices | whether a device can hit the rule – then a text in `DeviceMessages` | no |

## 2. Backend

In this order, with the skills of the backend; run from `schaffbar-backend/core-pos/`.

1. **README row** in "Business Rules / Invariants" of the aggregate: `| R-6 | <rule> | <Enforced in> |` – the class
   that checks it (`RfidReaderService.updateRfidReader()`, `` `RfidReaderDelete` use case ``) with exception and status.
   A rule that was in "Open Rules" ("Enforced only in the portal", "Planned") moves out of there in the same change.
2. **The exception** in `shared/exception/` (skill `aggregate`, "Where a rule lives"): extends
   `BusinessRuleViolationException`, `public static final String CODE` in UPPER_SNAKE_CASE, English message with the
   ids, static factories for variants. An existing exception whose code and portal text fit is used instead.
3. **The check** where the rule lives – an entity command, a service command or a use case – as an `ensure…(…)` with
   the rule id in a comment above it and the `throw` in a `throw…` helper (skill `aggregate`, "How a check is
   written"; a second aggregate: skill `use-case`).
4. **The API change** of the contract, if there is one (a new field, a changed body, a new endpoint): controller,
   `…ApiModel` and the request in the Bruno collection (`core-pos/api/<folder>/`) – skill `rest-api`, "The contract
   with the portal". A rule that only adds a `409` changes no Bruno request: the collection holds requests, not the
   expected answers.
5. **Tests** (skill `backend-testing`): on the level where it is enforced, one test for "allowed" and one per
   rejection (exception, `code`, nothing changed); a rule whose check reads the database also an acceptance test
   `<id in lower case without dash>_<the rule as a sentence>` (`r6_readerNamesAreUnique`) with both sides; the
   controller test of each endpoint that can hit it expects `409` with the code. A device can hit it: the code in
   `DeviceMessages.BY_CODE` and a device test in `integration/device/` (skill `rest-api`).
6. **Format rule only** (one field, no other rows): a Bean Validation annotation or an own constraint on command and
   request body, the controller test `…_with<WhatIsWrong>_is400` – no exception, no code.
7. `mvn -q test` passes.

## 3. Portal

In this order, with the skills of the portal; run from `schaffbar-portal/`.

1. **The translation** `shared.errors.<CODE>` in `public/i18n/de.json` and `en.json` – the text of the contract. The
   error interceptor shows it for every action that hits the rule; nothing else is needed for that.
2. **The API change** of the contract, if there is one: `shared/api/<resource>/` (zod schema, `…Request`), the
   mapper and the feature model (skills `api`, `feature`).
3. **The explanation before the action**, where the portal can know it from data it has:
   - the decision is a function of the feature model or a computed of the store (`canDeleteRfidReader(…)` in
     `rfid-reader.model.ts`) – never in a component (skill `feature`);
   - the page shows it: a disabled button with its hint next to it, the reason in `schbar-danger-zone` instead of the
     delete button, an error on the field of a form dialog (skills `component`, `dialog`, `design`).
   When the portal cannot know it before (another person's data, a race), the error of the backend is the explanation.
4. **A rule that was enforced only in the portal** keeps its check in the portal where it explains before the action –
   the backend is now the guarantee, the portal the early hint. A pure pre-check that only duplicates the backend's
   answer may go.
5. **Specs** for the changed model, store and component (skill `testing`); `npm run check` passes.
6. **In the browser** (skill `verify-ui`): the explanation before the action, and the error message – the `409` with
   the code simulated with `route.fulfill`, nothing written to the database; desktop and phone.

## 4. Docs

- The aggregate `README.md` – the rule row (backend, step 1); a rule the portal checks for the backend stays in
  "Enforced only in the portal" until the backend enforces it.
- `docs/entscheidungen.md` – a decision of the contract that is not obvious from the code (the portal behaviour, an
  exception to a rule), with its date.
- `docs/aufgaben.md` – the finished task deleted: the row of the rule in "Regeln, die nur das Portal prüft", a closed
  gap in "Backend-Funktionen für das Portal".

## Changing or removing a rule

The same places, from the README on:

- **Changed condition** – the README row, the check, its tests; the code stays when the rule is the same rule, and
  the portal's text is checked whether it still fits.
- **Renamed code** – the exception's `CODE`, the translations (`shared.errors.<NEW>`, the old key removed), the
  device text if any, every test that names it – in one change (the codes `ACTIVE_…` became `RUNNING_…` like this).
- **Removed rule** – the row, the check, the exception (if nothing else throws it), the translation and the portal's
  explanation; the id is not given to another rule.
- **A field that came with the rule** and changes or goes – controller, `…ApiModel`, the Bruno request and the
  portal's schema together (skill `rest-api`, "The contract with the portal").

## Checklist

- [ ] Contract written down, open points asked – not guessed.
- [ ] README row with id and "Enforced in"; moved out of "Open Rules".
- [ ] Exception with `CODE` (or an existing one), the check as `ensure…` with the rule id, in its place.
- [ ] API change of the contract: controller, `…ApiModel`, Bruno request, portal schema and mapper.
- [ ] Backend tests: allowed + each rejection on its level, acceptance test if it reads the database, controller
      tests expect the code, device text and test if a device can hit it; `mvn -q test`.
- [ ] Portal: translation de/en, explanation before the action from the feature model or store, specs,
      `npm run check`, checked in the browser.
- [ ] `docs/entscheidungen.md` and `docs/aufgaben.md` as far as they are touched.

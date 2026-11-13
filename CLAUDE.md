# Schaffbar – Kassensystem

Software for the SchaffBar workshop (Werkstatt): people check in and out with an RFID tag, tools are unlocked by a
switch box only for people with a certification, and the stay and the tool usages are billed at the end of a session.
Staff run it at the counter with the portal.

## Repository

| Folder | What |
| --- | --- |
| `schaffbar-portal/` | the staff portal – Angular 22, Material M3, NgRx signal store; own `CLAUDE.md` and skills in `.claude/skills/` |
| `schaffbar-backend/core-pos/` | the backend – Spring Boot 3.5, Java 25, PostgreSQL with Flyway; one aggregate per folder in `domain/` with a `README.md` of its rules, use cases in `use_case/`; Bruno collection in `api/`, DDD overview in `docs/` |
| `infrastructure/` | `docker-compose.yml` and the database init |
| `docs/` | `ui-redesign.md` (design and every portal decision with its date), `backend-features.md` (what the portal still needs from the backend), `roadmap.md` (open tasks), setup notes (`playwright-wsl.md`, …) |
| `TODO.md` | loose tasks and ideas of the maintainer |
| `deployment.md` | deployment on the workshop server |

## Languages

- Code, identifiers, code comments, `CLAUDE.md`, skills and the aggregate READMEs: English.
- The UI is German (English as second language in `schaffbar-portal/public/i18n/`); `docs/` and `TODO.md` are German.
- Answer in the language of the request.

## Glossary

The UI speaks German, the code English – and not always word for word.

| UI (German) | Code | Notes |
| --- | --- | --- |
| Benutzer, Person | `Customer` (backend), `User` (portal) | the same person; the portal calls it `user`, the API `customers` |
| Werkstatt | workshop | |
| Sitzung, offene Sitzung | `WorkshopSession` (`OPEN` → `PAID`) | starts with the first check-in, ends when it is billed ("Sitzung beenden & abrechnen") |
| Werkstattbesuch, eingecheckt / ausgecheckt | `WorkshopUsage` (`entryTime`, `exitTime`) | one stay from the gate at the entrance to the gate at the exit; a session can have several |
| Werkzeug | `Tool` | belongs to an area (Bereich): `HOLZ`, `METALL`, `KREATIV`, `FABLAB` |
| Werkzeugnutzung, läuft | `ToolUsage` | switched on and off at the tool's switch box |
| Einheiten | units | what is billed: each started 6 minutes are one unit (`BillableDuration`), for the stay and for each tool |
| Einweisung | `ToolCertification` (`ACTIVE`, `PAUSED`, `REVOKED`) | permission of a person to use a tool; pausieren, reaktivieren, entziehen |
| Einweiser | instructor (`Tool.instructors`) | may certify others for the tool |
| Einweisung empfohlen / Pflicht | `CertificationRequirement` `YELLOW` / `RED` | only `RED` blocks the switch box without certification |
| RFID-Tag | `RfidTag` | the physical tag; id = hex string (`04A31F9C`, shown `04A3 1F9C`) |
| Leihtag / fester Tag | `RfidTagAssignmentType` `TEMPORARY` / `FIXED` | a loan tag is given back at the counter when leaving |
| Zuweisung, wartet auf Tag | `RfidTagAssignment` (`WAITING_FOR_ASSIGNMENT` → `ASSIGNED`) | requested in the portal, completed when the tag is held to the reader at the counter; only one request may wait at a time |
| RFID-Lesegerät | `RfidReader` | a device that reads tags; its type decides what it does |
| RFID-Tag-Register, -Zuweiser | `RFID_TAG_REGISTER`, `RFID_TAG_ASSIGNER` | registers new tags / completes a requested assignment (the reader at the counter) |
| Türwächter Eingang / Ausgang | `GATE_KEEPER_IN` / `GATE_KEEPER_OUT` | check-in / check-out at the gate |
| Schaltbox | `SWITCH_BOX` | the reader at a tool; unlocks it together with the WLAN relay |
| WLAN-Relais | `WlanRelaisType` (Shelly 1, 2, Pro) | switches the tool's power over HTTP |
| Altersgruppe | `AgeGroup` `UNDER_16`, `UNDER_18`, `ADULT` | computed by the backend from the date of birth |
| Vereinsmitglied | `clubMember` | |
| Übersicht | dashboard | |

## Git

- Never run `git commit` or `git push` unless explicitly asked to. Leave all changes uncommitted in the working tree.
- If a tool refuses to run on a dirty working tree (e.g. `ng update`: "Repository is not clean"), do not commit to work around it. Ask first.

## Open tasks

- The open tasks after the UI redesign, their order and how to work on them: `docs/roadmap.md`. Mark a task as done
  there (date and commit) when it is finished.

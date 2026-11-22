# Roadmap nach dem UI-Redesign (Phase 1)

Offene Aufgaben nach Phase 1 des Redesigns (`ui-redesign.md`), in der vereinbarten Reihenfolge:
**A3 → S → K (mit A1) → A2 → B → C**, danach **P** (Agenten-Pipeline). In einer neuen Session auf diese Datei
verweisen („mach mit K weiter, siehe `docs/roadmap.md`“). Erledigtes abhaken und mit Datum und Commit vermerken.

## Arbeitsweise

- Nie committen oder pushen – Änderungen bleiben im Working Tree, der Commit ist Sache von Piotr.
- Entscheidungen mit Datum festhalten: Portal in `ui-redesign.md`, Backend-Regeln in den `README.md` der
  Domain-Aggregate (`schaffbar-backend/core-pos/src/main/java/de/schaffbar/core_pos/domain/*/README.md`). Eine Regel,
  die umgesetzt ist, wandert von „Open Rules“ nach „Business Rules / Invariants“.
- Backend: Use Cases in `use_case/`, Tests mit Mockito wie `CustomerDeleteTest`, ausführen aus
  `schaffbar-backend/core-pos` mit `mvn -q test` (es gibt kein `mvnw`).
- Portal: Konventionen und Definition of Done in `schaffbar-portal/CLAUDE.md`, alle Prüfungen mit `npm run check`.
- Im Browser nichts in der Datenbank ändern: Skill `verify-ui` (Playwright mit `page.route`, alles außer `GET`
  abfangen).

## S – Skills

Wissen aus Phase 1 als Skills festhalten (`schaffbar-portal/.claude/skills/<name>/SKILL.md`, Vorbild: `api`,
`component`, `dialog`) – `CLAUDE.md` behält je Thema nur die harte Regel in einer Zeile und verweist auf den Skill.
Agents können die Skills später nutzen. Reihenfolge:

| Skill | Inhalt | Aus `CLAUDE.md` |
| --- | --- | --- |
| `design` | Designsystem und Seitenaufbau: Listenseite (Kopf, Filter-Chips + Suche, `table-card`, ⋯-Menü, leerer Zustand), Detailseite (Kopf mit Meta-Badges, Tabs, Spalten, Gefahrenzone als letzte Karte über die volle Breite), welcher Baustein wofür (Status-Badge oder Text, Strich oder Hinweis, Auswahlkarten statt Select, `timeWithDay`), Tokens und Farbregeln, Material-Overrides, Verhalten auf dem Handy (Spalten brechen um, Badges unter den Namen, kein waagerechtes Scrollen) | „Design system“, „Buttons“, Anzeige-Teil von „Missing values“ |
| `testing` | Tests im Portal: `trigger()` / `settle()` mit `HttpTestingController` (`whenStable()` hängt bei offenen Requests), Fake-Timer nur für `setInterval` / `Date` und `advanceTimersToNextTimer`, `LOCALE_ID` `de-DE`, ⋯-Menüs im Overlay testen und aufräumen, Harnesses für Formulare (Felder mit Label darüber über ihr Input finden), Store-Mocks mit Signalen | Test-Hinweise aus „Forms“ (`provideDateAdapter()`) |
| `verify-ui` | Änderung im Browser abnehmen: Dev-Server starten (`npx ng serve --port 4200` im Hintergrund), Playwright mit `page.route` – alles außer `GET` abbrechen, Zustände mit simulierten Antworten herstellen (offene Sitzung, wartender Tag, laufendes Werkzeug, neues Lesegerät), Screenshots Desktop 1440 und Handy 390, waagerechtes Scrollen prüfen (`scrollWidth > innerWidth`), Projektion von `pageHeaderMeta` prüfen | Zeile „Im Browser nichts in der Datenbank ändern“ der Arbeitsweise oben |

Stand:

- [x] `design` – 12.11.2026; `CLAUDE.md` „Buttons“ und „Design system“ gekürzt, veraltete Beispiele in `CLAUDE.md`
  und den Skills `component` und `dialog` ersetzt (`user-contact` u. a. gibt es seit der Detailseite nicht mehr)
- [x] `testing` – 12.11.2026; `CLAUDE.md` hat einen kurzen Abschnitt „Tests“, die Skills `api`, `component` und
  `dialog` verweisen darauf
- [x] `verify-ui` – 12.11.2026
- [x] Root-`CLAUDE.md`: Projekt, Repo-Plan, Sprachen, Glossar UI ↔ Code – 13.11.2026
- [x] Skill `feature` aus `api` herauslösen – 13.11.2026 (`api` 445 → 294 Zeilen, Verweise auf Store/Modell/Mapper zeigen
  auf `feature`): Aufbau eines Features (Ordner, Importregeln, eigenes Modell pro Feature,
  Commands, Mapper, Store mit `mutation` und Polling, Geschäftsregeln im Modell); `api` behält nur `shared/api`,
  `component` nur das Innere einer Komponente
- [x] Portal-`CLAUDE.md` schlank – 13.11.2026 (262 → ~150 Zeilen, `npm run check`) (~120 Zeilen): wozu das Portal da ist, Befehle, Tabelle „Aufgabe → Skill“, harte
  Regeln je eine Zeile, Definition of Done, Kommentar-Regel (nur was der Code nicht sagt, keine JSDoc, die den Namen
  wiederholt), Übersetzungen (Schlüsselschema, immer `de` und `en`, kein „du“/„Sie“); npm-Script `check` für die
  fünf Prüfungen
- [x] Memory aufräumen – 13.11.2026 (OpenAPI-Codegenerierung nach B übernommen): Kommentar-Regel steht dann im Repo,
  „Seite nach dem Artboard“ im Skill `design`, die `shared/api`-Migration ist erledigt – die drei Einträge löschen
- [x] Backend-`CLAUDE.md` – 13.11.2026: `schaffbar-backend/CLAUDE.md` (Befehle, Architektur mit Verweis auf
  `ddd-overview.md`, Regeln, Code-Stil, Tests, Definition of Done); in `ddd-overview.md` / `ddd-uebersicht.md` Links,
  fehlendes Event und erledigte TODOs korrigiert
- Die Backend-Skills (statt `backend-rule`) entstehen am Ende von K aus dem vereinheitlichten Code (Entscheidung
  13.11.2026).

Später, wenn mehr Formulare kommen: `forms` (Signal Forms, kompakte Felder, Zähler „N ungespeicherte Änderungen“,
„Bitte N Felder prüfen“) – teils schon im Skill `dialog`.

## P – Agenten-Pipeline

Ziel: Für ein Issue, das Piotr annimmt (z. B. per Label), läuft der Weg von der Anforderung bis zum Pull Request mit
Agents. Skills sind das Wissen („wie macht man X hier“), Agents die Rollen mit Auftrag, Werkzeugen und Rechten –
mehrere Rollen nutzen dieselben Skills. Voraussetzung ist S.

Rollen (Subagents in `.claude/agents/*.md`), bewusst wenige – jede Übergabe verliert Wissen:

| Rolle | Macht | Lädt | Ergebnis |
| --- | --- | --- | --- |
| Spec (Anforderung + Design) | Issue, Prototyp-Artboard, Backend und `docs/` lesen, Lücken und Widersprüche finden, Fragen stellen statt raten | `design`, `feature`, Glossar | Spezifikation mit offenen Fragen und Vorschlägen |
| Implementation (mit Doku) | Code, Specs und Prüfungen nach den Skills, Entscheidungen in `ui-redesign.md` / READMEs | `feature`, `api`, `component`, `dialog`, `design`, `testing`, Backend-Skills aus K, `business-rule` | Branch mit Änderungen |
| Verify | im Browser abnehmen, Zustände simulieren | `verify-ui` | Bericht mit Screenshots |
| Review | den Diff gegen `CLAUDE.md` und die Skills prüfen – ohne die Begründung des Autors | alle Skills | Befunde |

Haltepunkte für Piotr: nach der Spec (Fragen beantworten, freigeben) und vor dem Merge. Die wertvollsten Momente im
Redesign waren Entscheidungen (Banner trotz WS-5, Leihtag-Filter, Löschkonzept) – die darf kein Agent allein treffen.
Übergaben nur über Dateien, Issue und PR, nicht über Chat-Verlauf.

Stufen:

1. [ ] Rollen als Subagents beschreiben (`.claude/agents/spec.md`, `implement.md`, `verify.md`, `review.md`).
2. [ ] Lokal an einem kleinen, klar abgegrenzten Issue ausprobieren – eine Regel aus A2.
3. [ ] Git-Regel für die Pipeline: ein Agent arbeitet auf seinem eigenen Branch und öffnet einen PR, `main` bleibt bei
   Piotr (Ausnahme zu „nie committen“ in `CLAUDE.md`, nur für die Pipeline).
4. [ ] Rechte: Prüfbefehle ohne Rückfrage in `.claude/settings.json`, Push und Merge nicht.
5. [ ] CI mit Backend und Testdaten (Docker Compose mit Seed) – Verify braucht ein laufendes Backend; lokal lief es
   gegen die echte Datenbank mit abgefangenen Schreibzugriffen, in CI geht das nicht.
6. [ ] GitHub: Claude Code Action, ausgelöst durch ein Label am Issue; Ergebnisse als Kommentare und PR.

## K – Backend-Konsistenz

Das Backend ist nicht so einheitlich wie das Portal. Agents kopieren, was sie im Code vorfinden – deshalb zuerst
vereinheitlichen, dann die Skills aus dem Ergebnis schreiben (wie im Portal: Piloten, Regeln, dann Skill `api`).
Entscheidung 13.11.2026.

1. [x] **Bestandsaufnahme** (13.11.2026) in `backend-consistency.md`: Funde nach Kategorien (Struktur, Namen,
   Exceptions und Fehlerantworten, Transaktionen und Events, Validierung, API-Modelle, Tests) – heute, Vorschlag,
   zu entscheiden.
2. [x] **Konventionen entscheiden** (13.11.2026) – mit Datum in `backend-consistency.md`.
3. [x] **Pilot** an `rfid_reader` (15.11.2026): Exceptions mit Basisklasse und Code, `@Query` / `@Command`,
   `RfidReaderReadFacade`, Controller aufgeräumt (Filter liefert Liste), `MacAddress` prüft das Format, Tests;
   Unterbau `ApiExceptionHandler`, `OutboxEventStore`, ArchUnit mit Freeze-Store, Spotless, Skill `aggregate`; Portal
   zeigt den übersetzten Code. Details: `backend-consistency.md`, Abschnitt „Pilot (K3)“.
4. [ ] **Ausrollen** Aggregat für Aggregat – die Arbeitsliste war der Freeze-Store `core-pos/archunit_store/` (leer
   und entfernt am 22.11.2026, alle ArchUnit-Regeln streng);
   dazu der `DeviceController` (E4) und das Dashboard als Read Facades (E17); die Tests des Aggregats nach dem
   Skill `backend-testing` (Abschnitt „Not everywhere yet“). Reihenfolge von außen nach innen (16.11.2026) – wer
   wenig aufruft, zuerst; der Werkstattablauf hängt an allem und kommt zuletzt:
   - [x] `rfid_tag` – 16.11.2026: ohne `CreateRfidTagCommand` (E1), `RfidTagReadFacade`, Use Cases `RfidTagCreate` /
     `RfidTagDelete`; RT-3 (zugewiesener Tag nicht löschbar) jetzt im Backend, neu RT-5 (Tag nur einmal registriert);
     Codes `RFID_TAG_ASSIGNED_TO_CUSTOMER`, `RFID_TAG_ALREADY_REGISTERED` im Portal übersetzt; Akzeptanztest
   - [x] `rfid_tag_assignment` mit Historie – 16.11.2026: Historie als innere Entity (E16), jede Änderung mit Event
     (E26, `RFID_TAG_UNASSIGNED` trägt den Historien-Eintrag); fünf Exceptions mit Code statt `RuntimeException`, TA-4
     in der Entity; Read Facades für Zuweisungen und Historie; die drei Tag-Use-Cases von `customer` gleich mit;
     Befehle antworten `204`; Akzeptanztest
   - [x] `customer` mit seinen Use Cases – 17.11.2026: `CustomerReadFacade`, `@Query` / `@Command`; Kontakt und
     Adresse heißen `changeContact` / `changeAddress` wie ihre Events (Use Cases `CustomerChangeContact` /
     `CustomerChangeAddress`, Pfade unverändert); C-6 bis C-8 jetzt auch im Backend (`@Email`, `@Size`, eigener
     Constraint `@DateOfBirth`, `400`); C-5 antwortet `409` mit `CUSTOMER_HAS_OPEN_WORKSHOP_SESSION` statt `500`
     (im Portal übersetzt); Befehle antworten `204`; Akzeptanztest
   - [x] `tool` – 18.11.2026, in sechs kleinen Schritten: sechs Exceptions mit Code statt `500` (im Portal
     übersetzt); `id` in `UpdateToolCommand` / `SetWlanRelaisCommand`, T-8 in der Entity; `@Query` / `@Command`,
     `ToolReadFacade`; T-14 bis T-17 jetzt im Backend (neu `TOOL_IN_USE`, eigener Constraint `@ToolDescription`,
     Namen ohne Unterschied von Groß- und Kleinschreibung); `CustomerDelete` holt nur die Werkzeuge der Person;
     Tippfehler in der Start-URL von `SHELLY_2` behoben (gespeicherte Werkzeuge behalten den alten Wert, bis das
     Relais neu gesetzt wird); Akzeptanztest
   - [x] `tool_certification` – 18.11.2026, in sechs kleinen Schritten: vier Exceptions mit Code (im Portal
     übersetzt); Entity-, Service- und Controller-Tests, `ToolCertificationReadFacade`, `@Query` / `@Command`; Use
     Cases `ToolCertificationCreate` / `…Pause` / `…Reactivate` / `…Revoke` / `…Delete`; eine fehlende Einweisung
     antwortet `404` statt `CUSTOMER_NOT_CERTIFIED_FOR_TOOL`; TC-4 jetzt „alles oder nichts“ (E11, `204` statt
     `BatchCertificationResult`); TC-5 im Backend (neu `CUSTOMER_IS_INSTRUCTOR`); TC-8 (nur Einweiser weisen ein)
     im README festgehalten; Akzeptanztest
   - [x] Werkstattablauf – 22.11.2026, in 14 kleinen Schritten: Use Cases `WorkshopUsageStart` / `…Stop`,
     `WorkshopSessionClose`, `ToolUsageStart` / `…Stop` / `…Toggle` (Schaltbox), Exceptions mit Code;
     `DeviceController` nach `web/device/` (E4) nur mit Use Cases und `DeviceReadFacade`, Geräte bekommen immer `200`;
     Übersicht als eine `DashboardReadFacade` in `web/dashboard/` (E17, Pfade `/api/v1/dashboard/…`, Portal
     umgestellt); `WlanRelaisType` mit Protokoll, Pfad und Befehlen, die Befehle nicht mehr pro Werkzeug gespeichert
     (V8), T-23 „Kein WLAN-Relais“; Freeze-Store entfernt, `TestArchitectureTest` (Stubs nur in `given…`), API-Antworten
     ohne Value Objects (E27); Akzeptanztests `WorkshopFlowAcceptanceTest` (Einchecken bis Abrechnen),
     `WorkshopSessionAcceptanceTest` und je Lesegerät in `integration/device/`
     - [ ] ganz zum Schluss: partielle Unique-Indizes in einer Flyway-Migration, damit zwei gleichzeitige Anfragen
       (Lesegerät meldet einen Tag doppelt) die Regeln nicht umgehen – eine laufende `workshop_usage` pro Person
       (WU-1), eine offene `workshop_session` pro Person (WS-1), bei `tool_usage` TU-3 und TU-9. Vorher prüft der
       Maintainer die echte Datenbank auf Duplikate (sonst scheitert die Migration beim Start); Akzeptanztest dazu aus dem Ergebnis (`schaffbar-backend/.claude/skills/`), voraussichtlich: Aggregat und Use Case,
   Fehlerbehandlung, Backend-Tests, API-Vertrag mit dem Portal; zum Schluss `business-rule` für Portal und Backend
   (`.claude/skills/` im Repo-Root): eine Regel von der README über Backend-Prüfung, Exception und Test bis zur
   Erklärung im Portal.
   - [x] `backend-testing` – 16.11.2026: Testebenen, Benennung, Integrationstests mit Testcontainers und
     `SchaffbarFixture`, Pilot `RfidReaderAcceptanceTest`

Danach A2 mit den Skills – vielleicht als erster Versuch der Pipeline (P).

## A – Backend: Regeln und Fehlermeldungen

### A3 – Löschkonzept für Benutzer ✅

Erledigt am 11.11.2026 (Commit `1b8ac72`). `CustomerDelete`: offene Sitzung sperrt, ein zugewiesener Tag wird frei,
eine wartende Anfrage abgebrochen (`RFID_TAG_ASSIGNMENT_CANCELLED`), eigene Einweisungen und die Rolle als Einweiser
werden gelöscht, bezahlte Sitzungen, Nutzungen und die Tag-Historie bleiben (customer-README C-4, C-5, C-9, C-11).

Das Löschen der Einweisungen prüft seit 18.11.2026 der Akzeptanztest `c9_c11` in `CustomerAcceptanceTest` gegen eine
echte Datenbank.

### A2 – Regeln, die heute nur das Portal prüft, im Backend erzwingen

Quelle: Abschnitte „Enforced only in the portal“ der READMEs. Jede Regel im passenden Use Case prüfen, mit einer
eigenen Exception ablehnen (Muster: `CustomerHasOpenWorkshopSessionException`, `RfidReaderHasToolAssignedException`)
und einen Test schreiben. Bis A1 kommen die Exceptions noch als `500` an.

| # | Regel | Wo |
| --- | --- | --- |
| R-4 | Socket-Name nur `a–z`, `0–9`, `-` | nur bei Änderung prüfen – bestehende Namen wie „Socket 43“ bleiben gültig (wie im Portal) |
| R-6 | Namen von Lesegeräten eindeutig (neu, auch im Portal noch nicht) | `RfidReaderUpdate`, danach Duplikat-Prüfung im Dialog „Lesegerät bearbeiten“ |

Erledigt im Rollout (K4): RT-3 (16.11.2026), C-6 bis C-8 (17.11.2026), T-14 bis T-17 (18.11.2026), TC-5
(18.11.2026).

### A1 – Problems API (Teil von K) – Grundlage erledigt

Erledigt am 15.11.2026 mit dem Pilot (K3):

- Backend: `ApiExceptionHandler` mit `ProblemDetail` (RFC 9457) – `404` / `409` mit stabilem `code`, `400`, `500`
  ohne Details (E7, E8). Exceptions von `rfid_reader` haben schon eine Basisklasse und einen Code.
- Portal: der Fehler-Interceptor zeigt die Übersetzung `shared.errors.<CODE>`, sonst die allgemeine Meldung.

Alle Exceptions haben eine Basisklasse und einen Code (zuletzt `NoCustomerAssigned`, 21.11.2026). Noch offen: nach dem Neustart des Backends einmal gegen das echte Backend prüfen. Danach werden reine Vorab-Prüfungen im
Portal optional (z. B. `getToolsByRfidReader` vor dem Typwechsel eines Lesegeräts, `canDeleteUser` in der
Benutzerliste) – behalten, wo sie den Button schon vorher erklären.

## B – Frontend ohne Backend

| Aufgabe | Notiz |
| --- | --- |
| `UserMultiSelect` | `tool-instructors` an `tool-certifications` angleichen (Suche im ganzen Namen, `<button matChipRemove>`), dann beide zu einer Komponente zusammenführen – Details in `ui-redesign.md`, „Spätere Aufgaben“ |
| Erfolgsmeldungen | Snackbar nach dem Speichern („Bandsäge gespeichert“); Fehler zeigt der Interceptor schon |
| Bestätigungsdialoge | Titel als Frage, Folgen erklären, bei Löschen Häkchen „nicht rückgängig“ (Prototyp `UserDeleteDialog`) |
| Dunkel-Modus | Tokens sind mit `light-dark()` vorbereitet, Umschalter fehlt, alle Seiten prüfen |
| Tab-Leiste unten auf dem Handy | statt Menü-Button, Prototyp „Übersicht – Mobil“ |
| Sprachumschalter DE/EN | in der Seitenleiste |
| API-Code generieren | später gewünscht (Entscheidung 03.11.2026): OpenAPI aus dem Backend (springdoc), daraus Schemas und Services (orval oder hey-api) – ersetzt die handgeschriebenen `*.api-model.ts`; braucht Backend-Arbeit |
| `ui-redesign.md` aufräumen | Abschnitt „Noch nicht umgesetzt“ ist veraltet (altes Theme, alte Tabs, rote Löschen-Buttons) |

## C – Backend-Funktionen für das Portal

Nach Feature gruppiert mit Beschreibung und Backend-Bedarf: **`backend-features.md`**. Vorgeschlagene Reihenfolge:

1. Online-Status der Lesegeräte (Heartbeat) – davon hängen Übersicht, Lesegeräte und Werkzeug-Hardware ab.
2. Tag-Anfrage abbrechen (Endpunkt; die Service-Methode gibt es seit A3) und Aufräumen wartender Anfragen – eine
   vergessene Anfrage blockiert alle anderen (TA-3).
3. Der Rest nach Bedarf: Kennzahlen der Übersicht, Betrag und Zahlungsart, Leihtag-Nummer, Lese-Protokoll, …

Fachlich zu klären, bevor es losgeht: Zahlungsart, Preise und Rabatte, ob laufende Werkzeuge beim Abrechnen
ausgeschaltet werden, ob es einen Zustand „Werkstatt geöffnet“ gibt.

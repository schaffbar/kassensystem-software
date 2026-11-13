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
3. [ ] **Pilot** an einem kleinen Aggregat (z. B. `rfid_reader`): alles auf den Zielzustand, inklusive Problems API (A1)
   für seine Exceptions und Tests.
4. [ ] **Ausrollen** Aggregat für Aggregat.
5. [ ] **Skills** aus dem Ergebnis (`schaffbar-backend/.claude/skills/`), voraussichtlich: Aggregat und Use Case,
   Fehlerbehandlung, Backend-Tests, API-Vertrag mit dem Portal; zum Schluss `business-rule` für Portal und Backend
   (`.claude/skills/` im Repo-Root): eine Regel von der README über Backend-Prüfung, Exception und Test bis zur
   Erklärung im Portal.

Danach A2 mit den Skills – vielleicht als erster Versuch der Pipeline (P).

## A – Backend: Regeln und Fehlermeldungen

### A3 – Löschkonzept für Benutzer ✅

Erledigt am 11.11.2026 (Commit `1b8ac72`). `CustomerDelete`: offene Sitzung sperrt, ein zugewiesener Tag wird frei,
eine wartende Anfrage abgebrochen (`RFID_TAG_ASSIGNMENT_CANCELLED`), eigene Einweisungen und die Rolle als Einweiser
werden gelöscht, bezahlte Sitzungen, Nutzungen und die Tag-Historie bleiben (customer-README C-4, C-5, C-9, C-11).

Noch offen daraus: die neue Methode `ToolCertificationRepository.deleteAllByCustomerId` ist nur mit Mocks getestet –
einmal mit laufendem Backend einen Benutzer mit Einweisungen löschen.

### A2 – Regeln, die heute nur das Portal prüft, im Backend erzwingen

Quelle: Abschnitte „Enforced only in the portal“ der READMEs. Jede Regel im passenden Use Case prüfen, mit einer
eigenen Exception ablehnen (Muster: `CustomerHasOpenWorkshopSessionException`, `RfidReaderHasToolAssignedException`)
und einen Test schreiben. Bis A1 kommen die Exceptions noch als `500` an.

| # | Regel | Wo |
| --- | --- | --- |
| T-17 | Werkzeug nicht löschen, solange es läuft | `ToolDelete` – laufende Nutzung über `ToolUsageService` |
| T-14 | Schaltbox nicht trennen, solange das Werkzeug läuft | `ToolClearRfidReader` |
| T-15 | Werkzeugnamen ohne Unterschied von Groß- und Kleinschreibung eindeutig | `ToolRepository.findByName` → case-insensitive, `ToolCreate` / `ToolUpdate` |
| T-16 | Beschreibung höchstens 255 Zeichen | `@Size(max = 255)` an den Commands statt Datenbankfehler |
| RT-3 | Zugewiesenen Tag nicht löschen | `RfidTagService.deleteRfidTag` (TODO im Code), `409` |
| TC-5 | Einweiser eines Werkzeugs nicht dafür einweisen | `CertifyCustomersForTool` |
| C-6 | E-Mail-Format | `@Email` an den Customer-Commands |
| C-7 | Postleitzahl genau 5 Zeichen | `@Size(min = 5, max = 5)` (oder `@Pattern`) |
| C-8 | Geburtsdatum in den letzten 100 Jahren und vor dem laufenden Jahr | eigene Validierung, gleiche Grenzen wie `dateOfBirthRange` im Portal |
| R-4 | Socket-Name nur `a–z`, `0–9`, `-` | nur bei Änderung prüfen – bestehende Namen wie „Socket 43“ bleiben gültig (wie im Portal) |
| R-6 | Namen von Lesegeräten eindeutig (neu, auch im Portal noch nicht) | `RfidReaderUpdate`, danach Duplikat-Prüfung im Dialog „Lesegerät bearbeiten“ |

Vorher klären: C-6 bis C-8 lehnen bestehende Datensätze nicht ab, die die Regel verletzen – nur neue und geänderte.

### A1 – Problems API (wird Teil von K)

- Backend: `@ControllerAdvice` mit `ProblemDetail` (RFC 9457). Fachliche Exceptions als `409` / `400` mit
  verständlichem `detail` statt `500` – alle aus A2 und die vorhandenen (`ToolNameAlreadyUsedException`,
  `RfidReaderHasToolAssignedException`, `CustomerHasOpenWorkshopSessionException`, …). Meldungen auf Deutsch oder ein
  stabiler Fehlercode, den das Portal übersetzt – entscheiden.
- Portal: der Fehler-Interceptor zeigt `detail` (oder den übersetzten Code) statt der allgemeinen Meldung.
- Danach werden reine Vorab-Prüfungen im Portal optional (z. B. `getToolsByRfidReader` vor dem Typwechsel eines
  Lesegeräts, `canDeleteUser` in der Benutzerliste) – behalten, wo sie den Button schon vorher erklären.

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

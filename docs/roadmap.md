# Roadmap nach dem UI-Redesign (Phase 1)

Offene Aufgaben nach Phase 1 des Redesigns (`ui-redesign.md`), in der vereinbarten Reihenfolge:
**A3 → A2 → A1 → B → C**. In einer neuen Session auf diese Datei verweisen („mach mit A2 weiter, siehe
`docs/roadmap.md`“). Erledigtes abhaken und mit Datum und Commit vermerken.

## Arbeitsweise

- Nie committen oder pushen – Änderungen bleiben im Working Tree, der Commit ist Sache von Piotr.
- Entscheidungen mit Datum festhalten: Portal in `ui-redesign.md`, Backend-Regeln in den `README.md` der
  Domain-Aggregate (`schaffbar-backend/core-pos/src/main/java/de/schaffbar/core_pos/domain/*/README.md`). Eine Regel,
  die umgesetzt ist, wandert von „Open Rules“ nach „Business Rules / Invariants“.
- Backend: Use Cases in `use_case/`, Tests mit Mockito wie `CustomerDeleteTest`, ausführen aus
  `schaffbar-backend/core-pos` mit `mvn -q test` (es gibt kein `mvnw`).
- Portal: Konventionen in `schaffbar-portal/CLAUDE.md`, Prüfungen `npx ng test --watch=false`, `npx ng lint`,
  `npx prettier --check src public`, `npx tsc -p tsconfig.spec.json --noEmit`, `npx ng build`.
- Im Browser nichts in der Datenbank ändern: Playwright mit `page.route`, alles außer `GET` abfangen.

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

### A1 – Problems API

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
| `ui-redesign.md` aufräumen | Abschnitt „Noch nicht umgesetzt“ ist veraltet (altes Theme, alte Tabs, rote Löschen-Buttons) |

## C – Backend-Funktionen für das Portal

Nach Feature gruppiert mit Beschreibung und Backend-Bedarf: **`backend-features.md`**. Vorgeschlagene Reihenfolge:

1. Online-Status der Lesegeräte (Heartbeat) – davon hängen Übersicht, Lesegeräte und Werkzeug-Hardware ab.
2. Tag-Anfrage abbrechen (Endpunkt; die Service-Methode gibt es seit A3) und Aufräumen wartender Anfragen – eine
   vergessene Anfrage blockiert alle anderen (TA-3).
3. Der Rest nach Bedarf: Kennzahlen der Übersicht, Betrag und Zahlungsart, Leihtag-Nummer, Lese-Protokoll, …

Fachlich zu klären, bevor es losgeht: Zahlungsart, Preise und Rabatte, ob laufende Werkzeuge beim Abrechnen
ausgeschaltet werden, ob es einen Zustand „Werkstatt geöffnet“ gibt.

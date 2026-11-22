# Backend-Konsistenz

Bestandsaufnahme des Backends (`schaffbar-backend/core-pos`) für Phase K der Roadmap, Stand 13.11.2026. Nur gelesen,
nichts geändert. Jede Kategorie hat drei Teile: **Heute** (mit Zahlen und Beispielen), **Vorschlag** (Zielzustand)
und **Zu entscheiden** (Fragen mit Nummer `E1`, `E2`, … – gesammelt am Ende mit Empfehlung).

Ziel: ein Backend, in dem jedes Aggregat, jeder Use Case und jede Exception gleich gebaut ist – damit Agents das
Richtige kopieren und die Backend-Skills (K5) beschreiben können, was im Code steht.

## Was schon einheitlich ist

Das bleibt so und kommt so in die Skills:

- Ein Ordner pro Aggregat in `domain/`, mit `README.md` (Regeln, Events), `web/` für Controller und API-Modell.
- Repositories sind überall package-private – nur der eigene Service greift auf sie zu.
- Ids als Value Objects (`CustomerId`, `ToolId`, …) in Signaturen und Views, Entities speichern den rohen Wert.
- Alle 10 Entities: `@Version`, statische Factory `of(…)`, Command-Methoden geben `List<SchaffbarEvent>` zurück.
- Alle Services: `@Service` + `@Validated` + `@RequiredArgsConstructor`, Abhängigkeiten als `private final @NonNull`.
- Service-Abfragen heißen `get…` (29 Methoden, keine Ausnahme).
- Alle Use Cases haben genau eine öffentliche Methode `process(…)`.
- API-Modelle: Records, MapStruct-`MAPPER`, `to…Command` für Request-Bodies.
- Create-Endpunkte antworten mit `201` und `Location`-Header ohne Body.

## 1. Struktur

### Heute

- Kein `*Commands` in `rfid_tag_assignment`, `rfid_tag_assignment_history`, `workshop_session`, `workshop_usage` – die
  übrigen 6 Aggregate haben eins.
- `tool_usage` und `workshop_usage` haben keinen Controller; sie werden über den `DeviceController` (Gerät an der Tür,
  Schaltbox) und die Sitzung gelesen.
- `domain/dashboard/` hat kein `README.md`. Seine Services rufen andere Services auf – als Lese-Modell erlaubt
  (`schaffbar-backend/CLAUDE.md`).
- Logging: `ToolDashboardService`, `WorkshopDashboardService` und `RfidTagAssignmentHistoryService` ohne `@Slf4j`,
  alle anderen Services mit.
- `device/` (Controller 381 Zeilen + `DeviceApiModel`) enthält Fachlogik, die sonst in Use Cases steht (siehe 2).

### Vorschlag

- Jedes Aggregat hat denselben Satz Dateien: Entity, `*Service`, `*Repository`, `*Views`, `*EventFactory`,
  `*PayloadMapper`, `README.md`; `*Commands` nur, wenn ein Command mehr als ein, zwei Value Objects trägt (E1);
  `web/` nur, wenn das Portal es aufruft.
- `dashboard/README.md`: was die Lese-Modelle zeigen und woraus.
- Logging einheitlich: jeder Service und Use Case mit `@Slf4j`, eine Zeile `info` pro Änderung (wie heute in den
  meisten Services).

### Zu entscheiden

- **E1** – `*Commands` Pflicht für jedes Aggregat mit Änderungen, auch wenn ein Command nur eine Id trägt?

## 2. Use Cases und Controller

### Heute

- 31 Use Cases in `use_case/`, zwei Namensmuster:
  - `<Aggregat><Aktion>` (21): `CustomerCreate`, `CustomerDelete`, `CustomerAssignRfidTag`, `RfidReaderChangeType`,
    `ToolAddInstructors`, `ToolSetWlanRelais`, …
  - `<Aktion><Objekt>` (10): `CertifyCustomersForTool`, `CloseSession`, `DeleteToolCertification`,
    `PauseToolCertification`, `ReactivateToolCertification`, `RevokeToolCertification`, `EnterWorkshop`,
    `LeaveWorkshop`, `StartToolUsage`, `StopToolUsage`.
- Felder im Controller: meist wie die Klasse (`customerCreate`), vier mit Suffix `UseCase` (`closeSessionUseCase`,
  `assignRfidTagUseCase`, `requestRfidTagAssignmentUseCase`, `unassignRfidTagUseCase`).
- 12 Use Cases haben nur eine Abhängigkeit – den eigenen Service – und reichen nur weiter (`CustomerCreate`,
  `CustomerUpdate*`, `RfidReaderCreate`, `RfidReaderUpdate`, `ToolCreate`, `ToolUpdate`, `ToolSet/ClearWlanRelais`,
  `ToolClearRfidReader`, `StopToolUsage`).
- Änderungen laufen in 6 von 7 Controllern über Use Cases; nur `RfidTagController` ruft für Anlegen und Löschen
  direkt `RfidTagService`. Die Regel in `schaffbar-backend/CLAUDE.md` („Controller ruft den Service des eigenen
  Aggregats“) erlaubt beides; `TODO.md` will „alle Mutationen in Use Cases“.
- `DeviceController`:
  - Fachlogik im Controller (welcher Lesegerät-Typ was auslöst, Sitzung anlegen nach dem Betreten).
  - legt Lesegeräte und Tags direkt über die Services an – an `RfidReaderCreate` vorbei.
  - eigener Fehlervertrag: Antwort `200` mit Fehler-Payload und deutschen Texten für das Display des Geräts.
  - zwei `RuntimeException` (siehe 4).
- REST-Details:
  - Query-Parameter gemischt: `customer-id`, `rfid-tag-id` (kebab) neben `macAddress`, `rfidReaderId`, `customerId`
    (camel).
  - Create-Endpunkte: `ResponseEntity<String>` (Customer, RfidReader, RfidTag) neben `ResponseEntity<Void>` (Tool) –
    alle ohne Body.
  - `getRfidReaders` gibt `ResponseEntity<?>` zurück.
  - `RfidTagController`: Pfad-Variable als roher `String` statt `RfidTagId` (TODO in `RfidTagCommands`).
  - `@Valid` an Pfad-Variablen mal da, mal nicht.
  - Der Unassign-Endpunkt heißt im Controller `assignRfidTag`.
  - `WorkshopSessionController`: `/{customerId}/active` – TODOs im Code wollen Query-Parameter bzw. die Session-Id.

### Vorschlag

- Ein Namensmuster für alle Use Cases: `<Aggregat><Aktion>` (die Mehrheit; im Ordner nach Aggregat sortiert):
  `PauseToolCertification` → `ToolCertificationPause`, `CloseSession` → `WorkshopSessionClose`, `EnterWorkshop` →
  `WorkshopUsageStart`. Die genauen Namen der 10 kommen mit E3. Controller-Feld = Klassenname in camelCase, ohne
  Suffix.
- **Jede Änderung geht über einen Use Case, jede Abfrage direkt an den Service.** Auch reine Weiterleitungen – ein
  fester Ort, an dem später eine Regel mit einem zweiten Aggregat dazukommt (A2), und ein Muster für Agents.
- `DeviceController` dünn: je Geräte-Endpunkt ein Use Case (`DeviceInit`, …), der die vorhandenen Use Cases nutzt
  (`RfidReaderCreate`, `WorkshopUsageStart`, `ToolUsageStart`, …). Der Vertrag mit der Firmware bleibt (Status, Payload,
  deutsche Texte), aber Fehler kommen als Exceptions und werden an einer Stelle in die Geräte-Antwort übersetzt.
- REST: Query-Parameter in camelCase (Spring-Standard, wie die JSON-Felder), Create gibt `ResponseEntity<Void>`
  zurück, typisierte Ids in allen Pfaden, `@Valid` überall gleich, Methodennamen wie die Aktion. Pfadänderungen
  (`customer-id`, `/active`) sind Vertragsänderungen – nur zusammen mit dem Portal (`shared/api`), im Rollout des
  Aggregats: `GET /rfid-tag-assignments?customerId=…`, `GET /workshop-sessions?customerId=…&status=OPEN`,
  `PUT /workshop-sessions/{workshopSessionId}/close`. Betroffen im Portal: `rfid-tag-assignments.api-service.ts`,
  `workshop-sessions.api-service.ts`; dazu die Bruno-Collection.

### Zu entscheiden

- **E2** – Jede Änderung über einen Use Case (auch reine Weiterleitungen) – oder nur, wenn ein zweites Aggregat
  beteiligt ist?
- **E3** – Namensmuster der Use Cases: `<Aggregat><Aktion>` (`ToolCertificationPause`) oder `<Aktion><Objekt>`
  (`PauseToolCertification`)? Umbenennen betrifft 10 bzw. 21 Klassen.
- **E4** – Device-API: Vertrag mit der Firmware unverändert lassen und nur die Logik in Use Cases ziehen?
- **E5** – REST aufräumen (Query-Parameter camelCase, `/active`): jetzt im Pilot und Rollout, oder erst mit OpenAPI
  (B) zusammen, weil das Portal ohnehin mitziehen muss?

## 3. Namen

### Heute

- „Open“ für Sitzung und Werkstattbesuch (`getOpenWorkshopSession`, Status `OPEN`), „Active“ für Werkzeugnutzung
  und im API-Pfad (`/workshop-sessions/{customerId}/active`), einmal `getActiveWorkshopSession`. TODO im
  `WorkshopSessionService`.
- Entity-Methoden `unassignEvents` / `cancelEvents` in `RfidTagAssignment` (TODO: Suffix weg).
- DTOs: `*ApiDto` (14), `*RequestBody` (19), `*Response` (3, nur Device), `BatchCertificationResult`.
- Tippfehler `NoWaitingAssingmentException`.
- TODOs zu Umbenennungen, die gespeicherte Daten betreffen: `WorkshopUsage` → `WorkshopSlot`,
  `WORKSHOP_USAGE_ENTERED` → `WORKSHOP_ENTERED`.

### Vorschlag

- „Open“ für alles ohne Ende (Sitzung, Werkstattbesuch, Werkzeugnutzung) – passt zum Status `OPEN`; im Portal heißt
  die laufende Werkzeugnutzung weiter „läuft“.
- Entity-Command-Methoden heißen wie die Aktion (`unassign`, `cancel`).
- `*ApiDto` / `*RequestBody` bleiben (der Portal-Skill `api` baut darauf); `BatchCertificationResult` →
  `BatchCertificationResultApiDto`; Device bleibt bei `*Response` (eigener Vertrag).
- Event-Typen und `WorkshopUsage` **nicht** umbenennen: Event-Typen stehen in der Outbox-Tabelle, das Glossar kennt
  `WorkshopUsage`.

### Zu entscheiden

- **E6** – „Open“ überall (auch Werkzeugnutzung)?

## 4. Exceptions und Fehlerantworten

### Heute

- 28 Exception-Klassen in `shared/exception/`, Meldungen meist englisch mit Ids, öffentliche Konstruktoren. Factories
  nur in `ResourceNotFoundException` und `RfidReaderHasToolAssignedException`.
- `NoWaitingAssingmentException`: deutscher Platzhalter „RFID nicht zugewiesen“ (TODO).
- 19 × `new RuntimeException(…)`: `RfidTagAssignmentService` ×5, `WorkshopSessionService` ×2, Repositories „more than
  one …“ ×6, `EnterWorkshop`, `WorkshopDashboardService`, `DeviceController` ×2, weitere mit `TODO: error`.
- `IllegalArgumentException` in Value Objects (Ids, `MacAddress`), `CustomerApiModel` und `BillableDuration`.
- Kein `@ControllerAdvice`: jede Exception erreicht das Portal als `500` – auch „nicht gefunden“ und Regelverstöße.
  Das Portal prüft deshalb vieles vorab (A2).

### Vorschlag

- Drei Arten von Fehlern, an der Klasse erkennbar:

  | Art | Beispiel | Basisklasse | HTTP |
  | --- | --- | --- | --- |
  | gibt es nicht | Kunde mit Id fehlt | `ResourceNotFoundException` (gibt es schon) | `404` |
  | Regel verletzt | offene Sitzung beim Löschen | neu: `BusinessRuleViolationException` | `409` |
  | Eingabe ungültig | Bean Validation, Value Object | `ConstraintViolationException`, `MethodArgumentNotValidException`, `IllegalArgumentException` | `400` |

  Alles andere ist ein Programmfehler → `500`.
- Ein `@RestControllerAdvice` (nur für `/api/v1`) übersetzt die Basisklassen in `ProblemDetail` (RFC 9457) mit
  stabilem Fehlercode (`code: "CUSTOMER_HAS_OPEN_WORKSHOP_SESSION"`), englischem `detail` und den Ids als
  Properties. Das Portal übersetzt den Code (`de.json` / `en.json`) – Texte bleiben dort, wo die Sprachen sind.
- „Kann nicht sein“-Fälle (zwei offene Sitzungen, keine Sitzung direkt nach dem Betreten) → `IllegalStateException`
  mit Ids: ein Programm- oder Datenfehler, keine eigene Klasse.
- Erwartbare Fälle mit `RuntimeException` heute (z. B. in `RfidTagAssignmentService`) → eigene Exception mit einer
  der Basisklassen.
- Konstruktor mit den Ids; statische Factories nur für Varianten (steht schon so in `schaffbar-backend/CLAUDE.md`).
  Tippfehler-Klasse umbenennen.

### Zu entscheiden

- **E7** – Fehler an das Portal: stabiler Code, den das Portal übersetzt (Vorschlag) – oder deutsche Meldung im
  `detail`?
- **E8** – Drei Basisklassen wie oben, `IllegalStateException` für „kann nicht sein“?

## 5. Transaktionen und Events

### Heute

- `@Transactional` gemischt:

  | | Spring | jakarta | keins |
  | --- | --- | --- | --- |
  | Services | 8 | 2 (`RfidTagAssignmentService`, `RfidTagAssignmentHistoryService`) | – |
  | Use Cases | 9 (Customer, RfidReader) | 21 (Tool, ToolCertification, Workshop, RfidTag-Zuweisung) | 1 |

- `CertifyCustomersForTool` ohne `@Transactional` (und als einziger `@Service` ohne `@Validated`) – bei einem Fehler
  mitten im Batch bleiben die ersten Einweisungen gespeichert. Ob das gewollt ist (`BatchCertificationResult` meldet
  Teilerfolge), ist nicht dokumentiert.
- Nirgends `readOnly = true`.
- `saveOutboxEvents` steht fast gleich in 9 Services (TODO „DRY“ im `CustomerService`).
- `RfidTagAssignmentHistoryService.moveToHistory` erzeugt kein Event – gegen die Regel „jede Zustandsänderung erzeugt
  Events“.

### Vorschlag

- Nur Spring `@Transactional` (kann `readOnly`, `rollbackFor`, Propagation; Spring-Standard). Use Case: die
  Transaktion der Aktion. Service: `@Transactional` an Änderungen (falls doch einmal direkt gerufen),
  `@Transactional(readOnly = true)` an Abfragen.
- `CertifyCustomersForTool`: transaktional und `@Validated` – oder Teilerfolg im README als Regel festhalten.
- Eine Komponente `OutboxEventStore` (in `shared/event/outbox/`) mit `save(List<SchaffbarEvent>)`, in jeden Service
  injiziert – statt Basisklasse (Services bleiben ohne Vererbung, leicht zu mocken).
- `moveToHistory` mit Event – oder im README begründen, warum nicht.

### Zu entscheiden

- **E9** – Spring `@Transactional` überall, `readOnly` an Abfragen?
- **E10** – Outbox: gemeinsame Komponente (Vorschlag) oder Basisklasse für Services?
- **E11** – `CertifyCustomersForTool`: alles oder nichts, oder Teilerfolg?

## 6. Validierung

### Heute

- Bean Validation an Service- und Use-Case-Parametern (`@NotNull @Valid CustomerId`) und an Request-Bodies.
- `@Valid` an Pfad-Variablen uneinheitlich (siehe 2).
- Nullbarkeit im API-Vertrag = Fehlen von `@NotNull` (Beispiel `ToolApiDto`) – das Portal liest daraus seine
  zod-Schemas.
- Value Objects prüfen im Konstruktor mit `IllegalArgumentException`; `MacAddress` prüft das Format nicht (TODO).
- Fachliche Formatregeln fehlen im Backend (A2: E-Mail, PLZ, Geburtsdatum, Beschreibung ≤ 255).

### Vorschlag

- Form und Format an den `*Commands` (gilt für Portal und Geräte gleich): `@Email`, `@Size`, `@Pattern`, eigene
  Annotation fürs Geburtsdatum. `*RequestBody` nur `@NotNull` / `@NotBlank` für die Pflichtfelder des Vertrags.
- Regeln mit Zustand oder zweitem Aggregat (Name eindeutig, Werkzeug läuft) im Use Case mit eigener Exception.
- Jedes nicht-nullbare Feld eines `*ApiDto` explizit `@NotNull` – Voraussetzung für OpenAPI (B).
- `MacAddress` prüft das Format.

### Zu entscheiden

- **E12** – Formatregeln an den Commands (Vorschlag) oder an den Request-Bodies?

## 7. API-Modelle

### Heute

- Einheitlich: Records, MapStruct-`MAPPER`, `to…Command`-Methoden für Bodies mit Pfad-Id.
- Nur die Nullbarkeit ist implizit (siehe 6); `getRfidReaders` mit `ResponseEntity<?>`.

### Vorschlag

- Bleibt, wie es ist; Nullbarkeit explizit, Rückgabetypen ausgeschrieben. Keine eigene Entscheidung nötig.

## 8. Tests

### Heute

- 8 Testklassen + `ViewBuilder`: 3 Use Cases (`CloseSession`, `CustomerDelete`, `RfidReaderDelete`) von 31,
  3 Entity-Methoden, 2 Billing-Klassen.
- Keine Tests mit Spring-Kontext oder Datenbank: Repository-Abfragen (z. B. `deleteAllByCustomerId`), Migrationen,
  MapStruct-Mapping und JSON-Format sind nicht abgedeckt.
- Keine Architekturtests; die Regeln aus `schaffbar-backend/CLAUDE.md` (Services rufen sich nicht gegenseitig,
  Repository nur im eigenen Paket, …) prüft niemand.

### Vorschlag

- Jeder Use Case mit eigener Logik bekommt seinen Test im Rollout (reine Weiterleitungen nicht).
- **ArchUnit** (steht auch in `TODO.md`): die Strukturregeln als Test – wie ESLint im Portal. Für Agents am
  wichtigsten: ein Verstoß fällt sofort auf, statt im Review.
- Später: `@DataJpaTest` mit Testcontainers (PostgreSQL) für Repository-Abfragen und Migrationen; braucht Docker
  beim Testen.
- `@WebMvcTest` für den `@ControllerAdvice` (Status und `ProblemDetail` je Basisklasse).

### Zu entscheiden

- **E13** – ArchUnit jetzt (im Pilot)?
- **E14** – Datenbanktests mit Testcontainers: im Rahmen von K oder später?

## 9. TODOs im Code

34 TODOs in `src/main`. Zuordnung:

| Gehört zu | TODOs |
| --- | --- |
| K (dieses Dokument) | eigene Exceptions statt `RuntimeException` (6), Open/Active, `unassignEvents`/`cancelEvents`, `saveOutboxEvents` DRY, `RfidTagId` statt `String`, Tippfehler und Meldung `NoWaitingAssingmentException`, `ifPresentOrElse` im `DeviceController`, `DeviceApiModel` Zugriff, `ValidationSupport` in `SchaffbarEvent`, `MacAddress`-Format |
| A2 | `RfidTagService`: zugewiesenen Tag nicht löschen, Tag-Id schon vorhanden |
| REST (E5) | `WorkshopSessionController` Pfade, „check if customer exists“ |
| fachlich offen | `EnterWorkshop` „gültige Einweisung zum Betreten“, `RfidTagAssignment` Zustand prüfen, `RfidTagAssignmentHistoryService` Vorbedingungen, Billing-Methoden trennen, `getBillingSummary` |
| nicht ändern | `WorkshopUsage` umbenennen, Event-Typen umbenennen, Outbox-Payload als JSONB / ganzes Event |

## Entscheidungen (K2)

| # | Frage | Empfehlung | Entschieden |
| --- | --- | --- | --- |
| E1 | `*Commands` für jedes Aggregat mit Änderungen | ja | 13.11.2026: **nein** – ein Command lohnt sich erst ab mehr als ein, zwei Value Objects; trifft das auf alle Änderungen eines Aggregats zu, hat es kein `*Commands` |
| E2 | jede Änderung über einen Use Case | ja, Abfragen direkt an den Service | 13.11.2026: ja – 15.11.2026 geändert: **Abfragen nicht direkt an den Service**, sondern über die Read Facade des Controllers (E17) |
| E3 | Namensmuster der Use Cases | `<Aggregat><Aktion>` | 13.11.2026: ja – 22.11.2026: das Präfix ist das Aggregat, dessen Zustand sich ändert (bei mehreren das, um das es geht), die Aktion ist der Befehl der Entity (E20; `RfidTagAssignmentRequest` statt `CustomerRequestRfidTagAssignment`, `RfidTagAssignmentAssignRfidTag` statt `CustomerAssignRfidTag`) |
| E4 | Device-API: Vertrag bleibt, Logik in Use Cases | ja | 13.11.2026: grundsätzlich ja, im Einzelfall am konkreten Endpunkt entscheiden |
| E5 | REST-Pfade und Query-Parameter aufräumen | im Rollout des Aggregats, Portal im selben Schritt (zuerst „mit OpenAPI (B)“ – betroffen sind aber nur zwei Portal-Dateien) | 13.11.2026: ja – 22.11.2026: bei `rfid_tag_assignment` vergessen, wird mit dem Backend-Review nachgeholt |
| E6 | „Open“ für alles ohne Ende | ja | 13.11.2026: ja – 22.11.2026 geändert: **„Open“ nur für die Sitzung** (Status `OPEN`), **„Running“ für Werkstattbesuch und Werkzeugnutzung** – wie V9 (`uq_…_running`), WS-5 und das Portal („läuft“); `Active…` wird überall umbenannt, auch API-Codes, Dashboard-Pfade und das Feld `active`, zusammen mit dem Portal |
| E7 | Fehler an das Portal | stabiler Code, Portal übersetzt | 13.11.2026: ja |
| E8 | drei Basisklassen, `IllegalStateException` für „kann nicht sein“ | ja | 13.11.2026: ja |
| E9 | Spring `@Transactional`, `readOnly` an Abfragen | ja | 13.11.2026: ja |
| E10 | Outbox als gemeinsame Komponente | ja (`OutboxEventStore`) | 13.11.2026: ja |
| E11 | `CertifyCustomersForTool` | alles oder nichts | 13.11.2026: ja |
| E12 | Formatregeln an den Commands | ja | 13.11.2026: ja |
| E13 | ArchUnit im Pilot | ja | 13.11.2026: ja |
| E14 | Datenbanktests mit Testcontainers | später (nach K) | 13.11.2026: später – 15.11.2026 geändert: **jetzt, vor dem Rollout** (E22–E24) |
| E15 | ArchUnit-Regeln mit vielen Altlasten | `FreezingArchRule`: bekannte Verstöße in `core-pos/archunit_store/`, nur neue schlagen fehl, behobene verschwinden beim nächsten Lauf | 15.11.2026: ja – 22.11.2026: Store leer, `freeze(…)` und Store entfernt, alle Regeln streng |
| E16 | `rfid_tag_assignment_history` hängt von `rfid_tag_assignment` ab (Typ, View) | Historie ins Paket `rfid_tag_assignment` ziehen | 15.11.2026: beim Rollout von `rfid_tag_assignment` entscheiden – 16.11.2026: ja, als **innere Entity** von `RfidTagAssignment` (eigenes Repository, sonst keine Bausteine); das Lösen löscht die Zuweisung und schreibt den Historien-Eintrag in einem Schritt, `RFID_TAG_UNASSIGNED` trägt den Eintrag; ArchUnit: Aggregat = Entity mit dem Namen des Pakets – 22.11.2026: auch im Web keine eigenen Bausteine: eine innere Entity bekommt einen Unterpfad im Controller ihres Aggregats (`GET /rfid-tag-assignments/history`) und eine Abfrage in dessen Read Facade, keinen eigenen Controller |
| E17 | Abfragen der Controller | eine ReadFacade pro Controller in `read_facade/` neben `use_case/` (`RfidReaderReadFacade`, `WorkshopSessionReadFacade`), `@Transactional(readOnly = true)`, auch wenn sie nur weiterreicht; Controller kennen nur Use Cases und ReadFacades; die Dashboard-Services werden ReadFacades, Controller und API-Modell ziehen nach `dashboard/web/` – in `domain/` liegen nur Aggregate | 15.11.2026: ja – teils ersetzt: statt `@Transactional(readOnly = true)` **`@Query`** (E21, 15.11.2026); Controller ohne Aggregat liegen in **`web/<object>/`** (`web/dashboard/`, 21.11.2026), nicht in `dashboard/web/` |
| E18 | Value Objects als Feld in Commands und Views | immer `@NotNull @Valid` (optional: nur `@Valid`) | 15.11.2026: ja |
| E19 | Id des eigenen Aggregats bei einer Änderung | im Command als `id`, Referenzen als `<aggregat>Id` | 15.11.2026: ja |
| E20 | Namen der Service-Commands | Verb + Aggregat (`pauseToolCertification`, `startToolUsage`) | 15.11.2026: ja – 22.11.2026 genauer: der Service-Befehl ist der Befehl der Entity, wo die Entity das Aggregat weglässt, steht es nach dem Verb (`pause()` → `pauseToolCertification`, `changeType()` → `changeRfidReaderType`); nennt sie etwas anderes, bleibt der Name (`assignRfidReader`, `setWlanRelais`); anlegen und löschen Verb + Aggregat; der Use Case heißt Aggregat + Befehl der Entity (`ToolAssignRfidReader`, `RfidTagAssignmentAssignRfidTag`) |
| E21 | Lesen und Schreiben im Code kennzeichnen | eigene Annotationen `@Query` (= `@Transactional(readOnly = true)`) und `@Command` (= `@Transactional`) in `shared/cqrs/`; jede öffentliche Methode von Services, Use Cases und Read Facades hat genau eine davon, kein direktes `@Transactional` mehr – Erweiterungen (Timeout, Logging) an einer Stelle | 15.11.2026: ja |
| E22 | Testebenen | jede Regel auf der niedrigsten Ebene getestet, die sie vollständig beweist: Value Object/Entity (ohne Mocks), **Service (Mockito für Repository und Outbox, echte Entity und EventFactory)**, Use Case (Mockito), Controller (Standalone-MockMvc), Integration – Akzeptanz (Regel oder Ablauf über alle Schichten) und technisch (Repository, Transaktion, Outbox, Migration), ArchUnit | 15.11.2026: ja |
| E23 | Integrationstests aufbauen und benennen | Fixture als Test-Bean (`SchaffbarFixture`) mit `given` / Auslöser / `then`, `Scenario` mit Zufallswerten statt Aufräumen, eigene Annotation `@IntegrationTest`; eine Klasse pro Aggregat oder Ablauf (`RfidReaderAcceptanceTest`), eine Methode pro Regel mit ihrer Nummer (`r3_…`) | 15.11.2026: Fixture und `@IntegrationTest` ja, Benennung im Pilot zeigen – 16.11.2026: Benennung ja (`r1_aMacAddressBelongsToOneReaderOnly`); Auslöser über die HTTP-API (`fixture.api()`), kein `clear()` nötig; Skill `backend-testing` |
| E24 | Testcontainers | ein Container `postgres:16.3` für den ganzen Testlauf, Flyway migriert einmal, `@ServiceConnection`; läuft in `mvn test` (kein eigenes Failsafe-Ziel, solange es schnell genug ist) | 15.11.2026: ja |
| E25 | Value Objects einheitlich | Interface `ValueObject<T>` mit `sameValueAs` als Default-Methode (Typparameter verhindert Vergleiche über Typen hinweg); `random()` nur für UUID-Ids – Werte von Geräten (`MacAddress`, `RfidTagId`, `IpAddress`) erfindet das System nie, Tests nehmen `RandomValues`; Enum für Fehlermeldungen heißt `ValueObjectName`; geprüft von `ValueObjectArchitectureTest` | 15.11.2026: ja |
| E26 | Jede Datenbankänderung erzeugt ein Event | auch Löschen und das Schreiben einer inneren Entity; die Änderung einer inneren Entity steht im Event des Aggregats (kein zweites Event); `@Command`-Methoden der Services speichern Events (ArchUnit) | 16.11.2026: feste Regel – die Historie ohne Event war eine Abkürzung |
| E27 | Typen in API-Antworten | keine Value Objects und keine `UUID` in `…ApiDto`: Ids und Werte (`IpAddress`) als `String` über `id.value`, Enums als Enum (auch in Anfragen: `RfidReaderType` statt `String`), Datum als `LocalDate`; geprüft von `ValueObjectArchitectureTest` – das JSON bleibt gleich | 22.11.2026: ja |
| E28 | Zwei gleichzeitige Anfragen (Tag doppelt gesendet) | partielle Unique-Indizes (V9) für WU-1, WS-1, TU-9 (deckt TU-3); die zweite Anfrage scheitert als `500` bzw. allgemeiner Fehler am Gerät, ohne Abbildung auf den Code der Regel – sollte kaum vorkommen; `workshop_session.status` `NOT NULL`; getestet mit je einem technischen Integrationstest | 22.11.2026: ja |
| E29 | Zustandsregel, wenn das Aggregat über seinen Zustand angesprochen wird („die offene Sitzung der Person“, „die wartende Anfrage“, „die laufende Nutzung“) statt über seine Id | der Service findet die Zeile im Zustand und wirft die eigene Exception (`409`); der Befehl der Entity behält eine Absicherung mit `IllegalStateException` und Kommentar, wer vorher prüft; eine innere Entity entsteht erst nach dem Fund. Über die Id geladen gilt weiter: eigener Zustand im Befehl der Entity (`ToolCertification.pause()`) | 22.11.2026: ja |
| E30 | Löschen | Entity laden (`findById` / `findOrThrow`, sonst `404`), dann `delete(entity)` + Event – nicht View + `deleteById` (liest doppelt, rohe Id); auch `existsBy…` über eine Überladung mit Value Object | 22.11.2026: ja |
| E31 | Typen in Event-Payloads | Enums als Enum (nicht `String`, das gespeicherte JSON bleibt gleich), Werte als Value Object; erlaubt sind auch Records aus `shared/` (`BillingTotal`) – nie eine Entity oder ein `@Embeddable` | 22.11.2026: ja |
| E32 | Status beim Anlegen | `201` mit `Location` auf `GET /{id}` der neuen Ressource; gibt es kein solches GET (über etwas anderes gefunden: `POST /rfid-tag-assignments`; mehrere: `/tool-certifications/batch`), `204` | 22.11.2026: ja |
| E33 | Formatregeln (Bean Validation) testen | eigene Logik eines Constraints im Unit-Test des Validators, zusammengesetzte ohne eigenen Test; pro Constraint und Endpunkt ein Controller-Test `…_is400`; am Command im Service kein Test; ein Akzeptanztest ist nicht verlangt, aber erlaubt (`c6`, `c7` bleiben, die Akzeptanztests werden später ausgebaut) | 22.11.2026: ja |
| E34 | Datum und Zeit im Controller-Test | der Standalone-MockMvc bekommt Boots Jackson-Konfiguration (`Json.messageConverter()`: `JavaTimeModule`, keine Timestamps), Datumsfelder werden gegen ihren Wert geprüft – der Controller-Test beweist das Format, das das Portal liest (E22), nicht erst der Akzeptanztest | 22.11.2026: ja |

## Pilot (K3)

`rfid_reader` mit `RfidReaderCreate`, `-Update`, `-ChangeType`, `-Delete` und `RfidReaderController`: klein, hat
schon einen Use-Case-Test und eine Exception mit Factories.

Schon erledigt (Unterbau): `ApiExceptionHandler` mit `ProblemDetail` (A1), `BusinessRuleViolationException`,
`OutboxEventStore`, ArchUnit mit Freeze-Store, Spotless, Skill `aggregate`.

Im Pilot noch: die Exceptions von `rfid_reader` mit Basisklasse und Code, Spring-`@Transactional` mit `readOnly`,
`RfidReaderReadFacade` (E17) samt ArchUnit-Regeln für Read Facades, Controller aufräumen (Rückgabetypen, `@Valid`),
`MacAddress` prüft das Format, Tests der Use Cases, Portal-Interceptor zeigt den übersetzten Code. Der
`DeviceController` (legt Lesegeräte an) kommt erst im Rollout dran.

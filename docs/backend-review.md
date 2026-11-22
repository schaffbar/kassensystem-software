# Backend-Review (22.11.2026)

Review des ganzen Backends nach dem Rollout (Roadmap K4) gegen die eigenen Regeln: `schaffbar-backend/CLAUDE.md`,
die Skills `aggregate` und `backend-testing` und die Entscheidungen in `backend-consistency.md`. Fünf Agenten haben
je einen Teil gelesen (Aggregate, `shared/`, Use Cases und Read Facades mit `web/`, Tests); die Funde mit ✔ sind
zusätzlich von Hand im Code geprüft, die anderen noch nicht – vor dem Ändern ansehen.

Abkürzungen: `M:` = `core-pos/src/main/java/de/schaffbar/core_pos/`, `T:` = `core-pos/src/test/java/de/schaffbar/core_pos/`.
Zeilennummern vom 22.11.2026, sie verschieben sich mit jeder Änderung.

## Reihenfolge (22.11.2026)

1. **Entscheidungen** (Abschnitt 1) – ohne Code.
2. **Regeldokumente** (Abschnitt 2) – Skills, `CLAUDE.md`, `backend-consistency.md`, mit den Entscheidungen. Zuerst die
   Regeln, damit falscher Code nicht weiter als Vorlage dient.
3. **Pro Aggregat** (Abschnitt 3) – Code, Tests und README eines Aggregats in einem Schritt, die neuen ArchUnit-Regeln
   mit ihrer Korrektur (alle Regeln sind streng).
4. **Übergreifend** (Abschnitt 4) – `shared/`, `web/device`, Read Facades, Fixture, ArchUnit, `ddd-overview.md`.
5. **Fehler im Verhalten** (Abschnitt 5) – zum Schluss, es gibt noch keinen Betrieb. Fällt ein Punkt in eine Datei, die
   ein früherer Schritt ohnehin ändert, darf er dort mit erledigt werden.

Ein erledigter Punkt bekommt sein Häkchen und das Datum.

## 1. Entscheidungen

- [x] **E6 „Open“ für alles ohne Ende** – entschieden 22.11.2026: „Open“ für die Sitzung, „Running“ für Werkstattbesuch
  und Werkzeugnutzung (E6 geändert); umbenannt wird in Abschnitt 3. War nicht umgesetzt ✔: der Code sagt `Active…`
  (`ToolUsageService`, `WorkshopUsageService`: `getActive…` / `hasActive…`; Exceptions `ActiveToolUsageExistsForSession`,
  `ActiveWorkshopUsageExistsForSession`, `NoActiveWorkshopUsageFound` – ihre Codes sind API; Dashboard
  `getActiveWorkshopUsers` / `getActiveToolUsages`, Pfade `/dashboard/active-users`, `/active-tools`, `Active…ApiDto`;
  Feld `active` in `ToolUsageSummaryApiDto` und `OpenWorkshopSessionViews`), V9 und WS-5 sagen „running“, das Portal
  „läuft“. Umsetzen (mit Portal, Übersetzungen, `DeviceMessages`) oder E6 neu entscheiden (z. B. Open für die Sitzung,
  Running für Nutzungen).
- [x] **E5 Query-Parameter in camelCase** – 22.11.2026: gilt weiter, wird im Schritt `rfid_tag_assignment` mit dem
  Portal nachgeholt ✔: `GET /rfid-tag-assignments` nimmt noch `rfid-tag-id` / `customer-id`
  (`M:domain/rfid_tag_assignment/web/RfidTagAssignmentController.java:53-54`); mit Portal
  (`rfid-tag-assignments.api-service.ts:34,38`) und Bruno.
- [x] **E3 Präfix der Use Cases** – 22.11.2026: das Präfix ist das Aggregat, dessen Zustand sich ändert (bei mehreren
  das, um das es geht – `WorkshopUsageStart` startet auch eine Sitzung); die Aktion ist der Befehl der Entity (E20).
  Umbenennen im Schritt `rfid_tag_assignment`: `CustomerRequestRfidTagAssignment` → `RfidTagAssignmentRequest`,
  `CustomerAssignRfidTag` → `RfidTagAssignmentAssignRfidTag`, `CustomerUnassignRfidTag` →
  `RfidTagAssignmentUnassignRfidTag` (Namen nach der Regel zu E20)
  (mit Tests, TA-7/TA-8, `ddd-overview.md`); Regel in Skill `aggregate` und `CLAUDE.md` (Abschnitt 2), ArchUnit in
  Abschnitt 4.
- [x] **`RfidTagAssignmentHistoryController`** – 22.11.2026: zusammenlegen (E16 ergänzt). Im Schritt
  `rfid_tag_assignment`: `GET /api/v1/rfid-tag-assignments/history` im `RfidTagAssignmentController`,
  `getRfidTagAssignmentHistory()` in `RfidTagAssignmentReadFacade`; `RfidTagAssignmentHistoryController` und
  `RfidTagAssignmentHistoryReadFacade` löschen, Test in `RfidTagAssignmentControllerTest`, Bruno anpassen. Kein Filter,
  solange das Portal die Historie nicht zeigt (der Endpunkt wird nicht aufgerufen). Regel „innere Entity: Unterpfad,
  kein eigener Controller“ in Skill `aggregate` und `CLAUDE.md` (Abschnitt 2).
- [x] **Zustandsregeln, wenn der Service die Zeile über ihren Zustand findet** – 22.11.2026 (E29): das Muster wird
  Regel, kein Umbau. Betrifft vier Aggregate: `WorkshopSession.close()`, `ToolUsage.stop()`, `WorkshopUsage.exit()`,
  `RfidTagAssignment` (TA-4, TA-5, TA-10). Der Service findet die Zeile im Zustand (offen, läuft, wartet, zugewiesen)
  und wirft die eigene Exception; die Entity behält eine Absicherung mit `IllegalStateException` und einem Kommentar,
  wer vorher prüft (fehlt heute überall); `RfidTagAssignmentHistory.of()` erst nach dem Fund einer `ASSIGNED`-Zuweisung.
  README: Ort der Regel ist der Service, nicht zusätzlich die Entity (TA-4, TA-5). Skill `aggregate`: Zeile in „Where a
  rule lives“ und Rezept der inneren Entity (Abschnitt 2); Kommentare und READMEs in den Schritten der vier Aggregate.
- [x] **Löschen einheitlich** – 22.11.2026 (E30): Entity laden (`findById` / `findOrThrow`, sonst 404), dann
  `delete(entity)` – ein Lesen, keine View im Befehl, keine rohe Id, `@Version` geprüft. Umstellen in den Schritten
  `rfid_reader` (`RfidReaderService:99-103`) und `rfid_tag` (`RfidTagService:68-72`; rohes `existsById` `:85` →
  Überladung `existsById(RfidTagId)`); Skill-Ablauf in Abschnitt 2; ArchUnit-Regel gegen `deleteById` / rohes
  `existsById` in Abschnitt 4 entscheiden.
- [x] **Namen der Service-Befehle** – 22.11.2026 (E20 geändert): der Service-Befehl ist der Befehl der Entity; wo die
  Entity das Aggregat weglässt (`pause()`, `changeType()`), steht es nach dem Verb (`pauseToolCertification`,
  `changeRfidReaderType`); nennt sie etwas anderes, bleibt der Name (`assignRfidReader`, `setWlanRelais`,
  `assignRfidTag`); anlegen und löschen: Verb + Aggregat. Der Use Case heißt Aggregat + Befehl der Entity
  (`ToolAssignRfidReader`). Die `Tool`-Befehle passen damit; umbenennen: `cancelRfidTagAssignmentRequest` →
  `cancelRfidTagAssignment` (Schritt `rfid_tag_assignment`), `WorkshopUsage.exit()` → `stop()` (Entscheidung unten).
- [x] **Payloads** – 22.11.2026 (E31): Enums als Enum (das JSON bleibt gleich) – umstellen in `rfid_reader`
  (`RfidReader*Payload.type`, dazu `RfidReaderCreatedPayload.macAddress` → `MacAddress`) und `rfid_tag_assignment`
  (`assignmentType`); Werte aus `shared/` (Value Objects, `BillingTotal`) sind erlaubt, nie eine Entity oder ein
  `@Embeddable`. Regel im Skill `aggregate` (Abschnitt 2).
- [x] **`closeWorkshopSession(CustomerId, BillingTotal, BillingTotal)`** – 22.11.2026: neues `WorkshopSessionCommands`
  mit `CloseWorkshopSessionCommand(id, workshopTotal, toolTotal)` (alle `@NotNull @Valid`, `id` nach E19); der Service
  lädt über die Id (sonst 404), WS-4 prüft nur der Use Case (er findet die Sitzung über ihren Zustand, E29); die
  Absicherung in `WorkshopSession.close()` bekommt `// WorkshopSessionClose finds only an open session (WS-4)`.
  Schritt `workshop_session`; API und Portal unverändert.
- [x] **Altersgruppe** – 22.11.2026: in die Domäne. `domain/customer/AgeGroup` (öffentliches Enum),
  `Customer.getAgeGroupOn(LocalDate day)` (Entity-Test an den Grenzen 16 und 18), `CustomerView.ageGroup` aus
  `CustomerViews` mit `LocalDate.now()`; `CustomerApiModel` übernimmt nur das Feld (`:35,51-63` und das innere Enum
  fallen weg); README C-12. JSON und Portal unverändert; Zeitzone von `LocalDate.now()` mit Abschnitt 5.
- [x] **`ResourceNotFoundException`** ohne `CODE`-Konstante – 22.11.2026: `CODE = "RESOURCE_NOT_FOUND"` einführen,
  `ApiExceptionHandler:30` und die 13 Literale in Tests (Controller, Akzeptanz, `ThenFixture`) nehmen die Konstante;
  nur `ApiExceptionHandlerTest` prüft das Literal einmal mit Kommentar (Vertrag mit dem Portal); die ArchUnit-Regel
  „declare `CODE`“ gilt auch für sie. Schritt `shared/`.
- [x] **Kaputte Daten am Gerät** – 22.11.2026: eine Zuweisung, deren Person es nicht gibt (C-4 löst den Tag beim
  Löschen), wirft `IllegalStateException` mit Tag und Person (E8) statt `ResourceNotFoundException.customer`
  (`M:read_facade/DeviceReadFacade.java:83-84`) – am Gerät „Unerwarteter Fehler“, im Log ein Fehler; `DeviceMessages`
  „Kunde nicht erkannt“ fällt weg. Kein Fremdschlüssel (C-11: Tabellen bewusst nicht verknüpft). Schritt `web/device`.
- [x] **R-8 an der Schaltbox** entscheidet der `DeviceController` (`:211-217`) – 22.11.2026: bleibt vorerst so; die
  Schnittstellen des `DeviceController` werden ohnehin umgebaut, dann neu entscheiden (Vorschlag:
  `ToolUsageToggle(customerId, switchBoxId)` mit `NoToolAssignedToSwitchBoxException`).
- [x] **Anlegen ohne `201`** – 22.11.2026 (E32): die heutige Praxis wird Regel in `CLAUDE.md` („Controller and API
  model“): `201` mit `Location` auf `GET /{id}` der neuen Ressource; ohne solches GET (über etwas anderes gefunden wie
  die Zuweisung, oder mehrere wie `/batch`) `204`. Code unverändert.
- [x] **Outbox-Relay** – 22.11.2026: vorerst kein Abnehmer geplant; die Felder `processed`, `processedAt`, `owner` (und
  Spalten, Index) bleiben als Vorbereitung. Die ungenutzten Methoden `OutboxEvent.markAsProcessed` und
  `OutboxEventRepository.findByProcessedFalseOrderByCreatedAtAsc` fallen weg (kommen mit dem Relay). `ddd-overview.md` /
  `ddd-uebersicht.md`: die Outbox ist heute das Änderungsprotokoll, ein Relay gibt es noch nicht. Schritt `shared/`.
- [x] **Namen**: `WorkshopUsage.exit()` neben `stopWorkshopUsage` / `WorkshopUsageStop` / `ToolUsage.stop()` –
  22.11.2026: `exit()` heißt künftig `stop()` (E20); Service, Use Case, Controller, API, Felder `entryTime` /
  `exitTime` und Event-Typen bleiben. Schritt `workshop_usage`.
- [x] **Bean Validation testen** – 22.11.2026 (E33): eigene Logik eines Constraints im Unit-Test des Validators
  (`DateOfBirthValidatorTest`), ein zusammengesetzter Constraint (`@PostalCode`, `@ToolDescription`) ohne eigenen Test;
  pro Constraint und Endpunkt ein Controller-Test `<endpoint>_with<WhatIsWrong>_is400`; am Command im Service kein
  Test (dieselben Annotationen, Mockito validiert nicht). Die Akzeptanztests `c6`, `c7`
  (`T:integration/CustomerAcceptanceTest:106,115`) bleiben – sie werden später ausgebaut; der Skill verlangt für eine
  Formatregel keinen, verbietet ihn aber nicht. Das Datumsformat beweist nach der Entscheidung zu den Controller-Tests
  (unten, Skill `backend-testing`) der Controller-Test – `c8` braucht keinen Kommentar dazu.

## 2. Regeldokumente

### Skill `backend-testing`

Erledigt 22.11.2026 (alle Punkte unten). Dazu entschieden: Controller-Tests bekommen Boots Jackson-Konfiguration
(`Json.messageConverter()` im Standalone-MockMvc) und prüfen Datum und Zeit gegen den Wert statt nur `exists()` – sie
beweisen das Format für das Portal (E22). Selbst festgelegt und bestätigt: jede Interaktion in einer `expect…`-Methode;
`Scenario` ein Wert pro Art, ein zweiter aus `RandomValues` nach seiner Rolle benannt; technische Tests nur, wo kein
Akzeptanztest hinkommt, keiner für „höchstens eine“ in `default`-Methoden.

- [x] Pfad in Zeile 8: `../docs/backend-consistency.md` stimmt vom Skill aus nicht.
- [x] Veraltete Beispiele: `given().switchBox(scenario)`, `toolWithSwitchBox(scenario, switchBox)` (die Fixture nimmt
  `MacAddress` / `String`); Use-Case-Beispiel `givenTool()` / `givenNoTool()` / `expectTypeChanged()`.
- [x] Referenz `RfidTagAssignmentTest` verstößt selbst gegen den Skill (Abschnitt 3, `rfid_tag_assignment`) – reparieren
  oder auf `ToolCertificationTest` (`expectRejected(thrown, entity, unchangedStatus)`) zeigen.
- [x] `expectSavedEvent(type, id)` bei `void`-Create (die Id kennt der Test nicht): `expectSaved…()` mit
  `verify(repo).save(captor.capture())` beschreiben – kein Captor im Stub.
- [x] Payloads: „jedes Event, das der Service baut“ (auch die Deleted-Events), nicht nur das Created-Event.
- [x] „Zwei Methoden pro Command/View“ passt nicht auf Create-Commands ohne eigene Id und Read-Facade-Views.
- [x] Was im Test bleiben darf: Zustand und Payload des Ergebnisses inline, Interaktionen und Wiederholtes in `expect…`;
  die Checkliste ist strenger als der Text; ein `InOrder`-Block ist mehr als eine Zeile.
- [x] Methodennamen: „ohne Methode, wenn die Klasse eine testet“ widerspricht dem eigenen Beispiel (`MacAddressTest`
  `of_…`).
- [x] JSON-Bodies: Textblock oder kurzer String (`"{}"` steht im Skill selbst, kein Test nutzt Textblöcke).
- [x] Gelten die Controller-Regeln für `ApiExceptionHandlerTest` (Testcontroller)?
- [x] Bean Validation nach E33 (Validator, Controller `…_is400`, kein Test am Command; Akzeptanztest erlaubt, nicht verlangt).
- [x] Controller: wo die Felder des Portals stehen (`schaffbar-portal/src/app/shared/api/<resource>/*.api-model.ts`);
  Datumsformate beweist erst der Integrationstest (Standalone-MockMvc ohne Boots Jackson); wie `Instant`-Felder
  geprüft werden.
- [x] Technische Tests: braucht jede abgeleitete Query / jede `default`-Methode mit Logik einen?
- [x] `Scenario` hat einen Wert pro Art – Regel für den zweiten Leser, Tag, Werkzeugnamen (heute ~20× `RandomValues`).
- [x] „SchaffbarApi kennt als einzige URLs und JSON“ meint die Anfragen (Given/Then lesen Antworten).
- [x] Namen der Akzeptanztests: Präfix pro Aggregat (`c4_`, `ta1_`, `ws3_`), zusammengefasste Regeln (`t2_t15_`),
  mehrere Methoden pro Regel.
- [x] Wie ein Device-Test seine Regelnummer trägt (TU-1, TU-2, TU-9, TU-10, WU-1, WU-2, TA-4 nur dort bewiesen).
- [x] Erwartete Werte: `new XyzCommand(…)` / `new XyzView(…)` im Test ist wie `.builder()` verboten.
- [x] Die erlaubte Seite einer Regel: über `api()` (nicht `given()`); `expectSuccess` auf `/device/*` ist immer wahr.
- [x] Vollständige Ablehnung: bei `ResourceNotFoundException` die `resource` prüfen; am Gerät: Zustand unverändert.
- [x] Muster, die die Tests einheitlich nutzen, aber nirgends stehen: `given…RejectedWith(…, RuntimeException)`,
  `expectSavedEvents(type, list)`, `@ParameterizedTest @EnumSource` über Zustände, erklärende `// when – …` in Unit-Tests.

### Skill `aggregate`

Erledigt 22.11.2026 (alle Punkte unten); dazu E16 (Unterpfad statt eigener Controller) und die bekannten
Abweichungen aus dem Review unter „Not everywhere yet“.

- [x] Ordnertabelle „(RfidReaderType, CustomerAddress – package-private)“: Enums sind public.
- [x] `of(…)` setzt `createdAt` – nicht jede Entity hat eins (`rfid_tag_assignment`, Historie).
- [x] Rezept für die innere Entity widerspricht „ein Befehl prüft zuerst den eigenen Zustand“ – nach E29: neue Zeile
  in „Where a rule lives“, der Service erzeugt die innere Entity erst nach dem Fund im richtigen Zustand.
- [x] Löschen: Service-Ablauf sagt `deleteById`, Repository sagt `delete(entity)` – nach E30 überall `delete(entity)`.
- [x] Repository-Beispiel `Optional<Tool> findByIpAddress(String)` bricht die eigene „höchstens eine Zeile“-Regel.
- [x] Namen der Service-Befehle: Regel aus E20 (geändert 22.11.2026) mit der Tabelle Entity → Service → Use Case.
- [x] Payloads nach E31: „enthalten Enums und Werte (Value Objects, Records aus `shared/` wie `BillingTotal`), nie eine
  Entity oder ein `@Embeddable`“.
- [x] Nicht beschrieben: „den offenen laden oder Regel-Exception“ – `orElseThrow(() -> new NoOpenWorkshopSessionException(…))`
  (Services und Use Cases); der Skill kennt beim Laden nur `ResourceNotFoundException`.
- [x] „Ein neues Aggregat“ erzeugt Ungenutztes: `Resource`-Wert und Converter nur, wenn danach gesucht wird.
- [x] Beispiele für Commands/Views weichen von `rfid_reader` ab (`socketName` fehlt).
- [x] Begriff: `CustomerAddress` (`@Embeddable`) heißt im README „Value Object“.
- [x] Der Begriff aus E6 (Entscheidung oben) gehört in den Skill.

### `schaffbar-backend/CLAUDE.md`

Erledigt 22.11.2026 (alle Punkte unten); dazu E3/E20 (Name des Use Case), E16 (Unterpfad), E32, E33, E34, E6
und der Abschnitt zu `web/device/`.

- [x] „Many exceptions do not have a base class yet (E8)“ – stimmt nicht mehr.
- [x] „Older code still has `@Transactional`“ – keins mehr; `OutboxEventStore` (`Propagation.MANDATORY`) nicht erwähnt.
- [x] Baum: `shared/` ohne `cqrs/`, `util/`, `web/`.
- [x] Pfade sind Plural (`/api/v1/customers`), nicht `<aggregate-in-kebab-case>`.
- [x] Ausnahmen für `web/device/`: Endpunkt ändert und liest, Strings statt Value Objects, `*Response` statt `…ApiDto`.
- [x] Read Facades: „`get…`“, `DeviceReadFacade` hat `is…`.
- [x] `<Aggregate><Action>`: welches Aggregat (Entscheidung oben).
- [x] Abschnitte: Use Cases und Read Facades nur `// helper`; ApiModel (`// mapping view to response`, `// response`,
  `// request body`, …); Exceptions `// static factories`.
- [x] Nicht beschrieben: ein Use Case ruft Use Cases (`ToolUsageToggle`), Read Facades werfen Regel-Exceptions, was
  `process` zurückgeben darf, Name der Views-Datei einer Read Facade.
- [x] `shared/` kennt kein Feature (ArchUnit `sharedDoesNotDependOnFeatures`) steht in keinem Dokument.
- [x] Status beim Anlegen nach E32 (`201` mit `Location` oder `204`).

### `backend-consistency.md`

Erledigt 22.11.2026: E2 und E17 als geändert markiert; E6 wurde mit der Entscheidung geändert; neu E29–E34 aus
den Entscheidungen.

- [x] E2 („Abfragen direkt an den Service“) ist durch E17 ersetzt, E17 (`@Transactional(readOnly = true)`,
  `dashboard/web/`) durch E21 und `web/dashboard/` – als ersetzt markieren wie E14/E15.
- [x] E6 nach der Entscheidung oben.

## 3. Pro Aggregat

### `rfid_reader` (Referenz-Implementierung – soll vorbildlich sein)

- [ ] Zuerst: `support/Json.messageConverter()` (Skill `backend-testing`, Controller); danach jeder Controller-Test
  im Schritt seines Aggregats mit `setMessageConverters(Json.messageConverter())` und Datumsfeldern gegen den Wert.
- [ ] README: `UpdateRfidReaderCommand` ohne `type` (Code: id, name, socketName); `RFID_READER_CREATED` trägt id und
  macAddress (nicht „Full snapshot“), `RFID_READER_UPDATED` auch `type`; Löschen fehlt bei den Befehlen.
- [ ] `RfidReaderApiModel.toRfidReaderCommand` → `toUpdateRfidReaderCommand` (`RfidReaderController:97`).
- [ ] Löschen nach der Entscheidung (`RfidReaderService:99-103`).
- [ ] Payloads nach E31: `type` als `RfidReaderType`, `RfidReaderCreatedPayload.macAddress` als `MacAddress`.
- [ ] `RfidReaderType`: Leerzeilen nach Kopf und vor Ende fehlen.
- [ ] Tests:
  - `RfidReaderTest:76,80` prüft `GATE_KEEPER_IN` statt `command.type()`.
  - Controller: Erfolg ohne name, socketName, createdAt (`:100-103, 156-159`); JSON als String (`:205, 250, 364`).
  - Fehlt: `createRfidReader` 409 `MAC_ADDRESS_ALREADY_USED` (R-1), 404 für update, changeType, delete.
  - `RfidReaderServiceTest:154`: Payload des Deleted-Events.
  - `RfidReaderChangeTypeTest:53`: 404 ohne `resource`.
  - `RfidReaderAcceptanceTest:26` r1 nur die abgelehnte Seite (danach löschen und dieselbe MAC wieder anlegen).

### `rfid_tag`

- [ ] README: `RFID_TAG_CREATED` trägt nur die id; RT-4 nennt `EnterWorkshop` / `StartToolUsage`.
- [ ] Löschen nach der Entscheidung (`RfidTagService:68-72`, rohes `existsById` `:85`).
- [ ] `RfidTag.java:56-57`: leerer Abschnitt `// command`.
- [ ] Tests:
  - `RfidTagServiceTest:142-147`: Captor im Stub, gelesen in `:60` → `expectSavedTag()`; `:85` Deleted-Payload.
  - Controller: `:85` prüft den Builder-Default `active == true`; `:100` nur die id; `:160` `returns204`, `:144, 173`
    `is409WithCode` (sonst `is204`, `is409WithTheCode`); `:239` JSON als String; fehlt: delete 404.
  - `RfidTagIdTest` fehlt (RT-1, leer).
  - `RfidTagAcceptanceTest:49` rt5 nur die abgelehnte Seite.

### `rfid_tag_assignment`

- [ ] Entscheidungen oben: Zustandsregeln, E5, E3, Historie-Controller, Namen, Payload-Typ.
- [ ] `RfidTagAssignmentApiModel` erbt nicht von `ValueObjectMapper` (`:21`).
- [ ] `RfidTagAssignmentRepository:25,40,55`: Meldungen ohne Ids.
- [ ] `RfidTagAssignment.java:84`: `isBlank` statt `isNull`.
- [ ] `RfidTagAssignmentStatus`, `RfidTagAssignmentType`: Leerzeilen.
- [ ] `ResourceNotFoundException.Resource` hat keinen Wert für die Zuweisung (nur nötig, wenn danach gesucht wird).
- [ ] Gerät: „ein zugewiesener Tag zeigt seinen Besitzer“ (`DeviceController.assignRfidTagOrShowOwner`, Kommentar TA-2)
  hat keine README-Zeile.
- [ ] Tests:
  - `RfidTagAssignmentTest` (Referenz im Skill!): Ablehnungen ohne „Zustand unverändert“ (`:66-75, 100-109, 129-138`);
    Helfer `waiting()` / `assigned()` (`:143-152`) nicht `given…`.
  - `RfidTagAssignmentServiceTest:284` `expectSavedEvent` ohne Id; `:278` Captor im Stub, gelesen `:73-79`;
    wiederholt: `verify(repo).delete(…)` `:176, 208`, `verifyNoInteractions(historyRepository)` `:192, 209`.
  - Fehlt im Service: unassign/cancel ohne jede Zuweisung; Anfrage, während die eigene noch wartet (TA-1).
  - Controller: `:92` `"ASSIGNED"` und History `:53` `"TEMPORARY"` sind Builder-Defaults; `:141, 180, 196` `returns204`;
    `:164` `is409WithCode`; JSON als String `:156, 260, 272, 278`.
  - Fehlt im Controller: request 409 `CUSTOMER_HAS_RFID_TAG_ASSIGNMENT` und 404; assign 409 `RFID_TAG_ALREADY_ASSIGNED`,
    `NO_RFID_TAG_ASSIGNMENT_WAITING` und 404; unassign 409 `NO_RFID_TAG_ASSIGNED` und 404; Filter ohne Treffer; beide
    Filter.
  - Use Cases: 404 ohne `resource` (`CustomerAssignRfidTagTest:57`, `CustomerRequestRfidTagAssignmentTest:57`,
    `CustomerUnassignRfidTagTest:56`); `RfidTagAssignmentReadFacadeTest` nur der leere Fall (`:59`).
  - Akzeptanz: ta1 nur abgelehnt (`:27`); erlaubte Seite über `given()` (`:94-95`); `RandomValues` `:60, 94, 157`.
  - Technisch: `default`-Methoden `findByCustomerId(CustomerId)`, `findWaitingForAssignment()`.

### `customer`

- [ ] Altersgruppe nach der Entscheidung (oben); die Methode steht heute unter `// mapping request body to command`.
- [ ] `CustomerApiModel` erbt nicht von `ValueObjectMapper` (`:27`).
- [ ] README: Löschen fehlt bei den Befehlen; `CustomerAddress` heißt „Value Object“ (`:22`).
- [ ] `CustomerViews.java:39`: `CustomerAddressView` ohne `@Valid`.
- [ ] `CustomerId.random()` (und `RfidReaderId.random()`) mit `new …` statt `….of(UUID.randomUUID())`.
- [ ] `CustomerDelete:25-29`: JavaDoc statt Regel-Ids (`// C-4`, `// C-9`, C-11).
- [ ] Tests:
  - `CustomerServiceTest:55-73`: Created-Payload nur die Adresse; `:171` Deleted-Payload.
  - Controller: Erfolg prüft wenige Felder (`:105-109, 124-127`); fehlt: 404 für update, contact, address, delete; 400
    für C-1, C-2, C-3 beim Anlegen, C-8 beim Ändern.
  - `CustomerDeleteTest`: `verify(…).deleteCustomer` viermal (`:70-73, 87-90, 104-107, 123-126`); Name von `:60`
    irreführend; Reihenfolge Einweisungen vor Person nicht geprüft; `:153` 404 ohne `resource`.
  - Akzeptanz: erlaubte Seite über `given()` (`:47, 68-69`); `expectSuccess` auf Gerät (`:95`); `RandomValues` `:81, 145`
    (nach dem Skill erlaubt, wenn nach der Rolle benannt).

### `tool`

- [ ] `ToolViews.java:42`: `instructors` ohne `@NotNull` (die API hat es).
- [ ] `Tool.java:53-54`: `name` ohne `@Column(unique = true)` (V4).
- [ ] README: Attribute ohne `instructorIds`; Befehle ohne assign/clear Lesegerät, clear WLAN-Relais,
  add/remove Einweiser, löschen.
- [ ] `ToolAssignRfidReader:34-36`: T-9 inline statt `ensureIsSwitchBox` mit `// T-9`.
- [x] Namen der Service-Befehle: passen nach E20 (geändert 22.11.2026) – nichts zu tun.
- [ ] Tests:
  - `ToolServiceTest`: feste Namen „Kreissäge“/„Bandsäge“ (`:84-85, 135-136`); Aufbau inline statt `given…`
    (`:232, 277, 326, 384, 405, 433`); `:450` Deleted-Payload; fehlt: T-15 zweite Hälfte (eigener Name plus anderer in
    anderer Schreibweise).
  - `CommandBuilder:153`: feste IP statt `RandomValues.ipAddress()`.
  - Controller: `never()` doppelt (`:246, 259`) → `expectNothingCreated()`; JSON als String (`:404, 464, 664`); fehlt:
    clearRfidReader 409 `TOOL_IN_USE` (T-14), delete 409 (T-17) und 404, 404 für update, assignRfidReader, setWlanRelais,
    clearWlanRelais, add/removeInstructors (T-22), 400 für setWlanRelais ohne IP (T-5), Anlegen ohne area/requirement
    (T-10), Ändern mit leerem Namen (T-1).
  - `ToolAssignRfidReaderTest:75` ohne `code`; `verifyNoInteractions` dreimal (`:63, 76, 89`); `:88` ohne `resource`;
    ebenso `ToolDeleteTest:83` (dazu `:54-56` inline), `ToolAddInstructorsTest:59`, `ToolRemoveInstructorsTest:59`.
  - `WlanRelaisTypeTest:12`: Name passt zu keiner Methode, der Test baut die URL selbst.
  - Akzeptanz: t9 nur abgelehnt (`:102`); t3_t8 (`:55`) zeigt nicht T-3s erlaubte Seite; t22 fehlt; `expectSuccess` auf
    Gerät (`:140`); `RandomValues` `:36, 61, 84, 122`.
  - Technisch: `findByNameIgnoreCase`, `findByInstructorIdsContaining`.

### `tool_certification`

- [ ] README: Payloads von PAUSED / REACTIVATED / REVOKED tragen id, customerId, toolId, status (`:68-70`); historische
  Notiz zu `WithEvents` (`:23-25`) entfernen.
- [ ] `ToolCertificationApiModel:48`: `// request` → `// request body`.
- [ ] `ToolCertificationController:93`: `@RequestBody` ohne `@NotNull`; `:81` ohne Filter ein nacktes `badRequest()` ohne
  `ProblemDetail`.
- [ ] Tests:
  - `ToolCertificationServiceTest:437` `expectSavedEvent` ohne Id; `:103-120` Payload ohne id; `:293, 325, 353`
    Deleted-Payload (`TOOL_CERTIFICATION_DELETED` trägt customerId, toolId); wiederholt `never().save` `:136, 153`,
    `deleteAll` `:334, 362`, `never().deleteAll` `:348, 376`; `:427` `createCommandFor` im Abschnitt `// given`.
  - Controller: 404 ohne Code (`:156, 229, 336`); `BatchCertifyRequestBody` als Body (`:173, 187`), verkettet (`:201`);
    `getToolCertifications(String filter, String id)` (`:407, 411`) → Überladungen; fehlt: Batch 409
    `CUSTOMER_NOT_INSTRUCTOR`, `CERTIFICATION_ALREADY_EXISTS`, `CERTIFICATION_PERMANENTLY_REVOKED`, 404; 404 für
    reactivate, revoke; Filter ohne Treffer; beide Filter.
  - `ViewBuilder.toolCertificationView()` ohne Überladung mit Id (`:154`).
  - `ToolCertificationCreateTest`: `new CreateToolCertificationCommand` und Verifikation inline (`:60-62`); `:74, 105`
    ohne `resource`.
  - Akzeptanz: tc1, tc4, tc5 nur abgelehnt (`:26, 63, 82`); `:52` Ausgangszustand über `then()` + `api()` statt
    `given()`; fehlt: tc8, 404 für pause/reactivate/revoke/delete (Suche nach Person + Werkzeug), pausierte Einweisung an
    der Schaltbox (`SchaffbarApi.pauseToolCertification`).

### `tool_usage`

- [ ] E6 nach der Entscheidung (`ToolUsageService:55, 62, 68, 82`, `ToolUsageRepository:28`, Meldung von
  `ToolNotInUseByCustomerException`).
- [ ] `ToolUsage.getDuration()` (`:92-98`) nur von Tests genutzt, README TU-7 – löschen.
- [ ] `ToolUsage.java:121-122`: leerer Abschnitt `// helper`.
- [ ] Guard in `getFinalBillableDuration` (`ToolUsageService:86, 163`) → `ensure…` mit Kommentar, wer vorher prüft
  (`WorkshopSessionClose`).
- [ ] Tests:
  - `ToolUsageServiceTest:302` `expectSavedEvent` ohne Id; `:126-144` Payload ohne id; fehlt: `getBillingTotalUntil`.
  - `ToolUsageTest:57-60`: Event-Prüfung inline statt `expectSingleEvent`.
  - `ToolUsageStartTest`: `new StartToolUsageCommand` (`:66, 81`), Verifikation doppelt; `:129, 142` ohne `resource`.
  - `ToolUsageToggleTest:47-48, 62-63` → `expectSwitchedOn()` / `expectSwitchedOff()`.
  - `SwitchBoxAcceptanceTest`: `:141` zwei Antworten in einer Methode; Ablehnungen ohne „Werkzeug läuft nicht“
    (`:193, 218, 254, 283`); `:234-247` ohne `Scenario`; fehlt: RED-Werkzeug mit Einweisung schaltet ein.
  - TU-3, TU-8, TU-12 sind über API und Geräte nicht erreichbar – Kommentar, wo sie bewiesen sind.

### `workshop_usage`

- [ ] E6 nach der Entscheidung (`WorkshopUsageService:44, 60, 66`, `WorkshopUsageRepository:21`).
- [ ] `WorkshopUsage.exit()` → `stop()` (Entscheidung oben), mit Tests und Kommentar an der Absicherung (E29).
- [ ] TODO `WorkshopSlot` (`WorkshopUsage.java:35`, README `:7`) und `EventType:73` widersprechen der Entscheidung, nicht
  umzubenennen – entfernen.
- [ ] `WorkshopUsage.getDuration()` (`:82-88`) nur von Tests genutzt, README WU-3 – löschen.
- [ ] Guard in `getFinalBillableDuration` (`WorkshopUsageService:70, 121`) → `ensure…`.
- [ ] README: Regel „Auschecken ohne offene Sitzung“ fehlt (`WorkshopUsageStop:47-51`); WU-8 sagt `STATE: ERROR`, das
  Gerät bekommt `STATE: END` mit Text in `ERROR`.
- [ ] Tests:
  - `WorkshopUsageServiceTest:208` `expectSavedEvent` ohne Id (`:155` umgeht es inline); `:108-123` Payload ohne id.
  - `WorkshopUsageTest:57-60` inline.
  - `WorkshopUsageStartTest:56-58, 70-71`, `WorkshopUsageStopTest:55-57` inline; `:83` / `:84` ohne `resource`.
  - `GateKeeperInAcceptanceTest`: Prüfungen direkt im Test (`:81-82, 113-115`); `:128` Ablehnung ohne „weiter ein
    Besuch“; Namen ohne Situation.
  - `GateKeeperOutAcceptanceTest`: Prüfungen direkt (`:80-81, 111` mit Index `[0]`); `:146` `expectSuccess` auf Gerät.

### `workshop_session`

- [ ] ✔ `WorkshopSession.close()` (`:101-106`) setzt die Felder direkt statt über die Setter.
- [ ] ✔ `WorkshopSessionController:35, 49`: `@PathVariable` ohne `@Valid`; `:52` `close` antwortet `200` statt `204`.
- [ ] `WorkshopSessionService:82`: `throwHasAlreadyOpenWorkshopSession` → `throw<Exception>`-Muster.
- [ ] `closeWorkshopSession` nach der Entscheidung: `CloseWorkshopSessionCommand`, Laden über die Id – damit entfällt
  das doppelte Laden und Prüfen (`WorkshopSessionClose:42-44`, `WorkshopSessionService:63-64`); README WS-4 → Use Case,
  WS-2 → Absicherung in der Entity.
- [ ] `WorkshopSessionApiModel:55-57, 98-100`: leere Abschnitte.
- [ ] Tests:
  - `WorkshopSessionServiceTest:136` `expectSavedEvent` ohne Id (`:98` inline).
  - `WorkshopSessionTest:59-62` inline.
  - Controller: `:121` 404 ohne Code; `:182, 186` `OpenWorkshopSessionView.builder()`, `ToolUsageSummaryView.builder()`
    mit Literalen (`:91-92, 101-102, 188`) → ViewBuilder; fehlt: close 409 `ACTIVE_WORKSHOP_USAGE_EXISTS_FOR_SESSION`
    (der einzige, den die API erzeugt), close 404.
  - `WorkshopSessionCloseTest:121`, `WorkshopSessionReadFacadeTest:154`: ohne `resource`.
  - WS-6 (gespeicherte Summen) ist über die API nicht lesbar – Kommentar, wo es bewiesen ist.
  - `WorkshopFlowAcceptanceTest:74` Prüfung direkt im Test (`expectNewWorkshopSession`); `RandomValues` `:30-31`,
    `WorkshopSessionAcceptanceTest:38, 65`.
  - Technisch: `default`-Methode `findOpenWorkshopSession()`.

## 4. Übergreifend

### `shared/`

- [ ] `SchaffbarEvent`: `implements ValidationSupport` ohne Aufruf (`:53`), `@NotBlank` auf `UUID id` (`:59`), öffentliche
  Konstruktoren (`:49-50`, nur die EventFactory soll Events bauen); auskommentiertes `@JsonFormat` (`:75`); `owner` nie
  gesetzt (`:81`, bleibt – Entscheidung Outbox); Tippfehler „afsEvent“ (`:166`); `toJsonNode` ungenutzt (`:200-207`); `validatePayload` doppelt zu
  `ValidationSupport` (`:209-215`).
- [ ] `ValidationSupport:12`: neue `ValidatorFactory` pro Aufruf, nie geschlossen → ein statischer `Validator`.
- [ ] Toter Code: `EventType.toEnum` (`:79-87`) und `UnsupportedEventTypeException`; `ResourceNotFoundException.Resource`
  `TOOL_USAGE`, `WORKSHOP_SESSION`, `WORKSHOP_USAGE` (Schreibweise uneinheitlich); `ValueObjectMapper:9-31` (die sechs
  `to…`-Methoden); `ValueObjectAssert.positive` (`:16, 30-40, 53-55`); `StringToIpAddressConverter`.
- [ ] `OutboxEvent:75`: TODO „whole event as JSON“ ist falsch – die Spalte enthält schon das ganze Event.
- [ ] Outbox nach der Entscheidung (oben): `markAsProcessed`, `findByProcessedFalseOrderByCreatedAtAsc` löschen; Felder
  bleiben, `owner` bleibt ungesetzt.
- [ ] `ResourceNotFoundException.CODE` nach der Entscheidung (oben), dazu `ExceptionArchitectureTest:40`.
- [ ] Abschnittsnamen: `static factory method`, `static factory methods`, `static factories` nebeneinander – nach
  `CLAUDE.md` (22.11.2026): `// static constructor` über dem einen `of(…)` (`OutboxEvent`), `// static factories` über
  mehreren (`SchaffbarEvent`, `EventType`).
- [ ] Tests: `ApiExceptionHandlerTest` ruft `mockMvc.perform` selbst (`:44-111`); `BillableDurationTest`,
  `BillingTotalTest` ohne Abschnitte und Methodenpräfix; `IpAddressTest` fehlt.

### `web/device`, `web/dashboard`, Read Facades

- [ ] Anzeigetexte im Controller statt in `DeviceMessages`: „Nicht Gefunden“ (`:73`), „Falsches Lesegerät“
  (`:102, 138`), „Lesegerät ohne Typ“ (`:103, 139`), „Kein Werkzeug“ (`:170, 216`), „Kein WLAN-Relais“ (`:174`); ein
  Text für „ohne Typ“ (`/init` sagt „Nicht Gefunden“), R-9 im README anpassen.
- [ ] `/card`: nach dem Umschalten fragt `isToolInUse` (`:221`) ab, was der Toggle gerade entschieden hat; `/init` liest
  den Leser zweimal (`:159-163`).
- [ ] `DeviceApiModel`: privater `@Builder` an den Request-Records ungenutzt (`:266, 271, 277`); Parameter `type` von
  `enterOk`, `leaveOk`, `startToolUsage`, `stopToolUsage` immer gleich; `CounterResponse.unexpectedRfidReaderType`
  (`:151`) auch für „Leser nicht gefunden“.
- [ ] `DeviceController`: zwei Leerzeilen (`:61-62`); keine Abschnitte; Ablehnungen mal `error` (`:94, 129, 215`), mal
  `info` (`:109, 145`).
- [ ] Zuweisung ohne Person → `IllegalStateException`, `DeviceMessages` ohne `CUSTOMER` (Entscheidung oben).
- [ ] Tests: `DeviceReadFacadeTest` fehlt Zuweisung ohne Person (`IllegalStateException`) und `getWorkshopStay` ohne Besuch; `:199-203`
  `finishedWorkshopUsageView()`; `DashboardControllerTest:50, 84` und `DashboardReadFacadeTest:70-71, 103` bauen Views
  mit `new` und prüfen Literale; `DashboardControllerTest:62` nur `entryTime` `exists()`.

### Integrationstests und Fixture

- [ ] Gerätaufrufe mit `hasStatus(OK)` / `expectSuccess` können nie scheitern: `GivenFixture:99, 117`
  (`customerLeftTheWorkshop`), `CustomerAcceptanceTest:95`, `ToolAcceptanceTest:140`, `GateKeeperOutAcceptanceTest:146` –
  den Zustand prüfen (`ICON`, kein Besuch läuft), `given().toolSwitchedOff(…)`.
- [ ] Abschnitte nach Aggregaten: `GivenFixture` „customers“ (`:64-118`) und „tools“ (`:120-187`), `ThenFixture`
  „workshop sessions“ (`:191-237`) enthalten andere Aggregate; `fullNameOf` ist kein `expect…`.
- [ ] `SchaffbarApi:233` `certifyCustomers` schreibt `Json.write(Map.of(…))` → Textblock.
- [ ] Device-Tests nach `<endpoint>_<situation>_<answer>`: `card_checksInAndGreetsWithTheEntryTime`,
  `card_checksOutWithEntryAndExitTime`, `card_switchesTheRunningToolsOff`, `counter_isTheWrongReader` (3×),
  `card_isTheWrongReader` (2×), `init_startsWithoutRelay` (4×).

### ArchUnit

- [ ] `aggregatesDoNotExposeUuids` übersieht `Set<UUID>` (`Tool.getInstructorIds`) → `mentions(…)`.
- [ ] Eine Read Facade pro Controller, die nach ihm benannt ist.
- [ ] `controllersKnowOnlyUseCasesAndReadFacades` verbietet nur Domain-Services → positiv: keine anderen Beans.
- [ ] Code-Regeln der `CLAUDE.md` gelten für alle Beans, ArchUnit prüft sie nur für Domain-Services
  (`serviceFieldsArePrivateFinal`, `servicesTakeCommandsOrValueObjects`, `serviceParametersAreValidated`); Views der Read
  Facades außerhalb der View-Regeln; Controller `@Validated`, `@PathVariable` / `@RequestParam` als Value Object; keine
  Schreibzugriffe in `@Query`.
- [ ] `WRITE_TRANSACTION` mit Resten aus dem Rollout (`ArchitectureSupport:66-69, 108-116`).
- [ ] `useCasesAreNamedAfterTheirAggregate` prüft nur das Präfix (nach der Entscheidung zu E3).
- [ ] `TestArchitectureTest` findet keine Stubs in `@BeforeEach` und in Helfern, die nicht `given…` heißen.
- [ ] Zeilen über 160 Zeichen: `ArchitectureSupport:101`, `DomainArchitectureTest:385, 442`.

### Übersichten

- [ ] `core-pos/docs/ddd-overview.md` / `ddd-uebersicht.md`: §4 `ToolCreate` („optional RFID reader validation“) stimmt
  nicht; es fehlen `ToolUsageToggle`, `ToolDelete`, `CustomerDelete`, `RfidReaderDelete`, `ToolAdd/RemoveInstructors`;
  Einleitung „Use Cases orchestrieren“ ist älter als E2; §5 ohne `DeviceReadFacade`; `RFID_READER_TYPE_CHANGED` fehlt
  (`:229` / `:230`).
- [ ] Outbox: „reliable event publishing for downstream consumers“ → heute Änderungsprotokoll, kein Relay
  (Entscheidung oben).

## 5. Fehler im Verhalten (zum Schluss)

- [ ] ✔ **Geräte bekommen 500 statt ihrer Antwort**: `/counter` baut `RfidTagId.of(…)` außerhalb des `try` und fängt beim
  Leser nur `ResourceNotFoundException` (`DeviceController:87-96`), `/card` im ersten `try` nur RNF/BRV (`:124-131`) – ein
  leerer Tag, eine kaputte MAC oder ein Datenbankfehler gehen an Springs Standard-500 vorbei an `DeviceMessages`.
  `@Valid` an den Device-Requests prüft nichts (keine Constraints). Mit Akzeptanztest „kaputte MAC“.
- [ ] ✔ **Uhrzeit am Gerät hängt an der Zeitzone der JVM** (`DeviceApiModel:84, 174, 189, 192`); Image und Compose
  setzen kein `TZ` – auf dem Server prüfen, feste Zone `Europe/Berlin`; betrifft auch `LocalDate.now()` der
  Altersgruppe (Entscheidung oben).
- [ ] ✔ **`IpAddress` nimmt `+1.+2.+3.+4` und `-0.-0.-0.-0`** (`Integer.parseInt` erlaubt ein Vorzeichen,
  `IpAddress:35`) – nur Ziffern zulassen; dazu `IpAddressTest`.
- [ ] ✔ **`Tool` gibt `Set<UUID> instructorIds` veränderbar heraus** (Klassen-`@Getter`, `Tool.java:42, 73-76`) – Änderung
  ohne Event möglich; `@Getter(AccessLevel.NONE)` am Feld.
- [ ] **`ToolCertificationCreate` prüft TC-8 vor der Existenz der Personen** (`:43-46, 63-70`): unbekannte Person plus
  Nicht-Einweiser gibt 409 statt 404.
- [ ] **`ToolRepository.findByIpAddress`** (`:19`) liefert `Optional` auf eine Spalte ohne Unique-Constraint – Liste und
  „höchstens eine“ im `default`.
- [ ] **`BillableDuration`** (`:25-26`): negative Dauer (kaputte Daten) als `IllegalArgumentException` → 400 statt 500.
- [ ] **`SchaffbarEvent`**: `@NotBlank` auf `UUID` würde bei einer Prüfung mit `UnexpectedTypeException` scheitern (siehe
  `shared/`).

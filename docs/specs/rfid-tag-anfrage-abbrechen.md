# Eine wartende Tag-Anfrage lässt sich im Portal abbrechen (TA-10)

<!--
Übergabedatei der Agenten-Pipeline (docs/aufgaben.md, „Agenten-Pipeline“): eine Datei pro Aufgabe, angelegt von der
Rolle Spec als Kopie dieser Vorlage – docs/specs/<kurzname>.md, Kurzname auf Deutsch in kebab-case
(r6-lesegeraete-namen-eindeutig.md). Sie liegt auf dem Branch der Aufgabe und wird nach dem Merge gelöscht.

- Jede Rolle schreibt nur ihren eigenen Abschnitt; die Abschnitte davor liest sie, ändert sie aber nicht.
- Was eine Rolle wissen muss, steht in dieser Datei – nicht im Chat-Verlauf.
- Eine Frage, die erst später auftaucht (das Backend findet einen Widerspruch zur Spec), kommt unter „Offene Fragen“
  im eigenen Abschnitt – und die Rolle hält an, statt zu raten.
- Nur Piotr beantwortet Fragen und gibt frei.
- Die Kommentare dieser Vorlage dürfen stehen bleiben.
-->

- **Quelle:** `docs/aufgaben.md`, Abschnitt „Backend-Funktionen für das Portal“ → „RFID-Tag-Zuweisung“, Zeile
  „Anfrage abbrechen“ (Punkt 2 der dortigen Reihenfolge) – zweiter Pilot der Agenten-Pipeline (Stufe 1), eine Funktion,
  keine neue Regel (TA-10 gibt es); Artboard „RFID-Tag zuweisen“ des Prototyps (`project/RfidAssignDialog.dc.html`),
  dazu `project/UserDetail.dc.html` und `project/RfidTags.dc.html`
- **Branch:** main (Pilot, lokal)
- **Stand:** Review fertig

## 1. Spec

### Auftrag

<!-- Was soll sich ändern und warum – zwei, drei Sätze, aus Sicht der Werkstatt. -->

An der Theke soll das Team eine Tag-Anfrage, die auf das Lesegerät wartet, wieder abbrechen können – mit „Zuweisung
abbrechen“ im Dialog „RFID-Tag zuweisen“ und auf der Karte „RFID-Tag“ im Profil der Person, solange sie „Wartet auf
Tag“ zeigt. Heute bleibt eine Anfrage stehen, bis ein Tag an das Lesegerät gehalten wird oder die Person gelöscht wird;
weil in der ganzen Werkstatt nur eine Anfrage warten darf (TA-3), blockiert eine vergessene oder mit der falschen Art
(Leihtag / fester Tag) gestellte Anfrage jede weitere. Das Backend kann abbrechen (TA-10), es fehlt der Endpunkt.

### Heute

<!-- Was es schon gibt: Regeln in den README der Aggregate, Endpunkte, Seiten und Dialoge im Portal, mit Pfaden. -->

**Backend** (Pfade ab `schaffbar-backend/core-pos/src/main/java/de/schaffbar/core_pos/`):

- `domain/rfid_tag_assignment/README.md`: TA-10 „A waiting request (`WAITING_FOR_ASSIGNMENT`) can be cancelled – it is
  deleted without history“ steht unter „Business Rules / Invariants“, „Enforced in“
  `RfidTagAssignmentService.cancelRfidTagAssignment()` (`NoRfidTagAssignmentWaitingException`, `409`), „used by
  `CustomerDelete`; no endpoint yet“. Tabelle „Commands“: „Cancel request | `cancelRfidTagAssignment` | customerId |
  – (`CustomerDelete`, C-4)“. Lifecycle „`WAITING_FOR_ASSIGNMENT` → (deleted on cancel, no history)“. TA-3 („Only one
  request … may wait at a time – in the whole workshop“) in `requestRfidTagAssignment()`
  (`RfidTagAssignmentAlreadyWaitingException`, `RFID_TAG_ASSIGNMENT_ALREADY_WAITING`).
- `domain/rfid_tag_assignment/RfidTagAssignmentService.cancelRfidTagAssignment(CustomerId)`: sucht die Zuweisung der
  Person, nur wenn sie wartet (`findByCustomerId` + `isWaitingForAssignment`), sonst
  `NoRfidTagAssignmentWaitingException.forCustomer(customerId)` (`409`, `NO_RFID_TAG_ASSIGNMENT_WAITING`, auch wenn der
  Tag schon zugewiesen ist); dann `RfidTagAssignment.cancel()` (Absicherung mit `IllegalStateException`), `delete`,
  Event `RFID_TAG_ASSIGNMENT_CANCELLED` (Payload id, customerId) in die Outbox. Keine Historie.
- Einziger Aufrufer heute: `use_case/CustomerDelete.releaseRfidTag()` (C-4). Es gibt keinen Use Case
  `RfidTagAssignmentCancel` und keinen Endpunkt.
- `domain/rfid_tag_assignment/web/RfidTagAssignmentController` (`/api/v1/rfid-tag-assignments`): `GET` (Filter
  `?customerId=`, `?rfidTagId=`), `GET /history`, `POST` (Anfrage, Body `{customerId, assignmentType}`, `204`),
  `PUT /assign` (Body `{rfidTagId}`), `PUT /unassign` (Body `{customerId}`, `204`). `RfidTagAssignmentApiModel` mit
  `UnassignRfidTagRequestBody(@NotNull @Valid CustomerId customerId)`.
- Vorbild Use Case: `use_case/RfidTagAssignmentUnassignRfidTag` – Person laden (`404` `ResourceNotFoundException.customer`),
  dann `unassignRfidTag(customerId)`. Konverter für Pfadvariablen: `shared/util/StringToCustomerIdConverter` gibt es,
  einen für `RfidTagAssignmentId` nicht.
- Pfade anderswo: `PUT /workshop-sessions/{customerId}/close` (eine Sitzung, über die Person adressiert, im Pfad);
  Skill `rest-api`: „an action without values: `PUT /{id}/<verb>`, no body“, „The id comes from the path, never from
  the body“ – `PUT /rfid-tag-assignments/unassign` mit der Person im Body weicht davon ab (Frage 1).
- Tests: `domain/rfid_tag_assignment/RfidTagAssignmentTest` (`cancel_…`), `RfidTagAssignmentServiceTest`
  (`cancelRfidTagAssignment_deletesTheWaitingRequestWithoutHistory`, `…_rejectsAnAssignedTag`,
  `…_rejectsACustomerWithoutAssignment`), `use_case/CustomerDeleteTest`, `integration/CustomerAcceptanceTest.c4_…`.
  `integration/RfidTagAssignmentAcceptanceTest` hat `ta1_` bis `ta8_` (ohne `ta4_`), **kein `ta10_`**;
  `integration/fixture/SchaffbarApi` hat `unassignRfidTag(CustomerId)`, nichts zum Abbrechen.
- Geräte: `web/device/DeviceMessages` hat für `NO_RFID_TAG_ASSIGNMENT_WAITING` „RFID nicht zugewiesen“ – das zeigt das
  Lesegerät an der Theke, wenn ein Tag ohne wartende Anfrage aufgelegt wird (TA-4). Kein Gerät bricht ab.
- Bruno `core-pos/api/rfid tag assignment/`: sieben Requests (u. a. `Unassign RFID tag.bru`, `seq: 6`), keiner zum
  Abbrechen. `core-pos/docs/ddd-overview.md` (und `ddd-uebersicht.md`), Tabelle der Use Cases: kein
  `RfidTagAssignmentCancel`.

**Portal** (Pfade ab `schaffbar-portal/src/app/`):

- Dialog „RFID-Tag zuweisen“ `users/components/assign-rfid-tag-dialog/`: drei Schritte aus dem Zustand der Zuweisung
  (`step` = `computed`): ohne Zuweisung „Art wählen“ (`choose`), wartend oder während der Anfrage (`requestPending`)
  „Tag auflegen“ (`wait`), zugewiesen „Fertig“ (`done`). Im Schritt `wait` nur der Button „Schließen“
  (`shared.actions.close`) und der Text „Der Dialog kann geschlossen werden – die Anfrage bleibt bestehen.“
  (`users.dialogs.assignRfidTag.waiting.message`); während der Anfrage noch ohne Zuweisung der Titel „Tag wird
  angefordert …“. Inputs `userId`, `userName`, `rfidTagAssignment`, `requestPending`, Output `assignmentRequested`
  (`AssignRfidTagCommand`).
- Geöffnet von `users/pages/user-detail/user-detail-dialogs.service.ts` (`assignRfidTag()`) mit `inputBinding` auf
  `store.rfidTagAssignment` / `store.assignRfidTag.isPending` und `outputBinding` auf `store.assignRfidTag` – der Dialog
  folgt dem Store, der die Zuweisung alle 3 s neu lädt, solange sie wartet (`pollRfidTagAssignment`,
  `RFID_TAG_POLL_INTERVAL_MS`; Entscheidung „„RFID-Tag zuweisen“ wartet“, 09.11.2026 – dort „2 Sekunden“, im Code 3000 ms).
- Karte „RFID-Tag“ `users/components/user-rfid-tag-card/` (Tab „Übersicht“, `user-detail-tab-overview`): im Zustand
  `WaitingForAssignment` Sanduhr, „Wartet auf Tag“, Art · „an das Lesegerät an der Theke halten“ und nur der Button
  „Zuweisung anzeigen“ (öffnet den Dialog über `assignmentRequested`). Zugewiesen: „Tag entfernen …“ (`danger-button`,
  `ConfirmActionDialogService` mit `destructive: true`, Output `rfidTagUnassigned` mit `UnassignRfidTagCommand`).
- Kopf der Seite (`user-detail.component.html`): Badge „Wartet auf Tag“; das ⋯-Menü bietet „RFID-Tag zuweisen“ nur
  ohne Zuweisung.
- Store `users/pages/user-detail/user-detail.store.ts`: `assignRfidTag`, `unassignRfidTag` als `mutation`, beide laden
  bei Erfolg `_assignmentResource` neu; kein `onError`. Modell `users/rfid-tag-assignment.model.ts` mit
  `AssignRfidTagCommand`, `UnassignRfidTagCommand { userId }`, `isWaitingForRfidTag`; Mapper
  `users/rfid-tag-assignment.mapper.ts`.
- API-Schicht `shared/api/rfid-tag-assignments/`: `requestRfidTagAssignment`, `unassignRfidTag` (`PUT …/unassign` mit
  `UnassignRfidTagRequest { customerId }`); kein Abbrechen, kein `PUT /assign`.
- Seite RFID-Tags `rfid-tags/pages/rfid-tag-list/`: Banner „Wartet auf Tag“ über der Liste (Entscheidung 08.11.2026)
  mit Name, Art, Hinweis und Link „Zum Profil →“, ohne Aktion; der `RfidTagsStore` (Feature `rfid-tags`) lädt nicht
  von selbst neu.
- Übersetzungen `schaffbar-portal/public/i18n/de.json` / `en.json`: `shared.errors.NO_RFID_TAG_ASSIGNMENT_WAITING`
  de „Niemand wartet auf einen Tag.“, en "Nobody is waiting for a tag."; `RFID_TAG_ASSIGNMENT_ALREADY_WAITING` de „Eine
  andere Person wartet noch auf ihren Tag – zuerst deren Tag an das Lesegerät an der Theke halten.“; kein Schlüssel
  „Zuweisung abbrechen“. Der Fehler-Interceptor zeigt `shared.errors.<CODE>`.

**Prototyp** (mit dem Werkzeug Artifact gelesen am 25.11.2026):

- `project/RfidAssignDialog.dc.html`, Schritt „Tag auflegen“: unter dem Text **nur** der Button „Zuweisung abbrechen“
  (umrandet, neutral – kein Rot), geschlossen wird über das X im Kopf. Ein Klick geht **ohne Rückfrage zurück auf
  „Art wählen“** (`back`), der Dialog bleibt offen. Text dort: „Du kannst den Dialog auch schließen – die Zuweisung
  bleibt aktiv.“
- `project/UserDetail.dc.html`: die Karte „RFID-Tag“ zeigt nur einen zugewiesenen Tag („Tag ersetzen …“, „Tag
  entfernen …“) – **einen Zustand „Wartet auf Tag“ der Karte gibt es dort nicht**; der Button auf der Karte hat kein
  Vorbild.
- `project/RfidTags.dc.html`: Kasten „Wartet auf Tag (1)“ in der Seitenleiste mit Link auf die Person, ohne Aktion.

**Daten** (lesend `GET http://127.0.0.1:5000/api/v1/rfid-tag-assignments` am 25.11.2026, lokales Backend): drei
Zuweisungen, alle `ASSIGNED`, keine wartet. Für diese Aufgabe ohne Bedeutung (keine Migration, keine neue Regel).
Nicht geprüft: die Daten auf dem Server der Werkstatt – nicht nötig.

### Vertrag

<!--
Die Entscheidungen, gegen die Backend und Portal bauen. Für eine Geschäftsregel die Tabelle aus dem Skill
business-rule („The contract“): Id und Regel, genaue Bedingung, wann geprüft wird, bestehende Daten, Ort der Regel,
Exception und Code, Fehlertext de/en, API-Änderung, Verhalten im Portal, Geräte. Für anderes: geänderte Endpunkte und
Felder, Fehlercodes, Verhalten im Portal, Geräte. Jede Zeile entschieden oder als offene Frage unten.
-->

| Entscheidung                 | Festgelegt                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
| ---------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Regel                        | Keine neue Regel, keine neue Id: TA-10 bekommt seinen Endpunkt; die Bedingung bleibt, wie sie ist – abbrechen lässt sich nur eine wartende Anfrage (`WAITING_FOR_ASSIGNMENT`) der Person, sie wird ohne Historie gelöscht (Service, README). Wer abbricht, prüft das Backend nicht (es gibt keine Benutzer im Backend).                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| Endpunkt                     | `PUT /api/v1/rfid-tag-assignments/{customerId}/cancel` **ohne Body** (Frage 1 (b), entschieden 25.11.2026) – Skill `rest-api` („an action without values: `PUT /{id}/<verb>`, no body“), wie `PUT /workshop-sessions/{customerId}/close`; Pfadvariable `@PathVariable @NotNull @Valid CustomerId customerId` (`StringToCustomerIdConverter` gibt es), kein `consumes`, kein Request-Body, kein neuer Record im `RfidTagAssignmentApiModel`. Antwort `204` ohne Body (Skill `rest-api`: „every other command“). `PUT /unassign` mit der Person im Body bleibt, wie es ist – die Abweichung hält die Rolle Backend in `docs/entscheidungen.md` fest (Umfang).                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| Use Case                     | Neu `use_case/RfidTagAssignmentCancel` – Name `<Aggregat><Befehl der Entity>`, Befehl `cancel()` (Skill `use-case`, „Name“). `process(@NotNull @Valid CustomerId customerId)` mit `@Command`, `void`: erst die Person laden (`orElseThrow(() -> ResourceNotFoundException.customer(customerId))`, `404`), dann `rfidTagAssignmentService.cancelRfidTagAssignment(customerId)` – wie `RfidTagAssignmentUnassignRfidTag` und TA-7. Der Service bleibt unverändert.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| Fehler                       | `404` `RESOURCE_NOT_FOUND`, `resource: CUSTOMER` – unbekannte Person (Use Case). `409` `NO_RFID_TAG_ASSIGNMENT_WAITING` – die Person hat keine wartende Anfrage: keine Zuweisung, schon zugewiesen (z. B. der Tag wurde gerade aufgelegt) oder schon abgebrochen (Service, bestehende `NoRfidTagAssignmentWaitingException.forCustomer`). `400` – keine gültige UUID (Konverter bzw. Body). Keine neue Exception, kein neuer Code.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| Fehlertext de/en             | `shared.errors.NO_RFID_TAG_ASSIGNMENT_WAITING` wird neu geschrieben, weil das Portal ihn jetzt beim Abbrechen zeigt und „Niemand wartet auf einen Tag.“ dort falsch klingt (die Person sah eben noch „Wartet auf Tag“): de „Keine Anfrage wartet mehr auf einen Tag – vielleicht wurde der Tag gerade zugewiesen oder die Anfrage schon abgebrochen.“, en "No request is waiting for a tag any more – the tag may just have been assigned or the request cancelled." Er passt zu beiden Varianten (`forCustomer`, `forRfidTag`) und sagt, was den Abbruch sperrt; zu tun ist nichts – die Seite zeigt den Stand gleich selbst (Zeile „Verhalten im Portal“). Grundlage: Entscheidung „Fehlertexte“ (15.11.2026), Skill `rest-api` („a text … fitting every variant“); bestätigt von Piotr im Chat, 25.11.2026 („Fehlertext passt so“). `DeviceMessages` („RFID nicht zugewiesen“) bleibt.                                                                                                                                                                                                                                                                                                                                                                                                                          |
| Fehlertext TA-3              | `shared.errors.RFID_TAG_ASSIGNMENT_ALREADY_WAITING` wird neu geschrieben, damit er den neuen Ausweg aus der Sperre nennt (Frage 3 (b), entschieden 25.11.2026): de „Eine andere Person wartet noch auf ihren Tag – zuerst deren Tag an das Lesegerät an der Theke halten oder ihre Anfrage abbrechen (Seite RFID-Tags zeigt, wer wartet).“, en "Another person is still waiting for their tag – hold their tag to the reader at the counter first, or cancel their request (the RFID tags page shows who is waiting)." Code und Exception bleiben; nur das Portal zeigt den Text (`POST /rfid-tag-assignments`, kein Gerät fragt an). Das Banner „Wartet auf Tag“ auf der Seite RFID-Tags bleibt ohne Aktion, sein Link „Zum Profil →“ führt zur Karte.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| API-Änderung                 | Nur der neue Endpunkt (Zeile „Endpunkt“), sonst nichts: kein neues Feld, `GET` unverändert; das Portal hat die Zuweisung mit Status und Person schon (`GET /rfid-tag-assignments?customerId=`). Portal `shared/api/rfid-tag-assignments/`: neue Methode `cancelRfidTagAssignment(customerId: string): Observable<void>` im API-Service, `PUT …/{customerId}/cancel` ohne Body – kein Request-Typ, kein Request-Mapper (Skill `feature`: Pfadvariablen bekommen keinen; der Store gibt `command.userId` weiter). Bruno: neuer Request „Cancel RFID tag assignment“ im Ordner `rfid tag assignment/`.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| Verhalten im Portal – Dialog | Im Schritt „Tag auflegen“ ein Button „Zuweisung abbrechen“ (umrandet, `matButton="outlined"`, **kein** `danger-button`), nur wenn es eine wartende Zuweisung gibt – nicht während „Tag wird angefordert …“ (dann gibt es noch nichts abzubrechen). Ein Klick bricht **ohne Rückfrage** ab (wie der Prototyp; Skill `dialog`: nur ein `danger-button` fragt; verloren geht nichts, was zwei Klicks nicht wiederherstellen). Danach zeigt der Dialog wieder „Art wählen“ und bleibt offen (Prototyp `back`) – das ergibt sich aus dem Store: die Zuweisung ist neu geladen `null`, `step` wird `choose`. Der Dialog gibt einen Command aus (Output, Event-Name, z. B. `rfidTagAssignmentCancelled` mit `CancelRfidTagAssignmentCommand { userId }`), der Dialog-Service bindet ihn mit `outputBinding` an die Store-Methode. Während der Abbruch läuft, ist der Button gesperrt (`isPending` der Mutation per `inputBinding`). In der Fußzeile des Schritts stehen **beide** Buttons: „Zuweisung abbrechen“ und das bestehende „Schließen“ (`shared.actions.close`, schließt mit `undefined`, die Anfrage bleibt) – anders als im Prototyp, der nur „Zuweisung abbrechen“ hat (Frage 2 (a), entschieden 25.11.2026); beide umrandet, die Rolle Portal hält die Abweichung in `docs/entscheidungen.md` fest (Umfang). |
| Verhalten im Portal – Karte  | Karte „RFID-Tag“ im Zustand „Wartet auf Tag“: neben „Zuweisung anzeigen“ ein zweiter umrandeter Button „Zuweisung abbrechen“, ohne Rückfrage, kein `danger-button` (Gründe wie im Dialog). Output der Karte mit `CancelRfidTagAssignmentCommand { userId }` (Event-Name wie `rfidTagUnassigned`), der Tab „Übersicht“ reicht ihn an den Store. Danach zeigt die Karte „Kein RFID-Tag zugewiesen.“ mit „RFID-Tag zuweisen …“, das Badge im Kopf verschwindet, das ⋯-Menü bietet wieder „RFID-Tag zuweisen“ – alles aus der neu geladenen Zuweisung. Die Buttons brechen auf dem Handy um (`.actions` hat `flex-wrap`).                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
| Verhalten im Portal – Store  | `users/rfid-tag-assignment.model.ts`: `CancelRfidTagAssignmentCommand { userId }` (Abschnitt `// commands`). `user-detail.store.ts`: `cancelRfidTagAssignment: mutation<CancelRfidTagAssignmentCommand>` im Block „mutations of the RFID tag assignment“, `onSuccess` lädt `_assignmentResource` neu – und **auch `onError`**: bei `409` (Tag gerade zugewiesen, schon abgebrochen) zeigen Dialog und Karte sofort den aktuellen Stand – „Fertig“ bzw. „Art wählen“ –, statt bis zum nächsten Abfragen „Wartet auf Tag“ mit einem Button, der wieder scheitert. Den Fehlertext zeigt der Interceptor. Das Abfragen alle 3 s endet von selbst, sobald nichts mehr wartet (`isWaitingForRfidTag`).                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| Texte                        | Button: „Zuweisung abbrechen“ / "Cancel assignment" (Text aus `docs/aufgaben.md` und dem Prototyp), Schlüssel `users.dialogs.assignRfidTag.waiting.cancel` und `users.pages.userDetails.rfidTag.cancelAssignment`. Der Text im Schritt „Tag auflegen“ (`…waiting.message`) bleibt.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| Geräte                       | Keine Änderung: kein Gerät bricht ab. Wird nach dem Abbruch ein Tag an das Lesegerät an der Theke gehalten, antwortet es wie heute ohne wartende Anfrage „RFID nicht zugewiesen“ (TA-4, `DeviceMessages`) – kein neuer Eintrag in `DeviceMessages.BY_CODE`, kein Gerätetest.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |

### Umfang

<!-- Was Backend, Portal und Doku ändern (Klassen, Seiten, README, entscheidungen.md, aufgaben.md) – und was
ausdrücklich nicht dazugehört. -->

**Backend** (Klassen ab `schaffbar-backend/core-pos/src/main/java/de/schaffbar/core_pos/`, Tests ab
`…/src/test/java/de/schaffbar/core_pos/`, Bruno und Doku ab `schaffbar-backend/core-pos/`):

- neu `use_case/RfidTagAssignmentCancel` (Zeile „Use Case“);
- `domain/rfid_tag_assignment/web/RfidTagAssignmentController`: neuer Handler `cancelRfidTagAssignment` im Abschnitt
  `// command`, `@PutMapping("/{customerId}/cancel")` mit der Person als Pfadvariable, ohne Body; Abhängigkeit
  `RfidTagAssignmentCancel`; `RfidTagAssignmentApiModel` bleibt unverändert;
- Tests nach Skill `backend-testing`: `use_case/RfidTagAssignmentCancelTest` (unbekannte Person → `404`, Service nicht
  aufgerufen; sonst weitergeleitet); `RfidTagAssignmentControllerTest` – `204`, `404` mit `RESOURCE_NOT_FOUND` und
  `resource`, `409` mit `NO_RFID_TAG_ASSIGNMENT_WAITING`, `400` für eine ungültige Id; `integration/RfidTagAssignmentAcceptanceTest`
  – neu `ta10_…` (die Prüfung liest die Datenbank, Skill `backend-testing`, „Which tests a change needs“): eine wartende
  Anfrage wird abgebrochen (danach keine Zuweisung, kein Eintrag in der Historie, eine andere Person kann anfragen –
  TA-3 ist frei), eine zugewiesene wird abgelehnt (`409`) und bleibt; dazu `SchaffbarApi.cancelRfidTagAssignment(CustomerId)`.
  Service und Entity haben ihre Tests schon;
- `domain/rfid_tag_assignment/README.md`: TA-10 „Enforced in“ – „used by `RfidTagAssignmentCancel`
  (`PUT /rfid-tag-assignments/{customerId}/cancel`) and
  `CustomerDelete`“ statt „used by `CustomerDelete`; no endpoint yet“; Tabelle „Commands“, Spalte „Use case“ der Zeile
  „Cancel request“: `RfidTagAssignmentCancel`, `CustomerDelete` (C-4). Regeltext, Lifecycle und Events bleiben;
- `docs/ddd-overview.md` und `docs/ddd-uebersicht.md`: Zeile `RfidTagAssignmentCancel` in der Tabelle der Use Cases
  (prüft Customer; „the customer exists, then their waiting request is cancelled (no history)“);
- Bruno `api/rfid tag assignment/Cancel RFID tag assignment.bru` (nächste `seq`);
- `../docs/entscheidungen.md`, „Backend: REST und Geräte“, mit dem Datum der Freigabe: „Abbrechen einer Tag-Anfrage“
  – `PUT /rfid-tag-assignments/{customerId}/cancel` ohne Body, die Person im Pfad nach dem Skill `rest-api` und wie
  `PUT /workshop-sessions/{customerId}/close`; `PUT /rfid-tag-assignments/unassign` mit der Person im Body weicht davon
  ab und bleibt vorerst, bis es in einer eigenen Aufgabe angeglichen wird (Frage 1 (b), 25.11.2026);
- keine Migration, kein neues Event, keine neue Exception, keine `DeviceMessages`. Für den Deploy: Neustart des
  Backends, zusammen mit dem Portal.

**Portal** (`schaffbar-portal/`):

- `src/app/shared/api/rfid-tag-assignments/rfid-tag-assignments.api-service.ts`: `cancelRfidTagAssignment(customerId)`
  – `PUT …/{customerId}/cancel` ohne Body; `rfid-tag-assignment.api-model.ts` und `users/rfid-tag-assignment.mapper.ts`
  bleiben unverändert; mit Spec;
- `src/app/users/rfid-tag-assignment.model.ts`: `CancelRfidTagAssignmentCommand`;
- `src/app/users/pages/user-detail/user-detail.store.ts`: Mutation `cancelRfidTagAssignment` (Erfolg und Fehler laden
  die Zuweisung neu), mit Spec;
- `src/app/users/components/assign-rfid-tag-dialog/`: Button „Zuweisung abbrechen“ im Schritt `wait`, Output, Input für
  den laufenden Abbruch; „Schließen“ bleibt daneben (Frage 2 (a)); mit Spec;
- `src/app/users/pages/user-detail/user-detail-dialogs.service.ts`: `inputBinding` / `outputBinding` für den Abbruch;
  Spec;
- `src/app/users/components/user-rfid-tag-card/`: Button und Output im Zustand „Wartet auf Tag“, mit Spec;
  `user-detail-tab-overview`: Handler an den Store, Spec;
- `public/i18n/de.json` und `en.json`: die zwei Button-Texte; `shared.errors.NO_RFID_TAG_ASSIGNMENT_WAITING` und
  `shared.errors.RFID_TAG_ASSIGNMENT_ALREADY_WAITING` mit den neuen Texten aus dem Vertrag;
- das Feature `rfid-tags` bleibt unverändert – das Banner bekommt keine Aktion (Frage 3 (b));
- `npm run check`; im Browser: Dialog im Schritt „Tag auflegen“ und Karte „Wartet auf Tag“ mit Abbruch, der `409`
  (simuliert, Tag „gerade zugewiesen“) mit dem neuen Text und dem Sprung auf „Fertig“, Desktop und Handy.

**Doku:**

- `docs/aufgaben.md`: die Zeile „Anfrage abbrechen“ unter „RFID-Tag-Zuweisung“ löschen, Punkt 2 der Reihenfolge in
  „Backend-Funktionen für das Portal“ auf „Wartende Anfragen aufräumen“ kürzen (Rolle Backend, Skill `rest-api`, „The
  contract with the portal“). Stufe 1 der „Agenten-Pipeline“ bleibt, bis Piotr den Pilot auswertet.
- `docs/entscheidungen.md` (Rolle Portal), Abschnitt „Portal: Benutzer“, mit dem Datum der Freigabe: „Zuweisung
  abbrechen“ im Dialog und auf der Karte, ohne Rückfrage, der Dialog geht zurück auf „Art wählen“; der Button auf der
  Karte ist nicht im Prototyp; im Schritt „Tag auflegen“ stehen „Zuweisung abbrechen“ und „Schließen“ nebeneinander –
  der Prototyp hat nur „Zuweisung abbrechen“; zwei Ausgänge mit verschiedener Folge (Anfrage weg / Anfrage bleibt)
  sind beide benannt (Frage 2 (a), 25.11.2026). Die bestehende Entscheidung „„RFID-Tag zuweisen“ wartet“
  (09.11.2026) bleibt – geschlossen bleibt die Anfrage weiter bestehen.
- `docs/entscheidungen.md` (Rolle Backend), „Backend: REST und Geräte“: der Pfad und die Abweichung von `/unassign`
  (Liste „Backend“ oben).

**Nicht dabei:**

- „Wartende Anfragen aufräumen“ (Scheduler, Anzeige „noch N min“) – die nächste Zeile in `docs/aufgaben.md`;
- „Tag ersetzen …“ (Prototyp `replacing`, `UserDetail.dc.html`) – eigene Zeile in `docs/aufgaben.md`;
- „Zuweisung abbrechen“ im ⋯-Menü des Seitenkopfs der Person – nicht verlangt; die Karte liegt im ersten Tab;
- `PUT /rfid-tag-assignments/unassign` auf die Person im Pfad umstellen – eine eigene Aufgabe, weil Backend, Portal
  und Bruno sich mitändern (die Abweichung steht dann in `docs/entscheidungen.md`);
- ein Button „Zuweisung abbrechen“ im Banner „Wartet auf Tag“ auf der Seite RFID-Tags (Frage 3 (b));
- die Seite RFID-Tags lädt sich nicht von selbst neu – ein Banner „Wartet auf Tag“ bleibt nach einem Abbruch an anderer
  Stelle stehen, bis jemand neu lädt (Aufgabe „Seiten aktualisieren sich selbst“ in `docs/aufgaben.md`);
- die Entscheidung „„RFID-Tag zuweisen“ wartet“ nennt 2 Sekunden, der Code fragt alle 3 s ab
  (`RFID_TAG_POLL_INTERVAL_MS = 3000`) – gefunden bei dieser Spec, nicht geändert.

### Offene Fragen

<!-- Jede Frage mit den Möglichkeiten und einer Empfehlung; Piotr antwortet darunter. Ohne beantwortete Fragen keine
Freigabe. -->

- [x] **1. Pfad des Endpunkts:** Wie wird die Anfrage der Person adressiert? Der Service bricht über die Person ab
      (`customerId`); im selben Controller trägt `PUT /unassign` die Person im Body, der Skill `rest-api` will die Id im
      Pfad („an action without values: `PUT /{id}/<verb>`, no body“, „The id comes from the path, never from the body“), wie
      `PUT /workshop-sessions/{customerId}/close`. – Möglichkeiten:
      (a) `PUT /api/v1/rfid-tag-assignments/cancel` mit Body `{ "customerId": "…" }` – wie `/unassign` daneben; neuer
      `CancelRfidTagAssignmentRequestBody`, im Portal Request-Typ und Mapper; widerspricht dem Skill, der Review würde es
      anmerken;
      (b) `PUT /api/v1/rfid-tag-assignments/{customerId}/cancel` ohne Body – nach dem Skill und wie
      `/workshop-sessions/{customerId}/close`; `StringToCustomerIdConverter` gibt es; `/unassign` bleibt vorerst anders (unter
      „Nicht dabei“), die Abweichung kommt in `docs/entscheidungen.md`;
      (c) `PUT /api/v1/rfid-tag-assignments/{rfidTagAssignmentId}/cancel` über die Id der Anfrage – die reine Form des
      Skills, das Portal kennt die Id; braucht aber einen neuen Konverter und einen zweiten Service-Befehl über die Id
      (mehr als „nur der Endpunkt“).
      – Empfehlung: (b) – folgt dem Skill und dem Vorbild der Sitzung, ohne den Service zu ändern; `/unassign` später in
      einem eigenen Schritt angleichen.
      Antwort: (b) – `PUT /api/v1/rfid-tag-assignments/{customerId}/cancel` ohne Body; `/unassign` bleibt vorerst, die
      Abweichung kommt in `docs/entscheidungen.md` (Piotr im Chat, 25.11.2026).

- [x] **2. „Schließen“ im Schritt „Tag auflegen“:** Der Prototyp hat dort nur „Zuweisung abbrechen“ (geschlossen wird
      über das X im Kopf), das Portal heute nur „Schließen“. – Möglichkeiten:
      (a) beide Buttons: „Zuweisung abbrechen“ (umrandet) und „Schließen“ – wer den Dialog nur schließen will, findet das
      ohne zu raten, und der Text darüber sagt, dass die Anfrage dann bestehen bleibt; Abweichung vom Prototyp in
      `docs/entscheidungen.md`;
      (b) wie der Prototyp nur „Zuweisung abbrechen“, schließen über das X im Kopf – „Schließen“ entfällt.
      – Empfehlung: (a) – zwei Ausgänge mit verschiedener Folge (Anfrage bleibt / Anfrage weg) sollen beide sichtbar
      benannt sein; mit nur einem Button liegt „Zuweisung abbrechen“ dort, wo bisher „Schließen“ stand, und wird aus
      Gewohnheit für Schließen gehalten.
      Antwort: (a) – beide Buttons, „Zuweisung abbrechen“ und „Schließen“; die Abweichung vom Prototyp kommt in
      `docs/entscheidungen.md` (Piotr im Chat, 25.11.2026).

- [x] **3. Wer den Fehler von TA-3 bekommt, und das Banner auf der Seite RFID-Tags:** Wer für eine zweite Person einen
      Tag anfragt, liest „Eine andere Person wartet noch auf ihren Tag – zuerst deren Tag an das Lesegerät an der Theke
      halten.“ – den neuen Ausweg nennt der Text nicht, und wer wartet, zeigt nur das Banner „Wartet auf Tag“ auf der Seite
      RFID-Tags (mit Link „Zum Profil →“). – Möglichkeiten:
      (a) nur Dialog und Karte, wie die Zeile in `docs/aufgaben.md`; der Text von `RFID_TAG_ASSIGNMENT_ALREADY_WAITING`
      bleibt;
      (b) wie (a), dazu ein neuer Text: de „Eine andere Person wartet noch auf ihren Tag – zuerst deren Tag an das
      Lesegerät an der Theke halten oder ihre Anfrage abbrechen (Seite RFID-Tags zeigt, wer wartet).“, en "Another person
      is still waiting for their tag – hold their tag to the reader at the counter first, or cancel their request (the RFID
      tags page shows who is waiting)."; das Banner bleibt ohne Aktion;
      (c) wie (b), dazu ein Button „Zuweisung abbrechen“ im Banner auf der Seite RFID-Tags – im Feature `rfid-tags` eigener
      Command, eigene Mutation im `RfidTagsStore`, Text im Fehler dann „… oder ihre Anfrage auf der Seite RFID-Tags
      abbrechen“.
      – Empfehlung: (b) – ohne den Hinweis findet niemand den neuen Weg aus der Sperre, und das Banner führt mit einem
      Klick zum Profil, wo die Karte abbricht; (c) wäre ein zweiter Ort mit eigenem Code für denselben seltenen Fall.
      Antwort: (b) – neuer Text für `RFID_TAG_ASSIGNMENT_ALREADY_WAITING` wie oben, das Banner bleibt ohne Aktion (Piotr
      im Chat, 25.11.2026). Dazu bestätigt: der neue Text von `NO_RFID_TAG_ASSIGNMENT_WAITING` aus dem Vertrag („Fehlertext
      passt so“).

### Freigabe

<!-- Nur Piotr: „Freigegeben am <Datum>“. Erst danach beginnt das Backend. -->

Freigegeben am 25.11.2026

## 2. Bericht Backend

<!--
- Geändert: Regeln (README-Zeilen), Exceptions und Codes, Klassen.
- Vertrag: wie in der Spec – oder jede Abweichung mit Grund (das Portal baut auf diesem Abschnitt).
- Tests: welche, Ergebnis von `mvn -q test` (Anzahl).
- Für den Deploy: Neustart, Migration.
- Nicht gemacht, nicht geprüft, offene Fragen.
-->

**Geändert** (Pfade ab `schaffbar-backend/core-pos/`):

- Regel: keine neue. TA-10 in `src/main/java/de/schaffbar/core_pos/domain/rfid_tag_assignment/README.md`, Spalte
  „Enforced in“: „… used by `RfidTagAssignmentCancel` (`PUT /rfid-tag-assignments/{customerId}/cancel`) and
  `CustomerDelete`“ statt „used by `CustomerDelete`; no endpoint yet“; Tabelle „Commands“, Zeile „Cancel request“,
  Spalte „Use case“: `RfidTagAssignmentCancel`, `CustomerDelete` (C-4). Regeltext, Lifecycle und Events unverändert.
- Exceptions und Codes: keine neuen – `NoRfidTagAssignmentWaitingException.forCustomer` (`409`,
  `NO_RFID_TAG_ASSIGNMENT_WAITING`) und `ResourceNotFoundException.customer` (`404`, `RESOURCE_NOT_FOUND`) gibt es.
- Neu `src/main/java/de/schaffbar/core_pos/use_case/RfidTagAssignmentCancel.java`: `process(@NotNull @Valid CustomerId)`,
  `@Command`, `void` – Person laden (`404`), dann `RfidTagAssignmentService.cancelRfidTagAssignment(customerId)`.
  Service, Entity, Event (`RFID_TAG_ASSIGNMENT_CANCELLED`) unverändert.
- `domain/rfid_tag_assignment/web/RfidTagAssignmentController`: Handler `cancelRfidTagAssignment`,
  `@PutMapping(value = "/{customerId}/cancel")`, `@PathVariable @NotNull @Valid CustomerId customerId`, ohne Body und
  ohne `consumes`, Antwort `204`; Abhängigkeit `RfidTagAssignmentCancel`. `RfidTagAssignmentApiModel` unverändert.
- Bruno: neu `api/rfid tag assignment/Cancel RFID tag assignment.bru` (`seq: 8`, `PUT …/:customerId/cancel`,
  `body: none`).
- Doku: `docs/ddd-overview.md` und `docs/ddd-uebersicht.md` – Zeile `RfidTagAssignmentCancel` (Customer; „the customer
  exists, then their waiting request is cancelled (no history)“) in der Tabelle der Use Cases;
  `../docs/entscheidungen.md`, „Backend: REST und Geräte“: „Abbrechen einer Tag-Anfrage“ (25.11.2026) mit der
  Abweichung von `/unassign`; `../docs/aufgaben.md`: Zeile „Anfrage abbrechen“ unter „RFID-Tag-Zuweisung“ gelöscht,
  Punkt 2 der Reihenfolge auf „Wartende Anfragen aufräumen“ gekürzt (laut Umfang „Rolle Backend“ – die Rolle Portal
  muss das nicht mehr tun).

**Vertrag:** wie in der Spec. Für das Portal:

- `PUT /api/v1/rfid-tag-assignments/{customerId}/cancel` – kein Request-Body, kein `Content-Type` nötig; Erfolg `204`
  ohne Body.
- Fehler: `404` mit `code: RESOURCE_NOT_FOUND`, `resource: CUSTOMER` (unbekannte Person); `409` mit
  `code: NO_RFID_TAG_ASSIGNMENT_WAITING` (keine Zuweisung, schon zugewiesen oder schon abgebrochen); `400` ohne `code`
  (keine gültige UUID im Pfad).
- Keine neuen Felder, `GET /rfid-tag-assignments` unverändert. Kein neuer Code; die zwei neuen Fehlertexte
  (`NO_RFID_TAG_ASSIGNMENT_WAITING`, `RFID_TAG_ASSIGNMENT_ALREADY_WAITING`) stehen im Vertrag und sind Sache des Portals.
- Geräte: unverändert, `DeviceMessages` nicht angefasst.

**Tests** (ab `src/test/java/de/schaffbar/core_pos/`):

- neu `use_case/RfidTagAssignmentCancelTest`: `cancelsTheRequestOfAnExistingCustomer`, `rejectsAnUnknownCustomer`
  (`ResourceNotFoundException`, `resource` `CUSTOMER`, Service nicht aufgerufen);
- `domain/rfid_tag_assignment/web/RfidTagAssignmentControllerTest`, Abschnitt `cancelRfidTagAssignment`: `…_is204`,
  `…_withoutWaitingRequest_is409WithTheCode`, `…_ofAnUnknownCustomer_is404` (Code und `resource`),
  `…_withInvalidCustomerId_is400` (Use Case nicht aufgerufen);
- `integration/RfidTagAssignmentAcceptanceTest`: `ta10_aWaitingRequestIsCancelledWithoutHistory` (danach keine
  Zuweisung, kein Eintrag in der Historie, ein zweites Abbrechen → `409`, eine andere Person kann anfragen – TA-3 frei;
  räumt mit `endWaitingRfidTagAssignment()` auf), `ta10_anAssignedTagIsNotCancelled` (`409` mit Code, der Tag bleibt
  zugewiesen);
- Fixture: `SchaffbarApi.cancelRfidTagAssignment(CustomerId)`, `ThenFixture.expectNoHistoryEntry(CustomerId)`.
- Service- und Entity-Tests von TA-10 gab es schon (unverändert). Keine neue ArchUnit-Regel.
- `mvn -q spotless:apply`, dann `mvn -q test` mit Docker: **690 Tests in 87 Klassen, 0 Fehler, 0 übersprungen**
  (gezählt aus `target/surefire-reports/`), davon 8 neu.

**Für den Deploy:** Neustart des Backends, zusammen mit dem Portal. Keine Migration, kein neues Event.

**Nicht gemacht, nicht geprüft, offene Fragen:**

- Nichts im Portal (Übersetzungen, API-Schicht, Dialog, Karte, Store) – Rolle Portal.
- Nicht gegen das laufende Backend geprüft (es läuft noch ohne den Endpunkt; kein Neustart durch den Agenten).
- `PUT /rfid-tag-assignments/unassign` angleichen ist laut „Nicht dabei“ eine eigene Aufgabe, steht aber noch nicht in
  `docs/aufgaben.md` – nicht eingetragen, weil es nicht zum Umfang gehört; Piotr entscheidet, ob es dort eine Zeile
  bekommt.
- Offene Fragen: keine.

## 3. Bericht Portal

<!--
- Geändert: API-Schicht, Feature, Seiten und Dialoge, Übersetzungen de/en.
- Entscheidungen, die in docs/entscheidungen.md eingetragen wurden.
- Specs, Ergebnis von `npm run check`.
- Für die Abnahme: welche Seiten, Zustände und Dialoge im Browser anzusehen sind und wie jeder Zustand entsteht
  (welche Anfrage mit welcher Antwort simuliert wird).
- Abweichungen vom Vertrag, nicht gemacht, offene Fragen.
-->

**Geändert** (Pfade ab `schaffbar-portal/`):

- API-Schicht `src/app/shared/api/rfid-tag-assignments/rfid-tag-assignments.api-service.ts`: neu
  `cancelRfidTagAssignment(customerId): Observable<void>` – `PUT …/rfid-tag-assignments/{customerId}/cancel`, als Body
  `{}` (Skill `api`: „Without payload pass `{}`“, wie `closeWorkshopSession`, `pauseToolCertification`; der Handler
  hat kein `@RequestBody` und kein `consumes`, liest ihn also nicht). API-Modell und Mapper unverändert.
- Feature `src/app/users/`:
  - `rfid-tag-assignment.model.ts`: `CancelRfidTagAssignmentCommand { userId }` unter `// commands`.
  - `pages/user-detail/user-detail.store.ts`: `cancelRfidTagAssignment: mutation<CancelRfidTagAssignmentCommand>` im
    Block „mutations of the RFID tag assignment“ – Pfadvariable `command.userId`, `onSuccess` **und** `onError` laden
    `_assignmentResource` neu (Kommentar warum).
  - Dialog `components/assign-rfid-tag-dialog/`: Input `cancelPending` (Standard `false`), Output
    `rfidTagAssignmentCancelled` (`CancelRfidTagAssignmentCommand`), Methode `cancelRfidTagAssignment()`. Schritt
    „Tag auflegen“: Fußzeile „Zuweisung abbrechen“ (`matButton="outlined"`, kein `danger-button`, gesperrt bei
    `cancelPending`), daneben das bestehende „Schließen“ – in dieser Reihenfolge, beide umrandet, mittig. „Zuweisung
    abbrechen“ erscheint nur mit einer Zuweisung, also nicht während „Tag wird angefordert …“. Nach dem Abbruch kommt
    die neu geladene Zuweisung `null` an, `step` wird `choose`.
  - `pages/user-detail/user-detail-dialogs.service.ts`: `inputBinding('cancelPending', store.cancelRfidTagAssignment.isPending)`,
    `outputBinding('rfidTagAssignmentCancelled', …)` → `store.cancelRfidTagAssignment(command)`.
  - Karte `components/user-rfid-tag-card/`: im Zustand „Wartet auf Tag“ neben „Zuweisung anzeigen“ ein zweiter
    umrandeter Button „Zuweisung abbrechen“ ohne Rückfrage; Output `rfidTagAssignmentCancelled`, Methode
    `cancelRfidTagAssignment()`. `.actions` hat schon `flex-wrap`.
  - Tab `pages/user-detail/user-detail-tab-overview/`: `(rfidTagAssignmentCancelled)` → `cancelRfidTagAssignment()` →
    Store.
- Übersetzungen `public/i18n/de.json` / `en.json`: neu `users.dialogs.assignRfidTag.waiting.cancel` und
  `users.pages.userDetails.rfidTag.cancelAssignment` („Zuweisung abbrechen“ / "Cancel assignment"); neu geschrieben
  `shared.errors.NO_RFID_TAG_ASSIGNMENT_WAITING` und `shared.errors.RFID_TAG_ASSIGNMENT_ALREADY_WAITING`, Wortlaut aus
  dem Vertrag.
- Feature `rfid-tags` unverändert (Frage 3 (b)); keine Regel nur im Portal, daher nichts im README des Aggregats.

**Entscheidungen:** `docs/entscheidungen.md`, „Portal: Benutzer“, neu „„Zuweisung abbrechen““ (25.11.2026): Dialog
und Karte, umrandet und ohne Rückfrage, der Dialog bleibt offen und geht zurück auf „Art wählen“; der Button auf der
Karte ist nicht im Prototyp; „Zuweisung abbrechen“ und „Schließen“ nebeneinander statt nur „Zuweisung abbrechen“ wie
im Prototyp (Frage 2 (a)); bei einem gescheiterten Abbruch lädt die Seite die Zuweisung sofort neu. Die Entscheidung
„„RFID-Tag zuweisen“ wartet“ (09.11.2026) bleibt unverändert. `docs/aufgaben.md`: nichts mehr zu tun (Zeile und
Reihenfolge hat die Rolle Backend schon angepasst).

**Tests:**

- `users/pages/user-detail/user-detail.store.spec.ts`: Abbruch schickt `PUT /rfid-tag-assignments/user-1/cancel` mit
  Body `{}` und lädt nur die Zuweisung neu (danach `null`, `isWaitingForRfidTag` `false`); bei `409`
  `NO_RFID_TAG_ASSIGNMENT_WAITING` wird die Zuweisung ebenfalls neu geladen (danach zugewiesen), `isPending` wieder
  `false`.
- `users/components/assign-rfid-tag-dialog/assign-rfid-tag-dialog.component.spec.ts`, Block `cancel`: wartend die
  Fußzeile „Zuweisung abbrechen“, „Schließen“ (Reihenfolge); Klick gibt `{ userId }` aus, ohne Rückfrage; während
  „Tag wird angefordert …“ nur „Schließen“; gesperrt bei `cancelPending`; Zuweisung `null` → Schritt „Art wählen“.
- `users/components/user-rfid-tag-card/user-rfid-tag-card.component.spec.ts`: wartend die Buttons „Zuweisung
  anzeigen“, „Zuweisung abbrechen“; Klick gibt `{ userId }` aus, die Bestätigung wird nicht geöffnet.
- `users/pages/user-detail/user-detail-tab-overview/user-detail-tab-overview.component.spec.ts`: Output der Karte →
  `store.cancelRfidTagAssignment`.
- `users/pages/user-detail/user-detail-dialogs.service.spec.ts`: sieben Bindings; neu ein Block mit echtem
  `MatDialog`, der die Bindings über ihre Namen prüft (Output → Store-Methode, `cancelPending` und
  `rfidTagAssignment` folgen dem Store).
- `npm run check`: siehe Ende dieses Abschnitts.

**Für die Abnahme** (Profil einer Person, Tab „Übersicht“; nichts in die Datenbank schreiben – Antworten simulieren):

1. **Karte „Wartet auf Tag“:** `GET /api/v1/rfid-tag-assignments?customerId=<id>` mit einer Zuweisung
   `status: WAITING_FOR_ASSIGNMENT`, `rfidTagId: null`, `assignmentDate: null` beantworten. Erwartet: Sanduhr, „Wartet
   auf Tag“, Buttons „Zuweisung anzeigen“ und „Zuweisung abbrechen“ (beide umrandet, nicht rot); Badge im Kopf. Auf
   dem Handy brechen die Buttons um.
2. **Abbruch auf der Karte, Erfolg:** `PUT /api/v1/rfid-tag-assignments/<id>/cancel` → `204`, das folgende `GET …?customerId=`
   → `[]`. Erwartet: keine Rückfrage, Karte „Kein RFID-Tag zugewiesen.“ mit „RFID-Tag zuweisen …“, Badge weg, ⋯-Menü
   bietet „RFID-Tag zuweisen“.
3. **Dialog, Schritt „Tag auflegen“:** wie 1, dann „Zuweisung anzeigen“. Erwartet: Fußzeile „Zuweisung abbrechen“ und
   „Schließen“, beide umrandet; „Schließen“ schließt, die Anfrage bleibt.
4. **Abbruch im Dialog, Erfolg:** wie 2 im Dialog. Erwartet: der Dialog bleibt offen und zeigt „Art wählen“ (Leihtag
   vorausgewählt); während der `PUT` läuft, ist „Zuweisung abbrechen“ gesperrt (Antwort verzögern).
5. **Abbruch scheitert, Tag gerade zugewiesen:** `PUT …/cancel` → `409` mit
   `{ "code": "NO_RFID_TAG_ASSIGNMENT_WAITING", … }`, das folgende `GET …?customerId=` → eine Zuweisung `ASSIGNED` mit
   `rfidTagId`. Erwartet: Snackbar „Keine Anfrage wartet mehr auf einen Tag – vielleicht wurde der Tag gerade
   zugewiesen oder die Anfrage schon abgebrochen.“; der Dialog springt sofort auf „Fertig“ mit der Tag-Id, die Karte
   zeigt den Tag (ohne auf das nächste Abfragen nach 3 s zu warten).
6. **Abbruch scheitert, schon abgebrochen:** `409` wie 5, `GET` → `[]`. Erwartet: derselbe Text, Dialog auf „Art
   wählen“ bzw. Karte „Kein RFID-Tag zugewiesen.“.
7. **Text TA-3:** im Dialog „Tag anfordern“ mit `POST /api/v1/rfid-tag-assignments` → `409`
   `{ "code": "RFID_TAG_ASSIGNMENT_ALREADY_WAITING" }`. Erwartet: „Eine andere Person wartet noch auf ihren Tag –
   zuerst deren Tag an das Lesegerät an der Theke halten oder ihre Anfrage abbrechen (Seite RFID-Tags zeigt, wer
   wartet).“, der Dialog geht zurück auf „Art wählen“.
8. Desktop und Handy für 1, 3 und 5; Englisch stichprobenhaft („Cancel assignment“).

Das laufende Backend kennt den Endpunkt erst nach einem Neustart (Bericht Backend) – ohne Neustart antwortet es auf
`PUT …/cancel` nicht mit `204`; deshalb simulieren.

**Abweichungen vom Vertrag:** keine. Zwei Punkte, die der Vertrag offen ließ und die nichts Sichtbares ändern bzw. sich
aus ihm ergeben: der `PUT` trägt `{}` als Body (Konvention der API-Schicht, das Backend liest keinen); auf der Karte ist
„Zuweisung abbrechen“ während des laufenden Abbruchs nicht gesperrt (der Vertrag verlangt das nur im Dialog; die
Mutation ignoriert einen zweiten Klick ohnehin, `exhaustMap`).

**Nicht gemacht:** kein Browser (Rolle Abnahme); das Banner auf der Seite RFID-Tags bleibt ohne Aktion und lädt nach
einem Abbruch nicht von selbst neu (laut „Nicht dabei“); die Abweichung „2 Sekunden“ / 3000 ms in der Entscheidung vom
09.11.2026 nicht angefasst (laut „Nicht dabei“).

**Offene Fragen:** keine.

**`npm run check`** (25.11.2026, 16:30): grün – Lint ohne Befund, Prettier sauber, Typen der Specs sauber, **581 Tests
in 111 Dateien bestanden**, Build fertig. Zwei frühere Läufe brachen mit Zeitüberschreitungen (5 s / 10 s) in nicht
geänderten Form-Specs ab (`update-rfid-reader-form`, `new-user-form`, `user-list` – je Lauf andere), während der
Rechner stark ausgelastet war (Load ~39 bei 4 Kernen); allein liefen sie grün, und der Lauf bei normaler Last war
vollständig grün.

### Nachbesserung nach dem Review

Zu den Befunden 1 und 2 aus Abschnitt 5 (Pfade ab `schaffbar-portal/`):

- **Befund 1** – `src/app/users/pages/user-detail/user-detail-dialogs.service.spec.ts` folgt jetzt der Ausnahme im
  Skill `testing`, „Dialogs“ (Entscheidung Piotr, 25.11.2026): der Test, der nur die Bindings zählte
  (`toHaveLength(7)`), ist weg, ebenso `assignRfidTag()` im Test „should open nothing before the user is loaded“ des
  Blocks mit dem gemockten `MatDialog` (die übrigen drei Dialoge bleiben dort). Der Block „RFID tag dialog“ mit echtem
  `MatDialog` prüft jedes Binding über seinen Namen: als Formulardialog geöffnet (`open` mit
  `AssignRfidTagDialogComponent` und `FORM_DIALOG_CONFIG`, per `vi.spyOn` auf den echten `MatDialog`); `userId` und
  `userName` aus der Person des Stores; `rfidTagAssignment` folgt dem Store (`null` → wartend → `null`);
  `assignmentRequested` ruft `store.assignRfidTag` mit dem Command, `requestPending` folgt
  `assignRfidTag.isPending`; `rfidTagAssignmentCancelled` ruft `store.cancelRfidTagAssignment`, `cancelPending` folgt
  `cancelRfidTagAssignment.isPending`; ohne geladene Person öffnet sich kein Dialog (`openDialogs` leer). Die Datei hat
  13 Tests (vorher 10).
- **Befund 2** – `src/app/users/components/assign-rfid-tag-dialog/assign-rfid-tag-dialog.component.ts`: der
  Klassenkommentar steht wieder im Wortlaut von c7ed232; was neu war, ist ein Zeilenkommentar an `step` („after a
  cancel the page reloads the assignment, it comes back `null` and the dialog is at the choice again“).

Kein Verhalten geändert, nichts Sichtbares – für die Abnahme nichts Neues. `docs/entscheidungen.md` unverändert (die
Entscheidung zum Testmuster steht im Skill).

**`npm run check`** (25.11.2026, 22:42): grün – Lint ohne Befund, Prettier sauber, Typen der Specs sauber, **584 Tests
in 111 Dateien bestanden**, Build fertig.

**Offene Fragen:** keine.

## 4. Abnahme im Browser

<!-- Seiten, Zustände (auch simulierte Antworten), Desktop und Handy; was gefunden wurde. Nichts in die Datenbank
geschrieben (Skill verify-ui); Screenshots liegen nicht im Repo. -->

Abgenommen am 25.11.2026 mit dem Dev-Server (`ng serve`, Stand `main` mit „[frontend] cancel rfid tag assignment“)
gegen das laufende lokale Backend (ohne den neuen Endpunkt, nicht neu gestartet). Profil von Erika Musterfrau
(`334e50c6-…`, in der Datenbank ohne Zuweisung), Tab „Übersicht“. In jedem Playwright-Aufruf zuerst `unrouteAll`, dann
jede Anfrage außer `GET` abgefangen: `PUT …/{customerId}/cancel` und `POST /rfid-tag-assignments` mit `route.fulfill`
beantwortet, alles andere abgebrochen (kein Abbruch kam vor); `GET …/rfid-tag-assignments?customerId=` mit eigenem JSON
(besteht das zod-Schema). Die Zuweisungen in der Datenbank danach per `GET` geprüft: unverändert. Screenshots in
`/tmp/claude-1000/-home-piotr-source-schaffbar-kassensystem-software/2b181c82-b220-46a2-ae53-74a6a3233689/scratchpad/`
(unten nur Dateinamen).

**Angesehen:**

1. **Karte „Wartet auf Tag“** (`GET` → eine Zuweisung `WAITING_FOR_ASSIGNMENT`, Leihtag, `rfidTagId`/`assignmentDate`
   `null`), Desktop 1440 und Handy 390: Sanduhr, „Wartet auf Tag“, „Leihtag · an das Lesegerät an der Theke halten“,
   Buttons „Zuweisung anzeigen“ und „Zuweisung abbrechen“, beide umrandet (`mdc-button--outlined`, Text dunkel, Rahmen
   grau, kein `danger-button`); Badge „Wartet auf Tag“ im Kopf neben dem Titel. Handy: Buttons untereinander (y 718 /
   766), kein seitliches Scrollen (`scrollWidth` 390). – wie erwartet. (`1-karte-wartet-desktop.png`,
   `…-desktop-detail.png`, `1-karte-wartet-phone.png`, `…-phone-detail.png`)
2. **Abbruch auf der Karte, Erfolg** (`PUT …/cancel` → `204`, danach `GET` → `[]`), Desktop: keine Rückfrage (kein
   Dialog offen), der `PUT` ging an `/rfid-tag-assignments/334e50c6-…/cancel` (Body `{}`), danach genau ein `GET`;
   Karte „Kein RFID-Tag zugewiesen.“ mit „RFID-Tag zuweisen …“, Badge weg, ⋯-Menü: „Bearbeiten“, „RFID-Tag zuweisen …“,
   „Benutzer löschen …“; keine Snackbar. – wie erwartet. (`2-karte-abgebrochen-desktop.png`, `2-menue-desktop.png`)
3. **Dialog, Schritt „Tag auflegen“** (wie 1, Art „Fest“, dann „Zuweisung anzeigen“), Desktop und Handy: Fußzeile
   mittig „Zuweisung abbrechen“, „Schließen“, beide umrandet, nicht rot; Inhalt scrollt nicht (`scrollHeight` =
   `clientHeight` 268). „Schließen“ schließt den Dialog, die Karte zeigt weiter „Wartet auf Tag“ (kein `PUT`). Dazu
   **„Tag wird angefordert …“** (`POST` verzögert mit `204` beantwortet), Desktop: nur „Schließen“; nach der Antwort
   „Leihtag an das Lesegerät an der Theke halten“ mit beiden Buttons. – wie erwartet.
   (`3-dialog-wartet-desktop.png`, `…-desktop-detail.png`, `3-dialog-wartet-phone.png`,
   `3b-dialog-wird-angefordert-desktop.png`)
4. **Abbruch im Dialog, Erfolg** (`PUT` → `204`, 2,5 s verzögert, danach `GET` → `[]`), Desktop und Handy: während
   der `PUT` läuft, ist „Zuweisung abbrechen“ gesperrt (`disabled`, grau), „Schließen“ nicht; danach bleibt der Dialog
   offen und zeigt „Art wählen“ mit „Leihtag“ vorausgewählt (vorher wartete „Fest“); keine Snackbar; nach dem
   Schließen zeigt die Karte „Kein RFID-Tag zugewiesen.“. – wie erwartet. (`4-dialog-abbruch-laeuft-desktop.png`,
   `4-dialog-art-waehlen-desktop.png`, `4-dialog-art-waehlen-phone.png`)
5. **Abbruch scheitert, Tag gerade zugewiesen** (`PUT` → `409` `application/problem+json` mit
   `code: NO_RFID_TAG_ASSIGNMENT_WAITING`, danach `GET` → `ASSIGNED`, `rfidTagId` `04A31F9C`), im Dialog und auf der
   Karte, je Desktop und Handy: Snackbar „Keine Anfrage wartet mehr auf einen Tag – vielleicht wurde der Tag gerade
   zugewiesen oder die Anfrage schon abgebrochen.“; der `GET` folgt 15–20 ms nach dem `409` – der Dialog steht 70–85 ms
   nach dem Klick auf „Fertig“ („Tag zugewiesen“, „04A3 1F9C gehört jetzt Erika Musterfrau (Leihtag).“), nicht erst
   nach dem Abfragen nach 3 s; die Karte zeigt `04A3 1F9C`, „Leihtag · zugewiesen am 25.11.2026“, „Tag entfernen …“,
   der Kopf „Leihtag · 04A3 1F9C“. Snackbar auf dem Handy umbrochen, ganz sichtbar. – wie erwartet.
   (`5-dialog-409-zugewiesen-desktop.png`, `…-phone.png`, `5-karte-409-zugewiesen.png`, `…-phone.png`,
   `5-karte-nach-409-desktop.png`, `…-phone.png`)
6. **Abbruch scheitert, schon abgebrochen** (`409` wie 5, `GET` → `[]`), Dialog Desktop und Handy, Karte Desktop:
   derselbe Text; der Dialog bleibt offen und zeigt „Art wählen“, die Karte „Kein RFID-Tag zugewiesen.“. – wie
   erwartet. (`6-dialog-409-schon-abgebrochen.png`, `…-phone.png`, `6-karte-409-schon-abgebrochen.png`)
7. **Text TA-3** (ohne Zuweisung, „RFID-Tag zuweisen …“, „Weiter – Tag auflegen“; `POST` → `409`
   `RFID_TAG_ASSIGNMENT_ALREADY_WAITING`), Desktop und Handy: Snackbar „Eine andere Person wartet noch auf ihren Tag –
   zuerst deren Tag an das Lesegerät an der Theke halten oder ihre Anfrage abbrechen (Seite RFID-Tags zeigt, wer
   wartet).“, der Dialog zeigt „Art wählen“. – wie erwartet. (`7-ta3-409-desktop.png`, `7-ta3-409-phone.png`)
8. **Englisch, Stichprobe** (das Portal hat keinen Sprachwechsel – `lang: 'de'` fest; simuliert, indem
   `/i18n/de.json` mit dem Inhalt von `en.json` beantwortet wurde), Desktop: Karte „Waiting for tag“, „Show
   assignment“, „Cancel assignment“; Dialog „Cancel assignment“, „Close“; Snackbar des `409` „No request is waiting for
   a tag any more – the tag may just have been assigned or the request cancelled.“ (`8-en-karte.png`,
   `8-en-dialog.png`)

Konsole: in allen Zuständen nur die erwarteten Einträge „Failed to load resource … 409“ der simulierten Antworten;
kein zod-Fehler, kein Angular-Fehler. Auf dem Bildschirm nur deutsche Texte, keine Übersetzungsschlüssel.

**Befunde:**

- In der Aufgabe: keine.
- Hinweis, kein Befund: auf dem Desktop (1440) stehen die zwei Buttons der Karte „Wartet auf Tag“ ebenfalls
  untereinander – die linke Spalte ist zu schmal für beide nebeneinander (Inhalt 386 px, Buttons 192 + 203 px). Der
  Vertrag nennt das Umbrechen nur fürs Handy, verbietet es auf dem Desktop aber nicht; sieht ordentlich aus
  (`1-karte-wartet-desktop.png`).
- Außerhalb der Aufgabe, schon bekannt: auf dem Handy (390) ist der Dialog „RFID-Tag zuweisen“ 600 px breit
  (`minWidth: DIALOG_WIDTH.medium`) und ragt links und rechts je 105 px über den Bildschirm – Titel, Schrittleiste und
  im Schritt „Art wählen“ die Radio-Karten und „Weiter – Tag auflegen“ sind abgeschnitten. Die neue Fußzeile des
  Schritts „Tag auflegen“ liegt zufällig ganz im Sichtbaren (x 31–359). Steht schon als Aufgabe „Dialoge auf dem Handy“
  in `docs/aufgaben.md` (gefunden bei R-6, 24.11.2026); kein neuer Eintrag. (`3-dialog-wartet-phone.png`,
  `4-dialog-art-waehlen-phone.png`, `7-ta3-409-phone.png`)

**Nicht geprüft:**

- Der echte Endpunkt: das laufende Backend kennt `PUT …/{customerId}/cancel` erst nach einem Neustart, und die
  Abnahme schreibt nichts in die Datenbank – Antworten (`204`, `409`) nur simuliert.
- Ob „Zuweisung abbrechen“ auf der Karte während des laufenden Abbruchs gesperrt ist – laut Bericht Portal nicht (der
  Vertrag verlangt es nur im Dialog); nicht im Browser angesehen.
- `404` (unbekannte Person) und `400` – das Portal schickt immer die Id der angezeigten Person; die Antworten zeigt der
  Interceptor ohne eigenen Text (nicht im Vertrag für das Portal).
- Die Karte nach dem Abbruch (Zustand 2) und die Karte bei „schon abgebrochen“ (6) nur auf dem Desktop; Englisch nur
  auf dem Desktop; kein Dunkelmodus.
- Die Seite RFID-Tags (Banner „Wartet auf Tag“) – laut Spec unverändert.

## 5. Review

<!-- Befunde gegen CLAUDE.md und die Skills: Datei und Zeile, verletzte Regel, muss / sollte, Stand (offen, behoben).
Ohne die Begründung des Autors gelesen. -->

Geprüft am 25.11.2026: `git diff ccdf3b9` (Commits 8892589, 06deaed, 8f9f0a2 und Arbeitsbaum), alle geänderten und
neuen Dateien ganz gelesen. Gelesen in dieser Reihenfolge: Abschnitt 1 der Spec, dann der Code, dann die Regeln
(`CLAUDE.md`, `schaffbar-backend/CLAUDE.md`, `schaffbar-portal/CLAUDE.md`, Skills `backend-testing`, `rest-api`,
Backend-Regeln zu Use Cases, Portal-Skills `api`, `testing`, `dialog`, `feature`, `component`), erst danach die
Abschnitte 2–4. Keine Tests laufen lassen – kein Befund hängt davon ab.

**Befunde:**

1. **sollte** – `schaffbar-portal/src/app/users/pages/user-detail/user-detail-dialogs.service.spec.ts:163–227`
   (dazu Zeile 109). Skill `testing`, „Dialogs“: „A page or page service that opens dialogs: mock `MatDialog`“. Der
   neue Block öffnet den Dialog-Service der Seite mit dem echten `MatDialog` – das Muster, das der Skill `dialog`
   („Testing“ → „Service“) für den Service eines geteilten Dialogs vorsieht. In der Datei stehen jetzt beide Muster für
   denselben Dialog; der Test mit dem Mock zählt nur die Bindings (`toHaveLength(7)`) und prüft keinen Namen. Zu ändern:
   Piotr entscheidet, welches Muster für einen Seiten-Service gilt, der einen Dialog mit `bindings` öffnet, und der Skill
   `testing` nennt es (Zeile 264). Danach folgt die Spec einem Muster – bleibt der echte Block, fällt der Test, der nur
   zählt, weg. Stand: behoben (Rolle Portal, erneut aufgerufen, 25.11.2026 – „Nachbesserung nach dem Review“ in
   Abschnitt 3).
   Entscheidung Piotr (im Chat, 25.11.2026): der echte `MatDialog` gilt für einen Seiten-Service, der einen Dialog mit
   `bindings` öffnet. Die Hauptsitzung hat dazu im Skill `testing` („Dialogs“) die Ausnahme unter „A page or page
   service that opens dialogs“ ergänzt (keine Rolle besitzt die Skills); die Spec-Datei zieht die Rolle Portal nach.
2. **sollte** – `schaffbar-portal/src/app/users/components/assign-rfid-tag-dialog/assign-rfid-tag-dialog.component.ts:33–39`.
   `schaffbar-portal/CLAUDE.md`, „Code“: „No class comments“. Die Änderung erweitert den JSDoc über der Klasse (er
   stammt aus c7ed232) um das Binding des Abbruchs und seine Folge. Zu ändern: was neu ist („nach einem Abbruch ist die
   neu geladene Zuweisung `null`, der Dialog steht wieder bei ‚Art wählen‘“), wird ein Zeilenkommentar an `step` und
   kommt nicht in den Klassenkommentar. Stand: behoben (Rolle Portal, erneut aufgerufen, 25.11.2026 – Klassenkommentar
   wieder wie in c7ed232, der neue Satz als Zeilenkommentar an `step`).

**muss: 0, sollte: 2.**

**Ohne Befund angesehen:**

- Vertrag Backend: `PUT /{customerId}/cancel` ohne Body und ohne `consumes`, `@PathVariable @NotNull @Valid CustomerId`,
  `204`. Der Use Case `RfidTagAssignmentCancel` (`@Command`, prüft erst die Person – `404` –, dann den Service) ist
  nach `<Aggregat><Befehl>` benannt; Service, Exception und `DeviceMessages` sind unverändert. Der Handler steht im
  Abschnitt `// command` und folgt dem Muster von `WorkshopSessionController.closeWorkshopSession`.
- Backend-Tests: der Use-Case-Test hat beide Pfade (Ablehnung mit `resource` und `verifyNoInteractions`). Die
  Controller-Tests decken `204`, `409` mit Code, `404` mit Code und `resource` sowie `400` (Use Case nicht aufgerufen)
  ab – alle Anfragen gehen über Methoden im Abschnitt `// when`, Stubs liegen nur in `given…`. Abnahme: `ta10_` für
  beide Seiten, beide scheitern ohne den Endpunkt; die wartende Anfrage wird mit `// cleanup` und
  `endWaitingRfidTagAssignment()` beendet. `SchaffbarApi`/`ThenFixture` stehen im Abschnitt des Aggregats.
- Doku: TA-10 „Enforced in“ und die Tabelle „Commands“ im README (der Rest der Tabelle wurde nur neu ausgerichtet).
  `ddd-overview.md`/`ddd-uebersicht.md`, Bruno (`seq: 8`, `body: none`, wie „Close workshop session“).
  `entscheidungen.md`: beide Einträge mit Datum und allen Punkten aus dem Umfang. `aufgaben.md`: Zeile gelöscht,
  Punkt 2 gekürzt.
- Portal: API-Methode mit `{}` als Body – Skill `api`, „Without payload pass `{}`“, wie `closeWorkshopSession`. Keine
  Spec für den API-Service, gegen „mit Spec“ im Umfang; das entspricht dem Skill `api` („No spec for the API service –
  it is tested through the store spec“) und ist kein Befund. Command unter `// commands`. Mutation mit `onSuccess` und
  `onError`; der Store-Test mit `409` würde ohne `onError` scheitern. Dialog: Button nur mit Zuweisung, gesperrt bei
  `cancelPending`, umrandet, daneben „Schließen“. Karte: zweiter umrandeter Button ohne Rückfrage, Output als Event
  benannt, Methode als Verb. Tab gibt den Command an den Store weiter. Übersetzungen de/en mit denselben Schlüsseln
  und dem Wortlaut des Vertrags (beide JSON-Dateien geladen und geprüft).
- Dialog-Test „should go back to the choice once the request is cancelled“ würde auch ohne die Änderung bestehen – laut
  Vertrag ergibt sich dieses Verhalten aus dem Store, ohne Code im Dialog. Der Test sichert eine Zeile des Vertrags
  (wartend → `null`) ab, die vorher nicht getestet war. Kein Befund.

**Berichte gegen den Code:**

- Bericht Backend: die Tests stimmen mit dem Code überein (8 neu: 2 Use Case, 4 Controller, 2 Abnahme). Der Satz zu
  „`/unassign` angleichen … nicht eingetragen“ stimmt nicht mehr: die Zeile steht in `docs/aufgaben.md`, hinzugefügt im
  Commit 8f9f0a2 „[docs]“. Das passt zu „Nicht dabei“ und ist kein Befund gegen den Code – nur ein Hinweis, falls die
  Zeile nicht von Piotr stammt.
- Bericht Portal: die Tests und die Abweichungen (Body `{}`, Karte nicht gesperrt – `exhaustMap` der Mutation) stimmen
  mit dem Code überein. Die Zahl „581 Tests“ nicht nachgezählt.

**Nicht geprüft:** `mvn -q test` und `npm run check` nicht laufen lassen (Testzahlen der Berichte nicht nachgezählt).
Der echte Endpunkt nicht gegen das laufende Backend geprüft, kein Browser. Der Handy-Fehler des Dialogs
(`DIALOG_WIDTH.medium`) gehört zur Aufgabe „Dialoge auf dem Handy“ und ist hier nicht bewertet.

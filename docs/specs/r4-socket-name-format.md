# Der Socket-Name besteht nur aus a–z, 0–9 und „-“ (R-4)

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

- **Quelle:** `docs/aufgaben.md`, Abschnitt „Regeln, die nur das Portal prüft“, Zeile R-4, und die letzte Frage unter
  „Fachlich zu klären“ (Socket-Name) – zweiter Pilot der Agenten-Pipeline (Stufe 1); Artboard „RFID-Lesegeräte –
  Dialog: Bearbeiten“ des Prototyps (`project/ReaderEditDialog.dc.html`)
- **Branch:** –
- **Stand:** Spec – wartet auf Freigabe

## 1. Spec

### Auftrag

<!-- Was soll sich ändern und warum – zwei, drei Sätze, aus Sicht der Werkstatt. -->

Ein Socket-Name eines Lesegeräts soll nur aus Kleinbuchstaben, Ziffern und Bindestrichen bestehen (`gate-out`,
`box-hobel`). Heute prüft das nur der Dialog „Lesegerät bearbeiten“; das Backend nimmt jeden Text an. Künftig lehnt
auch das Backend einen neuen oder geänderten Socket-Namen in einem anderen Format mit eigenem Code ab – ein
unveränderter alter Name wie „Socket 43“ bleibt gültig, damit niemand beim Umbenennen eines Geräts erst dessen
Socket-Namen reparieren muss.

### Heute

<!-- Was es schon gibt: Regeln in den README der Aggregate, Endpunkte, Seiten und Dialoge im Portal, mit Pfaden. -->

**Backend** (Pfade ab `schaffbar-backend/core-pos/src/main/java/de/schaffbar/core_pos/`):

- `domain/rfid_reader/README.md`: R-4 „A socket name contains only lowercase letters, digits and hyphens
  (`^[a-z0-9-]+$`)“ steht unter „Open Rules → Enforced only in the portal“ mit „Portal: checked only when the socket
  name changes – existing names like "Socket 43" stay valid. Backend accepts anything“. Attribut `socketName`:
  `String`, „optional“. Vergeben sind R-1 bis R-9; „Planned“ ist leer.
- `RfidReader.socketName` ist ein `String` ohne Constraint, Spalte `socket_name VARCHAR(255)`
  (`src/main/resources/db/migration/V1__init.sql`). `RfidReaderView`, `RfidReaderApiDto` und die Payload von
  `RFID_READER_UPDATED` (`shared/event/payload/RfidReaderEventPayload`) tragen ihn als `String`, nullable.
- Einen Socket-Namen setzt nur `PUT /api/v1/rfid-readers/{rfidReaderId}`: `domain/rfid_reader/web/RfidReaderController.updateRfidReader`
  → Use Case `use_case/RfidReaderUpdate` (Weiterleitung) → `RfidReaderService.updateRfidReader()` (lädt das
  Lesegerät, `404`; prüft R-6 nur mit Namen) → `RfidReader.update(command)` setzt `name` und `socketName` ungeprüft →
  Event `RFID_READER_UPDATED`. Body `UpdateRfidReaderRequestBody(name, socketName)` ohne Constraints, Command
  `UpdateRfidReaderCommand(id, name, socketName)`; Antworten `204`, `404`, `409` `RFID_READER_NAME_ALREADY_USED`.
- Angelegt wird ein Lesegerät ohne Socket-Namen (`POST /api/v1/rfid-readers`, `/device/init` über `RfidReaderCreate`,
  R-9); der Typwechsel (`PATCH …/type`) berührt ihn nicht.
- **Kein Gerät nutzt den Socket-Namen:** `web/device/DeviceController` und `DeviceApiModel` lesen und schicken ihn
  nicht (`DEVNAME` kommt aus dem Typ des Lesegeräts), ein Lesegerät meldet sich nur mit seiner MAC-Adresse. Lesend
  durchsucht, außerhalb dieses Repos: die Firmware in `../kassensystem/rfidReader_ArduinoIDE/` und
  `../rfid_controller/` kennt keinen Socket-Namen („socket“ steht dort nur für die TCP-Verbindung).
- Fehler: `shared/web/ApiExceptionHandler` – `409` mit `code` nur für `BusinessRuleViolationException`; `400` (Bean
  Validation, `IllegalArgumentException`) ohne `code`. Format einzelner Felder heute mit Bean Validation und `400`
  (R-7 `MacAddress`, C-6 bis C-8, T-16); C-6 bis C-8 „check new and changed data; existing rows that violate them stay
  until they are edited“ (`domain/customer/README.md`).
- Bruno `core-pos/api/rfid reader/Update RFID reader.bru`: Beispiel-Body mit `"socketName": "Socket 48"`.
- Testdaten `core-pos/data/users.sql`: „Socket 6“ und „Socket 48“. Builder der Tests: `builder/CommandBuilder` und
  `builder/ViewBuilder` mit `socket-43`. Tests: `domain/rfid_reader/RfidReaderTest`
  (`update_ofANewReaderWithoutType_changesNameAndSocketNameAndReturnsTheEvent`), `RfidReaderServiceTest`,
  `web/RfidReaderControllerTest` (`updateRfidReader_…`), `integration/RfidReaderAcceptanceTest`.

**Portal** (Pfade ab `schaffbar-portal/src/app/`):

- Dialog „Lesegerät bearbeiten“ `rfid-readers/components/update-rfid-reader-form/`: die Regel steht **in der
  Komponente** – `SOCKET_NAME_PATTERN = /^[a-z0-9-]+$/` und ein `validate(schema.socketName, …)` im `form(…)`:
  geprüft nur, wenn das Feld nicht leer ist und sich vom gespeicherten Wert unterscheidet (genauer Vergleich, ohne
  Trimmen); Fehler `{ kind: 'socketNameFormat', message: 'rfidReaders.dialogs.updateRfidReader.errors.socketNameFormat' }`
  unter dem Feld, sobald es berührt ist, spätestens beim Speichern; ein ungültiges Formular schließt den Dialog nicht.
  Ein leeres Feld wird `undefined` im `UpdateRfidReaderCommand`, `toUpdateRfidReaderRequest`
  (`rfid-readers/rfid-reader.mapper.ts`) schickt `null` – der Socket-Name ist optional.
- Im selben Dialog: Hinweis unter dem Feld (`…hints.socketName`) „Nur Kleinbuchstaben, Ziffern und Bindestriche. Muss
  mit der Konfiguration auf dem Gerät übereinstimmen.“ und, nach einer gültigen Änderung, die Warnung
  `changedSocketName` / `…hints.socketNameChanged` „Das Gerät verbindet sich erst wieder, wenn der Socket-Name auch auf
  dem Gerät geändert wurde:“ – beides aus dem Prototyp; der Code oben kennt keine solche Verbindung (Frage 1).
- Die Regel R-6 im selben Dialog ist nach Skill `feature` gebaut: `isRfidReaderNameUsed` in `rfid-readers/rfid-reader.model.ts`,
  `validateUniqueRfidReaderName` in `rfid-readers/rfid-reader.validators.ts` – Vorbild für R-4.
- Specs: `update-rfid-reader-form.component.spec.ts` („should reject a socket name with other characters than a–z,
  0–9 and "-"“, „should keep an unchanged socket name in an older format“, „should accept an empty socket name …“,
  „should warn only when the socket name was changed to a valid one“).
- Übersetzungen `schaffbar-portal/public/i18n/de.json`, `en.json`: `rfidReaders.dialogs.updateRfidReader.errors.socketNameFormat`
  (de „Ungültiges Zeichen – erlaubt sind a–z, 0–9 und „-““) gibt es, `shared.errors.INVALID_SOCKET_NAME` nicht.
  `core/http/error.interceptor.ts` zeigt `shared.errors.<CODE>`, ohne Code „Speichern fehlgeschlagen“.
- API-Schicht `shared/api/rfid-readers/rfid-reader.api-model.ts`: `socketName` ist `nullable`.

**Prototyp** (mit dem Werkzeug Artifact gelesen am 25.11.2026), Artboard „RFID-Lesegeräte – Dialog: Bearbeiten“
(`project/ReaderEditDialog.dc.html`), dazu `RfidReaders.dc.html`, `RfidReaderDetail.dc.html` und die Notiz der Zeile:

- prüft den Socket-Namen **immer** mit `/^[a-z0-9-]+$/` – auch unverändert und auch leer (ein leeres Feld ist dort ein
  Fehler, ohne eigenen Text wie „Bitte einen Namen eingeben“ beim Namen); Fehlertext wie im Portal;
- Hinweis „Muss mit der Konfiguration auf dem Gerät übereinstimmen.“ und Warnung „Das Gerät verbindet sich erst
  wieder, wenn der Socket-Name auch auf dem Gerät auf … geändert wurde.“; Notiz der Zeile: „Socket-Name mit
  Formatprüfung und Hinweis auf Geräte-Konfiguration“;
- keine Prüfung, ob ein Socket-Name schon vergeben ist; in den Beispieldaten sind alle verschieden (`gate-in`,
  `gate-out`, `desk-register`, `box-hobel`, …).
- Abweichungen des Portals – nur bei einer Änderung geprüft, leer erlaubt – stehen nicht in `docs/entscheidungen.md`
  (die erste steht in der README und in `docs/aufgaben.md`; eingeführt am 08.11.2026 mit `d83425e`).

**Daten** (lesend `GET http://127.0.0.1:5000/api/v1/rfid-readers` am 25.11.2026, lokales Backend): 6 Lesegeräte,
5 ohne Socket-Namen; „Switch Kreissäge“ hat „Socket 43“ – verletzt R-4 und bleibt laut Regel gültig. Kein Socket-Name
doppelt (es gibt nur einen). **Nicht geprüft: die Daten auf dem Server der Werkstatt** – für diesen Vertrag nicht
nötig (keine Migration, bestehende Namen bleiben).

### Vertrag

<!--
Die Entscheidungen, gegen die Backend und Portal bauen. Für eine Geschäftsregel die Tabelle aus dem Skill
business-rule („The contract“): Id und Regel, genaue Bedingung, wann geprüft wird, bestehende Daten, Ort der Regel,
Exception und Code, Fehlertext de/en, API-Änderung, Verhalten im Portal, Geräte. Für anderes: geänderte Endpunkte und
Felder, Fehlercodes, Verhalten im Portal, Geräte. Jede Zeile entschieden oder als offene Frage unten.
-->

| Entscheidung | Festgelegt |
| --- | --- |
| Id und Regel | `R-4` – behält die Id aus „Enforced only in the portal“ (README). Satz für die README: "A new or changed socket name contains only lowercase letters, digits and hyphens (`^[a-z0-9-]+$`); an unchanged one stays as it is (existing names like "Socket 43"), and a reader without a socket name is not checked". Steht unter dem Vorbehalt von Frage 1 (c). |
| Genaue Bedingung | Geprüft wird ein Socket-Name, der im Request nicht `null` ist und sich vom gespeicherten unterscheidet – genauer Vergleich, mit Groß-/Kleinschreibung, ohne Trimmen (wie das Portal heute: `===` gegen den gespeicherten Wert). Er muss als ganze Zeichenkette `^[a-z0-9-]+$` erfüllen: nur ASCII `a–z`, `0–9`, `-`, mindestens ein Zeichen – Umlaute, Großbuchstaben, Leerzeichen sind ungültig; Bindestriche am Anfang, Ende oder doppelt erlaubt (wie das Muster in README und Portal). Nicht geprüft: `null` (kein Socket-Name, auch das Entfernen eines vorhandenen) und ein unveränderter Wert („Socket 43“ bleibt, auch wenn im selben Request der Name geändert wird). Beispiele: „Socket 43“ → „socket 43“ abgelehnt (Leerzeichen), → „socket-43“ erlaubt; ohne → „Socket 43“ abgelehnt. Ein leerer String `""` ist ein Socket-Name und ungültig, wenn er sich vom gespeicherten unterscheidet – das Portal schickt für ein leeres Feld weiter `null`. Quelle: README R-4 und der Hinweis in `docs/aufgaben.md` („nur bei einer Änderung prüfen – bestehende Namen wie „Socket 43“ bleiben gültig (wie im Portal)“). Optional bleibt der Socket-Name unter dem Vorbehalt von Frage 3. |
| Wann geprüft | Bei jedem `PUT /api/v1/rfid-readers/{rfidReaderId}` mit geändertem Socket-Namen – der einzige Weg zu einem Socket-Namen (Code: Anlegen und `/device/init` ohne, Typwechsel ohne). |
| Bestehende Daten | Bleiben, wie sie sind: keine Migration, kein Bereinigen – ein alter Name wird erst geprüft, wenn ihn jemand ändert (Quelle wie oben). Lokal verletzt einer („Socket 43“), Testdaten `data/users.sql` „Socket 6“, „Socket 48“ bleiben; Server nicht geprüft, nicht nötig. |
| Ort der Regel | Der Befehl der Entity `RfidReader.update(command)`, vor dem Setzen der Felder, mit `// R-4` – Skill `aggregate`, „Where a rule lives“: „the aggregate's own state, the aggregate loaded by its id → the entity's command method, throws a dedicated exception“; die Regel braucht den gespeicherten Socket-Namen. Keine Bean-Validation-Annotation: sie sieht den gespeicherten Wert nicht und lehnte jedes `PUT` mit unverändertem „Socket 43“ ab – das Portal schickt Name und Socket-Name immer zusammen, ein Lesegerät mit altem Socket-Namen ließe sich dann nicht mehr umbenennen. Entscheidung „Formatregeln an den Commands“ (13.11.2026): „Regeln mit Zustand … im Service oder Use Case“ – hier die Entity, die der Service per Id lädt. Kein Value Object `SocketName`: es prüfte sich beim Erzeugen (Skill `aggregate`, „Value objects“) und ginge an „Socket 43“ kaputt; View, API und Event-Payload bleiben `String`. Service, Use Case `RfidReaderUpdate` und Controller bleiben unverändert. |
| Exception und Code | Neu: `shared/exception/InvalidSocketNameException` extends `BusinessRuleViolationException`, `CODE = "INVALID_SOCKET_NAME"`, also `409` (Name nach `InvalidRfidReaderTypeForToolException`). Message "Socket name '%s' of RFID reader [id: %s] may contain only a-z, 0-9 and '-'" mit dem abgelehnten Wert und der Id. `409` statt `400`, weil die Regel vom gespeicherten Zustand abhängt (Zeile davor; „Drei Arten von Fehlern“, 13.11.2026: Regel verletzt `409`) – und so bekommt das Portal einen Code statt „Speichern fehlgeschlagen“. |
| Fehlertext de/en | `shared.errors.INVALID_SOCKET_NAME` – de „Ungültiger Socket-Name – nur a–z, 0–9 und „-“ verwenden.“, en "Invalid socket name – use only a–z, 0–9 and \"-\"." (was sperrt, was zu tun ist: Entscheidung „Fehlertexte“, 15.11.2026; passt auch auf `""`). Der Fehler am Feld bleibt `rfidReaders.dialogs.updateRfidReader.errors.socketNameFormat` (Text des Prototyps, unverändert). |
| API-Änderung | Keine: kein neues Feld, kein neuer Endpunkt, `UpdateRfidReaderRequestBody` bleibt ohne Constraints, `GET` unverändert – das Portal hat den gespeicherten Socket-Namen schon in den Dialogdaten. `PUT /api/v1/rfid-readers/{rfidReaderId}` antwortet zusätzlich `409` mit `code: INVALID_SOCKET_NAME`. Bruno: nur der Beispielwert (Umfang). |
| Verhalten im Portal | Für das Team an der Theke unverändert: Fehler am Feld Socket-Name im Dialog „Lesegerät bearbeiten“, sobald das Feld berührt ist, spätestens beim Speichern; ein ungültiges Formular schließt den Dialog nicht; dieselbe Bedingung wie das Backend (leer und unverändert nicht geprüft). Der Code zieht um nach Skill `feature` („Business rules“) und Skill `business-rule` (3., Schritt 3), wie R-6: die Regel als reine Funktion in `rfid-reader.model.ts` mit `R-4` und dem Grund im Kommentar, der Validator in `rfid-reader.validators.ts`, die Komponente ruft nur noch den Validator; `SOCKET_NAME_PATTERN` und das `validate` im Formular entfallen dort. Lehnt das Backend trotzdem ab (z. B. hat jemand anderes den Socket-Namen gerade geändert, der Dialog vergleicht mit dem alten Stand), zeigt der Fehler-Interceptor den Text des Codes. Hinweis und Warnung zur Gerätekonfiguration: Frage 1. Die Abweichungen vom Prototyp hält die Rolle Portal mit Datum in `docs/entscheidungen.md`, „Portal: RFID-Lesegeräte“, fest: nur bei einer Änderung geprüft (seit 08.11.2026, `d83425e`); leer erlaubt (Frage 3). |
| Geräte | Keine: kein Geräte-Endpunkt setzt oder liest einen Socket-Namen (`DeviceController`, `DeviceApiModel`; `RfidReaderCreate` nur mit MAC-Adresse) – kein Eintrag in `DeviceMessages.BY_CODE`, kein Gerätetest. |

### Umfang

<!-- Was Backend, Portal und Doku ändern (Klassen, Seiten, README, entscheidungen.md, aufgaben.md) – und was
ausdrücklich nicht dazugehört. -->

Gilt für Frage 1 (a) und (b). Bei (c) entfällt alles hier; stattdessen wird R-4 entfernt (Skill `business-rule`,
„Removed rule“) – dann schreibt die Rolle Spec den Umfang neu.

**Backend** (Klassen ab `schaffbar-backend/core-pos/src/main/java/de/schaffbar/core_pos/`, Tests ab
`…/src/test/java/de/schaffbar/core_pos/`, Bruno ab `schaffbar-backend/core-pos/`):

- neu `shared/exception/InvalidSocketNameException` (`INVALID_SOCKET_NAME`, `409`);
- `domain/rfid_reader/RfidReader.update()`: R-4 prüfen, bevor etwas gesetzt wird (Skill `aggregate`, „How a check is
  written“), das Muster einmal im Backend; abgelehnt wird nichts geändert und kein Event erzeugt;
- Tests nach Skill `backend-testing`: `RfidReaderTest` – erlaubt: ein neuer gültiger Socket-Name, ein unveränderter
  alter („Socket 43“, auch bei geändertem Namen), `null` (ohne und beim Entfernen); abgelehnt: ein geänderter
  ungültiger (Großbuchstabe, Leerzeichen, Umlaut, `""`), auch von „ohne“ auf ungültig – Exception, `code`, Name und
  Socket-Name unverändert, kein Event. `RfidReaderControllerTest` – `PUT` antwortet `409` mit dem Code. Kein
  Akzeptanztest: die Regel lebt ganz in der Entity (Skill `backend-testing`, „Which tests a change needs“, wie R-7);
  `RfidReaderService` ändert sich nicht;
- `domain/rfid_reader/README.md`: R-4 aus „Enforced only in the portal“ nach „Business Rules / Invariants“ mit dem
  Satz aus dem Vertrag, „Enforced in“ `RfidReader.update()` (`InvalidSocketNameException`, `409`); portal: error on
  the field in "Lesegerät bearbeiten". „Enforced only in the portal“ steht danach auf „None.“; Attribut `socketName`
  „optional, format R-4 when changed“; bei Frage 2 (a) eine Zeile `R-10` unter „Planned“;
- Bruno `api/rfid reader/Update RFID reader.bru`: Beispielwert `"Socket 48"` → `"socket-48"`, damit der
  Beispiel-Request nicht an R-4 scheitert – der Request selbst bleibt;
- `docs/entscheidungen.md`, „Backend: Fehler und Regeln“, mit dem Datum der Freigabe: R-4 ist eine Regel mit Zustand
  in `RfidReader.update()` – `409` mit Code statt Bean Validation und `400`, weil nur ein geänderter Socket-Name
  geprüft wird;
- keine Migration, keine `DeviceMessages`, `core-pos/docs/ddd-overview.md` und `ddd-uebersicht.md` unverändert (kein
  Use Case, kein Event). Für den Deploy: Neustart des Backends, zusammen mit dem Portal.

**Portal** (`schaffbar-portal/`):

- `public/i18n/de.json` und `en.json`: `shared.errors.INVALID_SOCKET_NAME`; bei Frage 1 (b) außerdem
  `…hints.socketName` ohne den Satz zur Gerätekonfiguration, `…hints.socketNameChanged` entfällt;
- `src/app/rfid-readers/rfid-reader.model.ts`: die Regel R-4 als reine Funktion (Socket-Name und gespeicherter
  Socket-Name → erlaubt ja/nein), mit Spec;
- `src/app/rfid-readers/rfid-reader.validators.ts`: Validator für den Socket-Namen, Fehler wie heute
  `{ kind: 'socketNameFormat', message: 'rfidReaders.dialogs.updateRfidReader.errors.socketNameFormat' }`, mit Spec;
- `update-rfid-reader-form`: Validator statt Muster und `validate` in der Komponente; bei Frage 1 (b) entfallen
  `changedSocketName` und der Block `.socket-name-changed` (mit seinem Stil); die bestehenden Dialog-Specs bleiben
  grün;
- `src/app/shared/api/rfid-readers/` unverändert;
- `npm run check`; im Browser der Fehler am Feld (unverändert) und der `409` (simuliert), Desktop und Handy.

**Doku:**

- `docs/aufgaben.md`: die Zeile R-4 in „Regeln, die nur das Portal prüft“ löschen – die Tabelle ist danach leer: den
  Abschnitt und Punkt 3 der „Reihenfolge“ entfernen oder anpassen; die Frage zum Socket-Namen unter „Fachlich zu
  klären“ nach den Antworten auf Frage 1 und 2 löschen oder kürzen; bei Frage 2 (a) eine Zeile für die neue Regel.
  Stufe 1 der „Agenten-Pipeline“ bleibt, bis Piotr den Pilot auswertet.
- `docs/entscheidungen.md` (Rolle Portal), „Portal: RFID-Lesegeräte“: Socket-Name nur bei einer Änderung geprüft,
  anders als im Prototyp (08.11.2026); Socket-Name optional (Frage 3); bei Frage 1 (b) Hinweis und Warnung zur
  Gerätekonfiguration aus dem Prototyp weggelassen.

**Nicht dabei:**

- ein eindeutiger Socket-Name – Frage 2;
- bestehende Socket-Namen bereinigen (Migration) oder ein Value Object `SocketName`;
- das Feld Socket-Name entfernen – auch bei Frage 1 (c) eine eigene Aufgabe;
- Länge von Name und Socket-Name: beide haben im Request keine Größenbeschränkung, die Spalten `VARCHAR(255)` –
  ein längerer Wert endet vermutlich mit `500` (nicht ausprobiert). Eigene Lücke, gefunden bei dieser Spec;
- die Testdaten `core-pos/data/users.sql` („Socket 6“, „Socket 48“) – alte Namen, die R-4 gültig lässt;
- „Änderungen speichern“ erst nach einer Änderung und der Zähler „N ungespeicherte Änderungen“ (Prototyp) – Aufgabe
  „Skill `forms`“; „Dialoge auf dem Handy“ – eigene Aufgabe.

### Offene Fragen

<!-- Jede Frage mit den Möglichkeiten und einer Empfehlung; Piotr antwortet darunter. Ohne beantwortete Fragen keine
Freigabe. -->

- [ ] **1. Was ist der Socket-Name, und muss er zum Gerät passen?** Im Code wird er nur gespeichert und angezeigt:
  kein Geräte-Endpunkt liest oder schickt ihn, ein Lesegerät meldet sich nur mit seiner MAC-Adresse, und die Firmware
  in `../kassensystem/` und `../rfid_controller/` kennt keinen Socket-Namen. Der Dialog sagt aber „Muss mit der
  Konfiguration auf dem Gerät übereinstimmen“ und warnt nach einer Änderung „Das Gerät verbindet sich erst wieder,
  wenn der Socket-Name auch auf dem Gerät geändert wurde“ (beides aus dem Prototyp). – Möglichkeiten:
  (a) er ist auf dem Gerät oder anderswo außerhalb dieses Codes eingestellt und muss passen – R-4 wie im Vertrag,
  Hinweis und Warnung bleiben; Piotr sagt in einem Satz, was er ist (für die README und `docs/aufgaben.md`);
  (b) er ist nur eine Beschriftung ohne Wirkung am Gerät – R-4 wie im Vertrag, dazu in dieser Aufgabe den Hinweis ohne
  den Satz zur Gerätekonfiguration und die Warnung weg (Umfang);
  (c) er wird nicht gebraucht – R-4 kommt nicht ins Backend, sondern fällt weg: die Prüfung im Portal und die Zeile
  der README gehen, die Id R-4 wird nicht wieder vergeben; das Feld selbst bliebe bis zu einer eigenen Aufgabe.
  – Empfehlung: (b), wenn Piotr keine Einstellung am Gerät kennt – die Warnung verspricht dem Team an der Theke eine
  Folge, die es im Code nicht gibt; kennt er eine, (a).
  Antwort:

- [ ] **2. Muss der Socket-Name eindeutig sein?** Im Redesign geplant, in keiner README; der Prototyp prüft es nicht,
  seine Beispieldaten sind alle verschieden; lokal gibt es nur einen Socket-Namen. – Möglichkeiten:
  (a) ja, als eigene Regel `R-10` in einer eigenen Aufgabe (Zeile unter „Planned“ in der README und in
  `docs/aufgaben.md`) – mit eigenem Vertrag: Vergleich mit alten Namen wie „Socket 43“, Groß-/Kleinschreibung,
  Lesegeräte ohne Socket-Namen;
  (b) ja, in dieser Aufgabe als `R-10` – wie R-6: Prüfung in `RfidReaderService.updateRfidReader()`, eigener Code
  `409`, Fehler am Feld gegen die geladene Liste; die Rolle Spec ergänzt dann den Vertrag;
  (c) nein – die Frage fällt aus „Fachlich zu klären“ weg.
  – Empfehlung: (a), wenn der Socket-Name ein Gerät am Netz bezeichnet (Frage 1 (a)), sonst (c) – R-4 bleibt klein,
  und ob Eindeutigkeit nötig ist, hängt an der Bedeutung aus Frage 1.
  Antwort:

- [ ] **3. Socket-Name als Pflichtfeld (Prototyp)?** Im Prototyp ist ein leeres Feld ein Fehler (das Muster verlangt
  ein Zeichen, ohne eigenen Text); das Portal lässt es leer und schickt `null`, das Backend nimmt `null` an (README:
  „optional“); 5 von 6 Lesegeräten haben lokal keinen, neue melden sich ohne an (R-9). In `docs/entscheidungen.md`
  steht nichts dazu. – Möglichkeiten:
  (a) bleibt optional – R-4 prüft nur einen angegebenen Socket-Namen; die Rolle Portal hält die Abweichung vom
  Prototyp in `docs/entscheidungen.md` fest;
  (b) Pflichtfeld nur im Portal, das Backend nimmt weiter `null`;
  (c) eine eigene Regel in Backend und Portal, als neue Aufgabe.
  – Empfehlung: (a) – wie beim Namen in R-6 (24.11.2026): die meisten Lesegeräte haben keinen, und ein Pflichtfeld
  wäre eine eigene Regel mit eigenem Vertrag.
  Antwort:

### Freigabe

<!-- Nur Piotr: „Freigegeben am <Datum>“. Erst danach beginnt das Backend. -->

## 2. Bericht Backend

<!--
- Geändert: Regeln (README-Zeilen), Exceptions und Codes, Klassen.
- Vertrag: wie in der Spec – oder jede Abweichung mit Grund (das Portal baut auf diesem Abschnitt).
- Tests: welche, Ergebnis von `mvn -q test` (Anzahl).
- Für den Deploy: Neustart, Migration.
- Nicht gemacht, nicht geprüft, offene Fragen.
-->

## 3. Bericht Portal

<!--
- Geändert: API-Schicht, Feature, Seiten und Dialoge, Übersetzungen de/en.
- Entscheidungen, die in docs/entscheidungen.md eingetragen wurden.
- Specs, Ergebnis von `npm run check`.
- Für die Abnahme: welche Seiten, Zustände und Dialoge im Browser anzusehen sind und wie jeder Zustand entsteht
  (welche Anfrage mit welcher Antwort simuliert wird).
- Abweichungen vom Vertrag, nicht gemacht, offene Fragen.
-->

## 4. Abnahme im Browser

<!-- Seiten, Zustände (auch simulierte Antworten), Desktop und Handy; was gefunden wurde. Nichts in die Datenbank
geschrieben (Skill verify-ui); Screenshots liegen nicht im Repo. -->

## 5. Review

<!-- Befunde gegen CLAUDE.md und die Skills: Datei und Zeile, verletzte Regel, muss / sollte, Stand (offen, behoben).
Ohne die Begründung des Autors gelesen. -->

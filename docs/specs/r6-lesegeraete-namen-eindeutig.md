# Namen der Lesegeräte sind eindeutig (R-6)

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

- **Quelle:** `docs/aufgaben.md`, Abschnitt „Regeln, die nur das Portal prüft“, Zeile R-6 – Pilot der
  Agenten-Pipeline (Stufe 1); Artboard „RFID-Lesegeräte – Dialog: Bearbeiten“ des Prototyps
  (`project/ReaderEditDialog.dc.html`)
- **Branch:** –
- **Stand:** Portal fertig

## 1. Spec

### Auftrag

<!-- Was soll sich ändern und warum – zwei, drei Sätze, aus Sicht der Werkstatt. -->

Zwei Lesegeräte sollen nicht denselben Namen tragen können: am Namen erkennt das Team an der Theke ein Gerät in
Listen und Meldungen („Werkstatt Ausgang“, „Switch Bandsäge“), zwei gleiche Namen führen zum falschen Gerät. Heute nimmt
das Backend jeden Namen an, und auch das Portal prüft nichts. Künftig lehnt das Backend einen schon vergebenen Namen
mit eigenem Code ab, und der Dialog „Lesegerät bearbeiten“ zeigt den Fehler schon am Feld.

### Heute

<!-- Was es schon gibt: Regeln in den README der Aggregate, Endpunkte, Seiten und Dialoge im Portal, mit Pfaden. -->

**Backend** (Pfade ab `schaffbar-backend/core-pos/src/main/java/de/schaffbar/core_pos/`):

- `domain/rfid_reader/README.md`: R-6 „Reader names are unique“ steht unter „Open Rules → Planned (not implemented
  yet)“ mit „Planned duplicate check in "Lesegerät bearbeiten"“; unter „Enforced only in the portal“ steht nur R-4.
  Vergeben sind R-1 bis R-9.
- `RfidReader.name` ist optional, Spalte `name VARCHAR(255)` ohne Constraint (`src/main/resources/db/migration/V1__init.sql`);
  nur `mac_address` ist `UNIQUE` (R-1).
- Einen Namen setzt nur `PUT /api/v1/rfid-readers/{rfidReaderId}`: `domain/rfid_reader/web/RfidReaderController.updateRfidReader`
  → Use Case `use_case/RfidReaderUpdate` (reine Weiterleitung) → `RfidReaderService.updateRfidReader()` →
  `RfidReader.update()` → Event `RFID_READER_UPDATED`. Body `UpdateRfidReaderRequestBody(name, socketName)` ohne
  Constraints, Command `UpdateRfidReaderCommand(id, name, socketName)`; Antwort `204`, `404` bei unbekannter Id.
- Angelegt wird ein Lesegerät nur mit MAC-Adresse, ohne Namen (`POST /api/v1/rfid-readers` und `/device/init` über
  `RfidReaderCreate`, R-9); der Typwechsel (`PATCH …/type`) berührt den Namen nicht. Kein Geräte-Endpunkt liest oder
  setzt den Namen eines Lesegeräts (`web/device/DeviceController`).
- Vorbild T-2 / T-15 (Werkzeugnamen eindeutig, ohne Groß-/Kleinschreibung): `ToolService.ensureNameUnused(name, toolId)`
  mit `ToolRepository.findByNameIgnoreCase` (gibt eine `List` zurück), das eigene Werkzeug ausgenommen, kein Trimmen;
  `shared/exception/ToolNameAlreadyUsedException` (`TOOL_NAME_ALREADY_USED`, `409`); zusätzlich `UNIQUE (name)` exakt in
  der Datenbank (`V4__tool_name_unique.sql`); Akzeptanztest `ToolAcceptanceTest.t2_t15_toolNamesAreUniqueWithoutRegardToCase`.
  T-15 sagt zu bestehenden Daten: „two existing names differing only in case: the next edit renames one“.
- Tests der Lesegeräte (`schaffbar-backend/core-pos/src/test/java/de/schaffbar/core_pos/`):
  `domain/rfid_reader/RfidReaderServiceTest`, `domain/rfid_reader/web/RfidReaderControllerTest`,
  `integration/RfidReaderAcceptanceTest` (`r1_…`, `r3_…`, `r5_…`).

**Portal** (Pfade ab `schaffbar-portal/src/app/`):

- Dialog „Lesegerät bearbeiten“: `rfid-readers/components/update-rfid-reader-form/` – Felder Name und Socket-Name;
  geprüft wird nur R-4 (Format des Socket-Namens, nur bei einer Änderung). Der Name ist frei und optional: ein leeres
  Feld wird `undefined` im `UpdateRfidReaderCommand`, `toUpdateRfidReaderRequest` (`rfid-readers/rfid-reader.mapper.ts`)
  schickt `null`. Die Dialogdaten enthalten nur das Lesegerät selbst (`UpdateRfidReaderDialogData { rfidReader }`).
- Geöffnet wird der Dialog nur auf der Detailseite (`rfid-readers/pages/rfid-reader-detail/rfid-reader-detail.component.ts`,
  `editRfidReader()`: Button „Bearbeiten“ im Kopf, ⋯-Menü, Karte mit den Angaben). Der Detail-Store
  (`rfid-reader-detail.store.ts`) lädt nur dieses Lesegerät und sein Werkzeug; alle Lesegeräte lädt `RfidReadersStore`
  (`rfid-readers/rfid-readers.store.ts`, `providedIn: 'root'`, Lesegeräte und Werkzeuge).
- Vorbild im Portal: `validateUniqueToolName` (`tools/tool.validators.ts`) – Vergleich mit `toLowerCase()`, eigenes
  Werkzeug ausgenommen, Fehler `{ kind: 'nameExists', message: … }`; die Werkzeug-Detailseite gibt `ToolsStore.tools`
  als Signal in die Dialogdaten (`UpdateToolDialogData.existingTools`, `tools/pages/tool-detail/tool-detail.component.ts`).
- Übersetzungen (`schaffbar-portal/public/i18n/de.json`, `en.json`): `rfidReaders.dialogs.updateRfidReader.errors.socketNameFormat`
  gibt es, `shared.errors.RFID_READER_NAME_ALREADY_USED` nicht. API-Schicht `shared/api/rfid-readers/rfid-reader.api-model.ts`:
  `name` ist `nullable`.

**Prototyp** (mit dem Werkzeug Artifact gelesen am 24.11.2026), Artboard „RFID-Lesegeräte – Dialog: Bearbeiten“
(`project/ReaderEditDialog.dc.html`):

- prüft den Namen gegen die anderen Lesegeräte mit `trim().toLowerCase()`, Fehler am Feld „Ein anderes Gerät heißt
  bereits so“;
- verlangt einen Namen („Bitte einen Namen eingeben“) – im Portal heute optional, nicht in `docs/entscheidungen.md`
  festgehalten (Frage 3);
- „Änderungen speichern“ erst nach einer Änderung, Zähler „N ungespeicherte Änderungen“ – im Portal nicht (Aufgabe
  „Skill `forms`“ in `docs/aufgaben.md`);
- das ⋯-Menü der Liste (`project/ReaderRowMenu.dc.html`) hat „Bearbeiten …“, das Portal dort nur „Details öffnen“ und
  „Lesegerät löschen“.

**Daten** (lesend `GET http://127.0.0.1:5000/api/v1/rfid-readers` am 24.11.2026, lokales Backend): 6 Lesegeräte, alle
mit Namen – „Werkstatt Eingang“, „Werkstatt Ausgang“, „Theke“, „Switch Bandsäge“, „Switch Hobel“, „Switch Kreissäge“.
Kein Name doppelt, auch nicht ohne Groß-/Kleinschreibung oder ohne Leerzeichen am Rand; kein Name nur aus Leerzeichen.
**Nicht geprüft: die Daten auf dem Server der Werkstatt.**

### Vertrag

<!--
Die Entscheidungen, gegen die Backend und Portal bauen. Für eine Geschäftsregel die Tabelle aus dem Skill
business-rule („The contract“): Id und Regel, genaue Bedingung, wann geprüft wird, bestehende Daten, Ort der Regel,
Exception und Code, Fehlertext de/en, API-Änderung, Verhalten im Portal, Geräte. Für anderes: geänderte Endpunkte und
Felder, Fehlercodes, Verhalten im Portal, Geräte. Jede Zeile entschieden oder als offene Frage unten.
-->

| Entscheidung | Festgelegt |
| --- | --- |
| Id und Regel | `R-6` – behält die Id aus „Planned“ (README). Satz für die README: "The name of an RFID reader is unique among all readers, ignoring case; readers without a name are not compared". |
| Genaue Bedingung | Gleich heißt gleich ohne Groß-/Kleinschreibung (wie T-15; der Prototyp ebenso). Leerzeichen zählen mit: „Theke “ ist ein anderer Name als „Theke“, ein Name nur aus Leerzeichen ist ein Name und wird verglichen; nichts wird getrimmt, weder beim Vergleich noch beim Speichern (Frage 1 (a), entschieden 24.11.2026). Das Lesegerät selbst ist ausgenommen – es darf seinen Namen behalten oder nur die Schreibweise ändern (wie T-15). Ohne Namen (`null`) wird nicht verglichen: mehrere Lesegeräte ohne Namen sind normal, jedes neue meldet sich ohne an (R-9), und ein leeres Feld im Dialog schickt `null`. |
| Wann geprüft | Bei jedem `PUT /api/v1/rfid-readers/{rfidReaderId}` mit Namen, auch wenn der Name gleich bleibt (wie T-15). Das ist der einzige Weg zu einem Namen (Code: Anlegen und `/device/init` ohne Namen, Typwechsel ohne Namen). |
| Bestehende Daten | Lokal keine Dublette (oben), Server nicht geprüft. Dubletten gibt es nicht und wird es nicht geben (Piotr, 24.11.2026) – kein Satz dazu in der README, kein Test dafür. Kein Unique-Index, keine Migration: nur der Service prüft; zwei Personen, die im selben Augenblick denselben Namen speichern, kämen beide durch – hingenommen (Frage 2 (a), entschieden 24.11.2026). Die Serverdaten müssen deshalb vor dem Deploy nicht geprüft werden. |
| Ort der Regel | `RfidReaderService.updateRfidReader()` – „other rows of the same aggregate → the service command“ (Skill `aggregate`, „Where a rule lives“): erst laden (`404`), dann `ensureNameUnused(name, id)` mit `// R-6` darüber, dann `update`. Im `RfidReaderRepository` `findByNameIgnoreCase(String)` als `List` (keine Unique-Spalte, Skill `aggregate`, „Repository“). Der Use Case `RfidReaderUpdate` bleibt eine Weiterleitung – der Hinweis „`RfidReaderUpdate`“ in `docs/aufgaben.md` meint den Weg; die Prüfung braucht kein zweites Aggregat. |
| Exception und Code | Neu: `shared/exception/RfidReaderNameAlreadyUsedException` extends `BusinessRuleViolationException`, `CODE = "RFID_READER_NAME_ALREADY_USED"`, `409`; Message "RFID reader name '%s' is already used by RFID reader [id: %s]" mit dem Namen und der Id des anderen Lesegeräts (wie `ToolNameAlreadyUsedException`). `TOOL_NAME_ALREADY_USED` passt nicht, sein Text nennt ein Werkzeug (Skill `aggregate`: ein Code pro Klasse, sein Text passt zu allen Varianten). |
| Fehlertext de/en | `shared.errors.RFID_READER_NAME_ALREADY_USED` – de „Ein anderes Lesegerät heißt schon so – einen anderen Namen wählen.“, en "Another RFID reader already has this name – choose a different name." (was sperrt, was zu tun ist: Entscheidung „Fehlertexte“ vom 15.11.2026; gebaut wie `RFID_READER_HAS_TOOL_ASSIGNED`). Fehler am Feld im Dialog: `rfidReaders.dialogs.updateRfidReader.errors.nameExists` – de „Ein anderes Gerät heißt bereits so“ (Text des Prototyps), en "Another device already has this name". |
| API-Änderung | Keine: kein neues Feld, kein neuer Endpunkt – `GET /api/v1/rfid-readers` liefert alle Namen, das Portal hat damit die Liste. `PUT /api/v1/rfid-readers/{rfidReaderId}` antwortet zusätzlich `409` mit `code: RFID_READER_NAME_ALREADY_USED`. Bruno unverändert (Skill `business-rule`: ein neuer `409` ändert keinen Request). |
| Verhalten im Portal | Fehler am Feld Name im Dialog „Lesegerät bearbeiten“, gegen die geladene Liste aller Lesegeräte ohne das eigene, mit demselben Vergleich wie das Backend: ohne Groß-/Kleinschreibung, ohne Trimmen (anders als der Prototyp mit `trim()`), Lesegeräte ohne Namen nicht verglichen. Angezeigt wie der Fehler des Socket-Namens: sobald das Feld berührt ist, spätestens beim Speichern; ein ungültiges Formular schließt den Dialog nicht. Der Dialog bekommt die Lesegeräte als Signal in seinen Daten, die Detailseite nimmt sie aus `RfidReadersStore` – wie `existingTools` aus `ToolsStore` beim Werkzeug (T-15). Lehnt das Backend trotzdem ab (Liste veraltet, jemand hat gerade gespeichert), zeigt der Fehler-Interceptor den Text des Codes. Der Name bleibt optional – kein Pflichtfeld wie im Prototyp, ein leeres Feld schickt weiter `null`; die Rolle Portal trägt diese Abweichung vom Prototyp mit Datum in `docs/entscheidungen.md` ein, Abschnitt „Portal: RFID-Lesegeräte“ (Frage 3 (a), entschieden 24.11.2026). |
| Geräte | Keine: kein Geräte-Endpunkt setzt einen Namen (`DeviceController`; `RfidReaderCreate` nur mit MAC-Adresse) – kein Eintrag in `DeviceMessages.BY_CODE`, kein Gerätetest. |

### Umfang

<!-- Was Backend, Portal und Doku ändern (Klassen, Seiten, README, entscheidungen.md, aufgaben.md) – und was
ausdrücklich nicht dazugehört. -->

**Backend** (`schaffbar-backend/core-pos/`):

- neu `shared/exception/RfidReaderNameAlreadyUsedException`;
- `RfidReaderRepository.findByNameIgnoreCase(String)` (Frage 1 (a): ohne Trimmen);
- `RfidReaderService.updateRfidReader()`: laden (`404`), `ensureNameUnused` (R-6) nur mit Namen, dann ändern;
- Tests: `RfidReaderServiceTest` – erlaubt: ein freier Name, der eigene Name in anderer Schreibweise, ohne Namen neben
  anderen ohne Namen; abgelehnt: der Name eines anderen Lesegeräts, auch in anderer Schreibweise (Exception, `code`,
  kein Event gespeichert); `RfidReaderControllerTest` – `PUT` antwortet `409` mit dem Code;
  `integration/RfidReaderAcceptanceTest` – `r6_…` mit beiden Seiten;
- `domain/rfid_reader/README.md`: R-6 aus „Planned“ nach „Business Rules / Invariants“, „Enforced in“
  `RfidReaderService.updateRfidReader()` (`RfidReaderNameAlreadyUsedException`, `409`); portal: Fehler am Feld in
  „Lesegerät bearbeiten“. Der Abschnitt „Planned“ ist danach leer;
- keine Migration (Frage 2 (a)), kein Bruno, keine `DeviceMessages`, `docs/ddd-overview.md`
  unverändert (kein neuer Use Case, kein Event). Für den Deploy: Neustart des Backends, zusammen mit dem Portal.

**Portal** (`schaffbar-portal/`):

- `public/i18n/de.json` und `en.json`: `shared.errors.RFID_READER_NAME_ALREADY_USED`,
  `rfidReaders.dialogs.updateRfidReader.errors.nameExists`;
- Validator für den eindeutigen Namen nach dem Vorbild `validateUniqueToolName`, in
  `src/app/rfid-readers/rfid-reader.validators.ts` (Skill `feature`, „Folders“), mit Spec;
- `update-rfid-reader-form`: Lesegeräte (Signal) in den Dialogdaten, Validator am Feld Name;
- `rfid-reader-detail.component.ts`: gibt die Lesegeräte aus `RfidReadersStore` in die Dialogdaten;
- Specs für Validator, Dialog und Detailseite; `npm run check`; im Browser der Fehler am Feld und der `409`
  (simuliert), Desktop und Handy;
- `src/app/shared/api/rfid-readers/` bleibt unverändert.

**Doku:**

- `docs/aufgaben.md`: die Zeile R-6 in „Regeln, die nur das Portal prüft“ löschen und Punkt 3 der „Reihenfolge“
  anpassen; Stufe 1 der „Agenten-Pipeline“ bleibt, bis Piotr den Pilot auswertet.
- `docs/entscheidungen.md` (Rolle Portal), Abschnitt „Portal: RFID-Lesegeräte“, mit Datum 24.11.2026: der Name im
  Dialog „Lesegerät bearbeiten“ bleibt optional, anders als im Prototyp (Frage 3 (a)); der Namensvergleich trimmt
  nicht, anders als im Prototyp (Frage 1 (a)).

**Nicht dabei:**

- der Name als Pflichtfeld (Prototyp) – entschieden: bleibt optional (Frage 3 (a));
- Namen ohne Leerzeichen am Rand vergleichen oder speichern (Frage 1 (a));
- ein Unique-Index in der Datenbank (Frage 2 (a));
- R-4 (Format des Socket-Namens) im Backend – eigene Zeile in `docs/aufgaben.md`; ob der Socket-Name eindeutig sein
  muss, steht unter „Fachlich zu klären“;
- „Bearbeiten …“ im ⋯-Menü der Lesegeräteliste (Prototyp `ReaderRowMenu`);
- „Änderungen speichern“ erst nach einer Änderung und der Zähler „N ungespeicherte Änderungen“ (Prototyp) – Aufgabe
  „Skill `forms`“;
- Werkzeugnamen: `tool_name_unique` (V4) ist exakt, T-15 ohne Groß-/Kleinschreibung – bleibt, wie es ist.

### Offene Fragen

<!-- Jede Frage mit den Möglichkeiten und einer Empfehlung; Piotr antwortet darunter. Ohne beantwortete Fragen keine
Freigabe. -->

- [x] **1. Leerzeichen im Namen:** Sind „Theke“ und „Theke “ (Leerzeichen am Rand, z. B. von der Autokorrektur am
  Tablet) derselbe Name, und ist ein Name nur aus Leerzeichen ein Name? – Möglichkeiten:
  (a) wie T-15 bei den Werkzeugen: nur Groß-/Kleinschreibung zählt nicht, Leerzeichen zählen mit – „Theke “ ist ein
  anderer Name, „ “ ist ein Name; Backend `findByNameIgnoreCase`, das Portal vergleicht genauso;
  (b) wie der Prototyp: verglichen wird ohne Leerzeichen am Rand, gespeichert wird, was gesendet wurde; ein Name nur
  aus Leerzeichen zählt als kein Name – das Backend braucht eine eigene Abfrage (`lower(trim(name))`);
  (c) das Backend speichert den Namen ohne Leerzeichen am Rand (leer wird kein Name) und vergleicht dann wie (a), das
  Portal schickt ihn ebenso – ändert, was gespeichert wird; Werkzeugnamen blieben ungetrimmt.
  – Empfehlung: (a) – dieselbe Regel wie für Werkzeugnamen, ein einfacher Vergleich in Backend und Portal; Namen zu
  trimmen wäre eine eigene Aufgabe für Werkzeuge und Lesegeräte zusammen.
  Antwort (24.11.2026): (a) – wie T-15, nur Groß-/Kleinschreibung zählt nicht, Leerzeichen zählen mit.

- [x] **2. Unique-Index in der Datenbank:** Soll die Datenbank R-6 zusätzlich sichern? – Möglichkeiten:
  (a) nein, nur der Service prüft – zwei Personen, die im selben Augenblick denselben Namen speichern, kämen beide
  durch;
  (b) Migration `V10__…` mit einem Unique-Index auf `lower(name)` (Lesegeräte ohne Namen bleiben mehrfach erlaubt) –
  sichert auch diesen Fall; die Migration scheitert beim Deploy, wenn es auf dem Server schon Dubletten gibt (nicht
  geprüft – vorher `GET /api/v1/rfid-readers` auf dem Server), und sie muss zum Vergleich aus Frage 1 passen;
  (c) wie T-2: `UNIQUE (name)` exakt – sichert nur exakt gleiche Namen.
  – Empfehlung: (a) – kein Gerät setzt einen Namen, der gleichzeitige Fall ist an der Theke kaum denkbar (die Indizes
  vom 22.11.2026 gibt es nur für Abläufe an den Geräten), und der Deploy hängt nicht an ungeprüften Serverdaten.
  Antwort (24.11.2026): (a) – kein Unique-Index, nur der Service prüft.

- [x] **3. Name als Pflichtfeld (Prototyp):** Das Artboard verlangt im Dialog einen Namen („Bitte einen Namen
  eingeben“); das Portal lässt ihn leer und schickt `null`, das Backend nimmt `null` an; in `docs/entscheidungen.md`
  steht nichts dazu. – Möglichkeiten:
  (a) bleibt optional – die Rolle Portal hält die Abweichung vom Prototyp mit Datum in `docs/entscheidungen.md` fest,
  damit sie niemand „richtig“ umsetzt;
  (b) in dieser Aufgabe Pflichtfeld nur im Portal (`required` am Feld), das Backend nimmt weiter `null`;
  (c) eine eigene Regel (nächste freie Id `R-10`) in Backend und Portal, als neue Aufgabe in `docs/aufgaben.md` –
  nicht in dieser.
  – Empfehlung: (a) – R-6 braucht es nicht (ohne Namen wird nicht verglichen), und ein Pflichtname ist eine eigene
  Regel mit eigenem Vertrag (Lesegeräte melden sich ohne Namen an, R-9).
  Antwort (24.11.2026): (a) – Name bleibt optional; die Rolle Portal hält die Abweichung vom Prototyp in
  `docs/entscheidungen.md` fest.

### Freigabe

<!-- Nur Piotr: „Freigegeben am <Datum>“. Erst danach beginnt das Backend. -->

Freigegeben am 24.11.2026 (Piotr im Chat, eingetragen von der Hauptsitzung).

## 2. Bericht Backend

<!--
- Geändert: Regeln (README-Zeilen), Exceptions und Codes, Klassen.
- Vertrag: wie in der Spec – oder jede Abweichung mit Grund (das Portal baut auf diesem Abschnitt).
- Tests: welche, Ergebnis von `mvn -q test` (Anzahl).
- Für den Deploy: Neustart, Migration.
- Nicht gemacht, nicht geprüft, offene Fragen.
-->

### Geändert

Pfade ab `schaffbar-backend/core-pos/src/main/java/de/schaffbar/core_pos/`:

- **Regel** `domain/rfid_reader/README.md`: R-6 aus „Open Rules → Planned“ nach „Business Rules / Invariants“, mit dem
  Satz aus dem Vertrag; „Enforced in“ `RfidReaderService.updateRfidReader()` (`RfidReaderNameAlreadyUsedException`,
  `409`); portal: error on the field in "Lesegerät bearbeiten". „Planned“ steht jetzt auf „None.“; in den Attributen
  hat `name` „optional, unique ignoring case (R-6)“.
- **Exception** neu: `shared/exception/RfidReaderNameAlreadyUsedException` extends `BusinessRuleViolationException`,
  `CODE = "RFID_READER_NAME_ALREADY_USED"` (`409`), Message "RFID reader name '%s' is already used by RFID reader
  [id: %s]" mit dem Namen und der Id des anderen Lesegeräts.
- **Abfrage** `domain/rfid_reader/RfidReaderRepository.findByNameIgnoreCase(String)` → `List<RfidReader>` (abgeleitet
  von Spring Data, ohne Trimmen).
- **Prüfung** `domain/rfid_reader/RfidReaderService.updateRfidReader()`: lädt das Lesegerät (`404`), prüft dann – nur
  mit Namen (`nonNull`) – `ensureNameUnused(name, id)` (`// R-6`, das eigene Lesegerät ausgenommen, `throw` in
  `throwRfidReaderNameAlreadyUsedException`), dann `rfidReader.update(command)` und das Event `RFID_READER_UPDATED` wie
  bisher. Abgelehnt wird nichts geschrieben, kein Event.
- Unverändert: `use_case/RfidReaderUpdate` (Weiterleitung), Controller, `RfidReaderApiModel`, Bruno, `DeviceMessages`,
  Migrationen, `core-pos/docs/ddd-overview.md` / `ddd-uebersicht.md` (kein neuer Use Case, kein Event;
  `RfidReaderUpdate` steht dort weiter unter „only their own aggregate“).
- `docs/entscheidungen.md`, „Backend: Fehler und Regeln“: „Namen der Lesegeräte ohne Unique-Index“ (24.11.2026,
  Frage 2 (a)). Die Portal-Einträge zu Frage 1 (a) und 3 (a) bleiben der Rolle Portal, ebenso `docs/aufgaben.md`.

### Vertrag im Backend

Wie in der Spec, ohne Abweichung. Was das Portal braucht:

- `PUT /api/v1/rfid-readers/{rfidReaderId}`, Body unverändert `{ "name": string | null, "socketName": string | null }`
  (ohne Constraints); Antworten `204`, `404` mit `code: RESOURCE_NOT_FOUND`, `resource: RFID_READER` (vor der
  Prüfung), neu `409` (`application/problem+json`) mit `code: RFID_READER_NAME_ALREADY_USED`.
- `409`, wenn ein anderes Lesegerät den Namen trägt: ohne Groß-/Kleinschreibung, Leerzeichen zählen mit („Theke “ ≠
  „Theke“), ein Name nur aus Leerzeichen wird verglichen; `name: null` wird nicht verglichen. Geprüft bei jedem `PUT`
  mit Namen, auch wenn er gleich bleibt.
- Ein leerer String `""` ist für das Backend ein Name, nicht `null`, und würde verglichen – das Portal schickt für ein
  leeres Feld weiter `null` (`toUpdateRfidReaderRequest`).
- Kein neues Feld, kein neuer Endpunkt; `GET /api/v1/rfid-readers` unverändert (`name` nullable).
- Übersetzung (Rolle Portal), Text aus dem Vertrag: `shared.errors.RFID_READER_NAME_ALREADY_USED` – de „Ein anderes
  Lesegerät heißt schon so – einen anderen Namen wählen.“, en "Another RFID reader already has this name – choose a
  different name."

### Tests

- `domain/rfid_reader/RfidReaderServiceTest` (Pfade ab `core-pos/src/test/java/de/schaffbar/core_pos/`):
  - erlaubt: ein freier Name (`updateRfidReader_ofANewReaderWithoutType_changesNameAndSocketNameAndSavesTheEvent`, jetzt
    mit `givenReadersNamed(command.name())` ohne Treffer), der eigene Name in anderer Schreibweise
    (`updateRfidReader_keepsItsOwnNameInAnotherCase`), ohne Namen (`updateRfidReader_withoutName_isNotComparedWithOtherReaders`
    – keine Abfrage);
  - abgelehnt: `updateRfidReader_rejectsTheNameOfAnotherReader` („Theke“, „THEKE“) – Exception, `code`, Id des anderen Lesegeräts in der Message, Lesegerät unverändert, kein Event.
- `domain/rfid_reader/web/RfidReaderControllerTest.updateRfidReader_withTheNameOfAnotherReader_is409WithTheCode`.
- `integration/RfidReaderAcceptanceTest`:
  - `r6_readerNamesAreUniqueWithoutRegardToCase` – ein anderes Lesegerät mit dem Namen in Großbuchstaben → `409`, sein
    Name bleibt; mit einem Leerzeichen am Ende → `204`, so gespeichert; das Lesegerät selbst in Großbuchstaben → `204`.
    Der Zufallsname beginnt mit „Lesegerät“ – damit vergleicht der Test auch „ä“ mit „Ä“.
  - `r6_readersWithoutANameAreNotCompared` – neben einem Lesegerät ohne Namen wird der Name eines zweiten geleert → `204`.
- Fixture: `builder/RandomValues.rfidReaderName()`, `Scenario.rfidReaderName`, `SchaffbarApi.updateRfidReader(id, name)`
  (`null` → JSON `null`), `GivenFixture.rfidReader(macAddress, name)`, `ThenFixture.expectRfidReader(id, name)` und
  `expectRfidReaderWithoutName(id)`.
- Gegenprobe (danach zurückgesetzt): ohne `ensureNameUnused` scheitern die Ablehnungen im Service- und im
  Akzeptanztest; ohne die Bedingung `nonNull(name)` scheitern `…_withoutName_…` und
  `r6_readersWithoutANameAreNotCompared` – die Abfrage mit `null` findet die Lesegeräte ohne Namen und antwortet `409`.
- Über die Testliste der Spec hinaus: der Schritt mit dem Leerzeichen am Ende (Frage 1 (a)). Den Servicetest der
  bestehenden Dublette und ihren Satz in der README hat die Hauptsitzung auf Piotrs Wunsch gelöscht (24.11.2026) –
  Dubletten wird es nicht geben.
- `mvn -q spotless:apply`, dann `mvn -q test` mit Docker: **683 Tests in 86 Klassen, 0 Fehlschläge, 0 Fehler,
  0 übersprungen** (gezählt aus `target/surefire-reports/`). Nach dem Löschen des Servicetests sind es 682; erneut
  gelaufen ist nur `RfidReaderServiceTest` (12 Tests, 0 Fehlschläge).

### Für den Deploy

- Neustart des Backends, zusammen mit dem Portal. Keine Migration.
- Die Daten auf dem Server müssen vorher nicht geprüft werden: kein Index, keine Migration.

### Nicht gemacht, nicht geprüft, offene Fragen

- Nicht gemacht (Rolle Portal): Übersetzungen, Validator, Dialog, Detailseite, `docs/aufgaben.md`, die Einträge zu
  Frage 1 (a) und 3 (a) in `docs/entscheidungen.md`.
- Nicht geprüft: die Daten auf dem Server (laut Vertrag nicht nötig); zwei gleichzeitige Speicherungen desselben
  Namens (hingenommen, Frage 2 (a)); ob `upper()` in PostgreSQL und `toLowerCase()` im Portal bei Sonderzeichen
  außer den Umlauten gleich vergleichen – Umlaute sind im Testcontainer geprüft; der Server nutzt laut
  `infrastructure/docker-compose.yml` und `deployment.md` dasselbe Image `postgres:16.3` ohne eigene Locale, auf dem
  Server selbst nicht nachgesehen.
- Das laufende Backend und seine Datenbank nicht angefasst.
- Keine offenen Fragen.

## 3. Bericht Portal

<!--
- Geändert: API-Schicht, Feature, Seiten und Dialoge, Übersetzungen de/en.
- Entscheidungen, die in docs/entscheidungen.md eingetragen wurden.
- Specs, Ergebnis von `npm run check`.
- Für die Abnahme: welche Seiten, Zustände und Dialoge im Browser anzusehen sind und wie jeder Zustand entsteht
  (welche Anfrage mit welcher Antwort simuliert wird).
- Abweichungen vom Vertrag, nicht gemacht, offene Fragen.
-->

### Geändert

Pfade ab `schaffbar-portal/`:

- **API-Schicht** unverändert (`src/app/shared/api/rfid-readers/`): `GET /api/v1/rfid-readers` und
  `PUT /api/v1/rfid-readers/{rfidReaderId}` in `RfidReaderController` und `RfidReaderApiModel.java` nachgesehen – keine
  neue Form, `name` bleibt `nullable`.
- **Regel** `src/app/rfid-readers/rfid-reader.model.ts`, neuer Abschnitt `// update`:
  `isRfidReaderNameUsed(name, rfidReaderId, rfidReaders)` mit `R-6` im Kommentar – ohne Groß-/Kleinschreibung
  (`toLowerCase()`), ohne Trimmen, das eigene Lesegerät ausgenommen; ohne Namen (`null`, leeres Feld) wird nicht
  verglichen, Lesegeräte ohne Namen in der Liste auch nicht.
- **Validator** neu `src/app/rfid-readers/rfid-reader.validators.ts`: `validateUniqueRfidReaderName(path, rfidReaderId,
  rfidReaders)` nach dem Vorbild `validateUniqueToolName` – ruft die Regel des Modells, Fehler
  `{ kind: 'nameExists', message: 'rfidReaders.dialogs.updateRfidReader.errors.nameExists' }`; die Liste wird im
  Validator gelesen, mit einem Signal folgt die Prüfung der Liste.
- **Dialog „Lesegerät bearbeiten“** `src/app/rfid-readers/components/update-rfid-reader-form/`: neue Dialogdaten
  `existingRfidReaders: Signal<ExistingRfidReaderViewModel[]>` (`{ id, name }`), der Validator am Feld Name. Das
  Template ist unverändert: der Fehler steht in `schbar-field-error` unter dem Feld wie der des Socket-Namens – sobald
  das Feld berührt (verlassen) ist, spätestens beim Speichern; ein ungültiges Formular schließt den Dialog nicht, es
  geht kein `PUT` hinaus.
- **Detailseite** `src/app/rfid-readers/pages/rfid-reader-detail/rfid-reader-detail.component.ts`: injiziert
  `RfidReadersStore` und gibt dessen Signal `rfidReaders` als `existingRfidReaders` in die Dialogdaten (wie
  `existingTools` aus `ToolsStore`); lädt die Liste beim Öffnen der Seite und mit „Neu laden“ neu, wie die
  Werkzeug-Detailseite ihren `ToolsStore`. Die Seite lädt damit zusätzlich `GET /api/v1/rfid-readers` und
  `GET /api/v1/tools` (der Store lädt beide).
- **Übersetzungen** `public/i18n/de.json` und `en.json`: `shared.errors.RFID_READER_NAME_ALREADY_USED` mit dem Text
  des Vertrags, `rfidReaders.dialogs.updateRfidReader.errors.nameExists` – de „Ein anderes Gerät heißt bereits so“,
  en "Another device already has this name".
- **Doku:** `docs/entscheidungen.md` und `docs/aufgaben.md` (unten). Die README des Aggregats nicht angefasst – R-6
  steht dort seit dem Backend unter „Business Rules / Invariants“ mit dem Teil des Portals; „Enforced only in the
  portal“ betrifft R-6 nicht mehr.

### Entscheidungen

- `docs/entscheidungen.md`, „Portal: RFID-Lesegeräte“, beide vom 24.11.2026:
  - **Name optional** – das Feld darf leer bleiben, anders als im Prototyp (Frage 3 (a));
  - **Namensvergleich ohne Trimmen** – ohne Groß-/Kleinschreibung, Leerzeichen am Rand zählen mit, anders als im
    Prototyp (Frage 1 (a)).
- `docs/aufgaben.md`: die Zeile R-6 in „Regeln, die nur das Portal prüft“ gelöscht; Punkt 3 der „Reihenfolge“ nennt
  jetzt das Format des Socket-Namens (R-4). Stufe 1 der „Agenten-Pipeline“ bleibt.
- Gebaut nach den Skills, nicht in `entscheidungen.md`: die Regel ist eine Funktion des Modells (Skill `feature`,
  „Business rules“), der Validator in `rfid-reader.validators.ts` ruft sie nur auf – die Spec nannte den Validator,
  die Rolle verlangt die Regel im Modell.

### Tests

- `src/app/rfid-readers/rfid-reader.model.spec.ts`, `isRfidReaderNameUsed`: der Name eines anderen Lesegeräts, auch
  in anderer Schreibweise („SWITCH BANDSÄGE“); der eigene Name und seine Schreibweise; ein freier Name; Leerzeichen am
  Rand zählen („Theke “ frei, „ “ gegen „ “ belegt); ohne Namen (`null`, `''`) kein Vergleich.
- `src/app/rfid-readers/rfid-reader.validators.spec.ts` (neu): Fehler mit `kind` und Text des Dialogs; frei und leer
  ohne Fehler; die Prüfung folgt dem Signal der Liste.
- `.../update-rfid-reader-form.component.spec.ts`: abgelehnt – der Name eines anderen Lesegeräts in Großbuchstaben
  (Dialog bleibt offen, Fehler am Feld); der Fehler schon nach dem Verlassen des Feldes; erlaubt – der eigene Name in
  Großbuchstaben, „Theke “ (ungetrimmt im Command), ein leerer Name neben Lesegeräten ohne Namen; der Fehler erscheint,
  wenn die Liste sich bei offenem Dialog ändert.
- `.../rfid-reader-detail.component.spec.ts`: Dialogdaten mit `existingRfidReaders` = das Signal des
  `RfidReadersStore`; die Liste wird beim Öffnen und mit „Neu laden“ geladen.
- Gegenprobe (danach zurückgesetzt): ohne den Validator im Dialog scheitern drei Dialog-Tests (abgelehnt, nach dem
  Verlassen, folgt der Liste).
- `npm run check`: Lint, Prettier, Typen der Specs, **570 Tests in 111 Dateien, alle grün**, Build – bestanden.

### Für die Abnahme

Detailseite eines Lesegeräts `/rfid-readers/<id>` mit Namen (lokal z. B. „Werkstatt Eingang“), Dialog „Lesegerät
bearbeiten“ – geöffnet über „Bearbeiten“ im Kopf, „Bearbeiten …“ im ⋯-Menü oder die Karte mit den Angaben. Desktop
und Handy. Schreibzugriffe abfangen (Skill `verify-ui`); die Liste der Lesegeräte kommt echt aus `GET`.

1. **Fehler am Feld:** im Feld Name „theke“ (Name eines anderen Geräts, andere Schreibweise) eintippen, mit Tab
   verlassen → unter dem Feld rot „Ein anderes Gerät heißt bereits so“ statt des Hinweises. „Änderungen speichern“ →
   der Dialog bleibt offen, kein `PUT` im Netzwerk. Langer Text auf dem Handy: bricht der Fehler um?
2. **Erlaubt, ohne Fehler:** der eigene Name in anderer Schreibweise („WERKSTATT EINGANG“), „Theke “ mit Leerzeichen
   am Ende, ein leeres Feld. Speichern schickt ein `PUT` – mit `route.fulfill({ status: 204, body: '' })`
   beantworten, damit nichts in die Datenbank geht; der Dialog schließt.
3. **Ablehnung des Backends (Liste veraltet):** einen freien Namen eingeben, das `PUT` auf
   `/api/v1/rfid-readers/<id>` mit `route.fulfill({ status: 409, contentType: 'application/problem+json', body:
   JSON.stringify({ type: 'about:blank', title: 'Conflict', status: 409, detail: "RFID reader name 'X' is already
   used by RFID reader [id: …]", code: 'RFID_READER_NAME_ALREADY_USED' }) })` beantworten → Snackbar „Ein anderes
   Lesegerät heißt schon so – einen anderen Namen wählen.“; der Dialog ist schon zu, die Seite zeigt weiter den alten
   Namen.
4. **Lesegerät ohne Namen** (Liste mit `route.fetch()` holen und ein Gerät mit `name: null` ergänzen, oder ein neues
   Gerät ohne Typ, falls vorhanden): Dialog mit leerem Feld, Speichern ohne Fehler.
5. Optional: die Seite lädt jetzt zusätzlich `GET /api/v1/rfid-readers` und `GET /api/v1/tools` – beim Öffnen und mit
   „Neu laden“; keine sichtbare Änderung erwartet.

### Abweichungen, nicht gemacht, offene Fragen

- **Abweichungen vom Vertrag:** keine.
- **Über die Spec hinaus:** die Detailseite lädt die Liste der Lesegeräte beim Öffnen und mit „Neu laden“ neu (Vorbild
  Werkzeug-Detailseite) – sonst prüfte der Dialog gegen den Stand der letzten Listenansicht. Schlägt dieses Laden
  fehl, zeigt der Fehler-Interceptor „Die Daten konnten nicht geladen werden.“, und der Dialog prüft gegen eine leere
  Liste – dann bleibt das `409` des Backends die Erklärung.
- **Nicht gemacht** (laut Spec nicht dabei): der Name als Pflichtfeld, Trimmen, „Bearbeiten …“ im ⋯-Menü der Liste,
  „Änderungen speichern“ erst nach einer Änderung und der Zähler.
- **Nicht geprüft:** der Browser (Rolle Verify); ob `toLowerCase()` im Portal und `upper()` in PostgreSQL bei
  Sonderzeichen außer den Umlauten (etwa „ß“) gleich vergleichen – siehe Bericht Backend; weicht es ab, erklärt das
  `409` den Fall.
- Das Backend und seine Datenbank nicht angefasst, nichts in `schaffbar-backend/` geändert.
- **Offene Fragen:** keine.

## 4. Abnahme im Browser

<!-- Seiten, Zustände (auch simulierte Antworten), Desktop und Handy; was gefunden wurde. Nichts in die Datenbank
geschrieben (Skill verify-ui); Screenshots liegen nicht im Repo. -->

## 5. Review

<!-- Befunde gegen CLAUDE.md und die Skills: Datei und Zeile, verletzte Regel, muss / sollte, Stand (offen, behoben).
Ohne die Begründung des Autors gelesen. -->

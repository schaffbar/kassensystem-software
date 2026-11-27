# Entscheidungen

Was entschieden ist und weiter gilt, mit Datum – damit es niemand aus Versehen zurückdreht, auch kein Agent, der den
Prototyp „richtig“ umsetzen will. Die Regeln selbst stehen in den Skills und den `README.md` der Aggregate; hier steht,
was entschieden wurde, wo es nicht offensichtlich ist, und warum.

Pflege: eine neue Entscheidung kommt unter ihr Thema, mit Datum. Eine geänderte Entscheidung ersetzt die alte (beide
Daten), eine aufgehobene wird gelöscht. Offene Aufgaben stehen in [`aufgaben.md`](aufgaben.md).

## Zusammenarbeit und Agenten

- **Erst vereinheitlichen, dann Skills** (13.11.2026): Agenten kopieren, was sie im Code vorfinden – deshalb wurde das
  Backend vereinheitlicht, bevor seine Skills geschrieben wurden.
- **Rollen statt eines Agenten pro Thema** (23.11.2026): die Pipeline hat Rollen (Spec, Implementation Backend,
  Implementation Portal, Verify, Review); eine Geschäftsregel setzen sie mit dem Skill `business-rule` um, es gibt
  keinen eigenen Regel-Agenten. Ein Sub-Agent kann keinen Sub-Agenten starten – die Hauptsitzung ruft die Rollen auf.
- **Backend und Portal getrennt** (23.11.2026): je eine Rolle für Backend und Portal – eigene Skills, Prüfungen und
  Ordner, kleinerer Kontext; dafür eine Übergabe mehr, die der Pilot prüfen soll.
- **Übergabe in einer Datei pro Aufgabe** (24.11.2026): `docs/specs/<kurzname>.md` nach `docs/specs/vorlage.md`, jede
  Rolle schreibt ihren eigenen Abschnitt – alles zu einer Aufgabe an einem Ort, im PR sichtbar; nach dem Merge
  gelöscht, die Geschichte steht in git und im PR.
- **Entscheidungen trifft Piotr** (23.11.2026): ein Agent stellt offene Punkte als Frage, statt zu raten – die
  wertvollsten Momente im Redesign waren Entscheidungen (Banner trotz WS-5, Leihtag-Filter, Löschkonzept).

## Backend: Aufbau

- **Änderungen über Use Cases, Abfragen über die Read Facade** (13.11.2026, 15.11.2026): jede Änderung geht über einen
  Use Case, auch eine reine Weiterleitung; jede Abfrage eines Controllers über die eine Read Facade, die nach ihm
  benannt ist (`read_facade/` neben `use_case/`). Ein fester Ort, an dem später eine Regel mit einem zweiten Aggregat
  dazukommt, und ein Muster für Agenten.
- **Controller ohne Aggregat** (21.11.2026): in `web/<object>/` (`web/dashboard/`, `web/device/`) – in `domain/` liegen
  nur Aggregate.
- **`@Query` und `@Command`** (15.11.2026): jede öffentliche Methode von Services, Use Cases und Read Facades trägt
  genau eine davon – sie sagt, ob gelesen oder geschrieben wird, und bringt die Transaktion; kein direktes
  `@Transactional`, und wo doch, das von Spring, nicht jakarta (13.11.2026).
- **Jede Datenbankänderung erzeugt ein Event** (16.11.2026): auch Löschen und das Schreiben einer inneren Entity; die
  Änderung einer inneren Entity steht im Event ihres Aggregats. Feste Regel – die Historie ohne Event war eine
  Abkürzung.
- **Outbox als Komponente** (13.11.2026): `OutboxEventStore` wird in jeden Service injiziert, keine Basisklasse –
  Services bleiben ohne Vererbung und leicht zu mocken.
- **Outbox ohne Relay** (22.11.2026): vorerst ist kein Abnehmer geplant, die Outbox ist das Änderungsprotokoll;
  `processed`, `processedAt` und `owner` bleiben als Vorbereitung.
- **Innere Entity** (16.11.2026, 22.11.2026): `RfidTagAssignmentHistory` gehört zum Aggregat `RfidTagAssignment` –
  eigenes Repository, sonst keine eigenen Bausteine; im Web ein Unterpfad des Aggregats
  (`GET /rfid-tag-assignments/history`), kein eigener Controller.
- **`*Commands` nur bei Bedarf** (13.11.2026): ein Command-Record lohnt sich erst ab mehr als ein, zwei Werten; nimmt
  jede Änderung eines Aggregats nur ein, zwei Werte, hat es kein `*Commands`.
- **ArchUnit streng** (22.11.2026): keine Regel mit Freeze-Store; eine neue Regel wird einmal gegen einen absichtlichen
  Verstoß geprüft.
- **Namen der Use Cases ohne ArchUnit-Regel** (23.11.2026): das Präfix allein ließ das falsche Aggregat durch; die
  Regel steht im Skill `use-case`.
- **Altersgruppe in der Domäne** (22.11.2026): `Customer.getAgeGroupOn(day)`, nicht im API-Modell.

## Backend: Namen

- **Use Cases** (13.11.2026, 22.11.2026): `<Aggregat><Befehl der Entity>` – das Präfix ist das Aggregat, dessen
  Zustand sich ändert, bei mehreren das, um das es geht (`WorkshopUsageStart` startet auch eine Sitzung).
- **Service-Befehle** (15.11.2026, 22.11.2026): der Befehl der Entity; wo die Entity das Aggregat weglässt, steht es
  nach dem Verb (`pause()` → `pauseToolCertification`); nennt sie etwas anderes, bleibt der Name (`assignRfidReader`);
  anlegen und löschen: Verb + Aggregat.
- **Open und Running** (22.11.2026): „open“ nur für die Sitzung (Status `OPEN`), „running“ für Werkstattbesuch und
  Werkzeugnutzung – wie die Indizes `uq_…_running` und „läuft“ im Portal; nie „active“. Ausnahme Übersicht
  (23.11.2026): `/dashboard/active-users`, `/dashboard/active-tools`, `Active…View` / `…ApiDto` und `getActive…`
  bleiben – sie benennen, was die Übersicht zeigt; im Portal bleibt alles.
- **Gespeicherte Namen bleiben** (13.11.2026): Event-Typen stehen in der Outbox-Tabelle und werden nicht umbenannt,
  `WorkshopUsage` bleibt (Glossar). `WorkshopUsage.exit()` heißt `stop()` (22.11.2026); die Felder `entryTime` /
  `exitTime` bleiben.
- **Id im Command** (15.11.2026): die Id des eigenen Aggregats heißt `id`, Referenzen `<aggregat>Id`.

## Backend: Fehler und Regeln

- **Fehlercode statt Text** (13.11.2026): das Backend schickt einen stabilen `code`, das Portal übersetzt ihn – die
  Texte bleiben dort, wo die Sprachen sind; `detail` ist englisch und für das Log.
- **Drei Arten von Fehlern** (13.11.2026): gibt es nicht `404`, Regel verletzt `409`, Eingabe ungültig `400`; was nicht
  sein kann, ist eine `IllegalStateException` (`500`), keine eigene Klasse.
- **Zustandsregel über den Zustand** (22.11.2026): wird ein Aggregat über seinen Zustand gefunden („die offene Sitzung
  der Person“, „die wartende Anfrage“), wirft der Service oder Use Case die Exception der Regel; der Befehl der Entity
  behält eine Absicherung mit `IllegalStateException`.
- **Zwei gleichzeitige Anfragen** (22.11.2026): partielle Unique-Indizes für WU-1, WS-1 und TU-9; die zweite Anfrage
  scheitert mit `500` bzw. einem allgemeinen Fehler am Gerät – sollte kaum vorkommen.
- **Namen der Lesegeräte ohne Unique-Index** (24.11.2026): R-6 prüft nur `RfidReaderService.updateRfidReader()`, die
  Datenbank nicht – anders als `tool_name_unique` bei den Werkzeugen (T-2). Zwei Personen, die im selben Augenblick
  denselben Namen speichern, kämen beide durch – hingenommen: kein Gerät setzt einen Namen, und der Deploy hängt nicht
  an Dubletten, die auf dem Server niemand geprüft hat.
- **Formatregeln an den Commands** (13.11.2026): Bean Validation an den Commands, sie gilt für Portal und Geräte
  gleich; Regeln mit Zustand oder zweitem Aggregat im Service oder Use Case.
- **Einweisen: alle oder keiner** (13.11.2026): `ToolCertificationCreate` weist alle Personen ein oder niemanden.
- **Löschen über die Entity** (22.11.2026): die Entity laden (sonst `404`), dann `delete(entity)` und das Event – nie
  `deleteById` oder ein rohes `existsById`.
- **Löschen eines Benutzers** (11.11.2026): eine offene Sitzung sperrt; ein zugewiesener Tag wird frei, eine wartende
  Anfrage abgebrochen; die eigenen Einweisungen und die Rolle als Einweiser werden gelöscht; bezahlte Sitzungen,
  Nutzungen und die Tag-Historie bleiben (customer-README C-4, C-5, C-9, C-11). Die Tabellen sind bewusst nicht per
  Fremdschlüssel verknüpft (C-11).
- **Einweisungen eines gelöschten Einweisers** (24.11.2026): die Einweisungen, die die Person anderen gegeben hat,
  bleiben gültig; `certifiedBy` behält ihre Id und wird nicht geleert – kein Schreiben in fremde Einweisungen, kein
  zusätzliches Event; das Portal zeigt als Einweiser „–“ (C-13, TC-9).
- **Kaputte Daten am Gerät** (22.11.2026): eine Zuweisung, deren Person es nicht gibt, ist eine
  `IllegalStateException` – „Unerwarteter Fehler“ am Gerät, ein Fehler im Log.

## Backend: Typen und Zeit

- **Value Objects** (15.11.2026): als Feld in Commands und Views immer `@NotNull @Valid`; `random()` nur für
  UUID-Ids – Werte von Geräten (MAC, Tag-ID, IP) erfindet das System nie.
- **API-Antworten ohne Value Objects** (22.11.2026): Ids und Werte als `String`, Enums als Enum, ein Datum als
  `LocalDate` – das JSON bleibt gleich.
- **Event-Payloads** (22.11.2026): Enums als Enum, Werte als Value Object, Records aus `shared/` erlaubt – nie eine
  Entity oder ein `@Embeddable`.
- **Zeit der Werkstatt** (23.11.2026): gespeichert wird `Instant`; „heute“ und die Uhrzeit am Gerät kommen aus der
  festen Zone `Europe/Berlin` (`WorkshopTime`), nie aus der Zeitzone der JVM – unabhängig vom Server, `TZ` in Image
  oder Compose ist nicht nötig.

## Backend: REST und Geräte

- **Query-Parameter in camelCase** (13.11.2026): `?customerId=`, `?rfidTagId=`.
- **Status beim Anlegen** (22.11.2026): `201` mit `Location` auf `GET /{id}` der neuen Ressource; gibt es kein solches
  `GET`, `204`.
- **Ein `PATCH`** (23.11.2026): `PATCH /rfid-readers/{id}/type` bleibt.
- **Geräte-API** (13.11.2026): der Vertrag mit der Firmware bleibt (Feldnamen, deutsche Texte, immer `200`), die
  Logik liegt in Use Cases; im Einzelfall am Endpunkt entscheiden.
- **Schaltbox ohne Werkzeug (R-8)** (22.11.2026): entscheidet vorerst der `DeviceController`; neu entscheiden, wenn die
  Geräte-Schnittstellen umgebaut werden (Vorschlag: `ToolUsageToggle(customerId, switchBoxId)` mit
  `NoToolAssignedToSwitchBoxException`).
- **Bruno** (22.11.2026): jeder Endpunkt hat einen Request in `core-pos/api/`, ein Ordner pro Controller.
- **Abbrechen einer Tag-Anfrage** (25.11.2026): `PUT /rfid-tag-assignments/{customerId}/cancel` ohne Body – die Person
  im Pfad nach dem Skill `rest-api` („an action without values“), wie `PUT /workshop-sessions/{customerId}/close`; die
  Theke kennt die Person, nicht die Anfrage. `PUT /rfid-tag-assignments/unassign` mit der Person im Body weicht davon ab
  und bleibt vorerst, bis es in einer eigenen Aufgabe angeglichen wird (Backend, Portal und Bruno zusammen).

## Backend: Tests

- **Testebenen** (15.11.2026): jede Regel auf der niedrigsten Ebene, die sie vollständig beweist – Value Object und
  Entity ohne Mocks, Service und Use Case mit Mockito, Controller mit Standalone-MockMvc, Integration, ArchUnit.
- **Integrationstests** (15.11.2026, 16.11.2026): eine PostgreSQL in Testcontainers für den ganzen Lauf in
  `mvn test`; `SchaffbarFixture` mit `given` / `api()` / `then`, Zufallswerte statt Aufräumen; eine Methode pro Regel
  mit ihrer Nummer (`r1_…`).
- **Formatregeln testen** (22.11.2026): eigene Logik eines Constraints im Test des Validators, pro Constraint und
  Endpunkt ein Controller-Test `…_is400`, am Command im Service keiner.
- **Datum im Controller-Test** (22.11.2026): der Standalone-MockMvc bekommt Boots Jackson-Konfiguration, Datumsfelder
  werden gegen ihren Wert geprüft – der Controller-Test beweist das Format, das das Portal liest.

## Portal: allgemein

- **Prototyp** (23.10.2026): <https://claude.ai/artifact/MUj2FTVMMR2MEUWdbJ5vEM> – Maße, Farben und Texte daraus
  übernehmen, den Code (`.dc.html`, Inline-Styles) nie; Aufbau und Benutzung im Skill `design`.
- **Was das Backend nicht liefert, fehlt** (07.11.2026): Phase 1 des Redesigns stellte die vorhandenen Seiten auf das
  neue Design um, ohne Backend und ohne neue Funktionen; eine Anzeige ohne Daten wird weggelassen, nicht vorgetäuscht.
- **API-Code später generieren** (03.11.2026): OpenAPI aus dem Backend, daraus Schemas und Services – bis dahin von
  Hand (Skill `api`).
- **Fehlertexte** (15.11.2026): der Text eines Fehlercodes erklärt, was die Aktion sperrt und was zu tun ist; `detail`
  wird nie gezeigt.
- **Gefahrenzone auf jeder Detailseite** (10.11.2026): letzte Karte des ersten Tabs über die volle Breite, auch beim
  Werkzeug (im Prototyp fehlt sie dort); ist Löschen gesperrt, erklärt sie es, und das ⋯-Menü blendet den Eintrag aus.

## Portal: RFID-Lesegeräte

- **Typ ohne Bestätigung** (08.11.2026): der Typ wird ohne den Dialog „Typ ändern“ aus dem Prototyp gespeichert;
  gesperrt bleibt er bei einer Schaltbox mit Werkzeug.
- **Geräte ohne Typ** (08.11.2026): eigener Abschnitt „Neue Geräte – Typ festlegen“ (nicht im Prototyp); der Filter
  „Nicht zugeordnet“ zählt sie mit, nicht nur Schaltboxen ohne Werkzeug.
- **Name optional** (24.11.2026): im Dialog „Lesegerät bearbeiten“ darf der Name leer bleiben – anders als im Prototyp
  („Bitte einen Namen eingeben“); ein leeres Feld schickt keinen Namen. Neue Lesegeräte melden sich ohne Namen an
  (R-9), und R-6 vergleicht nur Lesegeräte mit Namen; ein Pflichtname wäre eine eigene Regel in Backend und Portal.
- **Namensvergleich ohne Trimmen** (24.11.2026): der Fehler „Ein anderes Gerät heißt bereits so“ am Namen vergleicht
  wie das Backend (R-6) ohne Groß-/Kleinschreibung, Leerzeichen am Rand zählen mit – „Theke “ ist ein anderer Name als
  „Theke“, ein Name nur aus Leerzeichen wird verglichen. Der Prototyp trimmt; gleich wie bei den Werkzeugnamen (T-15).

## Portal: RFID-Tags

- **„Wartet auf Tag“ als Banner** (08.11.2026): über der Liste statt als Karte in der Seitenleiste – eine offene
  Aufgabe an der Theke; ohne Anzahl, es wartet höchstens eine Person (TA-3).

## Portal: Werkzeuge

- **Jede Person kann Einweiser werden** (09.11.2026): der Filter „Nur Personen mit aktiver Einweisung“ im Dialog
  „Einweiser hinzufügen“ ist nur eine Hilfe.
- **„Einweiser hinzufügen“ ohne gesperrte Zeilen** (08.11.2026): es erscheinen nur Personen, die noch keine Einweiser
  sind – nicht „Bereits Einweiser“ oder „Erst reaktivieren“ wie im Prototyp.
- **Hardware einrichten** (08.11.2026): „· N nicht eingerichtet“ im Untertitel und ein Filter dafür (nicht im
  Prototyp) – kein Werkzeug bleibt absichtlich ohne Schaltbox und Relais; „Anlegen & Hardware einrichten“ entfällt
  vorerst.
- **Beschreibung höchstens 255 Zeichen** (08.11.2026): `tool.description` ist `VARCHAR(255)`, der Prototyp nennt 500.
- **Tab Hardware** (09.11.2026): zur Auswahl stehen nur freie Schaltboxen und die eigene; „Relais entfernen …“ mit
  Bestätigung statt einer Option „--“ im Typ.

## Portal: Benutzer

- **Filter „Mit Leihtag“** (09.11.2026): statt „Unter 18“ und „Ohne RFID-Tag“ aus dem Prototyp – zeigt am Ende des
  Tages, wer seinen Leihtag noch nicht abgegeben hat.
- **Keine Sortierauswahl** (09.11.2026): sortiert nach Nachname; nach einem Wert zu sortieren, den die Liste nicht
  zeigt, verwirrt – erst mit „Zuletzt da“.
- **Dialog „Neuen Benutzer hinzufügen“** (09.11.2026): ohne Überschrift „Person“, ohne Altersgruppe (die Grenzen kennt
  nur das Backend), ohne „Anlegen & RFID-Tag zuweisen“; der Hinweis auf einen Doppeleintrag kommt aus der geladenen
  Liste.
- **Aktionen nur auf der Detailseite** (10.11.2026): „Bearbeiten“, „RFID-Tag zuweisen …“ und „Sitzung beenden …“ nicht
  im ⋯-Menü der Liste.
- **Banner der offenen Sitzung** (09.11.2026, 10.11.2026): „In der Werkstatt seit …“ ab dem ersten Betreten;
  „Sitzung beenden & abrechnen“ ist gesperrt, solange die Person eingecheckt ist oder ein Werkzeug läuft (WS-5), danach
  frei; ohne das laufende Werkzeug aus dem Prototyp.
- **Zeiten im Dialog „Sitzung beenden & abrechnen“** (26.11.2026): jede Zeile zeigt den Beginn mit Datum, auch wenn er
  von heute ist. Liegen alle Zeiten der Tabelle (Werkstatt und Werkzeuge) auf einem Tag, steht das Ende nur als Uhrzeit
  (`10.11.26, 17:50 – 23:08`); liegt eine an einem anderen Tag, steht in allen Zeilen auch das Ende mit Datum
  (`10.11.26, 18:03 – 11.11.26, 23:56`). Das Ende einer laufenden Nutzung („jetzt“) zählt nicht mit. Statt
  `timeWithDay`: dort fehlte bei einem Ende von heute das Datum, und ein Besuch über mehrere Tage sah aus wie einer von
  wenigen Stunden.
- **Tab „Offene Sitzung“** (11.11.2026): eine Zeitleiste pro Werkstattbesuch, darunter die Werkzeuge nach Startzeit
  statt nach Werkzeug gruppiert.
- **„RFID-Tag zuweisen“ wartet** (09.11.2026): die Seite fragt die wartende Zuweisung alle 2 Sekunden ab, der Dialog
  springt selbst auf „Tag zugewiesen“; geschlossen bleibt die Anfrage bestehen.
- **„Zuweisung abbrechen“** (25.11.2026): im Dialog „RFID-Tag zuweisen“ (Schritt „Tag auflegen“) und auf der Karte
  „RFID-Tag“ im Zustand „Wartet auf Tag“, beide Male umrandet und ohne Rückfrage – verloren geht nichts, was zwei
  Klicks nicht wieder anfragen; der Dialog bleibt offen und geht zurück auf „Art wählen“. Der Button auf der Karte ist
  nicht im Prototyp (dort hat die Karte keinen Zustand „Wartet auf Tag“). Im Schritt „Tag auflegen“ stehen „Zuweisung
  abbrechen“ und „Schließen“ nebeneinander – der Prototyp hat nur „Zuweisung abbrechen“ und schließt über das X; zwei
  Ausgänge mit verschiedener Folge (Anfrage weg / Anfrage bleibt) sind so beide benannt. Scheitert der Abbruch (der Tag
  wurde gerade zugewiesen), lädt die Seite die Zuweisung sofort neu und zeigt den aktuellen Stand.
- **Bearbeiten in einem Dialog** (10.11.2026): Person, Kontakt und Adresse in einem Formular; gespeichert werden nur
  die geänderten Teile – bis zu drei Aufrufe, nicht atomar, bis das Backend einen Endpunkt dafür hat.

## Portal: Übersicht

- **Aktualisieren** (11.11.2026): die Seite lädt sich jede Minute selbst neu (ohne Fortschrittsbalken) und per Button.
- **„Braucht Aufmerksamkeit“** (11.11.2026): nur, wenn es etwas gibt – Lesegeräte ohne Typ, Werkzeuge ohne Schaltbox
  oder WLAN-Relais; ein Eintrag mit Link direkt dorthin, mehrere mit den ersten Namen und Link zur Liste. Nicht
  „eingecheckt ohne Tag“.
- **Zwei Kennzahlen** (11.11.2026): „In der Werkstatt“ und „Werkzeuge in Benutzung“; „Check-ins heute“ und
  „Lesegeräte online“ erst, wenn das Backend sie liefert.
- **Zwei Listen je halb so breit** (11.11.2026): im Prototyp 60/40.
- **Zeiten mit Datum** (11.11.2026): eine Zeit der Sitzung, die nicht von heute ist, zeigt das Datum (`timeWithDay`);
  wer seit einem früheren Tag eingecheckt ist, bekommt „Nicht ausgecheckt“.
- **Weggelassen** (11.11.2026): die globale Suche und „Benutzer hinzufügen“ im Kopf.

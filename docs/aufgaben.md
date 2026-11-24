# Aufgaben

Was offen ist, in der Reihenfolge, in der es angegangen wird. Eine erledigte Aufgabe wird hier gelöscht: die
Geschichte steht in git, was als Regel bleibt, in den Skills und den `README.md` der Aggregate, eine Entscheidung, die
dabei fällt, in [`entscheidungen.md`](entscheidungen.md). Lose Ideen des Maintainers: [`TODO.md`](../TODO.md).

In einer neuen Session auf diese Datei verweisen („mach mit der Agenten-Pipeline weiter, siehe `docs/aufgaben.md`“).
Wie gearbeitet wird (nie committen, Prüfungen, Browser ohne Schreibzugriffe): die `CLAUDE.md` im Repo-Root, in
`schaffbar-backend/` und in `schaffbar-portal/`.

## Reihenfolge

1. [Nach dem nächsten Deploy prüfen](#nach-dem-nächsten-deploy-prüfen) – sobald Backend und Portal auf dem Server
   laufen.
2. [Agenten-Pipeline](#agenten-pipeline): der Pilot.
3. [Regeln, die nur das Portal prüft](#regeln-die-nur-das-portal-prüft) – das Format des Socket-Namens (R-4).
4. [Portal ohne Backend](#portal-ohne-backend).
5. [Backend-Funktionen für das Portal](#backend-funktionen-für-das-portal) – vorher das
   [fachlich zu Klärende](#fachlich-zu-klären).
6. Der Rest der [Agenten-Pipeline](#agenten-pipeline): Branch, Rechte, CI, GitHub.

## Nach dem nächsten Deploy prüfen

Backend und Portal werden zusammen deployt (`deployment.md`); ein Agent deployt nicht. Seit dem Backend-Review
(22.–23.11.2026) haben sich die feste Zeitzone der Werkstatt, die Antworten an die Geräte und einige Fehlercodes und
Pfade geändert. Auf dem Server prüfen:

- [ ] Firmware: `/device/init` antwortet einem Lesegerät ohne Typ jetzt „Lesegerät ohne Typ“ statt „Nicht Gefunden“
  (R-9). Vergleicht die Firmware den Text?
- [ ] Uhrzeit am Display (`/init`, Ein- und Austrittszeit): sie kommt jetzt aus der festen Zone `Europe/Berlin`,
  nicht mehr aus der Zeitzone des Servers.
- [ ] Fehlermeldungen gegen das echte Backend: eine Aktion, die das Backend ablehnt, zeigt im Portal den übersetzten
  Text des Codes, nicht die allgemeine Meldung „Speichern fehlgeschlagen“.

## Agenten-Pipeline

Ziel: Für ein Issue, das Piotr annimmt (z. B. per Label), läuft der Weg von der Anforderung bis zum Pull Request mit
Agenten. Skills sind das Wissen („wie macht man X hier“), Agenten die Rollen mit Auftrag, Werkzeugen und Rechten –
mehrere Rollen nutzen dieselben Skills.

Rollen (Subagents in `.claude/agents/` im Repo-Root: `spec`, `implement-backend`, `implement-portal`, `verify`,
`review`), bewusst wenige – jede Übergabe verliert Wissen:

| Rolle | Macht | Lädt | Ergebnis |
| --- | --- | --- | --- |
| Spec (Anforderung + Design) | Issue, Prototyp-Artboard, Backend und `docs/` lesen, Lücken und Widersprüche finden, Fragen stellen statt raten; legt den Vertrag zwischen Backend und Portal fest (Regel-Id, Ort der Regel, Exception und Code, Fehlertext de/en, geänderte API-Form, Verhalten im Portal) | `design`, `feature`, Glossar, für eine Regel `business-rule` | Spezifikation als Datei, mit offenen Fragen und Vorschlägen |
| Implementation Backend (mit Doku) | Code, Tests und README nach den Skills, `mvn -q test`; ändert nichts im Portal | `aggregate`, `use-case`, `rest-api`, `backend-testing`, für eine Regel `business-rule` | Änderungen in `schaffbar-backend/`, Bericht als Datei (was sich am Vertrag geändert hat) |
| Implementation Portal (mit Doku) | API-Schicht, Feature, Übersetzungen, Specs nach den Skills, `npm run check`, Entscheidungen in `entscheidungen.md`; ändert nichts im Backend | `api`, `feature`, `component`, `dialog`, `design`, `testing`, für eine Regel `business-rule` | Änderungen in `schaffbar-portal/` |
| Verify | im Browser abnehmen, Zustände simulieren | `verify-ui` | Bericht mit Screenshots |
| Review | den Diff gegen `CLAUDE.md` und die Skills prüfen – ohne die Begründung des Autors | alle Skills | Befunde |

Ablauf: **Spec → Implementation Backend → Implementation Portal → Verify → Review.** Ein Sub-Agent kann keinen
Sub-Agenten starten – die Hauptsitzung (später die Action) ruft die Rollen nacheinander auf. Das Portal kommt nach dem
Backend, weil es dessen Code und API-Form übernimmt; beide arbeiten gegen den Vertrag aus der Spec, das Portal liest
dazu den Bericht des Backends.

Übergabe: eine Datei pro Aufgabe, `docs/specs/<kurzname>.md` nach der Vorlage
[`docs/specs/vorlage.md`](specs/vorlage.md). Die Spec legt sie an, jede Rolle schreibt ihren eigenen Abschnitt (Spec,
Bericht Backend, Bericht Portal, Abnahme im Browser, Review); sie liegt auf dem Branch der Aufgabe und wird nach dem
Merge gelöscht.

Haltepunkte für Piotr: nach der Spec (Fragen beantworten, freigeben) und vor dem Merge. Die wertvollsten Momente im
Redesign waren Entscheidungen (Banner trotz WS-5, Leihtag-Filter, Löschkonzept) – die darf kein Agent allein treffen.
Übergaben nur über Dateien, Issue und PR, nicht über den Chat-Verlauf.

Für jede Rolle:

- Sie läuft im Repo-Root. Aus `schaffbar-portal/` heraus werden die Skills des Root und des Portals angeboten, die
  des Backends (`schaffbar-backend/.claude/skills/`) nicht – eine Rolle liest jeden Skill über seinen Pfad ab dem
  Repo-Root (Tabelle im Skill `business-rule`).
- Grenzen: kein Commit, kein Push; das laufende Backend nicht starten, neu starten oder migrieren, nichts in seine
  Datenbank schreiben (lesende `GET` erlaubt); der Browser nur nach Skill `verify-ui`; offene Punkte als Frage, nie
  geraten.

Stufen:

1. [ ] Pilot: lokal an R-6 (Namen der Lesegeräte eindeutig) – betrifft Backend (`RfidReaderUpdate`, Exception, Tests)
   und Portal (Übersetzung, Prüfung im Dialog „Lesegerät bearbeiten“), prüft also die Übergabe. Dabei klären: ob die
   Rollen gefunden werden, wenn die Hauptsitzung in `schaffbar-portal/` läuft, und wie das Werkzeug `Artifact` der
   Rolle Spec ohne claude.ai (später in der GitHub Action) behandelt wird.
2. [ ] Git-Regel für die Pipeline: ein Agent arbeitet auf seinem eigenen Branch und öffnet einen PR, `main` bleibt bei
   Piotr (Ausnahme zu „nie committen“ in `CLAUDE.md`, nur für die Pipeline).
3. [ ] Rechte: Prüfbefehle ohne Rückfrage in `.claude/settings.json`, Push und Merge nicht.
4. [ ] CI mit Backend und Testdaten (Docker Compose mit Seed) – Verify braucht ein laufendes Backend; lokal läuft es
   gegen die echte Datenbank mit abgefangenen Schreibzugriffen, in CI geht das nicht.
5. [ ] GitHub: Claude Code Action, ausgelöst durch ein Label am Issue; Ergebnisse als Kommentare und PR.

## Regeln, die nur das Portal prüft

Aus den Abschnitten „Open Rules“ der `README.md` der Aggregate. Jede Regel mit dem Skill `business-rule`, vom Vertrag
bis zur Erklärung im Portal; danach steht sie unter „Business Rules / Invariants“ und fällt hier weg.

| Regel | Was | Hinweis |
| --- | --- | --- |
| R-4 | Socket-Name nur `a–z`, `0–9`, `-` | nur bei einer Änderung prüfen – bestehende Namen wie „Socket 43“ bleiben gültig (wie im Portal) |

## Portal ohne Backend

| Aufgabe | Notiz |
| --- | --- |
| Erfolgsmeldungen | Snackbar nach dem Speichern („Bandsäge gespeichert“); Fehler zeigt der Interceptor schon |
| Bestätigungsdialoge | Titel als Frage, Folgen erklären, beim Löschen Häkchen „nicht rückgängig“ oder den Namen eintippen (Prototyp `UserDeleteDialog`), ein gesperrter Zustand bietet den nächsten Schritt an |
| Dunkel-Modus | Tokens sind mit `light-dark()` vorbereitet, der Umschalter fehlt; alle Seiten prüfen |
| Tab-Leiste unten auf dem Handy | statt des Menü-Buttons, Prototyp „Übersicht – Mobil“ |
| Sprachumschalter DE/EN | in der Seitenleiste |
| Seiten aktualisieren sich selbst | Listen und Detailseiten wie die Übersicht per Polling statt Neu-laden-Button (Skill `feature`); sofort erst mit SSE/WebSocket (Backend) |
| Live-Dauer im Banner | „In der Werkstatt seit 14:05 · 1 h 23 min“ – die Startzeit ist da |
| Tag manuell erfassen | Dialog „ID manuell eingeben“ auf der Seite RFID-Tags; `POST /rfid-tags` gibt es; vorher das Format der Tag-ID klären ([fachlich zu klären](#fachlich-zu-klären)) |
| Werkzeug vom Lesegerät aus zuordnen | „Werkzeug zuordnen …“ in der Liste der Lesegeräte und am Status „Nicht zugeordnet“; `PUT /tools/{toolId}/rfid-reader/{rfidReaderId}` gibt es |
| ⋯-Menü der Werkzeugliste | „Bearbeiten …“ und „Einweisungen eintragen …“ – die Dialoge gibt es auf der Detailseite |
| Personenauswahl zusammenführen | „Einweisungen eintragen“ und „Einweiser hinzufügen“ haben je eine eigene Suche mit Häkchen-Liste – eine gemeinsame dumb component mit ViewModel; wer wählbar ist, entscheidet weiter der Store. Ob nicht wählbare Personen ausgegraut erscheinen (Prototyp) oder fehlen (heute, siehe Entscheidung „Einweiser hinzufügen“), vorher klären |
| Weitere Filter | z. B. „Unter 18“, „Ohne RFID-Tag“ in der Benutzerliste – nach Bedarf |
| Vorab-Prüfungen überprüfen | seit das Backend jede Regel mit Code ablehnt, kann eine reine Vorab-Prüfung gehen, die nur seine Antwort vorwegnimmt (`getToolsByRfidReader` vor dem Typwechsel, `canDeleteUser` in der Benutzerliste); sie bleibt, wo sie den Button schon vorher erklärt |
| Skill `forms` | wenn mehr Formulare kommen: Signal Forms, kompakte Felder, Zähler „N ungespeicherte Änderungen“, „Bitte N Felder prüfen“ – teils schon im Skill `dialog` |

## Backend-Funktionen für das Portal

Was dem Portal fehlt, weil das Backend die Daten oder Aktionen noch nicht hat – je Funktion, was das Portal zeigen
oder tun soll und was das Backend dafür braucht. Ein Punkt mit Regel-Id steht als „Planned“ im `README.md` seines
Aggregats. Reihenfolge:

1. Online-Status der Lesegeräte (Heartbeat) – davon hängen Übersicht, Lesegeräte und Werkzeug-Hardware ab.
2. Tag-Anfrage abbrechen und wartende Anfragen aufräumen – eine vergessene Anfrage blockiert alle anderen (TA-3).
3. Der Rest nach Bedarf.

### Übersicht

| Funktion | Beschreibung | Backend |
| --- | --- | --- |
| Check-ins heute | Kennzahl „Check-ins heute“ mit „N bereits ausgecheckt“ | Anzahl der Werkstatt-Eintritte des Tages und davon ausgecheckt (eigener Endpunkt oder Kennzahlen-Endpunkt) |
| Lesegeräte online | Kennzahl „Lesegeräte online N / gesamt“, Hinweis „Lesegerät offline“ unter „Braucht Aufmerksamkeit“ | Online-Status der Lesegeräte (siehe RFID-Lesegeräte) |
| Werkzeug gestört | Status „Offline“ im Werkzeugstatus, Hinweis „WLAN-Relais antwortet nicht“ | Online-Status des WLAN-Relais (siehe Werkzeuge) |
| Dauer der Werkzeugnutzung | „Lena Hoffmann · 57 min“ im Werkzeugstatus | `GET /dashboard/active-tools` mit der Startzeit der laufenden Nutzung |
| Sofort aktualisieren | heute lädt die Übersicht jede Minute neu; später sofort, wenn jemand ein- oder auscheckt | Server-Sent Events oder WebSocket für Ein- und Auschecken und Werkzeug an/aus |
| Statusbox „Werkstatt geöffnet“ | in der Seitenleiste: geöffnet, Personen anwesend, Werkzeuge aktiv, nicht eingerichtete Lesegeräte | ein Öffnungszustand der Werkstatt (gibt es ihn fachlich?), sonst aus vorhandenen Daten |

### Benutzer

| Funktion | Beschreibung | Backend |
| --- | --- | --- |
| Suche und Seiten | Suche über Name, E-Mail und Tag-ID, Filter-Zählungen, Seiten – heute lädt das Portal alle Benutzer | `GET /customers` mit Suche, Filtern, Zählungen und Paginierung |
| „Zuletzt da“ und Anzahl Einweisungen | Spalten der Benutzerliste, Sortierung nach „Zuletzt da“ | Felder in `GET /customers` (letzte Sitzung, Anzahl aktiver Einweisungen) |
| Doppeleintrag erkennen | Hinweis „Es gibt bereits …“ beim Anlegen – heute nur aus der geladenen Liste | Endpunkt, der ähnliche Benutzer findet (Name + Geburtsdatum, E-Mail) |
| Bearbeiten in einem Schritt | der Dialog speichert Person, Kontakt und Adresse – heute bis zu drei Aufrufe nacheinander, nicht atomar | ein Endpunkt für alle drei Teile |
| Löschen vorab ausblenden | die Liste blendet „Benutzer löschen“ bei offener Sitzung aus, statt erst beim Klick zu prüfen | offene Sitzungen aller Benutzer in einem Aufruf (oder ein Feld in `GET /customers`) |

### Sitzung und Abrechnung

| Funktion | Beschreibung | Backend |
| --- | --- | --- |
| Betrag | im Dialog „Sitzung beenden & abrechnen“ und in der Zusammenfassung | Preise (Werkstatt-Einheit, Werkzeug-Einheit, Mitgliedsrabatt?) und Berechnung beim Abschluss, Betrag in der Sitzung speichern |
| Zahlungsart | Bar / EC-Karte / Rechnung im Abschluss-Dialog | Feld der Sitzung, Parameter von `PUT /workshop-sessions/{customerId}/close` – fachlich klären |
| Laufende Werkzeuge beim Abschluss ausschalten | „Drechselbank ist noch eingeschaltet und wird beim Abschluss ausgeschaltet“ – heute sperrt das Backend den Abschluss (WS-5) | der Abschluss schaltet laufende Werkzeuge per Relais aus und beendet die Nutzungen – fachlich klären |
| Bestätigung „Sitzung abgeschlossen“ | zweiter Schritt im Dialog mit dem Ergebnis | Antwort von `close` mit den abgerechneten Summen (heute ohne Body) |

### RFID-Tag-Zuweisung

| Funktion | Beschreibung | Backend |
| --- | --- | --- |
| Anfrage abbrechen | „Zuweisung abbrechen“ im Dialog „RFID-Tag zuweisen“ und auf der Karte „Wartet auf Tag“ – wichtig, weil nur eine Anfrage warten darf (TA-3) | nur der Endpunkt: abbrechen kann der Service schon (TA-10) |
| Wartende Anfragen aufräumen | eine vergessene Anfrage blockiert nicht ewig; Anzeige „noch N min“ | ein Scheduler, der alte Anfragen nach einer Frist abbricht |
| Tag ersetzen | „Tag ersetzen …“ in einem Schritt – heute erst entfernen, dann neu zuweisen | Endpunkt, der die Zuweisung aufhebt und eine neue Anfrage anlegt |
| Beim Entfernen deaktivieren | Auswahl „wieder freigeben“ oder „deaktivieren (verloren)“ | TA-9: Option an `PUT /rfid-tag-assignments/unassign`, braucht RT-4 |
| Zuweisung vom Tag aus | auf der Seite RFID-Tags einem freien Tag eine Person zuweisen, ohne das Lesegerät an der Theke | Endpunkt Tag → Person |

### Einweisungen

| Funktion | Beschreibung | Backend |
| --- | --- | --- |
| Datum der Einweisung | beim Eintragen ein früheres Datum wählen (nachgetragene Einweisungen) | TC-6: Feld in `POST /tool-certifications/batch` |
| Grund | beim Pausieren (optional) und Entziehen (Pflicht), Anzeige in der Liste | TC-7: Feld der Einweisung, Parameter von `pause` / `revoke` |

### Werkzeuge

| Funktion | Beschreibung | Backend |
| --- | --- | --- |
| Online-Status des WLAN-Relais | „Online“ / „Antwortet nicht“ auf der Karte Hardware und im Werkzeugstatus | Heartbeat oder regelmäßige Abfrage des Relais, Status am Werkzeug |
| Verbindung testen | Button „Verbindung testen“ im Tab Hardware | T-19: Endpunkt, der das Relais anfragt und das Ergebnis liefert |
| Relais vorher ausschalten | Option beim Entfernen des WLAN-Relais | T-18: Option an `DELETE /tools/{toolId}/wlan-relais`, schaltet erst aus |
| Kennzahlen in der Liste | Anzahl aktiver Einweisungen und aktuelle Nutzung pro Werkzeug ohne Zusatzaufrufe | Felder in `GET /tools` |

### RFID-Tags

| Funktion | Beschreibung | Backend |
| --- | --- | --- |
| Deaktivieren / Reaktivieren | verlorene Tags sperren, mit Grund | RT-4: Endpunkte und Feld „Grund“; `RfidTag.active` gibt es schon, geprüft wird es nirgends |
| Leihtag-Nummer | Beschriftung des physischen Tags („T-04“) neben der ID | Feld des Tags |
| Rückgabe bestätigen | Status „Rückgabe ausstehend“ und Leihtag an der Theke als zurückgegeben markieren (Prototyp) | einen solchen Zustand gibt es nicht (`RfidTagAssignmentStatus` kennt nur `WAITING_FOR_ASSIGNMENT` und `ASSIGNED`) – erst klären, ob er nötig ist |
| Liste mit Zustand | Zustand, Person, Art und Datum in einem Aufruf – heute setzt das Portal Tags, Zuweisungen und Benutzer zusammen | Felder in `GET /rfid-tags` |
| Zuletzt gelesen | „Zuletzt gelesen am Tor Eingang um 14:05“ | Lese-Protokoll (siehe RFID-Lesegeräte) |

### RFID-Lesegeräte

| Funktion | Beschreibung | Backend |
| --- | --- | --- |
| Online-Status | „Online“ / „Offline seit 12:40“, „zuletzt gesehen“, „verbunden seit“ in Liste und Detailseite, Offline-Banner mit Prüfschritten | Heartbeat der Lesegeräte, Felder am Lesegerät |
| Lese-Protokoll | letzte Lesevorgänge, auch abgelehnte Tags (z. B. ohne Einweisung) | Protokoll der Lesevorgänge mit Ergebnis, Endpunkt pro Lesegerät |
| Verbindung erneut prüfen | Button auf der Detailseite | Endpunkt, der das Lesegerät anfragt |
| Personen manuell auschecken | im Offline-Banner eines Tors am Ausgang (Prototyp) | Endpunkt für `WorkshopUsageStop` – heute beendet nur das Gerät einen Werkstattbesuch |

### Querschnitt

| Funktion | Beschreibung | Backend |
| --- | --- | --- |
| Sofort aktualisieren überall | Listen und Detailseiten aktualisieren sich bei Änderungen | SSE oder WebSocket (siehe Übersicht) |
| API-Code generieren | die handgeschriebenen `*.api-model.ts` und API-Services werden aus dem Backend erzeugt (orval oder hey-api) | OpenAPI aus dem Backend (springdoc) |

## Fachlich zu klären

Fragen an die Werkstatt, nicht an den Code – vor den Funktionen, die davon abhängen:

- [ ] Zahlungsart beim Abschluss: gewollt? Welche Arten (Bar, EC-Karte, Rechnung)?
- [ ] Preise und Rabatte: Werkstatt-Einheit, Werkzeug-Einheit, Rabatt für Vereinsmitglieder?
- [ ] Schaltet „Sitzung beenden & abrechnen“ laufende Werkzeuge aus? Heute sperrt das Backend den Abschluss (WS-5).
- [ ] Gibt es einen Zustand „Werkstatt geöffnet“ oder Öffnungszeiten?
- [ ] Welche Kennzahlen sind an der Theke nützlich? Die vier Kacheln des Prototyps sind ein Vorschlag.
- [ ] Altersgruppen: unter 16 / unter 18 / Erwachsen mit Stichtag heute – richtig?
- [ ] „Rückgabe ausstehend“ (Prototyp): gibt es den Fall – ein Leihtag, der nach dem Auschecken nicht zurück ist? Nur,
  wenn das Tor am Ausgang offline war?
- [ ] Tag-IDs: immer 4 oder 7 Byte (8 bzw. 14 Hex-Zeichen)? – für „Tag manuell erfassen“.
- [ ] Gibt es einen Leihtag-Kasten mit nummerierten Tags? – für die Leihtag-Nummer.
- [ ] Was ist der Socket-Name genau, muss er auf dem Gerät gleich sein? Muss er eindeutig sein (im Redesign geplant,
  steht in keiner README)? – für R-4.

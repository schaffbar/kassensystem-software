# Backend-Funktionen für das Portal

Was dem Portal nach Phase 1 des Redesigns fehlt, weil das Backend die Daten oder Aktionen noch nicht hat – nach Feature
gruppiert, je mit Beschreibung (was das Portal zeigen oder tun soll) und dem, was im Backend nötig ist.

Nicht hier: Regeln, die das Portal schon prüft und das Backend erzwingen soll (Abschnitte „Open Rules“ in den
`README.md` der Domain-Aggregate), die Problems API (`TODO.md`). Das Löschkonzept für Benutzer ist umgesetzt (customer-README C-4, C-5, C-9, C-11). Herkunft und
Entscheidungen: `ui-redesign.md`.

## Übersicht

| Funktion | Beschreibung | Backend |
| --- | --- | --- |
| Check-ins heute | Kennzahl „Check-ins heute“ mit „N bereits ausgecheckt“ | Neuer Endpunkt (oder Feld in einem Kennzahlen-Endpunkt): Anzahl Werkstatt-Eintritte des Tages und davon ausgecheckt |
| Lesegeräte online | Kennzahl „Lesegeräte online N / gesamt“, Hinweis „Lesegerät offline“ im Block „Braucht Aufmerksamkeit“ | Online-Status der Lesegeräte (siehe RFID-Lesegeräte) |
| Werkzeug gestört / offline | Status „Offline“ im Werkzeugstatus, Hinweis „WLAN-Relais antwortet nicht“ | Online-Status des WLAN-Relais (siehe Werkzeuge) |
| Dauer der Werkzeugnutzung | „Lena Hoffmann · 57 min“ im Werkzeugstatus | `GET /tool-dashboard/active-tools` um die Startzeit der laufenden Nutzung erweitern |
| Live-Aktualisierung | Heute lädt die Übersicht jede Minute neu (Polling). Später sofort, wenn jemand ein- oder auscheckt | Server-Sent Events oder WebSocket für Ein-/Auschecken und Werkzeug an/aus |
| Statusbox „Werkstatt geöffnet“ | In der Seitenleiste: geöffnet, Personen anwesend, Werkzeuge aktiv, Anzahl nicht eingerichteter Lesegeräte | Öffnungszustand der Werkstatt (gibt es ihn fachlich?), sonst aus vorhandenen Daten |

## Benutzer

| Funktion | Beschreibung | Backend |
| --- | --- | --- |
| Suche und Paginierung | Suche über Name, E-Mail und Tag-ID, Filter-Zählungen, Seiten – heute lädt das Portal alle Benutzer | `GET /customers` mit Suche, Filtern, Zählungen und Paginierung |
| „Zuletzt da“ und Anzahl Einweisungen | Spalten der Benutzerliste, Sortierung nach „Zuletzt da“ | Felder in `GET /customers` (letzte Sitzung, Anzahl aktiver Einweisungen) |
| Duplikat-Prüfung | Hinweis „Es gibt bereits …“ beim Anlegen – heute nur aus der geladenen Liste | Endpunkt, der ähnliche Benutzer findet (Name + Geburtsdatum, E-Mail) |
| Bearbeiten in einem Schritt | Der Dialog speichert Person, Kontakt und Adresse – heute drei Aufrufe nacheinander, nicht atomar | Ein Endpunkt `PUT /customers/{id}` für alle drei Teile |
| Löschen vorab ausblenden | Die Liste blendet „Benutzer löschen“ bei offener Sitzung aus statt erst beim Klick zu prüfen | Offene Sitzungen aller Benutzer in einem Aufruf (oder Feld in `GET /customers`) |

## Sitzung und Abrechnung

| Funktion | Beschreibung | Backend |
| --- | --- | --- |
| Betrag | Betrag im Dialog „Sitzung beenden & abrechnen“ und in der Zusammenfassung | Preise (Werkstatt-Einheit, Werkzeug-Einheit, Mitgliedsrabatt?) und Berechnung beim Abschluss, Betrag in der Sitzung speichern |
| Zahlungsart | Bar / EC-Karte / Rechnung im Abschluss-Dialog | Neues Feld der Sitzung, Parameter von `PUT /workshop-sessions/{customerId}/close` – fachlich klären |
| Laufende Werkzeuge beim Abschluss ausschalten | „Drechselbank ist noch eingeschaltet und wird beim Abschluss ausgeschaltet“ – heute sperrt das Backend den Abschluss (WS-5) | Abschluss schaltet laufende Werkzeuge per Relais aus und beendet die Nutzungen – fachlich klären |
| Bestätigung „Sitzung abgeschlossen“ | Zweiter Schritt im Dialog mit Ergebnis | Antwort von `close` mit den abgerechneten Summen (heute 200 ohne Body) |
| Live-Dauer im Banner | „In der Werkstatt seit 14:05 · 1 h 23 min“ | Nichts nötig, Startzeit ist da – nur, falls die Dauer serverseitig gerechnet werden soll |

## RFID-Tag-Zuweisung

| Funktion | Beschreibung | Backend |
| --- | --- | --- |
| Anfrage abbrechen | „Zuweisung abbrechen“ im Dialog „RFID-Tag zuweisen“ und auf der Karte „Wartet auf Tag“ – wichtig, weil immer nur eine Anfrage warten darf (TA-3) | Nur noch der Endpunkt: `RfidTagAssignmentService.cancelRfidTagAssignmentRequest()` gibt es seit dem Löschkonzept |
| Wartende Anfragen aufräumen | Eine vergessene Anfrage blockiert die Person nicht ewig; Anzeige „noch N min“ | Scheduler, der alte Anfragen nach einer Frist beendet |
| Tag ersetzen | „Tag ersetzen …“ in einem Schritt – heute erst entfernen, dann neu zuweisen | Endpunkt, der die Zuweisung aufhebt und eine neue Anfrage anlegt |
| Beim Entfernen deaktivieren | Auswahl „wieder freigeben“ oder „deaktivieren (verloren)“ | Option an `PUT /rfid-tag-assignments/unassign` |
| Zuweisung vom Tag aus | Auf der RFID-Tags-Seite einem freien Tag eine Person zuweisen, ohne Zuweiser-Lesegerät | Endpunkt Tag → Person |

## Einweisungen

| Funktion | Beschreibung | Backend |
| --- | --- | --- |
| Datum der Einweisung | Beim Eintragen ein früheres Datum wählen (nachgetragene Einweisungen) | Feld im Batch-Command `POST /tool-certifications/batch` |
| Grund | Grund beim Pausieren (optional) und Entziehen (Pflicht), Anzeige in der Liste | Neues Feld der Einweisung, Parameter von `pause` / `revoke` |

## Werkzeuge

| Funktion | Beschreibung | Backend |
| --- | --- | --- |
| Online-Status des WLAN-Relais | „Online“ / „Antwortet nicht“ auf der Karte Hardware und im Werkzeugstatus | Heartbeat oder regelmäßige Abfrage des Relais, Status im Werkzeug |
| Verbindung testen | Button „Verbindung testen“ im Tab Hardware | Endpunkt, der das Relais anfragt und das Ergebnis liefert |
| Relais vorher ausschalten | Option beim Entfernen des WLAN-Relais (T-18) | Option an `clear-wlan-relais`, schaltet erst aus |
| Kennzahlen in der Liste | Anzahl aktive Einweisungen und aktuelle Nutzung pro Werkzeug ohne Zusatzaufrufe | Felder in `GET /tools` |

## RFID-Tags

| Funktion | Beschreibung | Backend |
| --- | --- | --- |
| Deaktivieren / Reaktivieren | Verlorene Tags sperren, mit Grund | Endpunkte und Feld „Grund“; `RfidTag.active` gibt es schon |
| Leihtag-Nummer | Beschriftung des physischen Tags („T-04“) neben der ID | Neues Feld des Tags |
| Rückgabe bestätigen | Leihtag an der Theke als zurückgegeben markieren, wenn das Auschecken ihn nicht freigegeben hat | Endpunkt für `WAITING_FOR_UNASSIGNMENT` → `UNASSIGNED` |
| Tag manuell erfassen | Tag-ID eintippen, wenn kein Registrier-Lesegerät da ist | Endpunkt zum Anlegen eines Tags mit ID |
| Liste mit Zustand | Zustand, Person, Art und Datum in einem Aufruf – heute setzt das Portal Tags und Zuweisungen zusammen | Felder in `GET /rfid-tags` |
| Zuletzt gelesen | „Zuletzt gelesen am Tor Eingang um 14:05“ | Lese-Protokoll (siehe RFID-Lesegeräte) |

## RFID-Lesegeräte

| Funktion | Beschreibung | Backend |
| --- | --- | --- |
| Online-Status | „Online“ / „Offline seit 12:40“, „zuletzt gesehen“, „verbunden seit“ in Liste und Detailseite | Heartbeat der Lesegeräte, Felder am Lesegerät – davon hängen Übersicht und Werkzeug-Hardware ab |
| Lese-Protokoll | Letzte Lesevorgänge inkl. abgelehnter Tags (z. B. ohne Einweisung) | Protokoll der Lesevorgänge mit Ergebnis, Endpunkt pro Lesegerät |
| Verbindung erneut prüfen | Button auf der Detailseite | Endpunkt, der das Lesegerät anfragt |
| Registriert am | Datum der ersten Anmeldung | `createdAt` gibt es – nur anzeigen, kein Backend nötig |

## Querschnitt

| Funktion | Beschreibung | Backend |
| --- | --- | --- |
| Live-Updates überall | Listen und Detailseiten aktualisieren sich bei Änderungen | SSE oder WebSocket (siehe Übersicht) |
| Erfolgsmeldungen mit Inhalt | „Bandsäge gespeichert“ statt allgemeiner Meldung | Nichts nötig – Frontend (B) |
| Sprachumschalter DE/EN | In der Seitenleiste | Nichts nötig – Frontend (B) |

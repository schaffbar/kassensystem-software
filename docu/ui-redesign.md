# UI/UX-Redesign – schaffbar-portal

Ziel: Neuer Look für das Werkstatt-Portal mit UX/UI-Best-Practices, angelehnt an die Website
[schaffbar-bb.de](https://schaffbar-bb.de/). Dieses Dokument fasst den Vorschlag zusammen: Designsystem, alle Seiten
und Dialoge pro Feature, was im Backend dafür fehlt und welche Annahmen noch zu prüfen sind.

Prototyp (klickbar, privat – zum Teilen im Share-Menü freigeben):
<https://claude.ai/artifact/MUj2FTVMMR2MEUWdbJ5vEM>

Stand: 23.10.2026. Noch nichts davon ist im Code umgesetzt.

## Inhalt

- [Prototyp benutzen](#prototyp-benutzen)
- [Designsystem](#designsystem)
- [UX-Grundsätze](#ux-grundsätze)
- [Feature: Übersicht](#feature-übersicht)
- [Feature: Benutzer](#feature-benutzer)
- [Feature: Werkzeuge](#feature-werkzeuge)
- [Feature: RFID-Tags](#feature-rfid-tags)
- [Feature: RFID-Lesegeräte](#feature-rfid-lesegeräte)
- [Backend-Lücken – Gesamtliste](#backend-lücken--gesamtliste)
- [Annahmen prüfen – Checkliste](#annahmen-prüfen--checkliste)
- [Umsetzung im Code](#umsetzung-im-code)

---

## Prototyp benutzen

- Der Canvas hat **eine Zeile pro Feature**: zuerst Übersicht/Liste, dann die zugehörigen Dialoge, dann die
  Detailseite und ihre Dialoge. Am Zeilenende steht eine Notiz mit den wichtigsten Änderungen.
- **Play** (blaues Symbol am Artboard) macht Seiten klickbar: Navigation, Tabs, Filter, Suche, ⋯-Menüs und Dialoge.
- **Tweaks** am Artboard:
  - `dark` – Dunkel-Modus (auf jeder Seite)
  - `offline` – Online-/Offline-Zustand (Lesegerät-Details)
  - Dialog-Varianten, z. B. `action` (Einweisung, Tag), `blocked`, `inUse`, `state`
- Alle Namen, Zahlen und IDs sind **Beispieldaten**. Platzhalter wie `[BETRAG]` oder `[Beschreibung …]` markieren
  fehlende Inhalte.
- Die Mockups wurden nicht im Browser gegen echte Daten getestet.

---

## Designsystem

### Farben

Abgeleitet aus dem Theme von schaffbar-bb.de (dunkles Navy, Neon-Grün aus dem Logo, Akzente Blau/Amber/Orange).

| Name | Hex | Verwendung |
| --- | --- | --- |
| Navy 950 | `#071021` | Seitenleiste, Hintergrund Dunkel-Modus, dunkle Buttons |
| Navy 900 | `#131C2C` | Hover in der Navigation, Karten im Dunkel-Modus |
| Navy 800 | `#1F2632` | Flächen im Dunkel-Modus |
| Neon-Grün | `#8CC80C` | Primär-Button (als Fläche), Logo, „Frei“, Fokus-Akzent in Tabs |
| Dunkelgrün | `#3F6A00` | Grün als **Text** (Links, „Aktiv“) auf Hell |
| Blau | `#2A8FFB` / Text `#1A5FB4` | „In Benutzung“, Info, Fokus-Ring |
| Amber | `#FFB039` / Text `#9A5B00` | Hinweis, „Pausiert“, „Einweisung empfohlen“ |
| Orange | `#FB582A` / Text `#C23A12` | „Einweisung Pflicht“, Fehler, Löschen |

Semantische Tokens im Hell-Modus (im Prototyp als CSS-Variablen):

| Token | Hell | Dunkel |
| --- | --- | --- |
| `--bg` | `#F4F6F9` | `#071021` |
| `--surface` | `#FFFFFF` | `#131C2C` |
| `--surface-2` | `#EEF1F5` | `#1F2632` |
| `--border` | `#DFE4EB` | `#2C3442` |
| `--text` | `#0B1526` | `#FCFCFC` |
| `--text-2` | `#4A5568` | `#BFC2C9` |
| `--text-3` | `#5F6B7D` | `#A5ADBA` |
| `--accent` | `#8CC80C` | `#8CC80C` |
| `--on-accent` | `#071021` | `#071021` |
| `--accent-text` | `#3F6A00` | `#9FDA1F` |
| `--info` | `#1A5FB4` | `#5AA9FF` |
| `--warn` | `#9A5B00` | `#FFB039` |
| `--danger` | `#C23A12` | `#FF7A52` |
| `--on-danger` | `#FFFFFF` | `#071021` |

### Kontrast-Regeln (WCAG AA ≥ 4,5:1)

| Kombination | Kontrast | Regel |
| --- | --- | --- |
| Neon-Grün auf Weiß | 2,0:1 | **Nie als Text** – nur als Fläche |
| Navy auf Neon-Grün | 9,4:1 | Primär-Button |
| Dunkelgrün `#3F6A00` auf Weiß | 6,4:1 | Grün als Text |
| Neon-Grün auf Navy 900 | 8,4:1 | Grün als Text im Dunkel-Modus |
| Blau `#1A5FB4` auf Info-Hintergrund `#E5F0FD` | 5,3:1 | Badge „In Benutzung“ |
| Text-3 `#5F6B7D` auf Weiß | 5,4:1 | Kleinste Grautöne (Labels, Metadaten) |

Status wird **nie nur über Farbe** gezeigt, immer Icon + Text (Rot/Grün-Sehschwäche).

### Typografie

Schrift **Inter** (wie auf der Website), für IDs/MAC/IP **JetBrains Mono**.

| Stil | Größe / Gewicht | Verwendung |
| --- | --- | --- |
| Seitentitel | 28 / 700 | `h1` |
| Dialogtitel | 20 / 700 | Dialoge |
| Kartenüberschrift | 16–17 / 700 | `h2` in Karten |
| Fließtext | 14 / 400 | Text, Tabellenzellen |
| Label | 13 / 500–600 | Formularlabels, Metadaten |
| Tabellenkopf | 12 / 600, Großbuchstaben | `th` |
| Kennzahl | 34 / 700, `tabular-nums` | KPI-Kacheln, Dauer |
| Mono | 13–17 | Tag-ID, MAC, IP, Socket |

### Komponenten und Maße

- **Buttons:** Primär (Neon-Grün, Navy-Text), Sekundär (weiß mit Rahmen), Abschließen (Navy), Gefahr (Rot).
- **Icon-Buttons** immer mit `aria-label`.
- **Status-Badges:** Frei/Aktiv, In Benutzung, Hinweis/Pausiert, Pflicht/Entzogen, Offline.
- **Filter-Chips** mit Anzahl, **Segment-Umschalter** (Bereich, Karten/Liste).
- **Tabs** linksbündig mit Anzahl-Badge.
- **⋯-Menü** pro Zeile statt einzelner Aktions-Buttons.
- **Dialoge:** Kopf (Titel + Untertitel + Schließen), Inhalt, Fußzeile (Abbrechen links, Hauptaktion rechts).
- **Abstände:** 4er-Raster (4, 8, 12, 16, 20, 24, 32).
- **Radien:** 8 Icon-Button, 10 Button/Input, 14 Karte, 16 Dialog, rund für Chips.
- **Touch-Ziele:** mindestens 44 × 44 px (Tablet an der Theke).
- **Layout:** Navy-Seitenleiste mit Gruppen *Betrieb* und *Hardware*, heller Inhaltsbereich. Unter ~800 px stapelt
  sich die Seitenleiste, mobil gibt es eine Tab-Leiste unten.

---

## UX-Grundsätze

Gelten für alle Seiten und Dialoge:

1. **Ausnahmen zuerst.** Probleme (Gerät offline, Person ohne Tag) aktiv anzeigen statt in Tabellen verstecken.
2. **Live statt Refresh-Button.** Daten aktualisieren sich selbst, mit Hinweis „Live · aktualisiert vor 5 s“.
3. **Status = Icon + Text**, nie nur ein Farbpunkt.
4. **Ganze Zeile klickbar** zur Detailseite, Aktionen im ⋯-Menü.
5. **Löschen nie als roter Button in jeder Zeile.** Gefährliche Aktionen gebündelt (⋯-Menü oder „Gefahrenzone“).
6. **Dialogtitel ist eine Frage, der Button nennt die konkrete Aktion** („Einweisung entziehen“ statt „OK“).
7. **Folgen vorher erklären**, z. B. „Danach kann Anna Becker nicht mehr einchecken“.
8. **Gesperrte Aktionen erklären**, statt sie stumm zu deaktivieren, und den nächsten Schritt anbieten
   („Sitzung beenden …“).
9. **Fokus bei gefährlichen Aktionen auf „Abbrechen“**; je nach Tragweite zusätzlich Häkchen oder Namen eintippen.
10. **Validierung live** am Feld plus Zusammenfassung beim Absenden; Pflichtfelder durch „(optional)“ an den anderen
    Feldern statt Sternchen.
11. **Ungespeicherte Änderungen zählen**, „Speichern“ erst aktiv, wenn sich etwas geändert hat.
12. **Barrierefreiheit:** echte `<button>`/`<a>`/`<label>`, `role="dialog"`/`alertdialog`, `aria-expanded` an Menüs,
    sichtbarer Fokus-Ring.

---

## Feature: Übersicht

### Seiten – Übersicht

| Seite | Was sich ändert |
| --- | --- |
| **Übersicht – Desktop** | Block „Braucht Aufmerksamkeit“ oben (Lesegerät offline, Person ohne RFID-Tag). Vier Kennzahlen: In der Werkstatt, Werkzeuge in Benutzung, Check-ins heute, Lesegeräte online. Liste „In der Werkstatt“ mit Dauer, Eincheckzeit und aktuellem Werkzeug. „Werkzeugstatus“ mit Bereichsfilter und Status als Icon + Text. Statusbox „Werkstatt geöffnet“ in der Seitenleiste, Sprache DE/EN. |
| **Übersicht – Mobil / Tablet** | Dunkel-Modus, zwei Kennzahlen, Hinweis-Karte, Liste „In der Werkstatt“, Tab-Leiste unten. |

### Backend fehlt – Übersicht

| Was | Art |
| --- | --- |
| Aggregierte Hinweise („Braucht Aufmerksamkeit“): Lesegeräte offline, eingecheckt ohne Tag, Relais gestört | Neuer Endpunkt |
| Kennzahlen: Check-ins heute, Anzahl unter 18, Lesegeräte online/gesamt | Neuer Endpunkt |
| Liste „In der Werkstatt“ mit aktuellem Werkzeug und Altersgruppe (heute `ActiveUser`: nur Name + `entryTime`) | Erweiterung |
| Werkzeugstatus mit Bereich und Störung (heute `ActiveTool`: nur aktive Werkzeuge) | Erweiterung |
| Live-Aktualisierung (Polling, SSE oder WebSocket) | Neu |
| Status „Werkstatt geöffnet“ | Neu |

### Annahmen prüfen – Übersicht

- Welche Kennzahlen sind an der Theke wirklich nützlich? Die vier Kacheln sind ein Vorschlag.
- Gibt es Öffnungszeiten/einen „Werkstatt geöffnet“-Zustand im System?

---

## Feature: Benutzer

### Seiten – Benutzer

| Seite | Was sich ändert |
| --- | --- |
| **Benutzer – Liste** | Große Suche (Name, E-Mail, RFID-Tag). Filter-Chips: Alle, In der Werkstatt, Vereinsmitglieder, Unter 18, Ohne RFID-Tag. Spalten: Name + E-Mail + „Mitglied“, Altersgruppe, RFID-Tag (Fest/Temporär/Kein Tag), Anzahl Einweisungen, „Zuletzt da“. ⋯-Menü statt Löschen-Button. |
| **Benutzer – Profil** | Brotkrümel, Kopf mit Avatar und Status-Chips. Banner „In der Werkstatt seit …“ mit Hauptaktion „Sitzung beenden & abrechnen“. Tabs Übersicht / Einweisungen / Offene Sitzung. Karten Kontakt & Adresse, RFID-Tag, Einweisungen (mit ⋯-Menü pro Eintrag), Gefahrenzone. |

### Dialoge und Menüs – Benutzer

| Dialog | Geöffnet aus | Inhalt |
| --- | --- | --- |
| **⋯-Menü** | Liste, Profil-Kopf | Profil öffnen, Bearbeiten, RFID-Tag zuweisen (nur ohne Tag), Sitzung beenden (nur wenn eingecheckt), Benutzer löschen |
| **Neuer Benutzer** | Liste | Gruppen Person / Kontakt / Rechnungsadresse. Altersgruppe wird aus Geburtsdatum berechnet. Live-Validierung (E-Mail, PLZ 5 Ziffern), Zusammenfassung beim Absenden, Warnung bei möglichem Doppeleintrag. Zweite Aktion „Anlegen & RFID-Tag zuweisen“. |
| **Löschen** | ⋯-Menü, Gefahrenzone | Zählt auf, was verloren geht. Löschen erst nach Bestätigungshäkchen. **Gesperrt bei offener Sitzung**, dann Button „Sitzung beenden …“. |
| **Bearbeiten** | Profil (Kopf, Karte Kontakt), ⋯-Menü | Person, Kontakt und Adresse in einem Formular. Zähler für ungespeicherte Änderungen. |
| **Sitzung beenden & abrechnen** | Profil (Banner, Tab Offene Sitzung), ⋯-Menü | Hinweis auf noch laufende Werkzeuge (werden ausgeschaltet). Posten mit Zeit, Dauer, Einheiten, Betrag. Zahlungsart Bar / EC-Karte / Rechnung. Bestätigung danach. |
| **RFID-Tag zuweisen / ersetzen** | Profil (Karte RFID-Tag), ⋯-Menü, „Anlegen & RFID-Tag zuweisen“ | Drei Schritte: Art wählen (Leihtag / Fest), Tag an „Theke Zuweiser“ halten, fertig. Beim Ersetzen Hinweis zum alten Tag. |
| **RFID-Tag entfernen** | Profil (Karte RFID-Tag), RFID-Tags-Liste | Folgen erklärt. Auswahl: wieder freigeben oder deaktivieren (Verlust). |
| **Einweisung pausieren / reaktivieren / entziehen / löschen** | Profil (⋯ pro Einweisung, Tab Einweisungen), Werkzeug-Details | Kontextkarte mit Werkzeug, Datum, Einweiser. Grund bei Entziehen Pflicht, bei Pausieren optional. Beim Löschen Hinweis, lieber zu entziehen. |

### Backend fehlt – Benutzer

| Was | Art |
| --- | --- |
| Serverseitige Suche über Name, E-Mail **und Tag-ID**, Filter-Zählungen, Paginierung | Erweiterung |
| „Zuletzt da“ und Anzahl Einweisungen pro Benutzer in der Liste | Erweiterung |
| Duplikat-Prüfung beim Anlegen (Name + Geburtsdatum / E-Mail) | Neuer Endpunkt |
| Bearbeiten in einem Dialog – heute getrennt: `PUT /users/{id}`, `/contact`, `/address` | Drei Aufrufe oder neuer Endpunkt |
| Löschen sperren bei offener Sitzung | Regel im Backend |
| **Betrag** beim Abschluss (heute nur Einheiten, `WorkshopSession` hat keinen Preis) | Neu |
| **Zahlungsart** beim Abschluss | Neues Feld |
| Laufende Werkzeuge beim Abschluss ausschalten | Prüfen / neu |
| **Grund** bei Pausieren/Entziehen einer Einweisung | Neues Feld |
| Tag beim Entfernen direkt **deaktivieren** (`RfidTag.active` existiert) | Erweiterung am Unassign-Endpunkt |

### Annahmen prüfen – Benutzer

- Ist „Zahlungsart“ fachlich gewollt (Kassensystem)? Welche Arten gibt es?
- Bleiben abgerechnete Sitzungen beim Löschen eines Benutzers erhalten? Im Dialog steht das so.
- Altersgruppen-Grenzen: unter 16 / unter 18 / Erwachsen, Stichtag heute – korrekt?
- Soll „Sitzung beenden“ wirklich laufende Werkzeuge automatisch ausschalten?

---

## Feature: Werkzeuge

### Seiten – Werkzeuge

| Seite | Was sich ändert |
| --- | --- |
| **Werkzeuge – Übersicht** | Umschalter Karten / Liste. Gruppiert nach Bereich, Bereichsfilter mit Anzahl, Suche. Karte zeigt Status, Einweisung Pflicht/empfohlen, Anzahl Eingewiesene, aktuelle Nutzung, Hardware („Schaltbox · Shelly Pro“ oder „Ohne Schaltbox – Nutzung wird nicht erfasst“). Liste mit ⋯-Menü. |
| **Werkzeuge – Details** | Brotkrümel, Kopf mit Bereich, Einweisungspflicht und Live-Status („In Benutzung · Lena Hoffmann seit 14:31“). Tabs Details / Einweisungen / Hardware. Hardware-Tab mit Online-Status, Shelly-Befehlsvorschau, „Verbindung testen“, „Schaltbox trennen …“, „Relais entfernen …“. |

### Dialoge und Menüs – Werkzeuge

| Dialog | Geöffnet aus | Inhalt |
| --- | --- | --- |
| **⋯-Menü** | Liste (Listenansicht), Details-Kopf | Details öffnen, Bearbeiten, Einweisungen eintragen, Werkzeug löschen. Karten sind ganze Links und haben kein Menü. |
| **Neues Werkzeug** | Übersicht | Name mit sofortiger Duplikat-Prüfung. Bereich und Einweisung (Empfohlen/Pflicht) als Auswahlkarten mit Erklärung. Beschreibung mit Zähler. Zweite Aktion „Anlegen & Hardware einrichten“. |
| **Löschen** | ⋯-Menü | Zählt Einweisungen, Schaltbox-Zuordnung und Relais-Konfiguration auf. Bestätigung durch Eintippen des Namens. **Gesperrt, solange das Werkzeug läuft.** |
| **Bearbeiten** | Details-Kopf, Karte Werkzeuginformationen, ⋯-Menü | Wie „Neu“, vorbefüllt. Warnt bei Wechsel der Einweisungspflicht vor den Folgen. |
| **Einweisungen eintragen** | Tab Einweisungen, ⋯-Menü | Mehrere Personen auf einmal mit Suche und Chips. Bereits Eingewiesene ausgegraut, Pausierte mit Hinweis „im Profil reaktivieren“. Einweiser (nur Einweiser dieses Werkzeugs) und Datum (Standard heute). |
| **Einweiser hinzufügen** | Karte Einweiser | Standardfilter „Nur Personen mit aktiver Einweisung“. Wer schon Einweiser ist, ist ausgegraut. |
| **Einweiser entfernen** | Karte Einweiser | Bisherige Einweisungen bleiben gültig. Warnung beim letzten Einweiser. |
| **Einweisung pausieren / entziehen / löschen** | Tab Einweisungen (⋯ pro Zeile) | Derselbe Dialog wie im Benutzerprofil. |
| **Schaltbox trennen** | Tab Hardware, Lesegeräte-Liste | Erklärt, dass danach die Einweisungspflicht **nicht mehr geprüft** wird. **Gesperrt, solange das Werkzeug läuft.** |
| **WLAN-Relais entfernen** | Tab Hardware | Option „Relais vorher ausschalten“ (Standard an). |

### Backend fehlt – Werkzeuge

| Was | Art |
| --- | --- |
| Pro Werkzeug: Anzahl aktive Einweisungen, aktuelle Nutzung, Störungsstatus | Erweiterung der Liste |
| Online-Status von Schaltbox und WLAN-Relais | Neu (Heartbeat) |
| „Verbindung testen“ für das Relais | Neuer Endpunkt |
| **Datum der Einweisung** beim Eintragen (`BatchCreateToolCertificationCommand` hat kein Datum) | Neues Feld |
| Löschen eines Werkzeugs sperren, solange es läuft | Regel im Backend |
| Schaltbox trennen sperren, solange das Werkzeug läuft | Regel im Backend |
| „Relais vorher ausschalten“ beim Entfernen | Neu |
| Nur Eingewiesene als Einweiser zulassen (falls gewollt) | Regel im Backend |

### Annahmen prüfen – Werkzeuge

- **Nur eingewiesene Personen können Einweiser werden.** Der Dialog filtert so, das lässt sich abschalten.
- „Pflicht“ bedeutet: Ohne aktive Einweisung gibt die Schaltbox nicht frei. „Empfohlen“: jeder darf, Hinweis wird
  angezeigt. Stimmt das mit der Logik der Schaltbox überein?
- Ohne Schaltbox wird die Nutzung nicht erfasst – aus dem Datenmodell abgeleitet.
- Sollen Löschen und Schaltbox-Trennen während laufender Nutzung wirklich gesperrt sein?

---

## Feature: RFID-Tags

### Seiten – RFID-Tags

| Seite | Was sich ändert |
| --- | --- |
| **RFID-Tags – Übersicht** | Statt der Spalte „Aktiv: true“ klare Zustände: *Zugewiesen*, *Rückgabe ausstehend*, *Frei*, *Deaktiviert*. Spalten: Tag-ID (Mono) + Leihtag-Nummer, Status, zugewiesene Person (verlinkt) mit Art und Datum, „Zuletzt gelesen“ mit Lesegerät. Seitenleiste mit Live-Karte „Neuen Tag registrieren“, Karte „Wartet auf Tag“ und Erklärung „Fest oder Leihtag?“. |

### Dialoge und Menüs – RFID-Tags

| Dialog | Geöffnet aus | Inhalt |
| --- | --- | --- |
| **⋯-Menü** | Liste | Nur, was im Zustand möglich ist. *Zugewiesen:* Profil, Zuweisung aufheben, Deaktivieren. *Rückgabe ausstehend:* Rückgabe bestätigen. *Frei:* Person zuweisen. *Deaktiviert:* Reaktivieren, Tag löschen. |
| **Person zuweisen** | ⋯-Menü (freier Tag) | Wartende Personen oben, Personen mit Tag ausgegraut. Art Leihtag / Fest. |
| **Deaktivieren / Reaktivieren / Löschen / Rückgabe bestätigen** | ⋯-Menü | Deaktivieren mit Grund (Verloren, Defekt, Sonstiges) und Warnung zur betroffenen Person. Rückgabe erst nach Häkchen „Tag liegt im Leihtag-Kasten“. Löschen nur für entsorgte Tags. |
| **Zuweisung aufheben** | ⋯-Menü | Derselbe Dialog wie „RFID-Tag entfernen“ im Profil. |
| **Tag manuell erfassen** | Register-Karte („Lesegerät nicht da?“) | Prüfung Hex-Format, Länge 8 oder 14, Duplikate. Leihtag-Nummer mit Vorschlag. |

### Backend fehlt – RFID-Tags

| Was | Art |
| --- | --- |
| Liste mit Zustand (aus `active` + `RfidTagAssignment.status`), Person, Art, Datum | Erweiterung |
| „Zuletzt gelesen“ mit Lesegerät | Neu (Lese-Protokoll) |
| **Leihtag-Nummer** (Beschriftung des physischen Tags) | Neues Feld |
| **Grund beim Deaktivieren** | Neues Feld |
| Tag deaktivieren / reaktivieren | Neuer Endpunkt |
| Rückgabe manuell bestätigen (`WAITING_FOR_UNASSIGNMENT` → `UNASSIGNED`) | Neuer Endpunkt |
| Zuweisung vom Tag aus (Tag → Person, ohne Zuweiser-Lesegerät) | Neuer Endpunkt |
| Manuelle Erfassung einer Tag-ID | Neuer Endpunkt |

### Annahmen prüfen – RFID-Tags

- Rückgabe ausstehend entsteht, wenn das „Tor Ausgang“ offline war – ist das der einzige Fall?
- Tag-IDs sind 4 oder 7 Byte (8 bzw. 14 Hex-Zeichen)?
- Gibt es einen physischen Leihtag-Kasten mit nummerierten Tags?

---

## Feature: RFID-Lesegeräte

### Seiten – RFID-Lesegeräte

| Seite | Was sich ändert |
| --- | --- |
| **RFID-Lesegeräte – Übersicht** | Offline-Hinweis oben. Gruppiert: *Eingang, Ausgang & Theke* als Karten, *Schaltboxen* als Tabelle mit zugeordnetem Werkzeug. Filter: Alle, Eingang & Theke, Schaltboxen, Nicht zugeordnet, Offline. Status „Nicht zugeordnet“ ist direkt klickbar. ⋯-Menü pro Schaltbox. |
| **RFID-Lesegeräte – Details** | Offline-Banner mit Prüfschritten, „Verbindung erneut prüfen“, „Personen manuell auschecken“. Geräteinformationen mit Kopier-Button für MAC. Typ als Auswahlkarten mit Erklärung. Letzte Lesevorgänge (auch abgelehnte). Gefahrenzone. |

### Dialoge und Menüs – RFID-Lesegeräte

| Dialog | Geöffnet aus | Inhalt |
| --- | --- | --- |
| **⋯-Menü** | Schaltboxen-Tabelle, Details-Kopf | Details, Bearbeiten, Werkzeug zuordnen (frei) oder Vom Werkzeug trennen (zugeordnet), Löschen. |
| **Werkzeug zuordnen** | ⋯-Menü, Status „Nicht zugeordnet“ | Nur Werkzeuge ohne eigene Schaltbox, mit Suche. Hinweis, danach das WLAN-Relais einzurichten. |
| **Vom Werkzeug trennen** | ⋯-Menü | Derselbe Dialog wie „Schaltbox trennen“ bei Werkzeugen. |
| **Bearbeiten** | Details-Kopf, Karte Geräteinformationen, ⋯-Menü | Name mit Duplikat-Prüfung. Socket-Name mit Formatprüfung (`a–z`, `0–9`, `-`) und Hinweis zur Geräte-Konfiguration. MAC schreibgeschützt. |
| **Typ ändern** | „Typ speichern …“ in den Details | Vorher → Nachher und konkrete Folgen. **Gesperrt, wenn der Schaltbox ein Werkzeug zugeordnet ist.** |
| **Löschen** | ⋯-Menü, Gefahrenzone | Hinweis, dass ein Online-Gerät sofort als neues Gerät wieder auftaucht. **Gesperrt bei zugeordnetem Werkzeug**, dann „Zuordnung lösen …“. |

### Backend fehlt – RFID-Lesegeräte

| Was | Art |
| --- | --- |
| **Online-Status und „zuletzt gesehen“** (`RfidReader` hat kein solches Feld) | Neu (Heartbeat) |
| „Verbunden seit“, „Registriert am“ | Neue Felder |
| Protokoll der letzten Lesevorgänge inkl. abgelehnter Tags | Neu |
| „Verbindung erneut prüfen“ | Neuer Endpunkt |
| Typwechsel sperren bei zugeordnetem Werkzeug – Fehlermeldung gibt es schon in den Übersetzungen (`switchBoxHasAssignedTool`) | Regel serverseitig sicherstellen |
| Löschen sperren bei zugeordnetem Werkzeug | Neue Regel |
| Eindeutigkeit von Name und Socket-Name | Regel im Backend |

### Annahmen prüfen – RFID-Lesegeräte

- Neue Lesegeräte erscheinen automatisch, sobald sie sich verbinden (es gibt kein „Anlegen“).
- Was genau ist der Socket-Name – der Kanal, über den das Gerät sich verbindet? Muss er auf dem Gerät gleich sein?
- Beschreibungen der Typen, besonders *RFID-Tag-Zuweiser* („weist der wartenden Person zu“) und *Schaltbox*
  („gibt nur mit gültiger Einweisung frei“).

---

## Backend-Lücken – Gesamtliste

Sortiert nach Art. „Neu“ = gibt es heute nicht, „Erweiterung“ = bestehenden Endpunkt/DTO ergänzen.

### Neue Felder

| Feld | Entität | Feature |
| --- | --- | --- |
| Zahlungsart | Sitzung | Benutzer |
| Betrag / Preis | Sitzung | Benutzer |
| Grund (Pausieren/Entziehen) | Einweisung | Benutzer, Werkzeuge |
| Datum der Einweisung beim Eintragen | Batch-Command Einweisung | Werkzeuge |
| Leihtag-Nummer | RFID-Tag | RFID-Tags |
| Grund der Deaktivierung | RFID-Tag | RFID-Tags |
| Online-Status, zuletzt gesehen, verbunden seit, registriert am | RFID-Lesegerät | Lesegeräte, Werkzeuge, Übersicht |
| Online-Status | WLAN-Relais | Werkzeuge |

### Neue Endpunkte

| Endpunkt | Feature |
| --- | --- |
| Hinweise „Braucht Aufmerksamkeit“ | Übersicht |
| Kennzahlen (Check-ins heute, Lesegeräte online …) | Übersicht |
| Live-Updates (Polling/SSE/WebSocket) | Übersicht, alle Listen |
| Duplikat-Prüfung Benutzer | Benutzer |
| Relais „Verbindung testen“ | Werkzeuge |
| Tag deaktivieren / reaktivieren | RFID-Tags |
| Rückgabe manuell bestätigen | RFID-Tags |
| Tag einer Person zuweisen (vom Tag aus) | RFID-Tags |
| Tag manuell erfassen | RFID-Tags |
| Lese-Protokoll (zuletzt gelesen, letzte Lesevorgänge) | RFID-Tags, Lesegeräte |
| Lesegerät „Verbindung erneut prüfen“ | Lesegeräte |

### Erweiterungen bestehender Endpunkte

| Endpunkt | Ergänzung |
| --- | --- |
| Dashboard `GET …/active-users` | aktuelles Werkzeug, Altersgruppe |
| Dashboard `GET …/active-tools` | alle Werkzeuge mit Status inkl. Störung, Bereich |
| Benutzer `GET …/users` | Suche inkl. Tag-ID, Filter-Zählungen, Paginierung, „zuletzt da“, Anzahl Einweisungen |
| Benutzer `PUT …/users/{id}` (+ `/contact`, `/address`) | optional zusammenführen für einen Bearbeiten-Dialog |
| Werkzeuge `GET …/tools` | Anzahl aktive Einweisungen, aktuelle Nutzung |
| Tag-Zuweisung `PUT …/unassign` | Option „Tag deaktivieren“ |
| RFID-Tags `GET …/rfid-tags` | Zustand, Person, Art, Datum, Leihtag-Nummer |
| Sitzung schließen `PUT …/{userId}/close` | Zahlungsart, laufende Werkzeuge ausschalten |
| Relais entfernen | Option „vorher ausschalten“ |

### Neue Regeln (serverseitig erzwingen)

| Regel | Feature |
| --- | --- |
| Benutzer nicht löschen, solange eine Sitzung offen ist | Benutzer |
| Werkzeug nicht löschen, solange es läuft | Werkzeuge |
| Schaltbox nicht trennen, solange das Werkzeug läuft | Werkzeuge, Lesegeräte |
| Nur Eingewiesene als Einweiser (falls gewollt) | Werkzeuge |
| Typwechsel einer Schaltbox mit Werkzeug verbieten | Lesegeräte |
| Lesegerät mit Werkzeug nicht löschen | Lesegeräte |
| Name und Socket-Name von Lesegeräten eindeutig | Lesegeräte |
| Werkzeugname eindeutig (Fehlermeldung `nameExists` existiert schon) | Werkzeuge |

---

## Annahmen prüfen – Checkliste

- [ ] Kennzahlen auf der Übersicht sind die richtigen
- [ ] Es gibt einen Zustand „Werkstatt geöffnet“ / Öffnungszeiten
- [ ] Zahlungsart beim Abschluss ist gewollt; welche Arten (Bar, EC-Karte, Rechnung)?
- [ ] Abgerechnete Sitzungen bleiben beim Löschen eines Benutzers erhalten
- [ ] Altersgruppen-Grenzen und Stichtag
- [ ] „Sitzung beenden“ schaltet laufende Werkzeuge automatisch aus
- [ ] „Pflicht“ = Schaltbox gibt ohne aktive Einweisung nicht frei; „Empfohlen“ = jeder darf
- [ ] Ohne Schaltbox wird die Nutzung nicht erfasst
- [ ] Nur eingewiesene Personen können Einweiser werden
- [ ] Löschen/Trennen während laufender Nutzung sperren
- [ ] „Rückgabe ausstehend“ entsteht nur, wenn „Tor Ausgang“ offline war
- [ ] Tag-ID-Format: 8 oder 14 Hex-Zeichen
- [ ] Physischer Leihtag-Kasten mit nummerierten Tags
- [ ] Lesegeräte registrieren sich selbst (kein „Anlegen“)
- [ ] Bedeutung des Socket-Namens
- [ ] Beschreibungen der Gerätetypen (besonders Zuweiser und Schaltbox)

---

## Umsetzung im Code

Vorschlag für die Reihenfolge, wenn der Entwurf passt:

1. **Theme:** Eigenes Angular-Material-Theme mit den Farben oben, Inter als Schrift, Tokens als CSS-Variablen inkl.
   Dunkel-Modus in `src/styles.scss`. Die bisherige Border-Radius-Überschreibung für Buttons fällt weg.
2. **Layout:** Seitenleiste (Navy, Gruppen Betrieb/Hardware, Logo) und Seitenkopf in `app.component` /
   `core/navigation`.
3. **Gemeinsame Komponenten** unter `shared/components`:
   - `StatusBadge` (Icon + Text, Varianten frei/in Benutzung/Hinweis/Pflicht/offline)
   - `RowMenu` (⋯-Menü mit `mat-menu`)
   - `ConfirmDialog` (ersetzt `confirm-delete`; Titel als Frage, Button mit konkreter Aktion, optional Häkchen oder
     Namen eintippen, gesperrter Zustand mit Erklärung)
   - `PageHeader` (Brotkrümel, Titel, Status-Chips, Aktionen)
   - `EmptyState`
4. **Seiten** in der Reihenfolge RFID-Lesegeräte → RFID-Tags → Werkzeuge → Benutzer → Übersicht (klein nach groß,
   passt zur Reihenfolge der [Angular-v22-Migration](angular-v22-migration.md)).
5. **Backend-Lücken** parallel, beginnend mit Online-Status/Heartbeat für Lesegeräte – davon hängen Übersicht,
   Lesegeräte und Werkzeug-Hardware ab.

Den Prototyp-Code (`.dc.html`) nicht übernehmen – er ist ein Entwurf mit Inline-Styles. Maße, Farben und Texte
daraus übernehmen, die Umsetzung mit Angular Material.

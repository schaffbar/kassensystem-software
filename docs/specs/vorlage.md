# <Aufgabe in einem Satz>

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

**Quelle:** <Issue, Abschnitt in docs/aufgaben.md, Artboard des Prototyps>
**Branch:** <name>
**Stand:** Spec – wartet auf Freigabe | freigegeben | Backend fertig | Portal fertig | abgenommen | Review fertig

## 1. Spec

### Auftrag

<!-- Was soll sich ändern und warum – zwei, drei Sätze, aus Sicht der Werkstatt. -->

### Heute

<!-- Was es schon gibt: Regeln in den README der Aggregate, Endpunkte, Seiten und Dialoge im Portal, mit Pfaden. -->

### Vertrag

<!--
Die Entscheidungen, gegen die Backend und Portal bauen. Für eine Geschäftsregel die Tabelle aus dem Skill
business-rule („The contract“): Id und Regel, genaue Bedingung, wann geprüft wird, bestehende Daten, Ort der Regel,
Exception und Code, Fehlertext de/en, API-Änderung, Verhalten im Portal, Geräte. Für anderes: geänderte Endpunkte und
Felder, Fehlercodes, Verhalten im Portal, Geräte. Jede Zeile entschieden oder als offene Frage unten.
-->

| Entscheidung | Festgelegt |
| --- | --- |
| | |

### Umfang

<!-- Was Backend, Portal und Doku ändern (Klassen, Seiten, README, entscheidungen.md, aufgaben.md) – und was
ausdrücklich nicht dazugehört. -->

### Offene Fragen

<!-- Jede Frage mit den Möglichkeiten und einer Empfehlung; Piotr antwortet darunter. Ohne beantwortete Fragen keine
Freigabe. -->

- [ ] <Frage> – Möglichkeiten: … – Empfehlung: …
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
- Abweichungen vom Vertrag, nicht gemacht, offene Fragen.
-->

## 4. Abnahme im Browser

<!-- Seiten, Zustände (auch simulierte Antworten), Desktop und Handy; was gefunden wurde. Nichts in die Datenbank
geschrieben (Skill verify-ui); Screenshots liegen nicht im Repo. -->

## 5. Review

<!-- Befunde gegen CLAUDE.md und die Skills: Datei und Zeile, verletzte Regel, muss / sollte, Stand (offen, behoben).
Ohne die Begründung des Autors gelesen. -->

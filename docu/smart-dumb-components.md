# Smart & Dumb Components – schaffbar-portal

Bestandsaufnahme aller Angular-Komponenten: was sie laut Ordner sein sollten, was sie heute sind, welches ViewModel
sie bekommen sollen, und ein Plan, wie alle zum Ziel kommen. Die Regeln selbst stehen im Skill `component`
(`schaffbar-portal/.claude/skills/component/SKILL.md`).

Stand: 05.11.2026, `main` @ `c939f31`.

## Inhalt

- [Begriffe](#begriffe)
- [Zusammenfassung](#zusammenfassung)
- [Bestandsaufnahme](#bestandsaufnahme)
- [Befunde im Detail](#befunde-im-detail)
- [Entscheidungen](#entscheidungen)
- [Plan](#plan)

---

## Begriffe

| Art             | Ordner                                                | Woran man sie erkennt                                                                                                                                         |
| --------------- | ----------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Smart**       | `<feature>/pages/`, Dialog-Container                  | weiß, **woher** Daten kommen und **wohin** Commands gehen: `inject()` von Store, `Router`, `MatDialog`, Dialog-Services, Snackbar. Template = Layout + Kinder |
| **Dumb**        | `<feature>/components/`, `shared/components/`, Modals | nur `input()` / `output()`, lokaler UI-Zustand (Formular, Edit-Modus, Suchtext). Injiziert keinen Store, keinen Router, kein API-Service                      |
| **Form-Dialog** | `<feature>/components/<name>-form/`                   | Dumb, darf aber `MAT_DIALOG_DATA` und `MatDialogRef` injizieren und schließt sich selbst (Skill `dialog`)                                                     |
| **Shell**       | `app.component`, `core/`                              | App-Rahmen (Navigation, 404) – weder Feature noch wiederverwendbar, fällt aus der Einteilung                                                                  |

**ViewModel** = der Input-Typ einer dumb component, exportiert **aus ihrer eigenen Datei**, mit genau den Feldern, die
sie liest – ohne Typen aus dem Feature-Model. Die Page füllt ihn:

- **ohne Mapper**, wenn das Feature-Model alle Felder hat – sie übergibt es direkt (strukturelle Typisierung,
  `strictTemplates` prüft die Bindung);
- **mit Mapping** (`computed` + reine `to<Name>ViewModel()` nach der Page-Klasse), wenn die Form abweicht
  (zusammengeführt, berechnet).

Braucht eine Komponente nur ein, zwei Ids oder einfache Werte, bekommt sie kein ViewModel. Heute haben nur die drei
Shared-Dialog-Modals eines.

## Zusammenfassung

- **36 Komponenten**, nach Ist: 10 smart, 19 dumb, 4 Form-Dialoge, 3 Shell.
- **Die Trennung ist schon weitgehend da.** Alle Pages injizieren den Store, alle Components kommunizieren über
  Inputs/Outputs mit Commands. Echte Verstöße gibt es an **6 Stellen**:

| #   | Komponente                    | Verstoß                                                                                |
| --- | ----------------------------- | -------------------------------------------------------------------------------------- |
| V1  | `tool-detail-info`            | dumb, injiziert aber `ToolsStore` (für die Namens-Eindeutigkeit)                       |
| V2  | `new-tool-form`               | Form-Dialog, injiziert `ToolsStore` (dito)                                             |
| V3  | `tool-rfid-reader-assignment` | Fachregel in der dumb component: nur Switch-Boxen sind zuweisbar                       |
| V4  | `tool-certifications`         | Fachregel in der dumb component: wer zertifiziert werden darf / wer zertifizieren darf |
| V5  | `user-detail-info`            | liegt in `pages/`, reicht aber nur durch                                               |
| V6  | `user-detail`                 | smart, aber viel Präsentation im Template (Chips, RFID-Tag-Buttons mit `@switch`)      |

- **ViewModels fehlen bei 16 Komponenten**:
  - **12 ohne Mapper** – ViewModel in der Komponente anlegen, die Page übergibt weiter das Feature-Model.
  - **4 mit Mapping in der Page** – sie joinen heute selbst: `tool-certifications`, `user-tool-certifications`,
    `tool-instructors`, `active-tools-widget`.
- **Erlaubt (D2)**: 3 dumb components öffnen selbst einen Bestätigungsdialog (`ConfirmDeleteDialogService` /
  `ConfirmActionDialogService`) – siehe D2 unter „Entscheidungen“.

## Bestandsaufnahme

Status: ✅ fertig · ⚠️ ViewModel fehlt · ❌ Verstoß (V1–V6)

ViewModel (Soll): **–** kein ViewModel nötig · **ohne Mapper** Page übergibt das Feature-Model · **Mapping** Page
mappt mit `to<Name>ViewModel()`

### core / app

| Komponente   | Ordner  | Soll  | Ist                                          | ViewModel (Soll) | Status |
| ------------ | ------- | ----- | -------------------------------------------- | ---------------- | ------ |
| `app`        | `app/`  | Shell | Shell                                        | –                | ✅     |
| `navigation` | `core/` | Shell | Shell (`BreakpointObserver`, `RouterOutlet`) | –                | ✅     |
| `not-found`  | `core/` | Shell | statisch                                     | –                | ✅     |

### dashboard

| Komponente            | Ordner        | Soll  | Ist   | Input heute                                                | ViewModel (Soll)                                                   | Status |
| --------------------- | ------------- | ----- | ----- | ---------------------------------------------------------- | ------------------------------------------------------------------ | ------ |
| `dashboard`           | `pages/`      | Smart | Smart | –                                                          | –                                                                  | ✅     |
| `active-tools-widget` | `components/` | Dumb  | Dumb  | `Tool[]` + `ActiveTool[]`, joint intern zu `ToolStatusRow` | **Mapping**: `ToolStatusViewModel[]` (Name, in Benutzung, von wem) | ⚠️     |
| `active-users-widget` | `components/` | Dumb  | Dumb  | `ActiveUser[]`                                             | ohne Mapper: `ActiveUserViewModel[]`                               | ⚠️     |

### rfid-readers

| Komponente                     | Ordner        | Soll        | Ist                                       | Input heute                  | ViewModel (Soll)                                                                        | Status |
| ------------------------------ | ------------- | ----------- | ----------------------------------------- | ---------------------------- | --------------------------------------------------------------------------------------- | ------ |
| `rfid-reader-list`             | `pages/`      | Smart       | Smart, rendert die Tabelle selbst         | –                            | –                                                                                       | ✅     |
| `rfid-reader-detail`           | `pages/`      | Smart       | Smart (Vorab-Prüfung + Snackbar + Dialog) | –                            | –                                                                                       | ✅     |
| `rfid-reader-detail-info`      | `components/` | Dumb        | Dumb                                      | `RfidReader`                 | ohne Mapper: `RfidReaderInfoViewModel`                                                  | ⚠️     |
| `change-rfid-reader-type-form` | `components/` | Form-Dialog | Form-Dialog                               | Dialog-Data `{ rfidReader }` | ohne Mapper: Dialog-Data `{ rfidReader: ChangeRfidReaderTypeViewModel }` (`id`, `type`) | ⚠️     |

### rfid-tags

| Komponente      | Ordner   | Soll  | Ist                               | ViewModel (Soll) | Status |
| --------------- | -------- | ----- | --------------------------------- | ---------------- | ------ |
| `rfid-tag-list` | `pages/` | Smart | Smart, rendert die Tabelle selbst | –                | ✅     |

### shared

| Komponente                       | Ordner               | Soll              | Ist                            | ViewModel                                      | Status |
| -------------------------------- | -------------------- | ----------------- | ------------------------------ | ---------------------------------------------- | ------ |
| `certification-status-chip`      | `shared/components/` | Dumb              | Dumb                           | – (nur ein Enum aus `shared/models`)           | ✅     |
| `field-error`                    | `shared/components/` | Dumb              | Dumb                           | – (`ReadonlyFieldTree` + Label-Key)            | ✅     |
| `close-session-dialog-container` | `shared/dialogs/`    | Smart (Container) | Smart, lädt per `httpResource` | – (bekommt eine Id, mappt für das Modal)       | ✅     |
| `close-session-dialog-modal`     | `shared/dialogs/`    | Dumb              | Dumb                           | `CloseSessionViewModel` (Mapping im Container) | ✅     |
| `confirm-action-dialog-modal`    | `shared/dialogs/`    | Dumb              | Dumb                           | `ConfirmActionDialogViewModel`                 | ✅     |
| `confirm-delete-dialog-modal`    | `shared/dialogs/`    | Dumb              | Dumb                           | `ConfirmDeleteDialogViewModel`                 | ✅     |

### tools

| Komponente                    | Ordner        | Soll        | Ist                                 | Input heute                                                                          | ViewModel (Soll)                                                                                                 | Status           |
| ----------------------------- | ------------- | ----------- | ----------------------------------- | ------------------------------------------------------------------------------------ | ---------------------------------------------------------------------------------------------------------------- | ---------------- |
| `tool-list`                   | `pages/`      | Smart       | Smart, rendert die Tabelle selbst   | –                                                                                    | –                                                                                                                | ✅               |
| `tool-detail`                 | `pages/`      | Smart       | Smart                               | –                                                                                    | – (füllt die ViewModels der Kinder)                                                                              | ✅               |
| `tool-detail-info`            | `components/` | Dumb        | Dumb                                | `ToolInfoViewModel` + `ExistingToolViewModel[]`                                      | ohne Mapper: `ToolInfoViewModel` + `ExistingToolViewModel[]` (`id`, `name`)                                      | ✅ (V1 erledigt) |
| `tool-instructors`            | `components/` | Dumb        | Dumb                                | `User[]` + `instructorIds` + `toolId`, joint intern                                  | **Mapping**: `InstructorViewModel[]` für Ausbilder und Kandidaten; `toolId` bleibt `string`                      | ⚠️               |
| `tool-certifications`         | `components/` | Dumb        | Dumb + `ConfirmDeleteDialogService` | `ToolCertification[]` + `User[]` + `instructorIds`, joint und filtert nach Fachregel | **Mapping**: `ToolCertificationRowViewModel[]` (mit `userName`); `certifiableUsers` / `certifiers` aus dem Store | ❌ V4            |
| `tool-rfid-reader-assignment` | `components/` | Dumb        | Dumb + `ConfirmActionDialogService` | `ToolRfidReaderViewModel` + `RfidReaderOptionViewModel[]`                            | `ToolRfidReaderViewModel` ohne Mapper, **Mapping**: `RfidReaderOptionViewModel[]` (mit `isAssignedToOtherTool`)  | ✅ (V3 erledigt) |
| `tool-wlan-relais`            | `components/` | Dumb        | Dumb                                | `Tool`                                                                               | ohne Mapper: `WlanRelaisViewModel` (`id`, `wlanRelaisType`, `ipAddress`)                                         | ⚠️               |
| `new-tool-form`               | `components/` | Form-Dialog | Form-Dialog                         | Dialog-Data `NewToolDialogData`                                                      | ohne Mapper: Dialog-Data `{ existingTools: Signal<ExistingToolViewModel[]> }`                                    | ✅ (V2 erledigt) |

### users

| Komponente                 | Ordner        | Soll        | Ist                                                                | Input heute                                              | ViewModel (Soll)                                                    | Status |
| -------------------------- | ------------- | ----------- | ------------------------------------------------------------------ | -------------------------------------------------------- | ------------------------------------------------------------------- | ------ |
| `user-list`                | `pages/`      | Smart       | Smart, rendert Tabelle + Suche selbst                              | –                                                        | –                                                                   | ✅     |
| `user-detail`              | `pages/`      | Smart       | Smart, aber Header-Präsentation im Template                        | –                                                        | – (füllt die ViewModels der Kinder)                                 | ❌ V6  |
| `user-detail-info`         | `pages/`      | Smart       | **Dumb** – reicht `user` und zwei Outputs nur durch                | `User`                                                   | – (wird aufgelöst)                                                  | ❌ V5  |
| `user-open-session`        | `pages/`      | Smart       | Smart, verschachtelt (nutzt den `UserDetailStore` der Elternseite) | `userId`                                                 | –                                                                   | ✅     |
| `user-contact`             | `components/` | Dumb        | Dumb                                                               | `User`                                                   | ohne Mapper: `ContactViewModel` (`id`, `email`, `phone`)            | ⚠️     |
| `user-address`             | `components/` | Dumb        | Dumb                                                               | `User`                                                   | ohne Mapper: `AddressViewModel` (`id`, `address`)                   | ⚠️     |
| `user-tool-certifications` | `components/` | Dumb        | Dumb + `ConfirmActionDialogService`                                | `ToolCertification[]` + `Tool[]`, joint zu `toolNameMap` | **Mapping**: `UserToolCertificationRowViewModel[]` (mit `toolName`) | ⚠️     |
| `tool-usage-summary`       | `components/` | Dumb        | Dumb                                                               | `ToolUsageSummary`                                       | ohne Mapper: `ToolUsageSummaryViewModel`                            | ⚠️     |
| `workshop-usage-summary`   | `components/` | Dumb        | Dumb                                                               | `WorkshopUsage[]` + zwei Summen                          | ohne Mapper: `WorkshopUsageViewModel[]`, Summen bleiben `number`    | ⚠️     |
| `new-user-form`            | `components/` | Form-Dialog | Form-Dialog                                                        | –                                                        | – (keine Daten)                                                     | ✅     |
| `update-user-form`         | `components/` | Form-Dialog | Form-Dialog                                                        | Dialog-Data `{ user }`                                   | ohne Mapper: Dialog-Data `{ user: UpdateUserViewModel }`            | ⚠️     |

## Befunde im Detail

### Store in dumb components (V1, V2)

`tool-detail-info` und `new-tool-form` injizieren `ToolsStore`, nur um `validateUniqueToolName(…, tools)` die Liste
aller Werkzeuge zu geben. Stattdessen kommt die Liste als Input – mit eigenem ViewModel, denn der Validator braucht
nur `id` und `name`:

```ts
// tool-detail-info.component.ts
export interface ExistingToolViewModel {
  id: string;
  name: string;
}

readonly existingTools = input.required<ExistingToolViewModel[]>();
validateUniqueToolName(schema.name, this.existingTools, () => this.tool().id);
```

Dafür nimmt `validateUniqueToolName` statt `() => Tool[]` nur noch `() => { id: string; name: string }[]` – der
Validator braucht vom Werkzeug ohnehin nur diese beiden Felder. `tool-detail` übergibt `toolsStore.entities()` direkt.
`new-tool-form` bekommt das Signal über `MAT_DIALOG_DATA` (`{ existingTools: this.store.entities }`), dann prüft der
Validator auch gegen eine Liste, die sich bei offenem Dialog ändert.

### Fachregeln in dumb components (V3, V4)

- `tool-rfid-reader-assignment`: `filteredRfidReaders` = nur `RfidReaderType.SwitchBox`. Welche Reader einem
  Werkzeug zugewiesen werden dürfen, ist eine Fachregel → `ToolDetailStore.assignableRfidReaders` (computed).
  **Umgesetzt:** Welcher Reader schon an einem anderen Werkzeug hängt, ist dagegen keine Regel, sondern ein Abgleich
  zweier Datenquellen (`toolsStore.entities()` + aktuelles Werkzeug). Das bleibt in der Page und landet beim Mapping
  als `isAssignedToOtherTool` in `RfidReaderOptionViewModel` – sonst müsste `ToolDetailStore` den `ToolsStore`
  einbinden. Die Komponente bekommt damit einen Input statt zwei (`rfidReaders` + `assignedRfidReaderIds`).
- `tool-certifications`: `filteredUsers` schließt bereits Zertifizierte und Ausbilder aus, `filteredCertifiers` nimmt
  nur Ausbilder – das sind die Regeln „wer kann zertifiziert werden“ und „wer darf zertifizieren“. →
  `ToolDetailStore.certifiableUsers` und `certifiers`; die Komponente filtert nur noch nach dem Suchtext (das ist
  UI-Logik und bleibt).

### Joins in dumb components (ViewModel mit Mapping)

`tool-certifications` (`userNameMap`), `user-tool-certifications` (`toolNameMap`), `tool-instructors`
(`instructors` aus `allUsers` + `instructorIds`) und `active-tools-widget` (`ToolStatusRow`) bekommen Rohlisten
und bauen sich die Anzeigezeile selbst. Künftig definiert die Komponente die Zeile als ViewModel
(`ToolCertificationRowViewModel { customerId, toolId, userName, status, certifiedAt }`), die Page mappt mit
`toCertificationRowViewModels(certifications, users)` in einem `computed`. Der Store bleibt bei Feature-Models und
Fachregeln. Die Komponente und ihr Test werden deutlich einfacher; nebenbei verschwindet `?? cert.toolId` aus den
Templates (stattdessen `userName | orDash` für gelöschte Benutzer).

### ViewModel ohne Mapper (12 Komponenten)

Bei allen anderen dumb components passt die Form schon: Das ViewModel ist eine Teilmenge des Feature-Models, die
Page übergibt es unverändert. Die Arbeit liegt ganz in der Komponente: Interface anlegen, Input umbenennen
(`user` → `contact`), Specs auf vollständige ViewModel-Literale umstellen – die `{ … } as User`-Casts in den Specs
fallen dabei weg.

### Pass-through-Komponente (V5)

`user-detail-info` liegt in `pages/`, hat aber keinen Store – es rendert `user-contact` + `user-address` und reicht
deren Outputs weiter. Mit ViewModels müsste es ein eigenes ViewModel nur zum Weiterreichen haben. → **auflösen**:
`user-detail` rendert die beiden Komponenten direkt (das Layout `tab-content--split` zieht mit in die Page).

### Präsentation in smart components (V6)

Das Template von `user-detail` baut die Chips (Altersgruppe, Vereinsmitglied, RFID-Tag-Status) und zeigt je nach
`RfidTagAssignmentStatus` andere Buttons – inklusive `'users.chips.userDetails.ageGroup.' + (… | lowercase)`, das
CLAUDE.md bereits als „not migrated yet“ führt. → dumb `user-detail-header` mit `UserDetailHeaderViewModel` (Name,
Altersgruppe, Vereinsmitglied, RFID-Tag-Zuordnung), gemappt in `user-detail` aus `user` + `rfidTagAssignment`;
Outputs `edit`, `reload`, `assignRfidTag: AssignRfidTagCommand`, `unassignRfidTag: UnassignRfidTagCommand`;
Enum-Keys über Pipes (`ageGroupKey`, `rfidTagAssignmentTypeKey`).

### Kleinigkeiten (smart components)

- `tool-detail`: `selectedTool`, `toolCertifications`, `allUsers`, `allRfidReaders` sind nur
  `computed(() => store.x())` – das Template kann `detailsStore.x()` direkt lesen (wie `user-detail` es tut).
- `rfid-reader-detail`: `hasAssignedTools(…).subscribe(…)` ohne `takeUntilDestroyed` (steht schon als TODO im Code).
- `tool-certifications.onSubmitBatch` emittiert den Command ohne typisierte Variable (CLAUDE.md „Forms“).

## Entscheidungen

### D1: ViewModel – entschieden (05.11.2026)

- Jede dumb component bekommt ein ViewModel, außer sie braucht nur ein, zwei Ids oder einfache Werte.
- Das ViewModel steht **in der Komponente selbst** (exportiert aus ihrer Datei) und enthält genau die Felder, die
  sie liest – keine Typen aus dem Feature-Model, Enums aus `shared/models` sind erlaubt.
- Hat das Feature-Model alle Felder, übergibt die Page es direkt (kein Mapper). Weicht die Form ab, mappt die Page in
  einem `computed` mit einer reinen `to<Name>ViewModel()` nach der Klasse. Der Store kennt keine ViewModels.
- Fachregeln bleiben im Store; die Page reicht das Ergebnis ins ViewModel.
- Outputs bleiben Commands des Features (CLAUDE.md „Commands“) – die Entkopplung betrifft die Inputs.

Warum: Das ViewModel ist der Vertrag der Komponente – genau das, was sie braucht. Ohne ViewModel müsste bei einer
Page mit mehreren Kindern entweder das Feature-Model oder der Store für jede Komponente wachsen. Durch die
strukturelle Typisierung kostet das ViewModel dort nichts, wo die Form schon passt.

Skill-Abschnitt: „ViewModel“.

### D2: Bestätigungsdialoge in dumb components – entschieden (05.11.2026)

`tool-certifications`, `tool-rfid-reader-assignment`, `user-tool-certifications` öffnen den Bestätigungsdialog
selbst, die Listen-Seiten tun es in der Page.

**Erlaubt** – aber nur `ConfirmDeleteDialogService` / `ConfirmActionDialogService`. Die Frage gehört zum
Button, den die Komponente rendert; beide Services liegen in `shared/`, haben keinen Zustand und liefern nur ja/nein.
Die Alternative (Output „möchte löschen“, Page fragt nach) bläht `tool-detail` und `user-detail` um je 3 Handler mit
Dialogtexten auf, ohne die Komponente wiederverwendbarer zu machen.

Skill-Abschnitt: „Dumb component → What it may inject“.

### D3: Tabellen der Listen-Seiten – entschieden (05.11.2026)

`tool-list`, `user-list`, `rfid-reader-list`, `rfid-tag-list` rendern ihre `mat-table` selbst.

**Vorerst so lassen**; eigene dumb components für die Tabellen sind für später möglich. Jede Tabelle hat genau einen Nutzer, `MatSort`/`MatPaginator` per `viewChild` sind dort
am einfachsten, und eine `…-table`-Komponente würde nur Inputs/Outputs durchreichen. Regel im Skill: Eine Page darf
eigenes Markup haben, solange es keinen eigenen Zustand und kein Formular hat; ausgelagert wird, was eigenen
UI-Zustand hat, wiederverwendet wird oder Fach-Fallunterscheidungen enthält (wie der Header in V6).

Skill-Abschnitt: „Smart component → Template“.

## Plan

Jede Phase ist für sich lauffähig (Build, Lint, Tests grün) und kann einzeln committet werden.

### P0 – Regeln festlegen

- [x] D1–D3 entscheiden
- [ ] Skill `component` fertigstellen (`schaffbar-portal/.claude/skills/component/SKILL.md`)
- [ ] CLAUDE.md: kurzer Abschnitt „Components“ mit Verweis auf den Skill (wie bei „Dialogs“)
- [ ] TODO.md: „Introduce ViewModel for all components“ präzisieren – „ViewModel in der Komponente für jede dumb
      component, außer sie braucht nur Ids / einfache Werte“

### P1 – Verstöße beheben und per ESLint absichern

- [x] V1 `tool-detail-info`: `existingTools = input.required<ExistingToolViewModel[]>()` statt `ToolsStore`;
      `validateUniqueToolName` auf `() => { id: string; name: string }[]`
- [x] V2 `new-tool-form`: `existingTools` über `MAT_DIALOG_DATA` statt `ToolsStore`
- [x] V3 `ToolDetailStore.assignableRfidReaders`; „bereits zugewiesen“ als `isAssignedToOtherTool` im ViewModel
      (Mapping in `tool-detail`)
- [ ] V4 `ToolDetailStore.certifiableUsers` + `certifiers`; `tool-certifications` filtert nur noch nach Suchtext
- [ ] V5 `user-detail-info` auflösen, `user-detail` rendert `user-contact` + `user-address` direkt
- [ ] ESLint-Block für `src/app/*/components/**` und `src/app/shared/components/**`: `no-restricted-imports` für
      `**/*.store`, `Router` aus `@angular/router`, `MatDialog` aus `@angular/material/dialog`, `MatSnackBar`,
      `@angular/common/http`. **Achtung:** In Flat Config ersetzt ein späterer Block dieselbe Regel – der neue Block
      muss das bestehende `shared/api`-Pattern wiederholen.

### P2 – ViewModels ohne Mapper (12 Komponenten)

Pro Komponente: ViewModel in der Komponente, Input ggf. umbenennen, Spec auf ViewModel-Literale. Die Pages ändern
sich höchstens beim Input-Namen. Feature für Feature, damit die Commits klein bleiben:

- [ ] users: `user-contact`, `user-address`, `tool-usage-summary`, `workshop-usage-summary`, `update-user-form`
- [ ] tools: ~~`tool-detail-info`~~ (mit V1 erledigt), ~~`tool-rfid-reader-assignment`~~ (mit V3 erledigt),
      `tool-wlan-relais`, ~~`new-tool-form`~~ (mit V2 erledigt)
- [ ] rfid-readers: `rfid-reader-detail-info`, `change-rfid-reader-type-form`
- [ ] dashboard: `active-users-widget`

### P3 – ViewModels mit Mapping in der Page (4 Komponenten)

- [ ] `tool-certifications`: `ToolCertificationRowViewModel` (mit `userName`), Mapping in `tool-detail`
- [ ] `tool-instructors`: `InstructorViewModel` für Ausbilder und Kandidaten, Mapping in `tool-detail`
- [ ] `user-tool-certifications`: `UserToolCertificationRowViewModel` (mit `toolName`), Mapping in `user-detail`
- [ ] `active-tools-widget`: `ToolStatusViewModel`, Mapping in `dashboard`
- [ ] Specs: Mapping in den Page-Specs (was beim Kind ankommt), Komponenten-Specs mit fertigen Zeilen

### P4 – Header aus `user-detail` (V6)

- [ ] dumb `users/components/user-detail-header/` mit `UserDetailHeaderViewModel`, Mapping in `user-detail`,
      Command-Outputs
- [ ] Pipes `ageGroupKey`, `rfidTagAssignmentTypeKey` (CLAUDE.md „Missing values“, Punkt „Not migrated yet“
      streichen)

### P5 – Aufräumen in smart components

- [ ] `tool-detail`: durchgereichte `computed`s entfernen
- [ ] `rfid-reader-detail`: `takeUntilDestroyed` für die Vorab-Prüfung
- [ ] `tool-certifications.onSubmitBatch`: typisierte Command-Variable

### Danach

Neue Komponenten folgen dem Skill; ESLint fängt Store-/Router-Zugriffe in `components/` ab. Was ESLint nicht
sieht (fehlendes ViewModel, Feature-Typen im ViewModel, Fachregeln oder Joins in dumb components), prüft ein Review
gegen die Checkliste im Skill.

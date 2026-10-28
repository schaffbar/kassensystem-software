# Angular v22 Migration – schaffbar-portal

Ziel: `schaffbar-portal` auf aktuelle Angular-v22-Best-Practices bringen (Signal Forms, `linkedSignal`, `httpResource`,
`@switch` mit `@default never`, Signal Queries, Lazy Loading).

Stand der Analyse: Angular 22.2, Angular Material 22.2, NgRx Signals 22.0, Vitest.

## Ausgangslage

Schon modern und bleibt so: Zoneless, Standalone, `inject()`, `input()`/`output()`, Built-in Control Flow
(`@if`/`@for`), `withComponentInputBinding`, NgRx SignalStore, Vitest.

Wichtig zu wissen:

- In v22 ist `OnPush` der Default – alle Komponenten laufen bereits damit. Alles, was im Template gelesen wird,
  sollte ein Signal sein, sonst wird es nicht zuverlässig aktualisiert.
- Angular Material 22.2 unterstützt Signal Forms (`[formField]`) direkt in `matInput`, `mat-select`, Datepicker,
  Checkbox, Chips und Autocomplete inkl. `<mat-error>`.

## Vorgehen

- Jeder Schritt ist für sich lauffähig.
- Nach jedem Schritt: `ng build`, `ng lint`, `ng test` und kurz manuell durchklicken.
  `ng build` ist Pflicht: die `testing`-Konfiguration läuft mit `aot: false`, Template-Typfehler fallen nur im
  Build auf.
- Schritte 1–2 sind risikoarm und machen 3–6 einfacher. 5 und 6 bringen am meisten.
- Bei Signal Forms und `httpResource` immer mit dem kleinsten Feature (RFID-Reader) anfangen, damit das Muster
  steht, bevor es an Users geht.

---

## Schritt 0 – Baseline

- [x] `ng build`, `ng lint`, `ng test` grün
- [ ] Eigenen Branch anlegen

## Schritt 1 – Bugs und Provider aufräumen

Diese Punkte zuerst, weil sie die spätere Migration direkt beeinflussen.

- [x] **Doppelte Store-Instanzen entfernen.** `ToolsStore`, `UsersStore`, `RfidReadersStore`, `RfidTagsStore` sind
      `providedIn: 'root'` _und_ zusätzlich in den `providers` der Listen-Komponenten eingetragen. Folge: zwei
      Instanzen, doppelte Requests, ein in der Liste angelegter User fehlt im Root-`UsersStore` (den das Tool-Detail
      nutzt). Beantwortet auch den Punkt in `TODO.md` („tools store … Is it the same instance?" – nein).
  - [x] [tool-list.component.ts](../schaffbar-portal/src/app/tools/pages/tool-list/tool-list.component.ts)
  - [x] [user-list.component.ts](../schaffbar-portal/src/app/users/pages/user-list/user-list.component.ts) – dort
        zusätzlich `TranslateService` aus `providers` entfernen
  - [x] [rfid-reader-list.component.ts](../schaffbar-portal/src/app/rfid-readers/pages/rfid-reader-list/rfid-reader-list.component.ts)
  - [x] [rfid-tag-list.component.ts](../schaffbar-portal/src/app/rfid-tags/pages/rfid-tag-list/rfid-tag-list.component.ts)
        (Spec anpassen: Mock dann über `TestBed`-`providers` statt `overrideComponent`)
  - [x] [user-detail.component.ts](../schaffbar-portal/src/app/users/pages/user-detail/user-detail.component.ts) –
        `RfidTagAssignmentService` aus `providers` entfernen (ist schon `providedIn: 'root'`)
        Hinweis: Die Listen rufen jetzt im Konstruktor `load…()` auf, damit sie beim Öffnen weiterhin frische Daten
        zeigen (vorher passierte das implizit über die neue Store-Instanz). Wegen `exhaustMap` bleibt es beim ersten
        Besuch bei einem Request.
- [x] **Im selben Schritt: `@ViewChild` → `viewChild()`** in den 4 Listen (`MatPaginator`, `MatSort`).
      Bewusst nicht `.required()`: das `computed` wird schon vor dem ersten Rendern der Tabelle gelesen und würde
      dann werfen. Mit `viewChild()` rechnet es einfach neu, sobald Paginator/Sort da sind.
      Das `dataSource`-`computed` liest heute nicht-reaktive `@ViewChild`-Werte; beim ersten Durchlauf sind
      `sort`/`paginator` `undefined`. Das funktioniert nur, weil die Entities später nochmal emittieren. Sobald die
      Store-Duplikate weg sind (Daten bereits gecacht), kann Sortierung/Paging sonst kaputtgehen.
      Migration: `ng generate @angular/core:signal-queries-migration`
- [x] [workshop-session.service.ts](../schaffbar-portal/src/app/users/shared/services/workshop-session.service.ts)
      `closeWorkshopSession`: `this.httpOptions` wird als **Body** gesendet → `put(url, {}, this.httpOptions)`
- [x] [app.config.ts](../schaffbar-portal/src/app/app.config.ts): `provideCheckNoChangesConfig(...)` nur im
      Dev-Build (z. B. über `environment.production` oder `isDevMode()`)
- [x] [app.config.ts](../schaffbar-portal/src/app/app.config.ts): `withXhr()` entfernen – wird nur für
      Upload-Progress gebraucht, sonst gilt der Fetch-Default
- [ ] Test: Liste → Detail → zurück; Sortieren + Paging funktionieren; nur ein Request pro Store

## Schritt 2 – Automatische Migrationen und Cleanup

- [x] `ng generate @angular/core:self-closing-tag` (`<mat-divider />`, `<schbar-navigation />`, …)
- [x] [navigation.component.ts](../schaffbar-portal/src/app/core/navigation/navigation.component.ts):
      `isHandset$` + `AsyncPipe` → `isHandset = toSignal(...)`; `routerLink="{{ item.route }}"` →
      `[routerLink]="item.route"`
- [x] Ungenutzte Imports entfernen:
  - [x] `MatChipsModule`, `MatTabsModule` in `rfid-reader-detail.component.ts`
  - [x] `MatSlideToggleModule` in `user-detail-info.component.ts`
  - [x] `MatRadioModule`, `MatSelectModule`, `MatCardModule` in `user-address.component.ts`
  - [x] `ReactiveFormsModule` in `user-list.component.ts`
  - [x] `MatInputModule`, `MatDialogModule` in den Listen (sofern kein Eingabefeld / nur `MatDialog`-Service)
  - [x] `router` in `rfid-tag-list.component.ts`, `title` in `app.component.ts`
- [x] Templates: `this.readonly()` → `readonly()`, `@if (!!data.entity)` → `@if (data.entity)`, `{{ '-' }}` → `-`
- [x] `AppComponent`: doppeltes `addLangs/setFallbackLang/use` prüfen – steht schon in `provideTranslateService`
- [x] `console.log` in den Stores entfernen
- [x] `protected` / `readonly` konsequent setzen (z. B. `ToolCertificationsComponent` ist komplett public).
      Regel: Inputs/Outputs `public readonly`, vom eigenen Template gelesen → `protected`, sonst `private`;
      nicht neu zugewiesene Felder `readonly`
- [x] ESLint-Regeln ergänzen (erst `warn`, in Schritt 8 auf `error`) – aktuell 0 Warnungen:
  - `@angular-eslint/prefer-signals`
  - `@angular-eslint/prefer-output-readonly`
  - `@angular-eslint/template/prefer-self-closing-tags`
  - `@angular-eslint/template/prefer-control-flow`

## Schritt 3 – `linkedSignal` statt `effect` zum Synchronisieren

Angular rät davon ab, mit `effect()` Werte in anderen State zu kopieren. Erst ohne Forms-Umbau üben:

- [x] [tool-rfid-reader-assignment.component.ts](../schaffbar-portal/src/app/tools/components/tool-rfid-reader-assignment/tool-rfid-reader-assignment.component.ts):
      `syncSelection = effect(...)` → `selectedRfidReaderId = linkedSignal(() => this.tool().rfidReaderId ?? '')`
- [x] [user-detail.component.ts](../schaffbar-portal/src/app/users/pages/user-detail/user-detail.component.ts):
      `ngOnInit` + `route.queryParams.subscribe` (ohne Cleanup) ersetzen durch
      `tab = input<string>()` (kommt über `withComponentInputBinding`) und
      `selectedTabIndex = linkedSignal(() => (this.tab() === 'open-session' ? 1 : 0))`.
      `OnInit` und `ActivatedRoute` entfallen.
- [x] [tool-certifications.component.ts](../schaffbar-portal/src/app/tools/components/tool-certifications/tool-certifications.component.ts):
      `filteredUsers` / `filteredCertifiers` sind einfache Arrow-Functions → `computed`

- [x] Test: [tool-rfid-reader-assignment.component.spec.ts](../schaffbar-portal/src/app/tools/components/tool-rfid-reader-assignment/tool-rfid-reader-assignment.component.spec.ts)
      (Vorauswahl, Reset bei neuem `tool`-Input, Abbrechen) – mit `MatSelectHarness`

Hinweis: `selectedTabIndex` braucht `linkedSignal<number>(...)`, sonst wird der Typ als `0 | 1` abgeleitet und
`(selectedIndexChange)` passt nicht.

Die übrigen 5 `effect`-Syncs (Formulare) verschwinden in Schritt 5.

## Schritt 4 – `@switch` mit `@default never;`

`@default never;` lässt den Build fehlschlagen, wenn ein Enum-/Union-Wert nicht behandelt wird.

- [x] **RFID-Chips** in [user-detail.component.html](../schaffbar-portal/src/app/users/pages/user-detail/user-detail.component.html):
      die 4 Booleans (`isRfidTagAssigned`, `waitingForAssignment`, `showAssignActions`, `showUnassignAction`) durch
      `@switch (assignment.status)` über alle 4 `RfidTagAssignmentStatus` ersetzen (Muster siehe Anhang C)
- [x] **`CertificationStatusChip`** anlegen (`ng g c shared/components/certification-status-chip`) mit
      `status = input.required<ToolCertificationStatus>()` und `@switch` innen. Einsetzen in
      `tool-certifications.component.html` und `user-tool-certifications.component.html` (ersetzt 2× das Markup mit
      3× `[class.x]`)
  - Warum eine eigene Komponente: in `*matCellDef="let cert"` ist `cert` vom Typ `any` – darauf prüft
    `@default never` nichts.
- [x] Aktionen pro Status in `user-tool-certifications.component.html` (`@if Active` / `@if Paused`, `Revoked`
      fehlt implizit) → `@switch` mit `@case (Revoked) {}` + `@default never;` – ebenfalls in einer typisierten
      Kind-Komponente oder über das Chip-Muster
- [x] Test: vorübergehend einen Enum-Wert ergänzen → Build muss fehlschlagen

Erkenntnisse:

- **`@switch` nie direkt auf einen Signal-Aufruf** (`@switch (status())`): TypeScript grenzt das Ergebnis eines
  Funktionsaufrufs nicht ein, `@default never` schlägt dann immer fehl. Erst `@let current = status();`, dann
  `@switch (current)`. Property-Zugriff auf eine `@let`-Variable (`cert.status`) funktioniert.
- **mat-table-Zeilen sind `any`:** in `user-tool-certifications` stellt
  `asCertification = (row: ToolCertification) => row` + `@let cert = asCertification(row)` den Typ wieder her.
- **Prettier ≥ 3.9 nötig:** ältere Versionen (bis 3.8) brechen bei `@default never;` ab. `prettier` wurde auf
  `^3.9.10` angehoben; die neue Version formatiert sonst nichts um.
- **Chip-Farben per `mat.chips-overrides(...)`**, nicht per `background-color`: `.active { background-color }` hat
  dieselbe Spezifität wie Materials eigene Chip-Regel – das Ergebnis hing von der Reihenfolge der Stylesheets im
  `<head>` ab (bei Tools grün, bei Users weiß). Das Override setzt die Token-Variable direkt am Chip.
- Gegenprobe gemacht: neuer Enum-Wert in `ToolCertificationStatus` und `RfidTagAssignmentStatus` → Build schlägt
  an allen 4 `@switch`-Stellen fehl.
- [ ] **Offene Frage (fachlich):** Bei `WaitingForUnassignment` zeigt der User-Header „Kein RFID-Tag“ und keine
      Aktionen – unverändert vom alten Verhalten übernommen, jetzt aber explizit sichtbar. Gewollt?

## Schritt 5 – Signal Forms

Muster: `form()`, `[formField]`, `submit()`, Fehler über `field().errors()`. `[readonly]` / `[disabled]` dürfen nicht
neben `[formField]` stehen – dafür `readonly()` / `disabled()` im Schema verwenden. `submit()` ersetzt das 8×
duplizierte `if (invalid) { markAllAsTouched(); return; }`.

- [x] **Pilot:** [change-rfid-reader-type-form](../schaffbar-portal/src/app/rfid-readers/components/change-rfid-reader-type-form/change-rfid-reader-type-form.component.ts)
      – 1 Select, `required`. Test: [change-rfid-reader-type-form.component.spec.ts](../schaffbar-portal/src/app/rfid-readers/components/change-rfid-reader-type-form/change-rfid-reader-type-form.component.spec.ts)

  **Festgelegtes Muster (gilt für alle weiteren Formulare):**
  - Model als eigenes `interface …Model` + `private readonly model = signal<…Model>(…)`;
    `protected readonly xyzForm = form(this.model, (schema) => { required(schema.type); … })` – Parameter heißt
    `schema`, nicht `s`
  - Kein `null` im Model: „nichts ausgewählt“ ist `''` (Typ z. B. `RfidReaderType | ''`), `required()` wertet `''`
    als leer
  - Template: `[formField]="xyzForm.type"` statt `formControlName`; kein `[formGroup]`, kein `<form>` nötig
  - Fehler: pro Feld **eine Zeile**, kein `@if` pro Prüfung:
    `<mat-error><schbar-field-error [field]="xyzForm.type" label="<label-key>" /></mat-error>`
    ([FieldErrorComponent](../schaffbar-portal/src/app/shared/components/field-error/field-error.component.ts)).
    Zeigt den ersten Fehler; Standard-Validatoren werden per `kind` über `shared.errors.*` übersetzt
    (`{{field}}` = Label, `{{value}}` = z. B. Mindestlänge). Material zeigt `<mat-error>` nur bei invalid + touched.
  - Fachliche Fehler bringen ihren Übersetzungsschlüssel per `message` mit – im Schema, nicht im Template:
    `validate(...)` → `{ kind: 'nameExists', message: 'tools.dialogs.addTool.errors.nameExists' }`,
    `pattern(schema.ip, IP_PATTERN, { message: '...' })`
  - Speichern: `submit(this.xyzForm, async () => { … })` – markiert alles als touched und ruft die Action nur bei
    gültigem Formular auf. Im Callback per Narrowing statt Cast auf den fachlichen Typ kommen
    (`const { type } = this.model(); if (!type) return;`)
  - Material 22.2 bindet `mat-select`/`matInput` und `<mat-error>` direkt an `[formField]` (Fehlerzustand,
    touched) – kein Zusatzcode nötig
  - Bedingte Felder: `hidden(schema.x, { when: ({ valueOf }) => … })` – ein ausgeblendetes Feld zählt **nicht** zur
    Gültigkeit des Formulars, Validatoren am Feld brauchen also kein eigenes `when`. Im Template
    `@if (!xyzForm.x().hidden())`
  - Formular, das einem Input folgt: `model = linkedSignal(() => toModel(this.input()))` statt `effect` + `reset()`.
    Abbrechen: `this.model.set(toModel(this.input())); this.xyzForm().reset();` – `reset()` setzt nur
    touched/dirty zurück, nicht die Werte. „Geändert?“ als `computed` (Model vs. `toModel(input)`)
  - **Jedes Formular** hat ein `<form [formRoot]="xyzForm" (submit)="onSave()">`; `onSave()` mit
    `submit(this.xyzForm, async () => …)` – keine `submission`-Option in `form()`. `FormRoot` verhindert das native
    Absenden (sonst Seiten-Reload) und Enter im Feld speichert.
    - Speichern-Button `type="submit"`, **alle anderen Buttons im `<form>` `type="button"`** (sonst speichert z. B.
      „Abbrechen“)
    - Button-Reihenfolge: Abbrechen, dann Speichern (bestätigende Aktion rechts)
    - Dialog: `<form>` umschließt `mat-dialog-content` und `mat-dialog-actions`, im SCSS `form { display: contents; }`
      (sonst bricht das Dialog-Layout); kein `cdkFocusInitial` auf Abbrechen – der Dialog fokussiert das erste Feld
  - Dialog-Ergebnis (Regel in [schaffbar-portal/CLAUDE.md](../schaffbar-portal/CLAUDE.md)): Abbrechen
    `[mat-dialog-close]="undefined"`, Speichern `this.dialogRef.close(result)` mit
    `inject(MatDialogRef<Comp, Result>)`; der Aufrufer öffnet typisiert `dialog.open<Comp, Data, Result>(…)` – keine
    handannotierten Typen im `subscribe`
  - Reine Anzeigefelder (nie bearbeitbar, z. B. MAC-Adresse, abgeleitete Befehle) bekommen die globale Klasse
    `readonly-field` (`src/styles.scss`). `[hideRequiredMarker]="true"` nur bei Formularen mit Lesemodus, nicht bei
    immer bearbeitbaren (sonst fehlt das Pflichtfeld-Sternchen)
  - Kein `[placeholder]`, der nur das `<mat-label>` wiederholt; `floatLabel="always"` statt `[floatLabel]="'always'"`
  - Inline-Bearbeitung: `isEditing = signal(false)` + **ein** `readonly(schema, { when: () => !this.isEditing() })`
    auf der Wurzel – `readonly` vererbt sich auf alle Felder (`parent.readonly() || eigene Regel`, durch die Tests
    der Inline-Formulare abgesichert); kein `[readonly]` im Template
  - Beim Wechsel in den Bearbeiten-Modus den Fokus ins erste Feld setzen:
    `this.xyzForm.firstField().focusBoundControl()` – der Bearbeiten-Button wird deaktiviert und verliert den Fokus,
    ohne Fokus im Formular funktioniert Enter nicht
  - Tests mit Harnesses: `MatSelectHarness`, `MatButtonHarness`, `MatFormFieldHarness.getTextErrors()`

- [x] [new-tool-form](../schaffbar-portal/src/app/tools/components/new-tool-form/new-tool-form.component.ts) –
      `nameExistsValidator` → `validate(s.name, ...)` (reagiert auf `toolsStore.entities()`); Casts
      `'' as ToolArea | ''` und `formValue.name!` entfallen
      Custom-Fehler: `return exists ? { kind: 'nameExists' } : undefined;` (nie `null`), im Template
      `getError('nameExists')`. Test: [new-tool-form.component.spec.ts](../schaffbar-portal/src/app/tools/components/new-tool-form/new-tool-form.component.spec.ts)
      (u. a. Re-Validierung, wenn sich die Tool-Liste ändert)
- [x] [tool-wlan-relais](../schaffbar-portal/src/app/tools/components/tool-wlan-relais/tool-wlan-relais.component.ts)
      – `hidden(schema.ipAddress, { when: … })` + `required` + `pattern(…, { message })`; `toSignal(valueChanges)`
      und `effect` entfallen, `isDirty` ist ein `computed`. Test: [tool-wlan-relais.component.spec.ts](../schaffbar-portal/src/app/tools/components/tool-wlan-relais/tool-wlan-relais.component.spec.ts)
- [x] Inline-Edit-Formulare – gleiches Muster 4× (`linkedSignal`-Model + `readonly(when)` + `isEditing`,
      Anhang A). Signal `readonly` dabei in `isEditing` umbenennen (invertiert), damit es nicht mit `readonly()` aus
      Signal Forms kollidiert.
  - [x] [rfid-reader-detail-info](../schaffbar-portal/src/app/rfid-readers/components/rfid-reader-detail-info/rfid-reader-detail-info.component.ts)
        – MAC-Adresse nicht mehr im Formular (nur Anzeige). Test: [rfid-reader-detail-info.component.spec.ts](../schaffbar-portal/src/app/rfid-readers/components/rfid-reader-detail-info/rfid-reader-detail-info.component.spec.ts)
  - [x] [tool-detail-info](../schaffbar-portal/src/app/tools/components/tool-detail-info/tool-detail-info.component.ts)
        – neu `required(name)` (Backend verlangt `@NotBlank`, bisher scheiterte ein leerer Name still mit 400).
        Neu auch Namens-Eindeutigkeit beim Umbenennen (Backend wirft `ToolNameAlreadyUsedException`): gemeinsame
        Regel [validateUniqueToolName](../schaffbar-portal/src/app/tools/tool.validators.ts) für Anlegen und Bearbeiten,
        das eigene Tool ist ausgenommen.
        Test: [tool-detail-info.component.spec.ts](../schaffbar-portal/src/app/tools/components/tool-detail-info/tool-detail-info.component.spec.ts)
  - [x] [user-contact](../schaffbar-portal/src/app/users/components/user-contact/user-contact.component.ts)
        (dabei `email()`-Validator ergänzen – fehlt heute, das Template prüft aber `hasError('email')`)
  - [x] [user-address](../schaffbar-portal/src/app/users/components/user-address/user-address.component.ts)
        – PLZ: `minLength`/`maxLength` im Schema setzen über `[formField]` auch das native `maxlength`; das statische
        `maxlength="5"` und die Template-Referenz für den Zähler entfallen. Neuer Typ `UserContact` im User-Model
- [x] [update-user-form](../schaffbar-portal/src/app/users/components/update-user-form/update-user-form.component.ts)
      (Datepicker, Checkbox)
- [x] Datepicker: `@angular/material-date-fns-adapter` + `date-fns` statt `provideNativeDateAdapter()`
      ([provideDateAdapter](../schaffbar-portal/src/app/shared/date/provide-date-adapter.ts), global in `app.config.ts`).
      Grund: `[formField]` überschreibt das statische `readonly` am Datumsfeld, getippte deutsche Daten konnte der
      native Adapter nicht parsen. Jetzt geht Tippen („31.1.1990“) und Picker. Gemeinsame Regeln
      `validateDateOfBirth` / `validatePostalCode` in [user.validators.ts](../schaffbar-portal/src/app/users/shared/validators/user.validators.ts)
- [x] [new-user-form](../schaffbar-portal/src/app/users/components/new-user-form/new-user-form.component.ts) –
      typisiertes Ergebnis statt `form.value` als `User`; `minLength`/`maxLength` für PLZ, `email()`
- [x] Suchfeld in [user-list](../schaffbar-portal/src/app/users/pages/user-list/user-list.component.html):
      `filter = signal('')`, im `dataSource`-`computed` setzen. Behebt nebenbei, dass `applyFilter` das
      `computed`-Objekt mutiert und der Filter beim Reload verloren geht (TODO im Template)
- [x] [tool-rfid-reader-assignment](../schaffbar-portal/src/app/tools/components/tool-rfid-reader-assignment/tool-rfid-reader-assignment.component.ts):
      `ngModel` → Signal Form mit einem Feld (oder beim `linkedSignal` aus Schritt 3 bleiben)
      Umgesetzt: Signal Form statt `ngModel`; „bereits zugewiesen“ als `validate()` mit `message`;
      `ChangeRfidReaderCommand.rfidReaderId` jetzt Pflicht (sonst URL `…/rfid-reader/undefined`)
- [x] Abschluss: `grep -r "ReactiveFormsModule\|FormsModule\|FormBuilder" src` → keine Treffer mehr
- [x] Commands nach der Regel in [schaffbar-portal/CLAUDE.md](../schaffbar-portal/CLAUDE.md) („Commands“) für die
      noch nicht migrierten Komponenten (bisher Inline-Typen bzw. `string`); Command im `*.model.ts`, Store- und
      Service-Methode nehmen denselben Typ:
  - [x] `tool-instructors`: `AddToolInstructorsCommand`, `RemoveToolInstructorsCommand`
  - [x] `tool-certifications` / `user-tool-certifications`: `DeleteToolCertificationCommand`,
        `PauseToolCertificationCommand`, `ReactivateToolCertificationCommand`, `RevokeToolCertificationCommand`
  - [x] `tool-rfid-reader-assignment`: `ClearRfidReaderCommand` statt `string`
  - [x] `new-user-form` / `update-user-form`: `CreateUserCommand` / `UpdateUserCommand` statt Formularwert
  - [x] außerdem `user-detail`: `AssignRfidTagCommand`, `UnassignRfidTagCommand`, `CloseWorkshopSessionCommand`
  - [x] Löschen in den Listen: `DeleteToolCommand`, `DeleteUserCommand`, `DeleteRfidReaderCommand`,
        `DeleteRfidTagCommand` (statt `string`) – damit nehmen alle Mutationen einen Command
- [x] Layout-Klassen vereinheitlichen: `.row`, `.col`, `.full-width` (und `.actions`/`.wlan-relais-actions`) sind in
      jedem Formular-SCSS kopiert, mit abweichenden Werten (`.row` mal `margin-top: 10px`, mal `gap: 1rem` +
      `margin-bottom`; `.col` mal mit, mal ohne `margin-right: 20px`; im WLAN-Relais `gap` **und** `margin-right` →
      doppelter Abstand). Nach der Migration aller Formulare einmal in die globalen Styles ziehen
      Umgesetzt: globale Klassen `form-row` / `form-col` / `form-actions` / `full-width` in `src/styles.scss`
      (Spalten 20px, Zeilen 0.5rem, Buttonleiste 1rem); Kopien aus 12 Komponenten-SCSS entfernt
- [x] Alte Fehler-Übersetzungen entfernen, sobald kein Template sie mehr nutzt: `shared.dialogs.errors.*`,
      `users.dialogs.addNewUser.errors.*` (in `de.json` und `en.json`); `tools.pages.toolDetails.errors.invalidIpAddress`
      und `tools.dialogs.addTool.errors.nameExists` bleiben (werden per `message` genutzt)

## Schritt 6 – `httpResource` statt Dirty-Flag-Muster

Die 3 Detail-Stores bauen von Hand nach, was `httpResource` liefert:
`setId` → `dirty = true` → `effect` in `onInit` → `rxMethod` laden → `dirty = false`.

Mit `httpResource` gibt es `value()`, `isLoading()`, `error()`, `hasValue()` und `reload()` geschenkt;
`dirty`, `setDirty`, `reloadX`, `loadSelectedX` und der `effect` fallen weg. Mutationen (PUT/POST/DELETE) bleiben
Methoden und rufen danach `reload()` auf. Muster siehe Anhang B.

- [x] **Pilot:** [rfid-reader-detail.store.ts](../schaffbar-portal/src/app/rfid-readers/pages/rfid-reader-detail/rfid-reader-detail.store.ts)

  **Festgelegtes Muster (gilt für die weiteren Detail-Stores):**
  - Entscheidung: Resource **im** SignalStore (nicht in der Page) – gleicher Aufbau für alle Detail-Stores,
    `UserDetailStore` wird ohnehin von Page und Kind-Komponente geteilt; öffentliche Store-API bleibt gleich
  - Service liefert die Resource: `xyzResource(id: () => string | null): HttpResourceRef<Xyz | undefined>` mit
    `httpResource(() => id() ? url : undefined)` – kein Request ohne id. Ersetzt `getXyz(id)`
  - Store: `withState({ xyzId })` + `withProps(store => ({ _xyzResource: service.xyzResource(store.xyzId) }))`;
    `withComputed`: `xyz` (`hasValue() ? value() : null` – bleibt beim Reload stehen, kein Flackern), `isLoading`,
    `hasLoadError`. `dirty`, `setDirty`, `loadSelectedXyz` und der `effect` in `onInit` entfallen
  - Mutationen bleiben `rxMethod` mit `withRequestStatus()` und rufen bei Erfolg `_xyzResource.reload()` auf
  - Page: `@if (store.isLoading()) <mat-progress-bar>` und `@if (store.hasLoadError())` →
    `shared.errors.loadFailed`
  - Tests mit `HttpTestingController`: vor `expectOne` nur `TestBed.tick()` (löst den Request aus), erst nach
    `flush` `await appRef.whenStable()` – eine laufende Resource gilt als offene Aufgabe, `whenStable()` davor hängt
    Test: [rfid-reader-detail.store.spec.ts](../schaffbar-portal/src/app/rfid-readers/pages/rfid-reader-detail/rfid-reader-detail.store.spec.ts)

- [x] Lade-/Fehlerzustand im Template anzeigen (`isLoading()` → Spinner, `error()` → Meldung/Snackbar). Heute
      werden `error`/`isPending` aus `withRequestStatus` nirgends angezeigt (vgl. `TODO.md`: „handle API errors",
      „Action feedback - Snackbar?")
      Lade-/Ladefehler-Zustand: beim Piloten umgesetzt, weitere Stores folgen. **Offen:** Fehler bei Mutationen
      (Speichern, Löschen …) werden weiterhin nicht angezeigt – Vorschlag: zentraler HTTP-Interceptor mit Snackbar
- [x] [tool-detail.store.ts](../schaffbar-portal/src/app/tools/pages/tool-detail/tool-detail.store.ts)
      (2 Resources: Tool, Zertifizierungen). Mutationen laden gezielt nur die betroffene Resource neu (vorher hat das
      Dirty-Flag immer beides geladen). Test: [tool-detail.store.spec.ts](../schaffbar-portal/src/app/tools/pages/tool-detail/tool-detail.store.spec.ts)
- [x] [user-detail.store.ts](../schaffbar-portal/src/app/users/shared/stores/user-detail.store.ts)
      (4 Resources: User, RFID-Assignment, Session, Zertifizierungen). RFID-Zuweisung per `parse` (erstes Element
      der Liste oder `null`); offene Session: Backend antwortet ohne Session mit 200 + leerem Body → `null`, kein
      Ladefehler. **Behobener Fehler:** `assignFixedRfidTag(this.id)`, `unassignRfidTag(this.id)`,
      `closeSession(this.userId)` übergaben das _Signal_ an eine `rxMethod` – die hätte bei jeder späteren Änderung
      der id erneut ausgelöst. Jetzt Commands mit Werten. Test: [user-detail.store.spec.ts](../schaffbar-portal/src/app/users/shared/stores/user-detail.store.spec.ts)
- [x] Mutationen vereinheitlicht: [mutation()](../schaffbar-portal/src/app/shared/state/mutation.ts) –
      `mutation<Command>({ request, onSuccess, onError? })` mit eigenem `isPending` je Aktion; 26 Methoden in 7 Stores
      umgestellt (−300 Zeilen). `withRequestStatus` bleibt als Beispiel für ein Store-Feature (nur noch `loadAll…`
      der Listen-Stores). Beispiel `isPending`: „Tool hinzufügen“ ist während des Anlegens deaktiviert
- [x] Fehler-Interceptor mit Snackbar: [error.interceptor.ts](../schaffbar-portal/src/app/core/http/error.interceptor.ts)
- [x] Dashboard (2 Resources: aktive User, aktive Tools); `activeUsers()` statt `entities()`
- [x] `getDuration()` im Dashboard-Template durch ein `now`-Signal (`toSignal(interval(60s))`) + `computed` ersetzt –
      die Dauer zählt jetzt minütlich weiter. Test: [active-users-widget.component.spec.ts](../schaffbar-portal/src/app/dashboard/components/active-users-widget/active-users-widget.component.spec.ts)
- [x] Listen-Stores (`ToolsStore`, `UsersStore`, `RfidReadersStore`, `RfidTagsStore`): Resource statt `withEntities` +
      `rxMethod` + `onInit`; `loadAll…()` → `reload…()`; Listen mit Fortschrittsbalken und Ladefehler-Hinweis.
      `withRequestStatus` wird danach nirgends mehr benutzt (bleibt als Beispiel). Test: [tools.store.spec.ts](../schaffbar-portal/src/app/tools/tools.store.spec.ts)

Entscheidung offen: Resource im SignalStore halten (`withProps(() => ({ _reader: httpResource(...) }))`) oder den
Detail-Store ganz auflösen und die Resources in der Page-Komponente halten (bei einem Store pro Seite einfacher).

## Schritt 7 – Routing und HTTP

- [x] [app.routes.ts](../schaffbar-portal/src/app/app.routes.ts): Lazy Loading per `loadComponent` bzw.
      `loadChildren` pro Feature – alle Routen per `loadComponent` (Migration `route-lazy-loading`); Initial-Bundle
      1,29 MB → 824 kB
- [x] `title` pro Route setzen – Übersetzungsschlüssel, `TranslatedTitleStrategy` zeigt „Werkzeuge · Schaffbar“
- [x] Menü in `navigation.component.ts` nutzt `ROUTE`-Konstanten statt Strings; ungenutztes `ROUTE.ABOUT` entfernen
- [x] Query-Strings per `{ params: { ... } }` statt String-Verkettung (`?toolId=${id}`, `?filter=${filter}`,
      `?customer-id=${userId}`, `?rfidReaderId=...`) – größtenteils schon durch die Resources (Schritt 6),
      `getToolsByRfidReader` per `params`, `getUsersByFilter` gelöscht
- [x] `Content-Type: application/json`-Header in den Services entfernen (setzt `HttpClient` selbst); Authorization
      später als Interceptor
- [x] `readonly http` → `private readonly http`
- [x] `UsersService`: `DatePipe` → `format()` aus `date-fns` (vorgezogen: der injizierte `DatePipe` brach die
      Store-Tests); `DatePipe` und `TranslatePipe` aus den Root-Providern in `app.config.ts` entfernt
- [x] `RfidTagAssignmentService`: Tippfehler `getRfidTagAssignmetByUser`; Rückgabetyp
      `Observable<RfidTagAssignment | null>` – erledigt durch `rfidTagAssignmentResource` (Schritt 6)
- [x] doppelten `ConfirmDelete`-Dialogcode (8×) in einen Helper/Service ziehen
      (`confirm(data): Observable<boolean>`) und dabei typisiert öffnen:
      `dialog.open<ConfirmDeleteComponent, ConfirmDeleteDialogData, boolean>` – `ConfirmDialogService`, 9 Aufrufer;
      Titel/Text sind jetzt Übersetzungsschlüssel, der Dialog übersetzt selbst
- [x] Optional: Material-Buttons vereinheitlichen (`mat-icon-button` vs. `matIconButton`; `color="primary|warn"`
      hat im M3-Theme `azure-blue` kaum Wirkung) – überall `matButton="filled|outlined"` / `matIconButton`; `color`
      entfernt, `warn` → globale Klasse `danger-button`; Eckenradius per `mat.button-overrides` statt `!important`
- [x] `@Injectable({ providedIn: 'root' })` → `@Service()` (neu in Angular 22) für alle 10 Services inkl.
      `ConfirmDialogService` und `TranslatedTitleStrategy`

## Schritt 8 – Abschluss

- [x] `ng build --configuration production`, Bundle-Größe vor/nach Lazy Loading vergleichen:

  |                       | vorher (raw / transfer) | nachher (raw / transfer)                   |
  | --------------------- | ----------------------- | ------------------------------------------ |
  | Initial-Bundle        | 1,29 MB / 235 kB        | 824 kB / 183 kB                            |
  | Lazy Chunks pro Seite | –                       | z. B. Tool-Detail 57 kB, User-Detail 39 kB |

- [x] ESLint-Regeln aus Schritt 2 auf `error` (`prefer-signals`, `prefer-output-readonly`,
      `template/prefer-self-closing-tags`, `template/prefer-control-flow`) – Lint ist sauber
- [x] Specs für die umgebauten Teile: alle Formulare, alle Stores (Listen, Details, Dashboard), `mutation()`,
      Fehler-Interceptor, `TranslatedTitleStrategy`, `ConfirmDialogService` + Dialog, `FieldError`, Status-Chip,
      Dashboard-Dauer – 145 Tests (vorher 1 Spec-Datei).
      Ohne Spec bleiben reine Darstellungs- und Seitenkomponenten (Navigation, Listen-/Detailseiten,
      Zertifizierungs- und Ausbilder-Tabellen, Usage-Summaries) – Kandidaten für später
- [x] `TODO.md`: „angular 22 - migrate to new forms“ und „dashboard store - withEntities vs withState“ entfernt
      (erledigt bzw. durch `httpResource` überholt)

Offene Punkte, die bei der Migration aufgefallen sind (stehen in `TODO.md` bzw. hier):

- `WaitingForUnassignment` zeigt im User-Header „Kein RFID-Tag“ – gewollt? (TODO.md)
- Fachliche Backend-Fehler kommen als `500` – Problems API (TODO.md, Gruppe „Fehlerbehandlung“)
- Erfolgsmeldungen (Snackbar nach dem Speichern) fehlen noch (TODO.md)
- ~~Benennung: `RfidReaderStore` / `RfidTagStore` (Singular) vs. `ToolsStore` / `UsersStore` (Plural)~~ – umbenannt in
  `RfidReadersStore` / `RfidTagsStore`

---

## Anhang A – Muster: Inline-Edit-Formular mit Signal Forms

Vollständig: [user-contact.component.ts](../schaffbar-portal/src/app/users/components/user-contact/user-contact.component.ts)

```ts
export class UserContactComponent {
  readonly user = input.required<User>();
  readonly contactChanged = output<UpdateUserContactCommand>();

  protected readonly isEditing = signal(false);

  // follows the user input
  private readonly model = linkedSignal(() => toModel(this.user()));

  protected readonly contactForm = form(this.model, (schema) => {
    // readonly is inherited by all fields
    readonly(schema, { when: () => !this.isEditing() });
    required(schema.email);
    email(schema.email);
  });

  protected onEdit(): void {
    this.isEditing.set(true);
    // the edit button gets disabled, so move the focus into the form – this also makes Enter submit the form
    this.contactForm.email().focusBoundControl();
  }

  protected onCancel(): void {
    this.model.set(toModel(this.user()));
    this.contactForm().reset();
    this.isEditing.set(false);
  }

  protected onSave(): void {
    submit(this.contactForm, async () => {
      const { email, phone } = this.model();
      const command: UpdateUserContactCommand = {
        id: this.user().id,
        email,
        phone,
      };
      this.contactChanged.emit(command);
      this.isEditing.set(false);
    });
  }
}

function toModel(user: User): ContactModel {
  return { email: user.email, phone: user.phone ?? "" };
}
```

```html
<form [formRoot]="contactForm" (submit)="onSave()">
  <mat-form-field class="full-width">
    <mat-label>{{ 'users.shared.userProperties.email' | translate }}</mat-label>
    <input matInput [formField]="contactForm.email" />
    <mat-error>
      <schbar-field-error
        [field]="contactForm.email"
        label="users.shared.userProperties.email"
      />
    </mat-error>
  </mat-form-field>

  @if (isEditing()) {
  <div class="form-actions">
    <button matButton="outlined" type="button" (click)="onCancel()">…</button>
    <button matButton="filled" type="submit">…</button>
  </div>
  }
</form>
```

## Anhang B – Muster: Detailseite mit `httpResource` im Store

Vollständig: [tool-detail.store.ts](../schaffbar-portal/src/app/tools/pages/tool-detail/tool-detail.store.ts),
Service-Regeln im Skill [api-service](../schaffbar-portal/.claude/skills/api-service/SKILL.md)

```ts
// Service
toolResource(id: () => string | null): HttpResourceRef<Tool | undefined> {
  return httpResource<Tool>(() => {
    const toolId = id();
    return toolId ? `${TOOLS_API_URL}/${toolId}` : undefined;
  });
}

// Store (in den providers der Seite)
export const ToolDetailStore = signalStore(
  withState<ToolDetailState>({ toolId: null }),
  withProps((store) => {
    const toolsService = inject(ToolsService);
    return {
      _toolsService: toolsService,
      // (re)load whenever the id changes
      _toolResource: toolsService.toolResource(store.toolId),
    };
  }),
  withComputed(({ _toolResource }) => ({
    tool: computed(() => (_toolResource.hasValue() ? _toolResource.value() : null)),
    isLoading: computed(() => _toolResource.isLoading()),
    hasLoadError: computed(() => !!_toolResource.error()),
  })),
  withMethods((store) => ({
    setToolId: signalMethod<string>((toolId) => patchState(store, { toolId })),
    updateTool: mutation<UpdateToolCommand>({
      request: (command) => store._toolsService.updateTool(command),
      onSuccess: () => store._toolResource.reload(),
    }),
  })),
);

// Seite
readonly id = input.required<string>();
constructor() {
  this.detailsStore.setToolId(this.id);
}
```

```html
@if (detailsStore.isLoading()) {
<mat-progress-bar mode="indeterminate" />
} @if (detailsStore.hasLoadError()) {
<p class="load-error">{{ 'shared.errors.loadFailed' | translate }}</p>
} @if (detailsStore.tool(); as tool) {
<schbar-tool-detail-info [tool]="tool" (toolUpdated)="onToolUpdated($event)" />
}
```

## Anhang C – Muster: exhaustiver `@switch`

```html
@let assignment = detailsStore.rfidTagAssignment(); @if (assignment) { @switch
(assignment.status) { @case (RfidTagAssignmentStatus.WaitingForAssignment) { ...
} @case (RfidTagAssignmentStatus.Assigned) { ... } @case
(RfidTagAssignmentStatus.WaitingForUnassignment) { ... } @case
(RfidTagAssignmentStatus.Unassigned) { ... } @default never; } } @else {
<mat-chip
  >{{ 'users.chips.userDetails.rfidTags.noRfidTag' | translate }}</mat-chip
>
}
```

Das Enum muss in der Komponente verfügbar sein: `protected readonly RfidTagAssignmentStatus = RfidTagAssignmentStatus;`

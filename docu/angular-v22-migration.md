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

- [x] **Doppelte Store-Instanzen entfernen.** `ToolsStore`, `UsersStore`, `RfidReaderStore`, `RfidTagStore` sind
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

- [ ] **RFID-Chips** in [user-detail.component.html](../schaffbar-portal/src/app/users/pages/user-detail/user-detail.component.html):
      die 4 Booleans (`isRfidTagAssigned`, `waitingForAssignment`, `showAssignActions`, `showUnassignAction`) durch
      `@switch (assignment.status)` über alle 4 `RfidTagAssignmentStatus` ersetzen (Muster siehe Anhang C)
- [ ] **`CertificationStatusChip`** anlegen (`ng g c shared/components/certification-status-chip`) mit
      `status = input.required<ToolCertificationStatus>()` und `@switch` innen. Einsetzen in
      `tool-certifications.component.html` und `user-tool-certifications.component.html` (ersetzt 2× das Markup mit
      3× `[class.x]`)
  - Warum eine eigene Komponente: in `*matCellDef="let cert"` ist `cert` vom Typ `any` – darauf prüft
    `@default never` nichts.
- [ ] Aktionen pro Status in `user-tool-certifications.component.html` (`@if Active` / `@if Paused`, `Revoked`
      fehlt implizit) → `@switch` mit `@case (Revoked) {}` + `@default never;` – ebenfalls in einer typisierten
      Kind-Komponente oder über das Chip-Muster
- [ ] Test: vorübergehend einen Enum-Wert ergänzen → Build muss fehlschlagen

## Schritt 5 – Signal Forms

Muster: `form()`, `[formField]`, `submit()`, Fehler über `field().errors()`. `[readonly]` / `[disabled]` dürfen nicht
neben `[formField]` stehen – dafür `readonly()` / `disabled()` im Schema verwenden. `submit()` ersetzt das 8×
duplizierte `if (invalid) { markAllAsTouched(); return; }`.

- [ ] **Pilot:** [change-rfid-reader-type-form](../schaffbar-portal/src/app/rfid-readers/components/change-rfid-reader-type-form/change-rfid-reader-type-form.component.ts)
      – 1 Select, `required`. Hier das Muster festlegen (Anhang A).
- [ ] [new-tool-form](../schaffbar-portal/src/app/tools/components/new-tool-form/new-tool-form.component.ts) –
      `nameExistsValidator` → `validate(s.name, ...)` (reagiert auf `toolsStore.entities()`); Casts
      `'' as ToolArea | ''` und `formValue.name!` entfallen
- [ ] [tool-wlan-relais](../schaffbar-portal/src/app/tools/components/tool-wlan-relais/tool-wlan-relais.component.ts)
      – `required(s.ipAddress, { when: ... })` + `hidden()` statt manueller Logik in `onSave`; `toSignal(valueChanges)`
      entfällt; `isDirty()` wird ein echtes `computed`
- [ ] Inline-Edit-Formulare – gleiches Muster 4× (`linkedSignal`-Model + `readonly(when)` + `isEditing`,
      Anhang A). Signal `readonly` dabei in `isEditing` umbenennen (invertiert), damit es nicht mit `readonly()` aus
      Signal Forms kollidiert.
  - [ ] [rfid-reader-detail-info](../schaffbar-portal/src/app/rfid-readers/components/rfid-reader-detail-info/rfid-reader-detail-info.component.ts)
  - [ ] [tool-detail-info](../schaffbar-portal/src/app/tools/components/tool-detail-info/tool-detail-info.component.ts)
  - [ ] [user-contact](../schaffbar-portal/src/app/users/components/user-contact/user-contact.component.ts)
        (dabei `email()`-Validator ergänzen – fehlt heute, das Template prüft aber `hasError('email')`)
  - [ ] [user-address](../schaffbar-portal/src/app/users/components/user-address/user-address.component.ts)
- [ ] [update-user-form](../schaffbar-portal/src/app/users/components/update-user-form/update-user-form.component.ts)
      (Datepicker, Checkbox)
- [ ] [new-user-form](../schaffbar-portal/src/app/users/components/new-user-form/new-user-form.component.ts) –
      typisiertes Ergebnis statt `form.value` als `User`; `minLength`/`maxLength` für PLZ, `email()`
- [ ] Suchfeld in [user-list](../schaffbar-portal/src/app/users/pages/user-list/user-list.component.html):
      `filter = signal('')`, im `dataSource`-`computed` setzen. Behebt nebenbei, dass `applyFilter` das
      `computed`-Objekt mutiert und der Filter beim Reload verloren geht (TODO im Template)
- [ ] [tool-rfid-reader-assignment](../schaffbar-portal/src/app/tools/components/tool-rfid-reader-assignment/tool-rfid-reader-assignment.component.ts):
      `ngModel` → Signal Form mit einem Feld (oder beim `linkedSignal` aus Schritt 3 bleiben)
- [ ] Abschluss: `grep -r "ReactiveFormsModule\|FormsModule\|FormBuilder" src` → keine Treffer mehr

## Schritt 6 – `httpResource` statt Dirty-Flag-Muster

Die 3 Detail-Stores bauen von Hand nach, was `httpResource` liefert:
`setId` → `dirty = true` → `effect` in `onInit` → `rxMethod` laden → `dirty = false`.

Mit `httpResource` gibt es `value()`, `isLoading()`, `error()`, `hasValue()` und `reload()` geschenkt;
`dirty`, `setDirty`, `reloadX`, `loadSelectedX` und der `effect` fallen weg. Mutationen (PUT/POST/DELETE) bleiben
Methoden und rufen danach `reload()` auf. Muster siehe Anhang B.

- [ ] **Pilot:** [rfid-reader-detail.store.ts](../schaffbar-portal/src/app/rfid-readers/pages/rfid-reader-detail/rfid-reader-detail.store.ts)
- [ ] Lade-/Fehlerzustand im Template anzeigen (`isLoading()` → Spinner, `error()` → Meldung/Snackbar). Heute
      werden `error`/`isPending` aus `withRequestStatus` nirgends angezeigt (vgl. `TODO.md`: „handle API errors",
      „Action feedback - Snackbar?")
- [ ] [tool-detail.store.ts](../schaffbar-portal/src/app/tools/pages/tool-detail/tool-detail.store.ts)
      (2 Resources: Tool, Zertifizierungen)
- [ ] [user-detail.store.ts](../schaffbar-portal/src/app/users/shared/stores/user-detail.store.ts)
      (4 Resources: User, RFID-Assignment, Session, Zertifizierungen)
- [ ] Optional: Dashboard (2 Resources). Dazu `getDuration()` im Template durch ein `now`-Signal (Intervall) +
      `computed` ersetzen – die Dauer aktualisiert sich heute nie.
- [ ] Optional/später: Listen-Stores. Sie werden geteilt (Dashboard, Tool-Detail, User-Detail) und können vorerst
      NgRx mit `rxMethod` bleiben.

Entscheidung offen: Resource im SignalStore halten (`withProps(() => ({ _reader: httpResource(...) }))`) oder den
Detail-Store ganz auflösen und die Resources in der Page-Komponente halten (bei einem Store pro Seite einfacher).

## Schritt 7 – Routing und HTTP

- [ ] [app.routes.ts](../schaffbar-portal/src/app/app.routes.ts): Lazy Loading per `loadComponent` bzw.
      `loadChildren` pro Feature
- [ ] `title` pro Route setzen
- [ ] Menü in `navigation.component.ts` nutzt `ROUTE`-Konstanten statt Strings; ungenutztes `ROUTE.ABOUT` entfernen
- [ ] Query-Strings per `{ params: { ... } }` statt String-Verkettung (`?toolId=${id}`, `?filter=${filter}`,
      `?customer-id=${userId}`, `?rfidReaderId=...`)
- [ ] `Content-Type: application/json`-Header in den Services entfernen (setzt `HttpClient` selbst); Authorization
      später als Interceptor
- [ ] `readonly http` → `private readonly http`
- [ ] `UsersService`: `DatePipe` → `formatDate()` aus `@angular/common`; danach `DatePipe` und `TranslatePipe` aus
      den Root-Providern in `app.config.ts` entfernen
- [ ] `RfidTagAssignmentService`: Tippfehler `getRfidTagAssignmetByUser`; Rückgabetyp
      `Observable<RfidTagAssignment | null>`
- [ ] Optional: doppelten `ConfirmDelete`-Dialogcode (8×) in einen Helper/Service ziehen
      (`confirm(data): Observable<boolean>`)
- [ ] Optional: Material-Buttons vereinheitlichen (`mat-icon-button` vs. `matIconButton`; `color="primary|warn"`
      hat im M3-Theme `azure-blue` kaum Wirkung)

## Schritt 8 – Abschluss

- [ ] `ng build --configuration production`, Bundle-Größe vor/nach Lazy Loading vergleichen
- [ ] ESLint-Regeln aus Schritt 2 auf `error`
- [ ] Specs für die umgebauten Teile (heute gibt es nur `rfid-tag-list.component.spec.ts`)
- [ ] `TODO.md`: „angular 22 - migrate to new forms" und Store-Instanz-Fragen abhaken

---

## Anhang A – Muster: Inline-Edit-Formular mit Signal Forms

```ts
import { Component, input, linkedSignal, output, signal } from '@angular/core';
import { email, form, FormField, readonly, required, submit } from '@angular/forms/signals';

interface ContactModel {
  email: string;
  phone: string;
}

function toModel(user: User): ContactModel {
  return { email: user.email, phone: user.phone ?? '' };
}

@Component({
  selector: 'schbar-user-contact',
  imports: [FormField, MatInputModule, MatButtonModule, MatIconModule, TranslatePipe],
  // ...
})
export class UserContactComponent {
  readonly user = input.required<User>();
  readonly contactChanged = output<ContactModel>();

  protected readonly isEditing = signal(false);

  // folgt automatisch dem Input – ersetzt effect() + setValue()
  protected readonly model = linkedSignal(() => toModel(this.user()));

  protected readonly contactForm = form(this.model, (s) => {
    required(s.email);
    email(s.email);
    readonly(s.email, { when: () => !this.isEditing() });
    readonly(s.phone, { when: () => !this.isEditing() });
  });

  protected cancel(): void {
    this.model.set(toModel(this.user()));
    this.contactForm().reset();
    this.isEditing.set(false);
  }

  protected save(): void {
    submit(this.contactForm, async () => {
      this.contactChanged.emit(this.model());
      this.isEditing.set(false);
    });
  }
}
```

```html
<mat-form-field>
  <mat-label>{{ 'users.shared.userProperties.email' | translate }}</mat-label>
  <input matInput [formField]="contactForm.email" />
  @for (error of contactForm.email().errors(); track error.kind) {
    <mat-error>{{ 'users.errors.' + error.kind | translate }}</mat-error>
  }
</mat-form-field>
```

## Anhang B – Muster: Detailseite mit `httpResource`

```ts
// Service
rfidReaderResource(id: () => string) {
  return httpResource<RfidReader>(() => `${RFID_READERS_API_URL}/${id()}`);
}

// Page-Komponente (Injection Context: Feld-Initializer)
readonly id = input.required<string>();
protected readonly rfidReader = this.rfidReaderService.rfidReaderResource(this.id);

protected onRfidReaderChanged(command: UpdateRfidReaderCommand): void {
  this.rfidReaderService.updateRfidReader(command).subscribe(() => this.rfidReader.reload());
}
```

```html
@if (rfidReader.isLoading()) {
  <mat-progress-bar mode="indeterminate" />
}
@if (rfidReader.error()) {
  <p>{{ 'shared.errors.loadFailed' | translate }}</p>
}
@if (rfidReader.hasValue()) {
  <schbar-rfid-reader-detail-info [rfidReader]="rfidReader.value()" (readerChanged)="onRfidReaderChanged($event)" />
}
```

## Anhang C – Muster: exhaustiver `@switch`

```html
@let assignment = detailsStore.rfidTagAssignment();
@if (assignment) {
  @switch (assignment.status) {
    @case (RfidTagAssignmentStatus.WaitingForAssignment) { ... }
    @case (RfidTagAssignmentStatus.Assigned) { ... }
    @case (RfidTagAssignmentStatus.WaitingForUnassignment) { ... }
    @case (RfidTagAssignmentStatus.Unassigned) { ... }
    @default never;
  }
} @else {
  <mat-chip>{{ 'users.chips.userDetails.rfidTags.noRfidTag' | translate }}</mat-chip>
}
```

Das Enum muss in der Komponente verfügbar sein: `protected readonly RfidTagAssignmentStatus = RfidTagAssignmentStatus;`

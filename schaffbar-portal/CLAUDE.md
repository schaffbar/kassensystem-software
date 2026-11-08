# Code conventions

## Folders

- `src/app/shared/api/<resource>/` – everything about the backend API, one folder per backend controller:
  `<x>.api-model.ts` (named after the backend `XApiModel.java`: zod schemas, `…ApiModel`, `…Request`) and
  `<resource>.api-service.ts` (`…ApiService`).
- `src/app/<feature>/` – the feature's own model (`<entity>.model.ts`: interfaces and commands), the mapper
  (`<entity>.mapper.ts`: API model → feature model, command → request), the list store (`<entities>.store.ts`),
  validators (`<entity>.validators.ts`), `pages/` (a detail store and the tab components next to their page:
  `pages/<entity>-detail/<entity>-detail.store.ts`, `pages/<entity>-detail/<entity>-detail-tab-<name>/`) and
  `components/`. No `shared/` folder inside a feature.
- A backend resource used by several features gives each feature its **own** model and mapper (on purpose, not a
  shared feature model): `tools/tool-certification.model.ts` and `users/tool-certification.model.ts`,
  each with the commands of its feature.
- `src/app/shared/models/` – only shared vocabulary that API models and features both use: enums (one file per enum,
  e.g. `rfid-reader-type.ts`). No entities, no commands.
- Only stores, mappers and dialog containers import from `shared/api`; pages and pipes use feature models, dumb
  components their own ViewModels (see "Components"; `shared/api` enforced by ESLint).
- A feature imports only from itself and `shared/`; `shared/` imports from no feature (both enforced by ESLint,
  `import/no-restricted-paths` – a new feature folder is added to its list in `eslint.config.js`). Data of another
  resource is loaded by the feature's own store (skill `api`).

## Services

- App-wide services are declared with `@Service()` (Angular 22), not `@Injectable({ providedIn: 'root' })`.
  Dependencies come from `inject()` – `@Service` classes cannot use constructor injection.
- A service that must not be provided automatically (only via a `providers` list) uses
  `@Service({ autoProvided: false })`.
- Signal stores keep `signalStore({ providedIn: 'root' }, …)` – that is NgRx, not a decorator.

## Helper functions vs. private methods

- **Pure functions** (no `this`, no signals, no injected services – the result depends only on the parameters) are
  module-level functions, **not exported**, and placed **after the class**.
  Examples: `toWlanRelaisFormModel(tool)`, `relevantValues(model)` in `tool-wlan-relais.component.ts`,
  `toErrorMessage(error)` in `field-error.component.ts`.
- A mapping function is named `to` + the exact target type, plural for a list – like the mappers of a feature
  (`toTool`, `toUpdateToolRequest`): `toContactFormModel`, `toToolCertificationRowViewModels`.
- **Everything that needs `this`** (component state, signals, forms, services) is a `private` method in the
  `// helper` section at the end of the class.

Why: an unexported function is private to the file (stronger than TypeScript's `private`), shows at a glance that it
has no state, and can be used anywhere without caring about `this`. The class itself only contains what belongs to the
component (inputs, outputs, signals, handlers).

## File sections

Files are divided by section comments – a separator line ending at column 79 (the indentation counts), then the name
of the section:

```ts
export class UserContactComponent {
  …
  // --------------------------------------------------------------------------
  // actions
  …
}

// ----------------------------------------------------------------------------
// mappers
```

- Components, in this order: (no header) injected services, inputs, outputs, signals, form, `computed`, constructor
  → `// actions` (methods the template calls) → `// helper` (`private` methods); after the class `// mappers`
  (`to…` functions) → `// helper` (other pure functions).
- Mappers (`*.mapper.ts`): `// API model → feature model` → `// command → request`.
- API models (`*.api-model.ts`): (no header) schemas and `…ApiModel` types → `// requests`.
- Feature models (`*.model.ts`): (no header) the model → further sections by content (`// WLAN relais` in
  `tool.model.ts`) → `// commands` last.
- API services: `// queries` → `// commands`.
- A section only exists when it has content.

## Commands

- Every action a component triggers for a change in the backend is passed on as a **command** – via `output.emit()`
  or when a dialog closes.
- Commands are defined in the feature's `*.model.ts`, section `// commands`, named verb + entity + `Command`
  (`UpdateToolCommand`, `UpdateUserContactCommand`, `ClearWlanRelaisCommand`).
- **The command contains the id(s)** of the affected entity. The component knows them from its input
  (e.g. `this.user().id`); parent components only pass the command on and never assemble one.
- Component output and store method take the same command type. The store passes it on to the API service as path
  variables + `…Request` (mapper `to…Request`) – no command types in `shared/api` (skill `api`).
- Also for a single id: `ClearWlanRelaisCommand { toolId }`, not `string`.
- The id field is always named after its entity – `toolId`, `rfidReaderId` – never just `id`, also when the command
  changes that entity itself: `UpdateToolCommand { toolId, … }`.
- Form models (`<Name>FormModel`, e.g. `ToolFormModel`) stay local in the component – they describe what is edited,
  the command describes what is sent to the backend.

## Stores

- Data that is displayed is loaded with `httpResource` (created by the service, e.g. `toolResource(id)`) inside the
  signal store. A one-off check at the time of an action (e.g. `getToolsByRfidReader` before changing the reader
  type) stays a plain `HttpClient` observable – it is no state of the page. The store wraps it and returns what the
  component needs (`canChangeRfidReaderType(): Observable<boolean>`), because components never import
  `shared/api`. A dialog container loads its data with
  `httpResource` directly, without a store (see "Dialogs").
- Store data is named after its content – `tools`, `users`, `rfidReaders` – not `entities`.
- A store method is named like its command without `Command`: `UpdateUserContactCommand` → `updateUserContact`,
  `PauseToolCertificationCommand` → `pauseToolCertification`.
- Every backend change is a store method built with `mutation<Command>({ request, onSuccess, onError? })`
  (`src/app/shared/state/mutation.ts`): double-click protection, its own `isPending` (`store.createTool.isPending()`),
  `onSuccess` usually reloads the affected resource. Errors are shown by the error interceptor; the callbacks must not
  throw.
- List stores and detail stores are built the same way: resource(s) in `withProps`, `withComputed` for the data
  (`hasValue() ? value() : …`), `isLoading`, `hasLoadError`, a `reload…()` method and the mutations. No
  `withEntities`, no `rxMethod` for loading, no `onInit` hook.
- `withRequestStatus` (`src/app/shared/state/request-status.feature.ts`) is kept only as an example of a custom
  store feature – it is not used anywhere.

## Subscriptions

- `subscribe()` only on observables that complete by themselves: `dialogRef.afterClosed()`, the `open()` of the
  dialog services, a one-off `HttpClient` call (`canChangeRfidReaderType`). They need no unsubscribe – they end after one
  value.
- Observables that never complete (`interval`, `BreakpointObserver`, `valueChanges`, router events) are never
  subscribed by hand: `toSignal()` in a component, `rxMethod` / `mutation()` in a store – both end with their
  injector.
- `takeUntilDestroyed(this.destroyRef)` when a callback must not run after the page was left – e.g. an HTTP check
  that opens a dialog (`changeRfidReaderType` in `rfid-reader-detail`). Dialogs close themselves on navigation
  (`closeOnNavigation`), their callbacks then get `undefined` / `false` and do nothing.

## Forms

- Every form that saves data has a `<form [formRoot]="xyzForm" (submit)="saveXyz()">`; `saveXyz()` calls
  `submit(this.xyzForm, async () => …)`.
- A field that only filters (search text, autocomplete) is no form: a plain `signal('')` set by `(input)`.
- The submit button names the action: a dialog that creates something says what it creates
  (`users.dialogs.addNewUser.confirm` → "Benutzer hinzufügen"), a dialog that changes something existing says
  "Änderungen speichern" (`shared.actions.saveChanges`), a form on a page "Speichern" (`shared.actions.save`).
  Cancel is always `shared.actions.cancel`.
- Inside such a `<form>` the save button is `type="submit"` and **every other button is `type="button"`** – a button
  without `type` submits the form, so e.g. "cancel" would save.
- The result of `saveXyz()` is built as an explicitly typed variable before it is passed on – for readability, even
  where `dialogRef.close()` / `output.emit()` would infer the type:
  `const command: CreateToolCommand = { … }; this.dialogRef.close(command);`
- Form layout uses the global classes from `src/styles.scss`: `form-row` (a row of fields), `form-col` (columns share
  the width), `form-actions` (button bar, right-aligned), `full-width` – no copies of `.row`/`.col` in component
  styles.
- Form fields that only display a value and are never editable get the global class `readonly-field`
  (`src/styles.scss`).
- A detail page shows its values read-only in a card (`<dl>`, see "Design system") and edits them in a form dialog
  opened by "Bearbeiten" – no inline edit form in the card (`rfid-reader-detail-info` + `update-rfid-reader-form`).
  The remaining inline forms (`user-contact`, `user-address`) are converted with the user page.
- Dates: the Material datepicker uses the date-fns adapter with the German locale (`provideDateAdapter()` in
  `app.config.ts`; tests of components with a datepicker add `provideDateAdapter()` to their providers). Typing
  (`31.1.1990`) and picking both work, so date inputs have no `readonly` attribute – `[formField]` would override it
  anyway. A date field in a form model is `Date | null`; ISO strings from the backend are converted with `parseISO`.

## Missing values

- Fields the backend can send as `null` are typed `| null` in the model (`type: RfidReaderType | null`), so the
  compiler forces the template to handle the case.
- A missing value is displayed with the `orDash` pipe (`src/app/shared/pipes/or-dash.pipe.ts`), not with `?? '-'` or
  `x ? … : '-'` in the template: `{{ usage.unitsUsed | orDash }}`, `{{ usage.endTime | date: 'medium' | orDash }}`.
  Only for display (tables, `readonly-field`) – editable form fields stay empty, otherwise the dash ends up in the
  command.
- The translation key of an enum value is built by a small pure pipe of the feature, not in the template – including
  the `null` case: `{{ reader.type | rfidReaderTypeKey | translate }}` (`rfidReaders.types.switch_box`,
  `rfidReaders.types.none` for `null`). One place for the mapping, testable, and the UI never shows
  `rfidReaders.types.null`.
- Dash or text: a dash when a value simply does not exist (yet), e.g. the end time of a running tool usage. When the
  missing value means someone has to act, show a translated text and highlight it – e.g. a new RFID reader without
  type: `rfidReaders.types.none` with a warning icon in error color, plus a hint in the detail view.

## Buttons

- Material buttons use the attribute syntax of the current API: `matButton="filled"` (main action, e.g. save),
  `matButton="outlined"` (secondary action, e.g. cancel), `matIconButton` – not `mat-flat-button`,
  `mat-stroked-button`, `mat-icon-button`.
- No `color="primary|warn"` – it has no effect in the M3 theme. Destructive actions (delete, revoke, pause, unassign)
  get the global class `danger-button` (`src/styles.scss`), which works for filled, outlined, text and icon buttons.
- Look and feel is changed with `mat.button-overrides(...)` / `mat.icon-button-overrides(...)`, not with
  `.mat-mdc-*` selectors and `!important`.

## Design system

The design itself (colors, type, pages, dialogs): `../docs/ui-redesign.md`.

- Colors only from the tokens in `src/styles.scss` (`--text`, `--text-2`, `--surface`, `--border`, `--accent`,
  `--danger`, …) – no hex values in component styles. Neon green `--accent` only as a surface; green text is
  `--accent-text`.
- Material components get their look from the `mat.*-overrides(...)` in `src/styles.scss`.
- Building blocks in `shared/components/`; their texts are passed **translated**:
  - `schbar-page-header` – `h1`, `subtitle`, `breadcrumbs`, `icon` (navy tile, detail pages); actions are projected,
    status badges and further meta (e.g. the MAC address) with `pageHeaderMeta`.
  - `schbar-status-badge` – a status as icon + text, never by color alone (`tone`: `success`, `info`, `warn`,
    `danger`, `neutral`). An enum gets its own badge that maps it once (`certification-status-badge`,
    `certification-requirement-badge`). Not inside a `mat-option`: `mat-select` takes the icon name into the text.
    Without `icon` only as `neutral` for a plain property, not a status (the type in the header of a reader).
  - `schbar-row-menu` – the ⋯ menu of a table row instead of action buttons; destructive entries come last after a
    `<mat-divider />`, get the global class `danger-menu-item` and ask first like a `danger-button`.
  - `schbar-empty-state` – instead of an empty table or list.
  - `schbar-danger-zone` – last card of a detail page with the destructive action (`danger-button`, asks first).
  - `schbar-filter-chips` – the filters of a list with counts (`[(selected)]` = id of the active filter); the counts
    ignore the search. Next to it the search: `mat-form-field` with the global class `search-field` (44px, no hint).
  - `schbar-toggle-chip` – a single filter that is on or off (`[(pressed)]`, optional icon and count), e.g.
    "Hardware einrichten" in the tool list; it looks like the filter chips (global class `chip`).
  - `schbar-segmented-control` – one choice out of a few next to each other (area, cards / list), optional count and
    icon per segment; filter chips are for a row of filters.
- Choices with an explanation are radio cards, not a `mat-select`: `<mat-radio-button class="radio-card">` with the
  global classes `radio-card-text`, `radio-card-label`, `radio-card-description` (reader type, tool area and
  certification requirement).
- Global classes for the page layout (`src/styles.scss`):
  - `detail-list` on a `<dl>` – the read-only values of a card (label above value, in a wrapping grid).
  - `card` (with `card-header`: `h2` left, actions right) – a section of a detail page; a dumb component that is one
    card sets it as `host: { class: 'card' }`.
  - `table-card` around a list table and its paginator, the table inside `table-scroll` (scrolls on narrow
    screens). The row that opens the detail page is a `clickable-row`; its ⋯ menu offers "Details" as well, because
    a row cannot be reached with the keyboard.
  - `mono` for IDs, MAC and IP addresses; `mono-field` on a `mat-form-field` that shows one.
- Every `matIconButton` has a translated `aria-label` (`shared.actions.reload`, `shared.actions.edit`, …).

## Components

Details, skeletons and tests: skill `component` (`.claude/skills/component/SKILL.md`).

- **Smart** (`<feature>/pages/`): knows where data comes from and where commands go – injects its store(s),
  `Router`, `MatDialog`, dialog services, `MatSnackBar`. Template: layout plus child components; output handlers only
  pass the command on to a store method.
- **Dumb** (`<feature>/components/`, `shared/components/`): only `input()` / `output()` and local UI state (form,
  edit mode, search text). Injects nothing – except `MAT_DIALOG_DATA` / `MatDialogRef` in a form dialog and
  `ConfirmDeleteDialogService` / `ConfirmActionDialogService` for a confirmation. No store, `Router`, `MatDialog`,
  `MatSnackBar`, `HttpClient` (enforced by ESLint).
- Business rules live in the store or the feature model, never in a component. A dumb component gets its data
  decided and combined – no filter by a rule (`assignableRfidReaders`), no join of lists (`userName` in the row).
- **ViewModel**: every dumb component exports its input type from its own file: `<ComponentName>ViewModel`, input
  `<componentName>`, with exactly the fields it reads and no feature model type inside (enums from `shared/models`
  are fine). Further ViewModels (rows, options) are named after their content. Only one or two ids / plain values →
  no ViewModel.
- The page fills the ViewModel: it passes the feature model directly when that has all fields (structural typing,
  checked by `strictTemplates`), otherwise it maps in a `computed` with a pure `to<Name>ViewModel()` after the class.
  The store never imports a ViewModel.
- No pass-through components. A list page may keep its own `mat-table`.
- **Names**: a method called from a template is named for what it does – verb + object, no `on…` prefix, no
  `…Dialog` suffix (`saveContact`, `discardContactChanges`, `deleteTool`, `openToolDetail`). A page or tab method
  that executes a command is named like the store method it calls (`updateUserContact`). An output is the event:
  object + participle (`contactChanged`, `certificationPaused`, `editRequested`).
- **Pages with tabs**: the page is the frame (loading, header, tab group); every tab is a smart tab component
  `<page>-tab-<name>/` in the page's folder that injects the page's store and composes dumb components.

## Dialogs

Details, skeletons and tests: skill `dialog` (`.claude/skills/dialog/SKILL.md`).

- **Shared dialogs** (`src/app/shared/dialogs/<name>-dialog/`) consist of a **modal** (`<name>-dialog-modal`: the
  whole dialog UI with inputs – a view model, loading state – and outputs only, no `MAT_DIALOG_DATA` /
  `[mat-dialog-close]` / `dialogRef`) and a **service** with one method `open(…)`. If the dialog has to load data, a
  **container** (`<name>-dialog-container`) gets the ids as `input.required()`, loads with the resource of the API
  service directly, maps the API model to the view model and closes `dialogRef` on the modal's outputs; otherwise the service connects the modal itself via
  `bindings: [inputBinding(…), outputBinding(…)]`.
- **Form dialogs** (`<feature>/components/<name>-form/`) are opened directly by one page and close themselves:
  `dialogRef.close(command)` on save, `[mat-dialog-close]="undefined"` on cancel (not `mat-dialog-close` without a
  value – that closes with `''`). They look like the prototype: `host: { class: 'form-dialog' }` (lines below the
  header and above the buttons), `<schbar-form-dialog-header [title] [subtitle]>` instead of `mat-dialog-title` (title,
  subtitle naming the entity, close button), compact fields with the label above (`<label class="field-label"
  for="…">` instead of `mat-label`), and the page opens them with `...FORM_DIALOG_CONFIG` (at the top of the window,
  focus on the first field instead of the close button, `disableClose`). A form inside a card (the hardware tab of a tool) gets the same compact fields
  and `field-label` with the global class `compact-form` on the `<form>`. Confirmations stay centered, without header
  line and close button.
- **Options**: `minWidth: DIALOG_WIDTH.small | medium | large` (`src/app/shared/dialogs/dialog-constants.ts`) and
  `disableClose: true`. `MatDialog.open<Component, Data, Result>` is always typed, so `afterClosed()` is
  `Result | undefined` – no hand-written types in `subscribe`.
- **Shared dialogs return yes/no** (or a result type of their own) and import nothing from a feature – no feature
  model, no command. The caller builds the command from its inputs and passes it to its store's `mutation()`; if the
  dialog loaded data itself, the caller reloads also on cancel.
- **Confirmations** (`true`/`false`): deleting goes through `ConfirmDeleteDialogService.open(viewModel)` (always
  `danger-button`), every other action (pause, revoke, clear, …) through `ConfirmActionDialogService.open(viewModel)`
  with `destructive: true | false`.
- **The confirm button names the action as verb + object** (`confirmLabel`, required in both dialogs): "Benutzer
  löschen", "Einweisung pausieren" – not "Löschen" or "OK". Users often read only the button, and similar actions
  (delete vs. revoke a certification) must stay apart. The key is the caller's: `<feature>.dialogs.<action>.confirm`.
- **Every button with `danger-button` asks first** – delete through `ConfirmDeleteDialogService`, everything else
  (unassign, remove, clear, pause, revoke) through `ConfirmActionDialogService` with `destructive: true`. No red
  button acts directly.
- **Buttons**: cancel first (`matButton="outlined"`), then confirm; every button `type="button"` (except submit in a
  form). **Focus**: form dialogs have no `cdkFocusInitial` (the first field gets it; Enter in a text field saves);
  destructive or irreversible confirmations put `cdkFocusInitial` on cancel.

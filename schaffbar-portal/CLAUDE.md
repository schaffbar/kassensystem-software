# Code conventions

## Folders

- `src/app/shared/api/<resource>/` – everything about the backend API, one folder per backend controller:
  `<x>.api-model.ts` (named after the backend `XApiModel.java`: zod schemas, `…ApiModel`, `…Request`) and
  `<resource>.api-service.ts` (`…ApiService`).
- `src/app/<feature>/` – the feature's own model (`<entity>.model.ts`: interfaces and commands), the mapper
  (`<entity>.mapper.ts`: API model → feature model, command → request), the list store (`<entities>.store.ts`),
  validators (`<entity>.validators.ts`), `pages/` (a detail store next to its page:
  `pages/<entity>-detail/<entity>-detail.store.ts`) and `components/`. No `shared/` folder inside a feature.
- A backend resource used by several features gives each feature its **own** model and mapper (on purpose, not a
  shared feature model): `tools/tool-certification.model.ts` and `users/tool-certification.model.ts`,
  each with the commands of its feature.
- `src/app/shared/models/` – only shared vocabulary that API models and features both use: enums (one file per enum,
  e.g. `rfid-reader-type.ts`). No entities, no commands.
- Only stores, mappers and dialog containers import from `shared/api`; components, pages and pipes use feature
  models (enforced by ESLint).
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
  Examples: `toModel(tool)`, `relevantValues(model)` in `tool-wlan-relais.component.ts`,
  `toErrorMessage(error)` in `field-error.component.ts`.
- **Everything that needs `this`** (component state, signals, forms, services) is a `private` method in the
  `// helper` section at the end of the class.

Why: an unexported function is private to the file (stronger than TypeScript's `private`), shows at a glance that it
has no state, and can be used anywhere without caring about `this`. The class itself only contains what belongs to the
component (inputs, outputs, signals, handlers).

## Commands

- Every action a component triggers for a change in the backend is passed on as a **command** – via `output.emit()`
  or when a dialog closes.
- Commands are defined in the feature's `*.model.ts`, section `// commands`, named verb + entity + `Command`
  (`UpdateToolCommand`, `UpdateUserContactCommand`, `ClearWlanRelaisCommand`).
- **The command contains the id(s)** of the affected entity. The component knows them from its input
  (e.g. `this.user().id`); parent components only pass the command on and never assemble one.
- Component output, store method and service method take the same command type.
- Also for a single id: `ClearWlanRelaisCommand { toolId }`, not `string`.
- Form models (`ToolInfoModel`, `ContactModel`, …) stay local in the component – they describe what is edited, the
  command describes what is sent to the backend.

## Stores

- Data that is displayed is loaded with `httpResource` (created by the service, e.g. `toolResource(id)`) inside the
  signal store. A one-off check at the time of an action (e.g. `getToolsByRfidReader` before changing the reader
  type) stays a plain `HttpClient` observable – it is no state of the page. The store wraps it and returns what the
  component needs (`hasAssignedTools(rfidReaderId): Observable<boolean>`), because components never import
  `shared/api`. A dialog container loads its data with
  `httpResource` directly, without a store (see "Dialogs").
- Every backend change is a store method built with `mutation<Command>({ request, onSuccess, onError? })`
  (`src/app/shared/state/mutation.ts`): double-click protection, its own `isPending` (`store.createTool.isPending()`),
  `onSuccess` usually reloads the affected resource. Errors are shown by the error interceptor; the callbacks must not
  throw.
- List stores and detail stores are built the same way: resource(s) in `withProps`, `withComputed` for the data
  (`hasValue() ? value() : …`), `isLoading`, `hasLoadError`, a `reload…()` method and the mutations. No
  `withEntities`, no `rxMethod` for loading, no `onInit` hook.
- `withRequestStatus` (`src/app/shared/state/request-status.feature.ts`) is kept only as an example of a custom
  store feature – it is not used anywhere.

## Forms

- Every form has a `<form [formRoot]="xyzForm" (submit)="onSave()">`; `onSave()` calls
  `submit(this.xyzForm, async () => …)`.
- Inside such a `<form>` the save button is `type="submit"` and **every other button is `type="button"`** – a button
  without `type` submits the form, so e.g. "cancel" would save.
- The result of `onSave()` is built as an explicitly typed variable before it is passed on – for readability, even
  where `dialogRef.close()` / `output.emit()` would infer the type:
  `const command: CreateToolCommand = { … }; this.dialogRef.close(command);`
- Form layout uses the global classes from `src/styles.scss`: `form-row` (a row of fields), `form-col` (columns share
  the width), `form-actions` (button bar, right-aligned), `full-width` – no copies of `.row`/`.col` in component
  styles.
- Form fields that only display a value and are never editable get the global class `readonly-field`
  (`src/styles.scss`).
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
  `rfidReaders.types.null`. Not migrated yet: `'users.chips.userDetails.…' + (… | lowercase)` in `user-detail`.
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
  value – that closes with `''`).
- **Options**: `minWidth: DIALOG_WIDTH.small | medium | large` (`src/app/shared/dialogs/dialog-constants.ts`) and
  `disableClose: true`. `MatDialog.open<Component, Data, Result>` is always typed, so `afterClosed()` is
  `Result | undefined` – no hand-written types in `subscribe`.
- **Shared dialogs return yes/no** (or a result type of their own) and import nothing from a feature – no feature
  model, no command. The caller builds the command from its inputs and passes it to its store's `mutation()`; if the
  dialog loaded data itself, the caller reloads also on cancel.
- **Confirmations** (`true`/`false`): deleting goes through `ConfirmDeleteDialogService.open(viewModel)` (button
  always "delete"), every other action (pause, revoke, clear, …) through `ConfirmActionDialogService.open(viewModel)`
  with its own `confirmLabel` and `destructive: true | false`.
- **Buttons**: cancel first (`matButton="outlined"`), then confirm; every button `type="button"` (except submit in a
  form). **Focus**: form dialogs have no `cdkFocusInitial` (the first field gets it, Enter saves); destructive or
  irreversible confirmations put `cdkFocusInitial` on cancel.

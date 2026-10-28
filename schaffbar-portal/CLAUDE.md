# Code conventions

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
  type) stays a plain `HttpClient` observable – it is no state of the page.
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

## Buttons

- Material buttons use the attribute syntax of the current API: `matButton="filled"` (main action, e.g. save),
  `matButton="outlined"` (secondary action, e.g. cancel), `matIconButton` – not `mat-flat-button`,
  `mat-stroked-button`, `mat-icon-button`.
- No `color="primary|warn"` – it has no effect in the M3 theme. Destructive actions (delete, revoke, pause, unassign)
  get the global class `danger-button` (`src/styles.scss`), which works for filled, outlined, text and icon buttons.
- Look and feel is changed with `mat.button-overrides(...)` / `mat.icon-button-overrides(...)`, not with
  `.mat-mdc-*` selectors and `!important`.

## Dialogs

- **Cancel closes without a result** (`undefined`), like Escape or a backdrop click:
  `<button mat-stroked-button type="button" [mat-dialog-close]="undefined">`.
  Not `mat-dialog-close` without a value – that closes with `''`.
- **Save / confirm closes with the result**: `this.dialogRef.close(command)` in `onSave()`, for confirmation dialogs
  `[mat-dialog-close]="true"`. The dialog component types its ref: `inject(MatDialogRef<MyDialogComponent, Result>)`.
- **The caller types the dialog when opening it**, so `afterClosed()` is checked by the compiler as `Result | undefined`
  – no hand-written types in `subscribe`:

  ```ts
  this.dialog
    .open<NewToolFormComponent, void, CreateToolCommand>(NewToolFormComponent, { … })
    .afterClosed()
    .subscribe((command) => {
      if (command) this.store.createTool(command);
    });
  ```

- **Delete confirmations** go through `ConfirmDialogService.confirm({ title, entity?, message })` – it opens
  `ConfirmDeleteComponent` with the common options and emits `true`/`false`. `title`, `message` (and `confirmLabel`)
  are translation keys, `entity` is plain text. Don't open `ConfirmDeleteComponent` directly.
- **Button order**: cancel first, then save/confirm (confirming action on the right).
- **Focus**: form dialogs have no `cdkFocusInitial` – the dialog focuses the first field, so Enter saves.
  Exception: destructive confirmations (delete) keep `cdkFocusInitial` on cancel, so an accidental Enter does nothing.

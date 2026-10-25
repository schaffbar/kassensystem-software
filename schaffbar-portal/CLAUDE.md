# Code conventions

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

- **Button order**: cancel first, then save/confirm (confirming action on the right).
- **Focus**: form dialogs have no `cdkFocusInitial` – the dialog focuses the first field, so Enter saves.
  Exception: destructive confirmations (delete) keep `cdkFocusInitial` on cancel, so an accidental Enter does nothing.

---
name: dialog
description: How to write or change a dialog in schaffbar-portal (Angular 22, Angular Material) – a shared dialog in `src/app/shared/dialogs/` built as modal (+ container if it loads data) + service, or a form dialog in a feature. Use when adding a new dialog or confirmation, a dialog that has to load data, opening a dialog from a page, changing dialog options, or reviewing or testing a dialog.
---

# Dialogs in schaffbar-portal

There are two kinds of dialogs:

| Kind              | Where                                       | Example                                         |
| ----------------- | ------------------------------------------- | ----------------------------------------------- |
| **shared dialog** | `src/app/shared/dialogs/<name>-dialog/`     | `confirm-delete-dialog`, `close-session-dialog` |
| **form dialog**   | `src/app/<feature>/components/<name>-form/` | `new-tool-form`, `update-user-form`             |

A **shared dialog** is the pattern for every dialog that is not a form: a confirmation, a summary, anything that
can be opened from more than one place. It always consists of a **modal** and a **service**, plus a **container**
when the dialog has to load data itself. A **form dialog** holds a form with its own state and is opened by one page
only – it stays one component (see "Form dialogs" at the end).

Reference implementations:

- [confirm-delete-dialog/](../../../src/app/shared/dialogs/confirm-delete-dialog/) – nothing to load: modal + service
- [close-session-dialog/](../../../src/app/shared/dialogs/close-session-dialog/) – loads data: container + modal +
  service

## Shared dialog: the parts

```
src/app/shared/dialogs/
  dialog-constants.ts                       DIALOG_WIDTH and other dialog constants
  <name>-dialog/                            one folder per dialog
    <name>-dialog-modal.component.{ts,html,scss,spec.ts}
    <name>-dialog-container.component.{ts,html,spec.ts}   only if the dialog loads data
    <name>-dialog.service.{ts,spec.ts}
```

| Part          | Gets                                     | Does                                                                                     | Knows `MatDialog` / `dialogRef`? |
| ------------- | ---------------------------------------- | ---------------------------------------------------------------------------------------- | -------------------------------- |
| **modal**     | inputs: `viewModel`, loading/error state | the whole dialog UI (title, content, buttons), emits outputs                             | no                               |
| **container** | ids as `input.required()`                | loads with `httpResource`, maps to the view model, renders the modal, closes `dialogRef` | `dialogRef` only                 |
| **service**   | what the caller has (ids, texts)         | opens the dialog with the common options, returns the result as `Observable`             | `MatDialog`                      |

Without loading the service plays the container's part: it opens the modal directly and connects its outputs to
`dialogRef` with `outputBinding`.

Decide first: **does the dialog need data the caller doesn't already have, or that must be current at the time of
the action** (e.g. totals that change while the page is open)? → container. Otherwise → modal + service.

## Modal

```ts
export interface ConfirmDeleteDialogViewModel {
  // translation keys
  title: string;
  message: string;
  confirmLabel?: string;
  // plain text
  entity?: string;
}

@Component({
  selector: 'schbar-confirm-delete-dialog-modal',
  templateUrl: './confirm-delete-dialog-modal.component.html',
  imports: [MatButtonModule, MatDialogModule, MatDividerModule, TranslatePipe],
})
export class ConfirmDeleteDialogModalComponent {
  readonly viewModel = input.required<ConfirmDeleteDialogViewModel>();

  readonly confirmed = output<void>();
  readonly cancelled = output<void>();
}
```

```html
@let currentViewModel = viewModel();

<h2 mat-dialog-title>{{ currentViewModel.title | translate }}</h2>

<mat-divider />

<mat-dialog-content>…</mat-dialog-content>

<mat-dialog-actions>
  <button matButton="outlined" type="button" cdkFocusInitial (click)="cancelled.emit()">…</button>
  <button class="danger-button" matButton="filled" type="button" (click)="confirmed.emit()">…</button>
</mat-dialog-actions>
```

Rules:

- Name `<Name>DialogModalComponent`, selector `schbar-<name>-dialog-modal`. The view model is
  `<Name>ViewModel` (or `<Name>DialogViewModel`), exported from the modal file – it is the modal's input type.
- **Inputs and outputs only.** No `MAT_DIALOG_DATA`, no `inject(MatDialogRef)`, no `[mat-dialog-close]`, no service,
  no store. The modal can be rendered and tested like any presentational component.
- The view model is shaped for display, not the API model: only the fields the modal shows, already combined
  (e.g. `workshop: { minutes, units }`). Comment only what the type can't say (translation key vs. plain text,
  what `null` means).
- An action that changes the backend emits a **command** (`closeConfirmed = output<CloseWorkshopSessionCommand>()`).
  The modal builds it from ids in its view model, like every component (CLAUDE.md "Commands"). A plain yes/no emits
  `output<void>()` (`confirmed`). Cancel is always `cancelled = output<void>()`.
- Layout: `mat-dialog-title`, `<mat-divider />`, `mat-dialog-content`, `mat-dialog-actions`. Buttons: cancel first
  (`matButton="outlined"`), then confirm (`matButton="filled"`, `danger-button` for destructive actions), every
  button `type="button"`.
- `cdkFocusInitial` on cancel for destructive or irreversible actions (delete, closing a session) – an accidental
  Enter must not confirm.
- A modal that receives loading state shows a `<mat-spinner diameter="40" />` while loading, an error text
  (`shared.errors.loadFailed`) on a load error, a hint instead of the data when the action is not possible
  (no data, a running usage, …) – and disables confirm in all these cases with a `computed` (`canClose`).

## Container (only if the dialog loads data)

```ts
@Component({
  selector: 'schbar-close-session-dialog-container',
  templateUrl: './close-session-dialog-container.component.html',
  imports: [CloseSessionDialogModalComponent],
})
export class CloseSessionDialogContainerComponent {
  private readonly dialogRef = inject(MatDialogRef<CloseSessionDialogContainerComponent, CloseWorkshopSessionCommand>);

  readonly customerId = input.required<string>();

  private readonly sessionResource = inject(WorkshopSessionService).openSessionResource(this.customerId);

  protected readonly isLoading = computed(() => this.sessionResource.isLoading());
  protected readonly hasLoadError = computed(() => !!this.sessionResource.error());
  protected readonly viewModel = computed(() => {
    const session = this.sessionResource.hasValue() ? this.sessionResource.value() : null;
    return session ? toCloseSessionViewModel(session) : null;
  });

  protected onCloseConfirmed(command: CloseWorkshopSessionCommand): void {
    this.dialogRef.close(command);
  }

  protected onCancelled(): void {
    this.dialogRef.close();
  }
}

function toCloseSessionViewModel(session: WorkshopSession): CloseSessionViewModel { … }
```

```html
<schbar-close-session-dialog-modal
  [isLoading]="isLoading()"
  [hasLoadError]="hasLoadError()"
  [viewModel]="viewModel()"
  (closeConfirmed)="onCloseConfirmed($event)"
  (cancelled)="onCancelled()" />
```

Rules:

- Name `<Name>DialogContainerComponent`, selector `schbar-<name>-dialog-container`. Template = the modal, nothing
  else; no `styleUrl`.
- Gets **only ids**, as `input.required()` – never whole entities (they could be outdated, which is why the dialog
  loads in the first place).
- Loads with the `httpResource` of the feature service (see skill `api-service`) and passes the input signal itself
  as the getter: `openSessionResource(this.customerId)`. The container holds the resource directly, **no store** –
  it is a single resource that lives exactly as long as the dialog. It loads on every open, so the data is current.
- The mapping API model → view model is a pure, unexported function after the class (`to<Name>ViewModel`), see
  CLAUDE.md "Helper functions".
- `hasValue() ? value() : null` – a load error must not throw in the template.
- The container does **not** execute the command – it closes with it. The caller's store executes it.

## Service

Without loading – opens the modal directly and connects inputs and outputs:

```ts
@Service()
export class ConfirmDeleteDialogService {
  private readonly dialog = inject(MatDialog);

  open(viewModel: ConfirmDeleteDialogViewModel): Observable<boolean> {
    const dialogRef: MatDialogRef<ConfirmDeleteDialogModalComponent, boolean> = this.dialog.open<
      ConfirmDeleteDialogModalComponent,
      void,
      boolean
    >(ConfirmDeleteDialogModalComponent, {
      bindings: [
        inputBinding('viewModel', () => viewModel),
        outputBinding('confirmed', () => dialogRef.close(true)),
        outputBinding('cancelled', () => dialogRef.close(false)),
      ],
      minWidth: DIALOG_WIDTH.medium,
      disableClose: true,
    });

    return dialogRef.afterClosed().pipe(map((confirmed) => confirmed === true));
  }
}
```

With loading – opens the container and passes the ids:

```ts
@Service()
export class CloseSessionDialogService {
  private readonly dialog = inject(MatDialog);

  open(customerId: string): Observable<CloseWorkshopSessionCommand | undefined> {
    return this.dialog
      .open<CloseSessionDialogContainerComponent, void, CloseWorkshopSessionCommand>(
        CloseSessionDialogContainerComponent,
        {
          bindings: [inputBinding('customerId', () => customerId)],
          minWidth: DIALOG_WIDTH.medium,
          disableClose: true,
        },
      )
      .afterClosed();
  }
}
```

Rules:

- Name `<Name>DialogService`, `@Service()`, `inject(MatDialog)`. **One method, always called `open`.**
- Inputs and outputs are connected with `bindings` (`inputBinding`, `outputBinding` from `@angular/core`) –
  **no `data` / `MAT_DIALOG_DATA`**.
- Options: `minWidth: DIALOG_WIDTH.small | medium | large` from `dialog-constants.ts` and `disableClose: true` –
  the dialog is closed by its buttons only.
- `open<Component, void, Result>` is typed, so `afterClosed()` is `Observable<Result | undefined>`. A yes/no dialog
  maps `undefined` to `false` (`map((confirmed) => confirmed === true))`).
- The service only opens. It neither loads data nor executes the command.

## Caller

```ts
private readonly closeSessionDialog = inject(CloseSessionDialogService);

protected closeSession(): void {
  this.closeSessionDialog.open(this.userId()).subscribe((command) => {
    if (command) {
      this.detailsStore.closeSession(command);
    } else {
      // the dialog loaded the session itself and may have shown a newer state than the page
      this.detailsStore.reloadOpenSession();
    }
  });
}
```

- The caller passes what it has (ids, texts), subscribes and hands the command to its store's `mutation()`, which
  reloads on success.
- If the dialog loaded data itself, the caller reloads its own data **also on cancel** – the dialog may have shown a
  newer state than the page.
- A button that opens a dialog for an action that is not possible right now is disabled on the page already
  (with a hint), the dialog checks again with current data.

## Testing

Modal – a presentational component, set inputs, subscribe to outputs:

- `fixture.componentRef.setInput('viewModel', …)`, then `await fixture.whenStable()`.
- One test per state (loading, error, no data, action not possible, data) incl. the disabled state of confirm; one
  test per output (`fixture.componentInstance.confirmed.subscribe(spy)`, click, expect).

Container – real `httpResource` with `HttpTestingController`, `MatDialogRef` as `{ close: vi.fn() }`:

- `fixture.componentRef.setInput('customerId', 'user-1')`, then **`fixture.detectChanges()`, not `whenStable()`** –
  `whenStable()` (and every harness) waits for the pending request and hangs. After
  `httpMock.expectOne(url).flush(…)` `await fixture.whenStable()` works again.
- Check what reaches the modal: `fixture.debugElement.query(By.directive(…Modal)).componentInstance.viewModel()`.
- Close: emit the modal's outputs and expect `dialogRef.close` with the command / without arguments
  (`toHaveBeenCalledExactlyOnceWith()`).

Service – with the **real** `MatDialog`, so the bindings are tested too:

- Providers: `provideZonelessChangeDetection(), provideTranslateService()` (+ `provideHttpClient()`,
  `provideHttpClientTesting()` for a container); `afterEach(() => TestBed.inject(MatDialog).closeAll())`.
- `service.open(…)`, `TestBed.tick()`, then check `TestBed.inject(MatDialog).openDialogs[0].componentInstance`, the
  request with the passed id (container) or click the buttons in `document.querySelector('schbar-…-modal')`.
- `afterClosed()` emits only after the close animation – `const result = firstValueFrom(service.open(…))`, close,
  `expect(await result)…`.

Caller – mock the service: `{ provide: CloseSessionDialogService, useValue: { open: vi.fn() } }`,
`open.mockReturnValue(of(command))` resp. `of(undefined)`.

## Form dialogs

A dialog whose content is a form (`new-tool-form`, `update-user-form`, …) lives in the feature
(`<feature>/components/<name>-form/`) and is opened directly by the one page that needs it. It keeps the form rules
from CLAUDE.md "Forms" and closes itself:

- data via `MAT_DIALOG_DATA`, `inject(MatDialogRef<…, Result>)`, `this.dialogRef.close(command)` in `onSave()`;
- cancel: `<button matButton="outlined" type="button" [mat-dialog-close]="undefined">` (not `mat-dialog-close`
  without a value – that closes with `''`); no `cdkFocusInitial`, the first field gets the focus so Enter saves;
- the page opens it typed – `this.dialog.open<NewToolFormComponent, void, CreateToolCommand>(…)` – with
  `minWidth: DIALOG_WIDTH.…` and `disableClose: true`.

Once a form dialog is needed in a second place, it moves to `shared/dialogs/` and gets a service.

## Checklist

- [ ] Kind decided: shared dialog (modal + service, + container if it loads data) or form dialog
- [ ] Folder `shared/dialogs/<name>-dialog/`, names `<name>-dialog-modal|container`, `<Name>DialogService.open`
- [ ] Modal: inputs (`viewModel`, loading state) and outputs only, command built from ids in the view model
- [ ] Container: ids as `input.required()`, `httpResource` directly, pure `to<Name>ViewModel`, closes `dialogRef`
- [ ] Service: `bindings` instead of `data`, `DIALOG_WIDTH`, `disableClose: true`, typed `open<…>`
- [ ] Caller executes the command via its store, reloads also on cancel if the dialog loaded data
- [ ] Buttons: cancel first, `type="button"`, `danger-button` / `cdkFocusInitial` for destructive actions
- [ ] Specs for modal, container and service (real `MatDialog`), caller spec with mocked service
- [ ] Comments only where the code can't say it
- [ ] `npx ng build`, `npx ng lint`, `npx ng test --watch=false`, `npx prettier --check "src/**/*.{ts,html,scss}"`

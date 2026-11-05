---
name: component
description: How to split a screen in schaffbar-portal (Angular 22) into smart components (pages – store, router, dialogs) and dumb components (inputs/outputs only), how a dumb component defines its ViewModel in its own file and how the page fills it (passing the feature model directly or mapping), where business rules belong, and how to test both. Use when adding or changing a page or a component, writing a ViewModel, deciding whether something is a page or a component, moving logic between them, extracting part of a template into a component, or reviewing or testing a component.
---

# Smart and dumb components in schaffbar-portal

Every component is one of these:

| Kind            | Where                                                                | Example                                           |
| --------------- | -------------------------------------------------------------------- | ------------------------------------------------- |
| **smart**       | `src/app/<feature>/pages/<name>/`                                    | `tool-detail`, `user-list`, `dashboard`           |
| **dumb**        | `src/app/<feature>/components/<name>/`, `src/app/shared/components/` | `user-contact`, `tool-wlan-relais`, `field-error` |
| **form dialog** | `src/app/<feature>/components/<name>-form/`                          | `new-tool-form`, `update-user-form`               |
| shell           | `app.component`, `src/app/core/`                                     | `navigation`, `not-found`                         |

Shared dialogs (modal = dumb, container = smart) follow the same split; their details are in skill `dialog`.

The split in one sentence: **smart knows where data comes from and where commands go, dumb knows how it looks and
what the user can do.** Neither holds business rules – those live in the store, the mapper or a function in the
feature model (`hasRunningUsage` in `workshop-session.model.ts`).

Reference implementations:

- smart: [tool-detail/](../../../src/app/tools/pages/tool-detail/) – store, child components, command handlers
- dumb with a ViewModel: [close-session-dialog-modal](../../../src/app/shared/dialogs/close-session-dialog/close-session-dialog-modal.component.ts)
- dumb with a form and a ViewModel: [user-contact/](../../../src/app/users/components/user-contact/)
- dumb with a main and further ViewModels: [tool-certifications/](../../../src/app/tools/components/tool-certifications/)

## What goes where

| Concern                                                        | Where                                                                  |
| -------------------------------------------------------------- | ---------------------------------------------------------------------- |
| loading, reloading, backend changes                            | store (skill `api`), called by the smart component                     |
| business rules (which readers are assignable, who may certify) | store `withComputed` / feature model function                          |
| the input type of a dumb component                             | **ViewModel in the dumb component's file**                             |
| filling the ViewModel (pass through or map)                    | smart component                                                        |
| navigation, opening a feature dialog, snackbar                 | smart component                                                        |
| building a command                                             | the component that knows the ids (CLAUDE.md "Commands") – usually dumb |
| form state, edit mode, search text, selection, focus           | dumb component                                                         |
| formatting for display (duration, dash, enum key)              | dumb component / pipe                                                  |

## ViewModel

A dumb component describes what it needs as a **ViewModel**: exactly the fields it reads, no more, no less. It is
the component's contract – the page decides how to fill it.

| The dumb component needs                          | Input                                                   |
| ------------------------------------------------- | ------------------------------------------------------- |
| one or two ids / plain values (an enum, a number) | `input.required<string>()` etc. – no ViewModel          |
| anything else                                     | `input.required<<Name>ViewModel>()` (or `…ViewModel[]`) |

| The page has                                           | It fills the ViewModel by                                                                                    |
| ------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------ |
| a feature model with all fields of the ViewModel       | passing the feature model **directly** – no mapper (structural typing, `strictTemplates` checks the binding) |
| data of a different shape (combined, derived, renamed) | a `computed` with a pure `to<Name>ViewModel()` function after the page class                                 |
| data that depends on a business rule                   | taking the result from the store (`assignableRfidReaders`) and passing / mapping that                        |

Rules:

- The ViewModel is defined **in the dumb component's file**: `export interface <Name>ViewModel` after the imports,
  before `@Component`. Not in the feature model, not in a file of its own, not in the store.
- **Names**:
  - The **main ViewModel** is named after the component: `<ComponentName>ViewModel`, its input
    `<componentName>` – `user-contact` → `UserContactViewModel`, `userContact`; `tool-detail-info` →
    `ToolDetailInfoViewModel`, `toolDetailInfo`. Never after the entity it happens to come from (`tool`, `user`).
  - **Further ViewModels** (rows, options, nested parts, a second input) are named after what they describe:
    `ToolCertificationRowViewModel`, `UserOptionViewModel`, `AddressViewModel`; their inputs likewise
    (`certifications`, `certifiers`).
  - A form dialog's ViewModel is its `<Name>DialogData` (skill `dialog`).
- Only the fields the template and the component read. A value the backend can send as `null` stays `| null`
  (CLAUDE.md "Missing values").
- Plain data only: strings, numbers, booleans, `Date`, nested objects/arrays of these, and enums from
  `shared/models`. **No feature model type inside a ViewModel** – otherwise the coupling comes back through the
  back door.
- No ids "for later" that the component doesn't use – but the ids the component needs for its commands are part of
  the ViewModel (`UserContactViewModel.id` for `UpdateUserContactCommand`).
- Not to be confused with the **form model** (`ContactFormModel`): local, not exported, describes what is edited
  (CLAUDE.md "Forms"). The ViewModel describes what comes in.
- The store never imports a ViewModel – it doesn't know the components. Mapping happens in the page.
- A form dialog's `…DialogData` is its ViewModel (same rules), e.g. `{ rfidReader: ChangeRfidReaderTypeViewModel }`.
  Data the dialog has to follow while it is open comes as a `Signal` of the ViewModel – the page passes the store
  signal itself (`NewToolDialogData { existingTools: Signal<ExistingToolViewModel[]> }`, `this.store.entities`).
- Outputs: a feature component keeps emitting the **commands** of its feature (CLAUDE.md "Commands"). A component in
  `shared/` imports no feature (ESLint), so its outputs emit `void` or a result type of its own (skill `dialog`).
  A feature component needed by a second feature moves to `shared/components/` and switches its outputs then.
- Never an API model as input (ESLint).

## Smart component

```ts
@Component({
  selector: 'schbar-tool-detail',
  templateUrl: './tool-detail.component.html',
  styleUrl: './tool-detail.component.scss',
  imports: [ToolDetailInfoComponent, ToolCertificationsComponent, MatProgressBarModule, TranslatePipe],
  providers: [ToolDetailStore],
})
export class ToolDetailComponent {
  protected readonly detailsStore = inject(ToolDetailStore);
  protected readonly toolsStore = inject(ToolsStore);

  // route parameter (withComponentInputBinding)
  readonly id = input.required<string>();

  protected readonly certificationRows = computed(() =>
    toCertificationRowViewModels(this.detailsStore.toolCertifications(), this.detailsStore.users()),
  );

  constructor() {
    this.detailsStore.setToolId(this.id);
  }

  protected onToolUpdated(command: UpdateToolCommand): void {
    this.detailsStore.updateTool(command);
  }
}

function toCertificationRowViewModels(
  certifications: ToolCertification[],
  users: User[],
): ToolCertificationRowViewModel[] {
  const userNames = new Map(users.map((user) => [user.id, `${user.firstName} ${user.lastName}`]));
  return certifications.map((certification) => ({
    customerId: certification.customerId,
    toolId: certification.toolId,
    userName: userNames.get(certification.customerId) ?? null,
    status: certification.status,
    certifiedAt: certification.certifiedAt,
  }));
}
```

<!-- prettier-ignore -->
```html
@if (detailsStore.isLoading()) {
  <mat-progress-bar mode="indeterminate" />
}
@if (detailsStore.hasLoadError()) {
  <p class="load-error">{{ 'shared.errors.loadFailed' | translate }}</p>
}
@if (detailsStore.tool(); as tool) {
  <!-- Tool has all fields of ToolDetailInfoViewModel, the tools all fields of ExistingToolViewModel -->
  <schbar-tool-detail-info [toolDetailInfo]="tool" [existingTools]="toolsStore.entities()" (toolUpdated)="onToolUpdated($event)" />
  <schbar-tool-certifications [certifications]="certificationRows()" … />
}
```

Rules:

- Lives in `pages/`. A route component, or a tab/section of one that uses the page's store (`user-open-session`
  injects the `UserDetailStore` provided by `user-detail`).
- Injects what connects the screen to the app: its store(s), `Router`, `MatDialog`, dialog services, `MatSnackBar`.
  Never `shared/api` (ESLint) – data comes from the store.
- Inputs are route parameters (`id`, `tab`) or, for a nested page, ids.
- Fills the ViewModels of its children (see "ViewModel"): store signals straight into the template where the shape
  fits, a `computed` + pure `to<Name>ViewModel()` after the class where it doesn't. No `computed(() => store.x())`
  that only renames.
- Output handlers pass the command on to a store method (`mutation()`), nothing else. Commands are built by the dumb
  child; the page only builds one when it has the ids itself (`assignFixedRfidTag()` with `this.id()`).
- **Template**: layout (header, loading bar, load error, tabs, grid) plus child components. Markup without state of
  its own may stay in the page – e.g. the `mat-table` of a list page, whose `MatSort`/`MatPaginator` it holds via
  `viewChild`. Extract a dumb component when a part has its own UI state or a form, is used twice, or switches on
  business states (the user header with chips and RFID tag buttons per `RfidTagAssignmentStatus`).
- No pass-through components: a component that only forwards inputs and outputs to its children is dissolved, the
  page renders the children itself.

## Dumb component

```ts
export interface UserContactViewModel {
  id: string;
  email: string;
  phone: string | null;
}

interface ContactFormModel {
  email: string;
  phone: string;
}

@Component({
  selector: 'schbar-user-contact',
  templateUrl: './user-contact.component.html',
  styleUrl: './user-contact.component.scss',
  imports: [FormField, FormRoot, FieldErrorComponent, MatInputModule, MatButtonModule, TranslatePipe],
})
export class UserContactComponent {
  readonly userContact = input.required<UserContactViewModel>();

  readonly contactChanged = output<UpdateUserContactCommand>();

  protected readonly isEditing = signal(false);

  // follows the userContact input
  private readonly model = linkedSignal(() => toModel(this.userContact()));

  protected readonly contactForm = form(this.model, (schema) => { … });

  protected onSave(): void {
    submit(this.contactForm, async () => {
      const { email, phone } = this.model();
      const command: UpdateUserContactCommand = { id: this.userContact().id, email, phone: phone || undefined };
      this.contactChanged.emit(command);
      this.isEditing.set(false);
    });
  }
}

function toModel(userContact: UserContactViewModel): ContactFormModel {
  return { email: userContact.email, phone: userContact.phone ?? '' };
}
```

The page passes the `User` directly – it has all fields of `UserContactViewModel`:
`<schbar-user-contact [userContact]="user" (contactChanged)="onContactChanged($event)" />`.

Rules:

- Lives in `components/` (or `shared/components/`). Talks to the outside **only through `input()` and `output()`**.
- Input types: a ViewModel from its own file or plain values (see "ViewModel").
- **What it may inject**: nothing – with two exceptions:
  - a form dialog: `MAT_DIALOG_DATA` and `MatDialogRef` (skill `dialog`, "Form dialogs");
  - a confirmation before it emits: `ConfirmDeleteDialogService` / `ConfirmActionDialogService` (shared, stateless,
    yes/no). The question belongs to the button the component renders.

  Never a store, `Router`, `MatDialog`, `MatSnackBar`, `HttpClient` or an API service (ESLint).

- **Gets everything it needs as input, already decided and combined.** No business rule inside
  (`assignableRfidReaders` instead of `rfidReaders` + a `SwitchBox` filter), no join of lists (`userName` in the row
  instead of `allUsers` + a `userNameMap`). Data for a validator comes as an input too
  (`existingTools = input.required<ExistingToolViewModel[]>()` for `validateUniqueToolName`), not from a store.
- Filtering by what the user typed (autocomplete search text) is UI logic and stays in the component.
- Every output for a backend change emits a **command** (CLAUDE.md "Commands"). Outputs without a backend change
  (`reload`, `userSelected`) emit `void` or the id.
- Local UI state in signals (`isEditing`, `searchText`, `selectedUsers`), form models local (CLAUDE.md "Forms"),
  pure helpers after the class.

## Testing

Dumb component – ViewModel in, command out, no store:

- Providers: `provideZonelessChangeDetection(), provideTranslateService()` (+ `provideDateAdapter()` with a
  datepicker). Nothing to mock – except a confirmation service:
  `{ provide: ConfirmActionDialogService, useValue: { open: vi.fn(() => of(true)) } }`.
- Test data is a **complete ViewModel literal** – no `as User` cast, no fields the component doesn't read:
  `const userContact: UserContactViewModel = { id: 'user-1', email: 'max@example.com', phone: null };`
- `fixture.componentRef.setInput('userContact', userContact)`, `await fixture.whenStable()`; subscribe a `vi.fn()` to each
  output and expect the command (`toHaveBeenCalledExactlyOnceWith({ id: 'user-1', … })`).
- Material harnesses for fields and buttons (`MatInputHarness`, `MatButtonHarness`) –
  see [user-contact.component.spec.ts](../../../src/app/users/components/user-contact/user-contact.component.spec.ts).

Smart component – store mocked, children real:

- `{ provide: ToolDetailStore, useValue: { tool: signal(tool), updateTool: vi.fn(), … } }` – with
  `providers: [Store]` on the component a TestBed provider doesn't reach it, override it:
  `TestBed.overrideComponent(…, { set: { providers: [...] } })`.
- The mapping is tested here, through what reaches the child:
  `fixture.debugElement.query(By.directive(ToolCertificationsComponent)).componentInstance.certifications()` – the
  `to<Name>ViewModel()` functions are not exported.
- A child's output calls the store method with the command.
- Business rules are tested in the **store spec**, not in the page.

## Checklist

- [ ] Kind decided: page (smart) in `pages/`, component (dumb) in `components/`, form dialog, shared component
- [ ] Dumb: ViewModel(s) exported from its own file, exactly the fields it reads, no feature model type inside;
      only ids / plain values → no ViewModel
- [ ] Names: main ViewModel `<ComponentName>ViewModel` with input `<componentName>`, further ViewModels after their
      content
- [ ] Dumb: only `input()` / `output()`; injects nothing (form dialog: dialog data/ref; confirmation services)
- [ ] Dumb gets data decided and combined: no business rule, no join of lists inside; validator data as input
- [ ] Outputs for backend changes emit commands with the ids (feature); `void` / own result type (shared)
- [ ] Smart: injects store / router / dialogs; feature model passed directly where it fits, otherwise `computed` +
      pure `to<Name>ViewModel()` after the class; handlers only call the store
- [ ] Smart template: layout + children; parts with own state, forms or business switches extracted; no
      pass-through components
- [ ] Specs: dumb with complete ViewModel literals, smart with mocked store incl. the mapping, rules in the store
      spec
- [ ] Comments only where the code can't say it
- [ ] `npx ng build`, `npx ng lint`, `npx ng test --watch=false`, `npx prettier --check "src/**/*.{ts,html,scss}"`

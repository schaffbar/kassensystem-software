---
name: component
description: How to split a screen in schaffbar-portal (Angular 22) into smart components (pages – store, router, dialogs) and dumb components (inputs/outputs only), how a dumb component defines its ViewModel in its own file and how the page fills it (passing the feature model directly or mapping), where business rules belong, and how to test both. Use when adding or changing a page or a component, writing a ViewModel, deciding whether something is a page or a component, moving logic between them, extracting part of a template into a component, or reviewing or testing a component.
---

# Smart and dumb components in schaffbar-portal

Every component is one of these:

| Kind            | Where                                                                | Example                                           |
| --------------- | -------------------------------------------------------------------- | ------------------------------------------------- |
| **smart**       | `src/app/<feature>/pages/<name>/`                                    | `tool-detail`, `user-list`, `dashboard`           |
| **dumb**        | `src/app/<feature>/components/<name>/`, `src/app/shared/components/` | `user-contact-card`, `tool-wlan-relais`, `field-error` |
| **form dialog** | `src/app/<feature>/components/<name>-form/`                          | `new-tool-form`, `update-user-form`               |
| shell           | `app.component`, `src/app/core/`                                     | `navigation`, `not-found`                         |

Shared dialogs (modal = dumb, container = smart) follow the same split; their details are in skill `dialog`.

The split in one sentence: **smart knows where data comes from and where commands go, dumb knows how it looks and
what the user can do.** Neither holds business rules – those live in the store, the mapper or a function in the
feature model (`hasRunningUsage` in `workshop-session.model.ts`).

Reference implementations:

- smart with tabs: [tool-detail/](../../../src/app/tools/pages/tool-detail/) – the page as frame, three tab
  components with mappings and command handlers, a spec per tab
- smart, small: [dashboard/](../../../src/app/dashboard/pages/dashboard/) – mappings that join store data, links in
  the dumb children
- dumb with a ViewModel the page fills with the feature model directly: [user-contact-card/](../../../src/app/users/components/user-contact-card/)
- dumb with a ViewModel, a confirmation and a command: [user-rfid-tag-card/](../../../src/app/users/components/user-rfid-tag-card/)
- dumb with a form (`linkedSignal` from the input) and a ViewModel: [tool-wlan-relais/](../../../src/app/tools/components/tool-wlan-relais/)
- dumb with a main and a further ViewModel: [tool-rfid-reader-assignment/](../../../src/app/tools/components/tool-rfid-reader-assignment/)
- dumb with further ViewModels only (rows, options) and a plain id: [tool-certifications/](../../../src/app/tools/components/tool-certifications/)

## What goes where

| Concern                                                        | Where                                                                  |
| -------------------------------------------------------------- | ---------------------------------------------------------------------- |
| loading, reloading, backend changes                            | store (skill `api`), called by the smart component                     |
| business rules (which readers are assignable, who may certify) | store `withComputed` / feature model function                          |
| matching two sources (is the reader used by another tool?)     | page mapping (`to…ViewModel`) – a join, not a rule                     |
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
    `<componentName>` – `user-contact-card` → `UserContactCardViewModel`, `userContactCard`; `tool-detail-info` →
    `ToolDetailInfoViewModel`, `toolDetailInfo`. Never after the entity it happens to come from (`tool`, `user`).
  - A **modal of a shared dialog** follows skill `dialog` instead: input `viewModel`, type `<Name>DialogViewModel`
    (`ConfirmDeleteDialogViewModel`, `CloseSessionDialogViewModel`) – the service binds it by that name.
  - **Further ViewModels** (rows, options, nested parts, a second input) are named after what they describe:
    `ToolCertificationRowViewModel`, `UserOptionViewModel`, `AddressViewModel`; their inputs likewise
    (`certifications`, `certifiers`). A row of a table ends in `RowViewModel`.
  - **Lists**: a component whose only input is a list gets just the row ViewModel, no main ViewModel –
    `toolStatuses` of type `ToolStatusRowViewModel[]`. When it needs more than the list (totals, a title), the main
    `<ComponentName>ViewModel` contains the list and the rest – not several inputs side by side.
- Only the fields the template and the component read. A value the backend can send as `null` stays `| null`
  (CLAUDE.md "Missing values").
- Plain data only: strings, numbers, booleans, `Date`, nested objects/arrays of these, and enums from
  `shared/models`. **No feature model type inside a ViewModel** – otherwise the coupling comes back through the
  back door.
- No ids "for later" that the component doesn't use – but the ids the component needs for its commands are part of
  the ViewModel (`UserRfidTagCardViewModel.userId` for `UnassignRfidTagCommand`).
- Not to be confused with the **form model** (`WlanRelaisFormModel`): local, not exported, describes what is edited
  (CLAUDE.md "Forms"). The ViewModel describes what comes in.
- The store never imports a ViewModel – it doesn't know the components. It may provide feature-model lists that a
  rule or several children need (`instructors`, reused as `certifiers`; `certifiableUsers`) – the page passes them
  directly. Any reshaping into a ViewModel happens in the page.
- A form dialog's ViewModel is its `<Name>DialogData` (same rules, skill `dialog`), e.g.
  `{ rfidReader: ChangeRfidReaderTypeViewModel }`.
  Data the dialog has to follow while it is open comes as a `Signal` of the ViewModel – the page passes the store
  signal itself (`NewToolDialogData { existingTools: Signal<ExistingToolViewModel[]> }`, `this.store.tools`).
- Outputs: a feature component keeps emitting the **commands** of its feature (CLAUDE.md "Commands"). A component in
  `shared/` imports no feature (ESLint), so its outputs emit `void` or a result type of its own (skill `dialog`).
  A feature component needed by a second feature moves to `shared/components/` and switches its outputs then.
- Never an API model as input (ESLint).

## Names of methods and outputs

A method called from a template is named for **what it does**, an output for **what happened** in the component:

```
dumb:   (click)="unassignRfidTag()"        unassignRfidTag() { … this.rfidTagUnassigned.emit(command); }
smart:  (rfidTagUnassigned)="unassignRfidTag($event)"
                                           unassignRfidTag(command) { this.detailsStore.unassignRfidTag(command); }
```

| What                                    | Name                                                       | Examples                                                                    |
| --------------------------------------- | ---------------------------------------------------------- | --------------------------------------------------------------------------- |
| method in a dumb component              | verb + object of the UI step                               | `saveWlanRelais`, `discardWlanRelaisChanges`, `pauseToolCertification`      |
| method in a page / tab for a command    | the name of the store method it calls                      | `unassignRfidTag`, `pauseToolCertification`, `updateTool`                   |
| method in a page opening a dialog first | the action the dialog leads to                             | `createTool`, `deleteTool`, `editUser`, `changeRfidReaderType`              |
| navigation                              | `open` + target                                            | `openToolDetail`, `showOpenSession` (switches a tab)                        |
| output                                  | object + participle – the event, from the component's view | `rfidTagUnassigned`, `certificationPaused`, `editRequested`                 |

- Always with an object – `saveWlanRelais()`, not `save()`: one rule without exceptions, readable in the template, and a
  search finds exactly one place.
- No `on…` prefix (it names the trigger, not the action), no `…Dialog` suffix (the dialog is part of the action).
- An output is never named like a method of the same class – that is why outputs are events and methods verbs.
- Outputs of shared dialogs stay `confirmed` / `cancelled` (skill `dialog`).
- A mapping function (pure, after the class) is `to` + the exact target type, plural for a list:
  `toWlanRelaisFormModel`, `toToolCertificationRowViewModels` – like the mappers of a feature (`toTool`).

## Smart component

A tab component of `tool-detail` – the page itself is only the frame (see "Pages with tabs"):

```ts
@Component({
  selector: 'schbar-tool-detail-tab-certifications',
  templateUrl: './tool-detail-tab-certifications.component.html',
  styleUrl: './tool-detail-tab-certifications.component.scss',
  imports: [ToolCertificationsComponent],
})
export class ToolDetailTabCertificationsComponent {
  protected readonly detailsStore = inject(ToolDetailStore);

  protected readonly certificationRows = computed(() =>
    toToolCertificationRowViewModels(this.detailsStore.toolCertifications(), this.detailsStore.users()),
  );

  // --------------------------------------------------------------------------
  // actions

  protected deleteToolCertification(command: DeleteToolCertificationCommand): void {
    this.detailsStore.deleteToolCertification(command);
  }
}

// ----------------------------------------------------------------------------
// mappers

function toToolCertificationRowViewModels(
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
@if (detailsStore.tool(); as tool) {
  <schbar-tool-certifications
    [toolId]="tool.id"
    [certifications]="certificationRows()"
    [certifiableUsers]="detailsStore.certifiableUsers()"
    [certifiers]="detailsStore.certifiers()"
    (certificationDeleted)="deleteToolCertification($event)" … />
}
```

Where the shape fits, the feature model goes in directly – in `tool-detail-tab-info` the `Tool` has all fields of
`ToolDetailInfoViewModel`, the tools all fields of `ExistingToolViewModel`:
`<schbar-tool-detail-info [toolDetailInfo]="tool" [existingTools]="toolsStore.tools()" … />`.

Rules:

- Lives in `pages/`. A route component (page) or a tab component of a page (see "Pages with tabs").
- Injects what connects the screen to the app: its store(s), `Router`, `MatDialog`, dialog services, `MatSnackBar`.
  Never `shared/api` (ESLint) – data comes from the store.
- Inputs are route parameters (`id`, `tab`) or, for a tab component, ids.
- Fills the ViewModels of its children (see "ViewModel"): store signals straight into the template where the shape
  fits, a `computed` + pure `to<Name>ViewModel()` after the class where it doesn't. No `computed(() => store.x())`
  that only renames.
- Output handlers pass the command on to a store method (`mutation()`), nothing else. Commands are built by the dumb
  child; the page only builds one when the action starts in the page itself – e.g. after its own confirmation
  dialog (`DeleteToolCommand` in `tool-list`, `CloseWorkshopSessionCommand` in `user-detail-dialogs.service`).
- **Template**: layout (header, loading bar, load error, tabs, grid) plus child components. Markup without state of
  its own may stay in the page – e.g. the `mat-table` of a list page, whose `MatSort`/`MatPaginator` it holds via
  `viewChild`. Extract a dumb component when a part has its own UI state or a form, is used twice, or switches on
  business states (the RFID tag card per `RfidTagAssignmentStatus`).
- No pass-through components: a component that only forwards inputs and outputs to its children is dissolved, the
  page renders the children itself.

### Pages with tabs

A detail page with tabs is a **frame**: loading bar, load error, header, `mat-tab-group`. Every tab is a smart **tab
component** in the page's folder:

```
users/pages/user-detail/
  user-detail.component.*            frame
  user-detail.store.ts
  user-detail-dialogs.service.ts     dialogs opened by the header and several tabs (skill `dialog`)
  user-detail-tab-overview/          UserDetailTabOverviewComponent        schbar-user-detail-tab-overview
  user-detail-tab-certifications/    UserDetailTabCertificationsComponent  schbar-user-detail-tab-certifications
  user-detail-tab-session/           UserDetailTabSessionComponent         schbar-user-detail-tab-session
```

- Name `<page>-tab-<name>` – the tab folders stay together and visibly belong to their page.
- It injects the **page's store** (`providers: [UserDetailStore]` on the page), so it only works inside its page. For
  its part it does what a page does: fills the ViewModels of its dumb children, handles their outputs, opens its
  dialogs.
- Inputs: only what the page knows and the store doesn't (`usedBy` of `tool-detail-tab-info`, from the tool list);
  data comes from the store. Outputs for what belongs to the page: `openSessionRequested` of
  `user-detail-tab-overview` switches the tab, `deleteRequested` of `tool-detail-tab-info` deletes via the header's
  flow.
- Every tab gets a tab component, also one with a single child – no exceptions.
- The page keeps what belongs to the whole page: the header and its actions, the selected tab (`tab` query
  parameter).
- Layout: the page wraps each tab in its padding (`<div class="tab-content">`), the tab component lays out its
  children (`:host`).

## Dumb component

```ts
export interface UserRfidTagCardViewModel {
  userId: string;
  userName: string;
  // null = no tag and none requested
  assignment: {
    assignmentType: RfidTagAssignmentType;
    status: RfidTagAssignmentStatus;
    rfidTagId: string | null;
    assignmentDate: Date | null;
  } | null;
}

@Component({
  selector: 'schbar-user-rfid-tag-card',
  templateUrl: './user-rfid-tag-card.component.html',
  styleUrl: './user-rfid-tag-card.component.scss',
  imports: [DatePipe, MatButtonModule, MatIconModule, TranslatePipe, OrDashPipe, RfidTagIdPipe, RfidTagAssignmentTypeKeyPipe],
  host: { class: 'card' },
})
export class UserRfidTagCardComponent {
  private readonly confirmActionDialog = inject(ConfirmActionDialogService);

  readonly userRfidTagCard = input.required<UserRfidTagCardViewModel>();

  // opens the dialog that requests a tag – the page decides how
  readonly assignmentRequested = output<void>();
  readonly rfidTagUnassigned = output<UnassignRfidTagCommand>();

  // --------------------------------------------------------------------------
  // actions

  protected unassignRfidTag(): void {
    const { userId, userName } = this.userRfidTagCard();
    this.confirmActionDialog
      .open({ title: …, message: …, entity: userName, confirmLabel: …, destructive: true })
      .subscribe((confirmed) => {
        if (confirmed) {
          const command: UnassignRfidTagCommand = { userId };
          this.rfidTagUnassigned.emit(command);
        }
      });
  }
}
```

The ViewModel combines the user and the assignment, so the tab maps it in a `computed` with
`toUserRfidTagCardViewModel(user, assignment)` after its class. Where the feature model has all fields, the page
passes it directly: `<schbar-user-contact-card [userContactCard]="user" (editRequested)="editUser()" />`.

A dumb component with a form keeps the form model local and follows its input with `linkedSignal`
([tool-wlan-relais](../../../src/app/tools/components/tool-wlan-relais/tool-wlan-relais.component.ts):
`model = linkedSignal(() => toWlanRelaisFormModel(this.toolWlanRelais()))`).

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
  (`editRequested`, `assignmentRequested`, `openSessionRequested`) emit `void` or the id.
- Local UI state in signals (`isEditing`, `searchText`, `selectedUsers`, the area filter of `tool-status-card`), form models local (CLAUDE.md "Forms"),
  pure helpers after the class.

## Testing

Dumb component – ViewModel in, command out, no store:

- Providers: `provideZonelessChangeDetection(), provideTranslateService()` (+ `provideDateAdapter()` with a
  datepicker). Nothing to mock – except a confirmation service:
  `{ provide: ConfirmActionDialogService, useValue: { open: vi.fn(() => of(true)) } }`.
- Test data is a **complete ViewModel literal** – no `as User` cast, no fields the component doesn't read:
  `const assigned: UserRfidTagCardViewModel = { userId: 'user-1', userName: 'Max Mustermann', assignment: { … } };`
- `fixture.componentRef.setInput('userRfidTagCard', assigned)`, `await fixture.whenStable()`; subscribe a `vi.fn()` to each
  output and expect the command (`toHaveBeenCalledExactlyOnceWith({ id: 'user-1', … })`).
- Material harnesses for fields and buttons (`MatInputHarness`, `MatButtonHarness`) –
  see [update-user-form.component.spec.ts](../../../src/app/users/components/update-user-form/update-user-form.component.spec.ts).
  More test patterns: skill `testing`.

Smart component – store mocked, children real (a tab component the same way, but it has no `providers` of its own:
the store mock goes into the `TestBed` providers):

- `{ provide: ToolDetailStore, useValue: { tool: signal(tool), updateTool: vi.fn(), … } }` – with
  `providers: [Store]` on the component a TestBed provider doesn't reach it, override it:
  `TestBed.overrideComponent(…, { set: { providers: [...] } })`.
- The mapping is tested here, through what reaches the child:
  `fixture.debugElement.query(By.directive(ToolCertificationsComponent)).componentInstance.certifications()` – the
  `to<Name>ViewModel()` functions are not exported.
- A page with tabs: the page spec covers only the frame (header, reload); the content of a tab is tested in the tab
  spec, no tab selection needed. (Selecting a tab in a test needs `MatTabGroupHarness.selectTab({ label })` and
  `{ provide: MATERIAL_ANIMATIONS, useValue: { animationsDisabled: true } }` – the content is attached only when the
  tab animation ends.)
- A child's output calls the store method with the command: emit it on the child instance
  (`child(UserRfidTagCardComponent).rfidTagUnassigned.emit({ userId: 'user-1' })` with a small `child()` helper
  around `By.directive`, see [user-detail-tab-overview.component.spec.ts](../../../src/app/users/pages/user-detail/user-detail-tab-overview/user-detail-tab-overview.component.spec.ts))
  and expect the store / router call.
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
- [ ] Names: methods verb + object (page/tab: like the store method), outputs object + participle; no `on…`,
      no `…Dialog`
- [ ] Smart: injects store / router / dialogs; feature model passed directly where it fits, otherwise `computed` +
      pure `to<Name>ViewModel()` after the class; handlers only call the store
- [ ] Smart template: layout + children; parts with own state, forms or business switches extracted; no
      pass-through components
- [ ] Page with tabs: frame only; every tab a tab component `<page>-tab-<name>/` in the page's folder
- [ ] Specs: dumb with complete ViewModel literals, smart with mocked store incl. the mapping, rules in the store
      spec
- [ ] Sections: `// actions`, `// helper` in the class; `// mappers`, `// helper` after it (CLAUDE.md "File
      sections")
- [ ] Comments only where the code can't say it
- [ ] `npx ng build`, `npx ng lint`, `npx ng test --watch=false`, `npx prettier --check "src/**/*.{ts,html,scss}"`

# schaffbar-portal

The staff portal of the SchaffBar workshop: people, their RFID tags, certifications and open sessions, tools with
their hardware, RFID tags and readers, and an overview of who is in the workshop. Angular 22 (zoneless, signals,
signal forms), Angular Material M3, NgRx signal store, ngx-translate, zod. Project overview, glossary (UI ↔ code) and
git rules: `../CLAUDE.md`.

## Commands

```bash
npm start                                    # dev server on http://localhost:4200, backend on http://127.0.0.1:5000
npm run check                                # lint, prettier, spec types, all tests, build – before handing over
npx ng test --watch=false --include 'src/app/users/**/*.spec.ts'   # one folder (glob ends in .spec.ts)
```

## Skills

The rules below are the short form; the skills have the details, skeletons, references and tests. Load the skill
before working on its topic.

| Task | Skill |
| --- | --- |
| where code belongs, feature folders, import boundaries, feature model, commands, mappers, stores, polling | `feature` |
| an endpoint: zod schema, `…ApiModel`, `…Request`, API service, fixtures | `api` |
| a page or component: smart / dumb, ViewModel, names, tabs | `component` |
| a dialog: shared dialog, confirmation, form dialog, page dialog service | `dialog` |
| how it looks: tokens, building blocks, list / detail page, status vs. dash, phone | `design` |
| writing or fixing specs | `testing` |
| looking at a change in the browser without touching the database | `verify-ui` |

## Code

- **Structure:** a feature imports only from itself and `shared/`; `shared/` imports no feature; only stores, mappers
  and dialog containers import `shared/api` (all enforced by ESLint). `shared/models` holds enums only. Each feature
  has its own model of another resource's data and loads it in its own store.
- **Services:** `@Service()` (Angular 22), not `@Injectable({ providedIn: 'root' })`; dependencies via `inject()`.
  Only via a `providers` list: `@Service({ autoProvided: false })`. Signal stores keep
  `signalStore({ providedIn: 'root' }, …)`.
- **Pure functions** (no `this`, no signals, no injected services) are module-level, **not exported**, **after the
  class**; mapping functions are named `to` + the exact target type, plural for a list
  (`toToolCertificationRowViewModels`). Everything that needs `this` is a `private` method in the `// helper` section
  at the end of the class.
- **File sections:** a separator line ending at column 79 (indentation counts), then the section name:

  ```ts
  export class UserRfidTagCardComponent {
    …
    // --------------------------------------------------------------------------
    // actions
  }

  // ----------------------------------------------------------------------------
  // mappers
  ```

  Components: (no header) injected services, inputs, outputs, signals, form, `computed`, constructor → `// actions`
  → `// helper`; after the class `// mappers` → `// helper`. Mappers: `// API model → feature model` →
  `// command → request`. API models: schemas → `// requests`. Feature models: model → sections by content →
  `// commands` last. API services: `// queries` → `// commands`. A section only exists when it has content.
- **Comments only for what the code can't say** – why, a non-obvious rule, what `null` means. No class comments, no
  JSDoc that repeats a name or signature; a method that seems to need one gets a better name.
- **Commands** carry the ids named after their entity (`toolId`, never `id`), are built by the component that knows
  the ids as a typed variable and passed on unchanged by pages; every backend change is a store `mutation<Command>`
  (skill `feature`).
- **Subscriptions:** `subscribe()` only on observables that complete (`afterClosed()`, dialog services, one-off
  checks); never on `interval` / `valueChanges` / router events – `toSignal()` in a component, `rxMethod` in a store.

## Components

- **Smart** (`<feature>/pages/`): store, `Router`, `MatDialog`, dialog services; layout plus child components; output
  handlers pass the command on to a store method.
- **Dumb** (`<feature>/components/`, `shared/components/`): only `input()` / `output()` and local UI state; injects
  nothing except dialog data/ref in a form dialog and the confirmation services (ESLint).
- Business rules live in the feature model or the store, never in a component; a dumb component gets its data
  decided and combined.
- Every dumb component exports its input type `<ComponentName>ViewModel` (input `<componentName>`) from its own file –
  exactly the fields it reads, no feature model type inside.
- Names: template methods verb + object (`saveWlanRelais`, `deleteTool`), no `on…`, no `…Dialog`; a page method that
  executes a command is named like the store method; outputs are events (`rfidTagUnassigned`, `editRequested`).
- A page with tabs is the frame; every tab is a smart `<page>-tab-<name>/` component that injects the page's store.

## Forms

- A form that saves data: `<form [formRoot]="xyzForm" (submit)="saveXyz()">`, `saveXyz()` calls `submit(…)`. A field
  that only filters (search) is no form – a plain `signal('')`.
- Inside a `<form>` the save button is `type="submit"`, **every other button `type="button"`** (otherwise cancel
  saves).
- The submit button names the action: create → what is created ("Benutzer hinzufügen"), edit in a dialog →
  `shared.actions.saveChanges`, a form on a page → `shared.actions.save`; cancel is always `shared.actions.cancel`.
- Form models (`<Name>FormModel`) stay local in the component. A date field is `Date | null`; the datepicker uses the
  date-fns adapter with the German locale, typing (`31.1.1990`) and picking both work – no `readonly`.
- Layout with the global classes `form-row`, `form-col`, `form-actions`, `full-width`, `readonly-field`; compact fields
  with the label above in form dialogs and `compact-form` (skill `dialog`).
- A detail page shows values read-only in a card and edits them in a form dialog opened by "Bearbeiten".

## Missing values

- Fields the backend can send as `null` are `| null` in the model, so the template has to handle them.
- A missing value is shown with the `orDash` pipe, not `?? '-'` – only for display; editable fields stay empty.
- An enum's translation key comes from a small pure pipe (`rfidReaderTypeKey`), including the `null` case – never
  built in the template.
- Dash, highlighted hint, badge or plain text: skill `design` ("Status, text or dash").

## Design

- Colors only from the tokens in `src/styles.scss` – no hex values; neon green `--accent` only as a surface, green
  text is `--accent-text`.
- Material's look only with `mat.*-overrides(...)` – no `.mat-mdc-*` selectors, no `!important`.
- Buttons with the attribute syntax (`matButton="filled"`, `matButton="outlined"`, `matIconButton`), no `color`;
  every `matIconButton` has a translated `aria-label`.
- Building blocks from `shared/components/` and the global classes instead of own copies; a status is icon + text
  (`schbar-status-badge`), never color alone.
- Every detail page that can delete its entity ends its first tab with `schbar-danger-zone` over the full width; a
  rule that forbids deleting is explained there instead of the button.
- Times of a session use `timeWithDay`, not `date: 'shortTime'`.

## Dialogs

- Shared dialogs (`shared/dialogs/<name>-dialog/`): modal + service (+ container when it loads data); they return
  yes/no and import nothing from a feature – the caller builds the command.
- Form dialogs (`<feature>/components/<name>-form/`): close themselves with the command,
  `[mat-dialog-close]="undefined"` on cancel, `host: { class: 'form-dialog' }`, opened with `...FORM_DIALOG_CONFIG`.
- The same dialogs from several places of a page: a page service `<page>-dialogs.service.ts`.
- `MatDialog.open<Component, Data, Result>` always typed; `minWidth: DIALOG_WIDTH.…`, `disableClose: true`.
- **Every `danger-button` / `danger-menu-item` asks first** – delete via `ConfirmDeleteDialogService`, everything else
  via `ConfirmActionDialogService` (`destructive: true`). The confirm button names the action as verb + object
  ("Benutzer löschen", key `<feature>.dialogs.<action>.confirm`); cancel first, `cdkFocusInitial` on cancel only for
  destructive confirmations.

## Translations

- Every text through ngx-translate, in `public/i18n/de.json` **and** `en.json` – always both, same keys.
- Keys by place: `<feature>.pages.<page>.…`, `<feature>.dialogs.<action>.{title,message,confirm}`, `<feature>.menu.…`;
  what several features use under `shared.…` (`shared.actions.save`, `shared.toolAreas.HOLZ`). An unused key is
  removed.
- German texts address nobody directly – no "du", no "Sie": "Tag an das Lesegerät an der Theke halten", "Bitte 2
  Felder prüfen". Terms as in the glossary (`../CLAUDE.md`).

## Definition of done

1. The change follows the rules above and its skills; specs for new or changed behaviour (skill `testing`).
2. `npm run check` passes.
3. A UI change is looked at on desktop and phone without writing to the database (skill `verify-ui`).
4. Decisions with their date in `../docs/ui-redesign.md`; a rule the portal checks for the backend in the aggregate's
   `README.md` ("Enforced only in the portal"); backend gaps in `../docs/backend-features.md`; a finished task ticked
   off in `../docs/roadmap.md`.
5. Nothing committed (`../CLAUDE.md`).
6. The report says what was checked and what was not.

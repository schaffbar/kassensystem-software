---
name: feature
description: How a feature of schaffbar-portal (Angular 22, NgRx signal store) is built – its folders (model, mapper, store, pages, components), the import boundaries between features and shared/ (enforced by ESLint), its own model per feature, commands, mappers, list and detail stores with httpResource, mutation(), one-off checks and polling, where business rules live, subscriptions, and how to add a new feature. Use when adding or changing a feature, a store, a feature model, a command or a mapper, when deciding where code belongs or whether an import is allowed, when a feature needs data of another resource, or when reviewing any of these.
---

# Features in schaffbar-portal

A feature is a folder under `src/app/` – `dashboard`, `users`, `tools`, `rfid-tags`, `rfid-readers`. It owns its
model, its commands, its mappers and its stores, and talks to the backend only through `shared/api` (skill `api`).
How its components are built: skill `component`; dialogs: skill `dialog`; look: skill `design`.

```
backend ──JSON──▶ shared/api/<resource>/          <feature>/
                    *.api-model.ts  ◀───────────── *.mapper.ts ──▶ *.model.ts (model, rules, commands)
                    *.api-service.ts ◀──────────── *.store.ts  ──▶ pages/ ──▶ components/
                              │                         ▲
                              └──── shared/models/ ─────┘   enums: the vocabulary of both
```

Reference features:

- [rfid-readers/](../../../src/app/rfid-readers/) – the smallest complete one: list and detail store, model with a
  rule (`canDeleteRfidReader`), mapper, one-off check (`canChangeRfidReaderType`)
- [users/](../../../src/app/users/) – several resources joined, a detail store with mutations of five resources,
  polling, a page service for dialogs, several models of other resources (`tool.model.ts`, `tool-certification.model.ts`)
- [dashboard/](../../../src/app/dashboard/) – a feature without its own resource: five resources, rules in the model,
  polling the whole store

## Folders

```
src/app/<feature>/
  <entity>.model.ts               feature model, rules (pure functions), commands
  <entity>.mapper.ts (+ .spec)    API model → feature model, command → request
  <entities>.store.ts (+ .spec)   list store (signalStore, providedIn: 'root' when several pages share it)
  <entity>.validators.ts          form validators of the feature
  <x>-key.pipe.ts                 enum → translation key, only used by this feature (shared ones: shared/pipes)
  pages/
    <entity>-list/                list page
    <entity>-detail/
      <entity>-detail.component.* the page (frame)
      <entity>-detail.store.ts    detail store, provided by the page
      <entity>-detail-dialogs.service.ts   dialogs opened from several places (skill `dialog`)
      <entity>-detail-tab-<name>/ one smart component per tab
  components/
    <name>/                       dumb components, form dialogs (<name>-form/)
```

No `shared/` folder inside a feature. Something two features need goes to `src/app/shared/` – an enum to
`shared/models/`, a pipe to `shared/pipes/`, a component to `shared/components/` (it then takes translated texts and
emits no feature commands).

## Import boundaries (ESLint)

| From | may import |
| --- | --- |
| a feature | itself, `shared/`, `app.routes.ts` – **never another feature** (`import/no-restricted-paths`) |
| `shared/` | only `shared/` |
| stores, mappers, dialog containers | `shared/api` |
| pages, components, pipes and their specs | **not** `shared/api` – they see feature models and ViewModels only |
| everything | `shared/models` (enums) |

A new feature folder is added to the list in `eslint.config.js` (`['dashboard', 'rfid-readers', …].map(…)`),
otherwise its boundaries are not checked.

## One model per feature

- A backend resource used by several features gives **each feature its own model and mapper**, with only the fields
  and commands that feature needs: `tools/tool-certification.model.ts` and `users/tool-certification.model.ts`;
  `Tool` is `{ id, name, area, certificationRequirement }` in `users`, `{ id, name, area, rfidReaderId,
  wlanRelaisType }` in `dashboard`. The duplication is intended – features stay independent.
- Data of another resource is loaded by the feature's **own store** with the API service (`customersResource()` for
  the names of instructors in `user-detail.store.ts`) – never through another feature's store.
- `shared/models` holds enums only – no entities, no commands.

## Feature model

```ts
export interface RfidReader {
  id: string;
  macAddress: string;
  type: RfidReaderType | null;
  name: string | null;
  socketName: string | null;
  createdAt: Date;
}

// ----------------------------------------------------------------------------
// list

export interface RfidReaderListItem extends RfidReader {
  assignedTool: AssignedTool | null;
}

// a switch box is disconnected at its tool first – otherwise the tool would point to a reader that no longer exists
export function canDeleteRfidReader(rfidReader: RfidReaderListItem): boolean {
  return !rfidReader.assignedTool;
}

// ----------------------------------------------------------------------------
// commands

export interface UpdateRfidReaderCommand {
  rfidReaderId: string;
  name?: string;
  socketName?: string;
}
```

- Shaped for the feature: `Date` instead of ISO strings, only the fields it uses, combined values where it helps
  (`UserListItem` = user + tag + in workshop). It starts as a copy of the API model and drifts when the feature needs
  it to.
- Fields the backend can send as `null` stay `| null`, so the template has to handle the case.
- Sections: (no header) the model → further sections by content (`// list`, `// WLAN relais`, `// update – only the
  parts that changed are sent`) → `// commands` last.

### Business rules

A rule is a **pure, exported function in the model** (`canDeleteTool`, `hasStayedOvernight`, `isHardwareSetUp`,
`canCloseWorkshopSession`), with a comment saying **why** and, if it only lives in the portal, the rule number of the
backend README (`T-17, only in the portal`). The store uses it in `withComputed` (`canDeleteUser`,
`toolsWithoutHardware`), the page passes the result on. Components never decide a rule (skill `component`). Each rule
gets a spec in `<entity>.model.spec.ts`.

When the portal checks a rule the backend does not enforce yet, it goes into the aggregate's `README.md`, section
"Enforced only in the portal".

### Commands

- Every backend change a component triggers is a **command**, emitted by `output()` or returned when a dialog
  closes; the page only passes it on.
- Named verb + entity + `Command` (`UpdateToolCommand`, `UnassignRfidTagCommand`), defined in the model, section
  `// commands`.
- **It contains the id(s)**, named after the entity – `toolId`, `rfidReaderId`, never `id`, also when the command
  changes that entity itself (`UpdateToolCommand { toolId, … }`). Also for a single id: `ClearWlanRelaisCommand
  { toolId }`, not `string`.
- Optional fields `?:` for values the user may leave empty (`phone?: string`); the mapper turns them into `null`.
- Built as an explicitly typed variable before it is emitted:
  `const command: UnassignRfidTagCommand = { userId }; this.rfidTagUnassigned.emit(command);`
- One command may cover several endpoints when the user does one thing (`UpdateUserCommand` = person, contact and
  address; the store sends only the changed parts).

## Mapper

```ts
// ----------------------------------------------------------------------------
// API model → feature model

export function toRfidReader(apiModel: RfidReaderApiModel): RfidReader {
  return {
    id: apiModel.id,
    macAddress: apiModel.macAddress,
    type: apiModel.type,
    name: apiModel.name,
    socketName: apiModel.socketName,
    createdAt: parseISO(apiModel.createdAt),
  };
}

// ----------------------------------------------------------------------------
// command → request

export function toUpdateRfidReaderRequest(command: UpdateRfidReaderCommand): UpdateRfidReaderRequest {
  return { name: command.name ?? null, socketName: command.socketName ?? null };
}
```

- File `<entity>.mapper.ts`, named like the model file (`dashboard.model.ts` → `dashboard.mapper.ts`); exported pure
  functions `to<Entity>(apiModel)` and `to<Action>Request(command)`.
- **Every field explicitly, no spread** – otherwise new API fields leak into the feature and ids into request bodies.
- Every request body gets a mapper, also a trivial one. Path variables don't: the store passes `command.rfidReaderId`.
- Dates with `parseISO` / `format` from `date-fns` (`parseISO` creates a local date; `new Date('1990-01-31')` would be
  UTC midnight); nullable: `apiModel.endTime ? parseISO(apiModel.endTime) : null`. Missing values `?? null`.
- Nested API models get one mapper each; only the top-level one is exported (`toWorkshopUsage` stays private).
- Joining several resources into one model is the mapper's job too (`toUserListItems(customers, assignments,
  activeUsers)`), with the join explained in a comment.
- Enums pass through unchanged.
- A spec per function: a `null` value, a date, missing command fields → `null`, no id in the request body.

## Store

```ts
export const RfidReaderDetailStore = signalStore(
  withState<RfidReaderDetailState>({ rfidReaderId: null }),
  withProps((store) => {
    const rfidReadersApi = inject(RfidReadersApiService);
    return {
      _rfidReadersApi: rfidReadersApi,
      _router: inject(Router),
      // (re)loads whenever the id changes
      _rfidReaderResource: rfidReadersApi.rfidReaderResource(store.rfidReaderId),
    };
  }),
  withComputed(({ _rfidReaderResource }) => ({
    // hasValue() stays true while reloading, so the page keeps showing the reader instead of flickering
    rfidReader: computed(() => (_rfidReaderResource.hasValue() ? toRfidReader(_rfidReaderResource.value()) : null)),
    isLoading: computed(() => _rfidReaderResource.isLoading()),
    hasLoadError: computed(() => !!_rfidReaderResource.error()),
  })),
  withMethods((store) => ({
    setRfidReaderId: signalMethod<string>((rfidReaderId) => patchState(store, { rfidReaderId })),
    reloadRfidReader(): void {
      store._rfidReaderResource.reload();
    },
    updateRfidReader: mutation<UpdateRfidReaderCommand>({
      request: (command) =>
        store._rfidReadersApi.updateRfidReader(command.rfidReaderId, toUpdateRfidReaderRequest(command)),
      onSuccess: () => store._rfidReaderResource.reload(),
    }),
    deleteRfidReader: mutation<DeleteRfidReaderCommand>({
      request: (command) => store._rfidReadersApi.deleteRfidReader(command.rfidReaderId),
      onSuccess: () => void store._router.navigate([ROUTE.RFID_READERS]),
    }),
  })),
);
```

- **Displayed data** is an `httpResource` of the API service, created in `withProps` (private, `_` prefix). The store
  maps in `withComputed` (`hasValue() ? map(value()) : null / []`) – components only see the computed.
- Names after the content (`tools`, `users`, `rfidReaders`), never `entities`. `isLoading`, `hasLoadError`, a
  `reload…()` that reloads every resource of the store.
- A **detail store** has the id in its state (`set…Id` as `signalMethod`, called by the page with its `id` input) and
  is provided by the page (`providers: [XDetailStore]`). A **list store** has no state, loads on first use and is
  `providedIn: 'root'` when other pages read it (`ToolsStore` for `usedBy` on the tool page).
- **Every backend change is a `mutation<Command>`** (`src/app/shared/state/mutation.ts`): double-click protection, its
  own `isPending`, `onSuccess` reloads exactly the affected resource (or navigates after a delete). Errors are shown
  by the error interceptor; the callbacks must not throw. Named like the command without `Command`
  (`UnassignRfidTagCommand` → `unassignRfidTag`).
- Several requests for one command: `concat(...requests).pipe(toArray())` – one after the other, `onSuccess` once.
- **One-off check** at the time of an action (does a tool still use this reader?): a plain observable of the API
  service, wrapped by the store, which returns only what the component needs –
  `canChangeRfidReaderType(): Observable<boolean>`, `canDeleteUser(userId): Observable<boolean>`.
- **Rules** in `withComputed` with the model functions (`canCloseWorkshopSession`, `toolsWithoutHardware`); a second
  `withComputed` for values derived from the first.
- **Polling** a page that stays open: an `rxMethod` that reloads on an interval, called by the page in its
  constructor – it ends with the page:

  ```ts
  pollDashboard: rxMethod<number>(
    pipe(switchMap((intervalMs) => interval(intervalMs).pipe(tap(() => store.reloadDashboard())))),
  ),
  // or only while a signal is true: rxMethod<boolean>(… waiting ? interval(…) : EMPTY …)
  ```

  Its `isLoading` then counts only the first load (`resource.status() === 'loading'`), so the progress bar does not
  flicker on every poll.
- No `withEntities`, no `rxMethod` for loading, no `onInit` hook. The store never imports a ViewModel.
- Sections of `withMethods` blocks by topic with a comment (`// mutations of the certifications – reload the
  certifications on success`).

## Subscriptions

- `subscribe()` only on observables that complete by themselves: `afterClosed()`, the `open()` of dialog services,
  a one-off check. No unsubscribe needed.
- Observables that never complete (`interval`, `BreakpointObserver`, `valueChanges`, router events) are never
  subscribed by hand: `toSignal()` in a component, `rxMethod` / `mutation()` in a store – both end with their
  injector.
- `takeUntilDestroyed(this.destroyRef)` when a callback must not run after the page was left (an HTTP check that then
  opens a dialog). Dialogs close themselves on navigation.

## A new feature

1. Folder `src/app/<feature>/`, add it to the feature list in `eslint.config.js`.
2. API: `shared/api/<resource>/` for every controller it needs (skill `api`).
3. Model with commands and rules, mapper + spec, store + spec.
4. Route in `app.routes.ts` (`ROUTE.<FEATURE>`, `title` = translation key), entry in the navigation
   (`core/navigation/navigation.component.ts`, group "Betrieb" or "Hardware").
5. Translations under `<feature>.*` in `public/i18n/de.json` and `en.json`.
6. Pages and components (skills `component`, `design`, `dialog`), specs (skill `testing`).

## Checklist

- [ ] Code in the right place: model / mapper / store in the feature, enums in `shared/models`, nothing from another
      feature (`npx ng lint`)
- [ ] Own model for another resource's data, loaded by the feature's own store
- [ ] Rules as pure functions in the model, with why and a spec; portal-only rules noted in the backend README
- [ ] Commands with the ids named after the entity, typed variable before emit
- [ ] Mapper: every field explicit, dates with `parseISO`, `?? null`; spec per function
- [ ] Store: resources in `withProps`, mapping in `withComputed`, every change a `mutation` that reloads exactly what
      changed; polling only via `rxMethod`, `isLoading` without the polls
- [ ] Store spec with every resource flushed and each mutation's request and reload checked (skill `testing`)

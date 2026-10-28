---
name: api-service
description: How to write or change an API service in schaffbar-portal (Angular 22) – a `*.service.ts` that talks to the schaffbar-backend via `httpResource` and `HttpClient`. Use when adding a new feature service, adding or changing an endpoint (query or command), wiring a service into a signal store, or reviewing a service.
---

# API services in schaffbar-portal

An API service is a thin, stateless translation layer between the backend REST API and the signal stores:

- **queries** (data that is displayed) → a method returning an `httpResource`
- **commands** (every change in the backend) → a method taking a `…Command` and returning an `Observable`

State, loading flags, error handling, reload-after-save and double-click protection live in the **store**, not in
the service. Reference implementations:
[dashboard.service.ts](../../../src/app/dashboard/dashboard.service.ts) (queries only),
[tools.service.ts](../../../src/app/tools/tools.service.ts) (queries, one-off check, commands),
[users.service.ts](../../../src/app/users/shared/services/users.service.ts) (commands with a converted request body).

## Before writing code

1. Look up the endpoint in the backend controller
   (`../schaffbar-backend/core-pos/src/main/java/de/schaffbar/core_pos/domain/<feature>/web/*Controller.java`):
   path, HTTP method, query parameter names (e.g. `customer-id` vs. `customerId`), request body and response body
   (does it return the created entity, a list where one value is expected, or an empty body?).
2. Decide per endpoint: is the result **displayed** (→ resource) or a **one-off check at the time of an action**
   (→ plain observable), or does it **change** something (→ command)?
3. Commands need a command interface in the feature's `*.model.ts` (section `// commands`) – see CLAUDE.md
   "Commands".

## File skeleton

```ts
import { HttpClient, HttpResourceRef, httpResource } from '@angular/common/http';
import { inject, Service } from '@angular/core';

import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import { CreateToolCommand, DeleteToolCommand, Tool } from './tool.model';

const TOOLS_API_URL = `${environment.apiBaseUrl}/api/v1/tools`;

@Service()
export class ToolsService {
  private readonly http = inject(HttpClient);

  // --------------------------------------------------------------------------
  // queries

  // --------------------------------------------------------------------------
  // commands
}
```

- `@Service()` (Angular 22), not `@Injectable({ providedIn: 'root' })`. Dependencies only via `inject()` –
  `@Service` classes cannot use constructor injection.
- One module-level constant `<FEATURE>_API_URL` built from `environment.apiBaseUrl`; a second constant only if the
  backend uses a second base path (see `DASHBOARD_TOOL_API_URL`).
- `private readonly http = inject(HttpClient)` – only if the service has commands or one-off checks (a pure query
  service like `DashboardService` doesn't need it).
- Sections `// queries` and `// commands`, separated by the `// ----` line.

## Queries → `httpResource`

```ts
/**
 * Loads all tools; reload with `reload()`.
 * Must be called in an injection context (e.g. a field initializer or `withProps` of a signal store).
 */
toolsResource(): HttpResourceRef<Tool[] | undefined> {
  return httpResource<Tool[]>(() => TOOLS_API_URL);
}

/**
 * Loads the tool whenever the id changes; no request while the id is `null`.
 * Must be called in an injection context (e.g. a field initializer or `withProps` of a signal store).
 */
toolResource(id: () => string | null): HttpResourceRef<Tool | undefined> {
  return httpResource<Tool>(() => {
    const toolId = id();
    return toolId ? `${TOOLS_API_URL}/${toolId}` : undefined;
  });
}
```

Rules:

- Name: `<entity>Resource` / `<entities>Resource` / `<entities>By<Filter>Resource`
  (`toolResource`, `usersResource`, `certificationsByToolResource`).
- Return type is spelled out: `HttpResourceRef<T | undefined>` (`undefined` = not loaded yet / no request).
- Parameters are **getter functions** (`() => string | null`), never plain values – the resource reloads when the
  signal changes. Return `undefined` from the request function while a required id is missing → no request.
- Query parameters go through the request object, **never** string concatenation (`?toolId=${id}`):

  ```ts
  return id ? { url: TOOL_CERTIFICATIONS_API_URL, params: { toolId: id } } : undefined;
  ```

- Response shape differs from what the page needs → `parse` option, with a comment why
  (`rfidTagAssignmentResource`: backend returns a list, the page needs one value or `null`):

  ```ts
  return httpResource(
    () => { … },
    // the backend returns a list; the customer has at most one current assignment
    { parse: (assignments) => (assignments as RfidTagAssignment[])[0] ?? null },
  );
  ```

- "Nothing there" answered with 200 + empty body → type it as `T | null`
  (`openSessionResource(): HttpResourceRef<WorkshopSession | null | undefined>`) and say so in the JSDoc.
- The JSDoc always contains the line _"Must be called in an injection context (e.g. a field initializer or
  `withProps` of a signal store)."_ – `httpResource` throws otherwise.
- No `subscribe`, no `toSignal`, no caching in the service – the resource **is** the state, owned by the store.

## One-off checks → plain observable

Only for a read that happens **at the time of an action** and is not shown on the page
(e.g. "does a tool still use this reader?" before changing the reader type):

```ts
/** One-off check at the time of an action (no displayed state, so no resource). */
getToolsByRfidReader(rfidReaderId: string): Observable<Tool[]> {
  return this.http.get<Tool[]>(TOOLS_API_URL, { params: { rfidReaderId } });
}
```

Prefix `get…`, plain value parameters, the JSDoc line above. If you are unsure → it is displayed data → resource.

## Commands → `Observable`

```ts
createTool(command: CreateToolCommand): Observable<Tool> {
  return this.http.post<Tool>(TOOLS_API_URL, command);
}

changeRfidReader(command: ChangeRfidReaderCommand): Observable<void> {
  const url = `${TOOLS_API_URL}/${command.toolId}/rfid-reader/${command.rfidReaderId}`;
  return this.http.put<void>(url, {});
}

removeInstructors(command: RemoveToolInstructorsCommand): Observable<void> {
  const url = `${TOOLS_API_URL}/${command.toolId}/instructors`;
  return this.http.delete<void>(url, { body: { instructorIds: command.instructorIds } });
}

deleteTool(command: DeleteToolCommand): Observable<void> {
  return this.http.delete<void>(`${TOOLS_API_URL}/${command.id}`);
}
```

Rules:

- One method per backend action, named like the command without the `Command` suffix
  (`UpdateUserContactCommand` → `updateUserContact`).
- **Always exactly one parameter: the command** – also for a single id (`DeleteToolCommand { id }`, not `string`).
  Ids for the URL come from the command. Component output, store method and service method use the same command type.
- Return type matches the backend response: `Observable<void>` for an empty body, `Observable<Entity>` when the
  backend returns the entity.
- Body:
  - The backend takes the command as is → pass `command` directly.
  - The backend expects another shape → build a `requestBody` (or an inline object with the needed fields):

    ```ts
    const requestBody = {
      ...command,
      dateOfBirth: format(command.dateOfBirth, 'yyyy-MM-dd'),
    };
    return this.http.post<User>(USERS_API_URL, requestBody);
    ```

  - Dates: `format()` / `parseISO()` from `date-fns` – never inject `DatePipe` or other pipes into a service.
- **The second parameter of `put` / `post` / `patch` is the body.** A PUT/POST without payload passes `{}` –
  otherwise the options end up as the body (real bug that happened in `closeWorkshopSession`).
- DELETE with payload: `{ body: … }` in the options.
- A URL that doesn't fit on the line goes into a `const url` first.

## Don'ts

- No `HttpHeaders` / `httpOptions` / `Content-Type: application/json` – `HttpClient` sets it. Auth will come as an
  interceptor, not per service.
- No error handling (`catchError`, snackbars, `console.error`) – the global `errorInterceptor` shows a snackbar,
  `mutation()` keeps the store alive; the service just lets the error through.
- No `subscribe`, `tap` side effects or state (signals, `BehaviorSubject`, caches) in the service.
- No public `http` field, no constructor injection, no `@Injectable`.
- No unused methods – delete them instead of keeping them "for later".
- No string-concatenated query strings.

## Wiring into a store

Resources and commands are only used through signal stores. The one exception is a one-off check, which the
component may call directly at the time of the action (`rfid-reader-detail.component.ts` →
`getToolsByRfidReader`). The store creates the resources in `withProps` and wraps each command in `mutation()` –
details in CLAUDE.md "Stores":

```ts
export const ToolsStore = signalStore(
  { providedIn: 'root' },
  withProps(() => {
    const toolsService = inject(ToolsService);
    return {
      _toolsService: toolsService,
      _toolsResource: toolsService.toolsResource(),
    };
  }),
  withComputed(({ _toolsResource }) => ({
    // hasValue() stays true while reloading, so the list doesn't flicker
    entities: computed(() => (_toolsResource.hasValue() ? _toolsResource.value() : [])),
    isLoading: computed(() => _toolsResource.isLoading()),
    hasLoadError: computed(() => !!_toolsResource.error()),
  })),
  withMethods((store) => ({
    reloadTools(): void {
      store._toolsResource.reload();
    },
    createTool: mutation<CreateToolCommand>({
      request: (command) => store._toolsService.createTool(command),
      onSuccess: () => store._toolsResource.reload(),
    }),
  })),
);
```

A detail store keeps the id in its state (`withState({ toolId: null })`) and passes the state signal itself – it
is already a `() => string | null`: `_toolResource: toolsService.toolResource(store.toolId)` in
`withProps((store) => …)`. The page only sets the id; the resource loads and reloads on its own.

## Testing

API services get no spec of their own – they are tested through the **store spec** with `HttpTestingController`
(see [tools.store.spec.ts](../../../src/app/tools/tools.store.spec.ts)):

- Providers: `provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting()`;
  `afterEach(() => httpMock.verify())`.
- `TestBed.tick()` **before** `httpMock.expectOne(…)` – the resource sends its request only after change detection.
- `await TestBed.inject(ApplicationRef).whenStable()` only **after** `flush(…)` – before it, it hangs.
- Match requests by method and URL (`req.method === 'POST' && req.url === url`); check `params` and `body` of
  commands where the service converts something (dates, subsets).

## Checklist

- [ ] Endpoint checked in the backend controller (path, method, param names, response)
- [ ] Command interface in `*.model.ts` under `// commands`
- [ ] `@Service()`, `inject()`, `private readonly http`, `<FEATURE>_API_URL`, `// queries` / `// commands`
- [ ] Displayed data → `…Resource()` with getter params, `undefined` while an id is missing, JSDoc
- [ ] Commands take exactly one command, body as 2nd argument (`{}` if empty), no headers, no error handling
- [ ] Store wires resource + `mutation()`, store spec covers the new request
- [ ] `npx ng build`, `npx ng lint`, `npx ng test --watch=false`, `npx prettier --check "src/**/*.{ts,html,scss}"`

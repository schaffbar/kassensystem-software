---
name: api
description: How to talk to the schaffbar-backend in schaffbar-portal (Angular 22) – the API layer in `src/app/shared/api/<resource>/` (zod schema, `…ApiModel`, `…Request`, `…ApiService` with `httpResource`/`HttpClient`), the feature model and mapper, and wiring both into a signal store. Use when adding or changing an endpoint (query or command), migrating an old `*.service.ts` to `shared/api`, adding a field or enum from the backend, writing a mapper, or reviewing or testing any of these.
---

# API layer in schaffbar-portal

The backend API exists once, so everything about it lives in one place: `src/app/shared/api/`. Features never see
the API's shapes – they work with their own models, and a mapper translates between the two:

```
backend ──JSON──▶ shared/api/<resource>/            feature (<feature>/)
                    <entity>.api-model.ts ◀──────── <entity>.mapper.ts ──▶ <entity>.model.ts
                    <resource>.api-service.ts ◀──── <feature>.store.ts      (feature model, commands)
                              │                          ▲
                              └──── shared/models/ ──────┘   enums: shared vocabulary of both
```

- **API layer** (`shared/api/<resource>/`): what goes over the wire. The zod schema validates every response at
  runtime, so a changed backend contract shows up as a clear error instead of `undefined` somewhere in a template.
- **Feature** (`<feature>/`): what the feature works with – its model, its commands, its mapper, its store.
- **Shared vocabulary** (`shared/models/`): enums, one file per enum. Used by API models and features alike.

Reference implementations:
[shared/api/rfid-readers/](../../../src/app/shared/api/rfid-readers/) +
[rfid-readers/](../../../src/app/rfid-readers/) (resource + detail resource, enum with `null`, date, commands with
body), [shared/api/rfid-tags/](../../../src/app/shared/api/rfid-tags/) + [rfid-tags/](../../../src/app/rfid-tags/)
(smallest case).

## Who may import what

| From                                                     | may import `shared/api` | may import `shared/models` |
| -------------------------------------------------------- | ----------------------- | -------------------------- |
| `shared/api/**`                                          | –                       | yes                        |
| mappers, stores, dialog containers                       | yes                     | yes                        |
| components, pages (`*.component.ts`), pipes, their specs | **no** (ESLint)         | yes                        |

Components get feature models from the store and emit commands – they don't know the API exists. ESLint enforces it
(`no-restricted-imports` for `**/shared/api/**` in `eslint.config.js`).

## Before writing code

Look up the endpoint in the backend
(`../schaffbar-backend/core-pos/src/main/java/de/schaffbar/core_pos/domain/<feature>/web/`):

- `*Controller.java`: path, HTTP method, path variables, query parameter names (`customer-id` vs. `customerId`),
  response status (201 with only a `Location` header = no body).
- `*ApiModel.java`: the response record `…ApiDto` (→ schema) and the request record `…RequestBody` (→ `…Request`).
  Check every field's nullability: without `@NotNull` / `@NotBlank` (or a primitive type) the field can be `null` –
  Jackson always writes `null` fields, so they are **`.nullable()`, never `.optional()`**.

## Files

```
src/app/shared/models/
  rfid-reader-type.ts                   export enum RfidReaderType { … }
src/app/shared/api/
  api-url.ts                            API_URL (`…/api/v1`)
  parse.ts                              parseWith(schema, endpoint)
  <resource>/                           one folder per backend controller (rfid-readers, customers, …)
    <x>.api-model.ts                    like backend XApiModel.java: schemas, …ApiModel, …Request
    <resource>.api-service.ts           …ApiService
src/app/<feature>/
  <entity>.model.ts                     feature model + commands
  <entity>.mapper.ts                    to<Entity>, to<Action>Request (+ .spec.ts)
  <feature>.store.ts                    resource + mapping + mutations
```

Names: folder = the backend path (`rfid-readers`), files `rfid-reader.api-model.ts` / `rfid-readers.api-service.ts`,
class `RfidReadersApiService`, schema `rfidReaderSchema`, type `RfidReaderApiModel`, request
`UpdateRfidReaderRequest`.

- API names follow the **backend**: the folder and the service are named after the controller path
  (`/api/v1/workshop-dashboard` → `workshop-dashboard/`, `workshop-dashboard.api-service.ts`), the model file after
  the backend `…ApiModel.java` (`WorkshopDashboardApiModel.java` → `workshop-dashboard.api-model.ts`), the types
  inside after its records (`WorkshopDashboardEntryApiDto` → `WorkshopDashboardEntryApiModel`,
  `UpdateRfidReaderRequestBody` → `UpdateRfidReaderRequest`). Feature names follow the **feature**: the dashboard
  calls the same thing `ActiveUser`. The mapper connects them (`toActiveUser`).
- One feature using two controllers gets two API folders and injects both services in its store
  (`dashboard` → `shared/api/workshop-dashboard/` + `shared/api/tool-dashboard/`).
- The mapper is named like the feature's model file: `rfid-reader.model.ts` → `rfid-reader.mapper.ts`,
  `dashboard.model.ts` (several models) → `dashboard.mapper.ts`.

## API model

```ts
import * as z from 'zod';

import { RfidReaderType } from '../../models/rfid-reader-type';

export const rfidReaderSchema = z.object({
  id: z.string(),
  macAddress: z.string(),
  type: z.enum(RfidReaderType).nullable(),
  name: z.string().nullable(),
  socketName: z.string().nullable(),
  createdAt: z.iso.datetime(),
});

export type RfidReaderApiModel = z.infer<typeof rfidReaderSchema>;

// ----------------------------------------------------------------------------
// requests

export interface UpdateRfidReaderRequest {
  name: string | null;
  socketName: string | null;
}
```

Java → zod:

| Backend (`…ApiDto`)                   | Schema                                         |
| ------------------------------------- | ---------------------------------------------- |
| `@NotBlank String`, `@NotNull String` | `z.string()`                                   |
| `@NotNull UUID`, ids                  | `z.string()` (ids are opaque)                  |
| `String`                              | `z.string().nullable()`                        |
| `boolean` / `@NotNull Boolean`        | `z.boolean()`                                  |
| `long`, `int`, `@PositiveOrZero long` | `z.number()`                                   |
| `@NotNull Instant`                    | `z.iso.datetime()`                             |
| `LocalDate`                           | `z.iso.date()`                                 |
| enum as `String`                      | `z.enum(RfidReaderType)` (+ `.nullable()`)     |
| `List<X>`                             | `z.array(xSchema)`                             |
| nested record                         | its own schema (`toolUsageSchema`), referenced |

Rules:

- **`import * as z from 'zod'`** – `import { z }` pulls all of zod into the bundle (80 instead of 25 kB gzip). ESLint
  rejects it.
- The schema describes the wire format only: **no `.transform()`, no defaults**. Dates stay strings
  (`z.iso.datetime()` checks the format), `null` stays `null` – converting is the mapper's job.
- The type is always derived: `type XApiModel = z.infer<typeof xSchema>` – never written by hand.
- Unknown fields are dropped by `z.object` – a new backend field breaks nothing.
- Enums come from `shared/models/<enum-name>.ts` (a TypeScript `enum`, values = the backend strings);
  `z.enum(RfidReaderType)` accepts it directly. No enum is defined in an API model.
- Requests are plain interfaces mirroring the backend `…RequestBody` field by field (outgoing data is not
  validated). Missing values are `null`, not optional: `name: string | null`.

## API service

```ts
const RFID_READERS_API_URL = `${API_URL}/rfid-readers`;

@Service()
export class RfidReadersApiService {
  private readonly http = inject(HttpClient);

  // --------------------------------------------------------------------------
  // queries

  rfidReaderResource(id: () => string | null): HttpResourceRef<RfidReaderApiModel | undefined> {
    return httpResource(
      () => {
        const rfidReaderId = id();
        return rfidReaderId ? `${RFID_READERS_API_URL}/${rfidReaderId}` : undefined;
      },
      { parse: parseWith(rfidReaderSchema, 'GET /rfid-readers/{rfidReaderId}') },
    );
  }

  // --------------------------------------------------------------------------
  // commands

  updateRfidReader(rfidReaderId: string, request: UpdateRfidReaderRequest): Observable<void> {
    return this.http.put<void>(`${RFID_READERS_API_URL}/${rfidReaderId}`, request);
  }

  deleteRfidReader(rfidReaderId: string): Observable<void> {
    return this.http.delete<void>(`${RFID_READERS_API_URL}/${rfidReaderId}`);
  }
}
```

General:

- `@Service()` (Angular 22), dependencies via `inject()`. `private readonly http` only if the service has commands or
  one-off checks.
- One constant `<RESOURCE>_API_URL` built from `API_URL` (`shared/api/api-url.ts`) – never from `environment`
  directly.
- Sections `// queries` and `// commands`, separated by the `// ----` line.
- **No JSDoc and no comments on the methods** – name and signature say what a method does
  (`rfidReaderResource(id)`, `updateRfidReader(rfidReaderId, request)`, `getToolsByRfidReader(rfidReaderId)`). If a
  method seems to need an explanation, find a better name. The only comment is one for what the code can't say (why
  a list is reduced to one value, see below).
- The service is stateless: no `subscribe`, no `tap`, no signals, no caching, no error handling (the
  `errorInterceptor` shows HTTP errors, `mutation()` keeps the store alive). No `HttpHeaders` / `Content-Type`.
- No unused methods – an endpoint the portal doesn't call gets no method.

### Queries → `httpResource` + `parseWith`

- Name: `<entity>Resource` / `<entities>Resource` / `<entities>By<Filter>Resource`.
- Return type spelled out: `HttpResourceRef<XApiModel | undefined>` (`undefined` = not loaded yet / no request).
- **Every resource validates**: `{ parse: parseWith(schema, 'GET /path/{param}') }`. The endpoint label uses the
  Spring path syntax of the controller. A list is `parseWith(z.array(xSchema), …)`.
- `parseWith` logs the endpoint and the zod path (`✖ Invalid input … → at type`) and throws; the resource goes into
  its error state and the page shows its load error. Watch the console when a page says "load failed".
- Parameters are getter functions (`() => string | null`); return `undefined` from the request function while a
  required id is missing. Query parameters go through `{ url, params }`, never string concatenation.
- 200 with an empty body ("nothing there") → `parseWith(xSchema.nullable(), …)`, type
  `XApiModel | null | undefined` – the type says it, no comment needed.
- The backend returns a list where the portal needs one value → validate the list, then pick, with a comment why:
  ```ts
  // the backend returns a list; the customer has at most one current assignment
  const parseAssignments = parseWith(z.array(assignmentSchema), 'GET /rfid-tag-assignments');
  return httpResource(() => …, { parse: (response) => parseAssignments(response)[0] ?? null });
  ```
- A resource must be created in an injection context (field initializer, `withProps` of a store) – `httpResource`
  throws otherwise, so no reminder in the code is needed.

### One-off checks → observable + `parseWith`

A read at the time of an action that is not shown on the page (e.g. "does a tool still use this reader?") is a plain
observable, validated the same way:

```ts
getToolsByRfidReader(rfidReaderId: string): Observable<ToolApiModel[]> {
  return this.http
    .get<unknown>(TOOLS_API_URL, { params: { rfidReaderId } })
    .pipe(map(parseWith(z.array(toolSchema), 'GET /tools?rfidReaderId')));
}
```

Prefix `get…`, plain value parameters. If you are unsure → it is displayed data → resource.

### Commands → `Observable<void>`

- The signature mirrors the controller: **path variables as plain arguments, the body as `…Request`** –
  `updateRfidReader(rfidReaderId, request)`, `deleteRfidReader(rfidReaderId)`. No command types in `shared/api`.
- Name like the backend action (`updateRfidReader`, `changeRfidReaderType`, `closeWorkshopSession`).
- `Observable<void>` unless the backend really returns a body (then validate it like a query). 201 with only a
  `Location` header is `void`.
- **The second argument of `put` / `post` / `patch` is the body.** Without payload pass `{}` – otherwise the options
  end up as the body. DELETE with payload: `{ body: … }`.

## Feature model

```ts
import { RfidReaderType } from '../shared/models/rfid-reader-type';

export interface RfidReader {
  id: string;
  macAddress: string;
  type: RfidReaderType | null;
  name: string | null;
  socketName: string | null;
  createdAt: Date;
}

// ----------------------------------------------------------------------------
// commands

export interface UpdateRfidReaderCommand {
  id: string;
  name?: string;
  socketName?: string;
}
```

- Shaped for the feature: `Date` instead of ISO strings, only the fields the feature uses, combined values where it
  helps. It starts as a copy of the API model and drifts when the feature needs it to.
- `| null` stays where the value can be missing (CLAUDE.md "Missing values").
- Commands stay here, as described in CLAUDE.md "Commands" – components emit them, stores receive them. They never go
  to the API service directly.

## Mapper

```ts
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

export function toUpdateRfidReaderRequest(command: UpdateRfidReaderCommand): UpdateRfidReaderRequest {
  return {
    name: command.name ?? null,
    socketName: command.socketName ?? null,
  };
}
```

- File `<feature>/<entity>.mapper.ts`, exported pure functions: `to<Entity>(apiModel)` for responses,
  `to<Action>Request(command)` for bodies.
- **Every field explicitly, no spread** (`...apiModel`, `...command`) – otherwise new API fields leak into the
  feature, and ids end up in request bodies.
- Every body gets a mapper, also a trivial one (`{ type: command.type }`) – one rule, no exceptions. Path variables
  don't need one: the store passes `command.id`.
- Dates: `parseISO` / `format` from `date-fns`; a nullable date `apiModel.endTime ? parseISO(apiModel.endTime) : null`.
  Missing values: `?? null`.
- Nested API models (a session with usages) get one mapper each; only the top-level one is exported, the others are
  unexported functions in the same file (`toWorkshopUsage`, `toToolUsage`).
- A resource answering `null` ("nothing there") is mapped in the store like the container does it:
  `const session = resource.hasValue() ? resource.value() : null; return session ? toWorkshopSession(session) : null;`
- Enums pass through unchanged.

## Store

```ts
withProps((store) => {
  const rfidReadersApi = inject(RfidReadersApiService);
  return {
    _rfidReadersApi: rfidReadersApi,
    // (re)loads whenever the id changes
    _rfidReaderResource: rfidReadersApi.rfidReaderResource(store.rfidReaderId),
  };
}),
withComputed(({ _rfidReaderResource }) => ({
  // hasValue() stays true while reloading, so the page doesn't flicker
  rfidReader: computed(() => (_rfidReaderResource.hasValue() ? toRfidReader(_rfidReaderResource.value()) : null)),
  isLoading: computed(() => _rfidReaderResource.isLoading()),
  hasLoadError: computed(() => !!_rfidReaderResource.error()),
})),
withMethods((store) => ({
  updateRfidReader: mutation<UpdateRfidReaderCommand>({
    request: (command) => store._rfidReadersApi.updateRfidReader(command.id, toUpdateRfidReaderRequest(command)),
    onSuccess: () => store._rfidReaderResource.reload(),
  }),
  deleteRfidReader: mutation<DeleteRfidReaderCommand>({
    request: (command) => store._rfidReadersApi.deleteRfidReader(command.id),
    onSuccess: () => store._rfidReadersResource.reload(),
  }),
})),
```

- The resource holds the API model; the store maps in `withComputed` (`.map(toX)` for lists, `toX(…)` for one).
  Components only see the computed.
- The mutation maps command → path variables + request.
- Everything else (state, `isLoading`, `hasLoadError`, `reload…()`) as in CLAUDE.md "Stores".

## Testing

No spec for the API service – it is tested through the store spec. Mappers get their own spec.

Store spec (`HttpTestingController`):

- Providers: `provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting()`;
  `afterEach(() => { httpMock.verify(); vi.restoreAllMocks(); })`.
- **Test data is typed as API model** (`const reader: RfidReaderApiModel = { … }`) and complete – the schema
  validates it, so a missing field fails the test (that is the point). Expect the mapped value:
  `expect(store.rfidReader()).toEqual(toRfidReader(reader))`.
- Check the body of every command with a request mapper: `expect(update.request.body).toEqual({ name: …, socketName: null })`.
- One test per store that an invalid response becomes a load error:
  `vi.spyOn(console, 'error').mockImplementation(() => undefined)`, flush `[{ id: 'id-1' }]`,
  `expect(store.hasLoadError()).toBe(true)`.
- `TestBed.tick()` **before** `httpMock.expectOne(…)`; `await TestBed.inject(ApplicationRef).whenStable()` only
  **after** `flush(…)`.

Fixtures: when several specs need the same complex API model (a session with usages), the resource folder gets
`<x>.api-fixtures.ts` with complete, valid objects and overrides:
`workshopSessionFixture({ toolUsageSummaries: [toolUsageSummaryFixture({ active: true })] })`. Simple models are
written inline in the spec.

Mapper spec: one test per function – a `null` value, a date (`new Date('…Z')`), missing command fields → `null`, no
id in the request.

Component specs use feature models (`createdAt: new Date(…)`) and never API models.

## Migrating an old `*.service.ts`

Not migrated yet: everything except `rfid-tags`, `rfid-readers`, `dashboard` and `workshop-sessions` (old pattern: `<feature>/*.service.ts` or
`users/shared/services/`, one class with commands as parameters, no validation).

1. Backend: controller + `…ApiModel.java` → schema and requests (table above).
2. Enums of the resource → `shared/models/<enum-name>.ts`; update all imports.
3. `shared/api/<resource>/`: api model + api service; delete the old service.
4. Feature: model (keep commands, `Date` for dates), mapper + spec.
5. Stores: inject the `…ApiService`, map in `withComputed`, map in `mutation()`.
6. Specs: test data as complete API models, expectations via the mapper, body checks, one schema-error test;
   component fixtures with `Date`.
7. `npx tsc -p tsconfig.spec.json --noEmit` (the build doesn't type-check specs) + the checks below.

## Checklist

- [ ] Endpoint checked in controller and `…ApiModel.java` (path, method, params, nullability, response status)
- [ ] Schema: `import * as z`, `.nullable()` for nullable fields, `z.iso.datetime()`, `z.enum(X)` from
      `shared/models`, no transforms; type via `z.infer`
- [ ] Requests mirror `…RequestBody`, `| null` for missing values
- [ ] Service: `API_URL`, every resource with `parseWith`, commands `(pathVariable, request)`, body as 2nd argument,
      no JSDoc
- [ ] Feature model + mapper (every field explicit, no spread) + mapper spec
- [ ] Store maps in `withComputed` and `mutation()`; no component imports `shared/api`
- [ ] Store spec: complete API-model test data, body checks, schema-error test
- [ ] `npx ng build`, `npx ng lint`, `npx ng test --watch=false`, `npx tsc -p tsconfig.spec.json --noEmit`,
      `npx prettier --check "src/**/*.{ts,html,scss}"`

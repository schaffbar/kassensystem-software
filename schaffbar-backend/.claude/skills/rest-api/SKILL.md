---
name: rest-api
description: How the HTTP side of the core-pos backend is built and answers – REST controllers in `domain/<aggregate>/web/` and `web/<object>/` (paths, methods, status codes, value objects as path variables and request parameters, one endpoint = one query or one command), the `*ApiModel` (MapStruct mapper, `…ApiDto` responses with ids as strings, `…RequestBody` requests with value objects, nullability as the contract with the portal), the error answers (`ResourceNotFoundException` → 404, `BusinessRuleViolationException` → 409 with its `code`, format errors → 400, bugs → 500 – `ApiExceptionHandler`, `ProblemDetail`, the translation in the portal), and the device endpoints in `web/device/` (the firmware's contract, `*Response` records, always 200 with a text from `DeviceMessages`). Use when adding or changing an endpoint, a request or response field, a status code or an error code, when an exception has to reach the portal or a device, when a device answer changes, or when reviewing a controller or an API model.
---

# The REST API of core-pos

Two kinds of clients call the backend, each with its own contract:

| Client | Controllers | Contract | Errors |
| --- | --- | --- | --- |
| the portal (staff at the counter) | `domain/<aggregate>/web/<Aggregate>Controller`, `web/<object>/<Object>Controller`, path `/api/v1/…` | JSON as the `…ApiDto` / `…RequestBody` records; the portal's zod schemas mirror them (portal skill `api`) | `ProblemDetail` (RFC 9457) with a stable `code` the portal translates |
| the devices (RFID readers, switch boxes) | `web/device/DeviceController`, path `/device/…` | the firmware's field names, unchanged since the firmware cannot change with the backend | always `200`, the text for the display in the body |

A controller only translates between HTTP and the application: a change goes to a use case, a query to its read
facade (skill `use-case`) – never a service, no logic of its own beyond choosing which call to make.

Reference implementations: `domain/rfid_reader/web/` (`RfidReaderController`, `RfidReaderApiModel`) – query, filter,
create with `Location`, update, change of one field, delete; `shared/web/ApiExceptionHandler`; `web/device/`
(`DeviceController`, `DeviceApiModel`, `DeviceMessages`).

## Controller

```java
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/api/v1/rfid-readers")
public class RfidReaderController {

    private final @NonNull RfidReaderReadFacade rfidReaderReadFacade;

    private final @NonNull RfidReaderCreate rfidReaderCreate;

    private final @NonNull RfidReaderUpdate rfidReaderUpdate;

    // ------------------------------------------------------------------------
    // query

    @GetMapping(value = "/{rfidReaderId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RfidReaderApiDto> getRfidReader(@PathVariable @NotNull @Valid RfidReaderId rfidReaderId) {
        RfidReaderApiDto rfidReader = this.rfidReaderReadFacade.getRfidReader(rfidReaderId) //
                .map(RfidReaderApiModel.MAPPER::toRfidReaderApiDto) //
                .orElseThrow(() -> ResourceNotFoundException.rfidReader(rfidReaderId));

        return ResponseEntity.ok(rfidReader);
    }

    // ------------------------------------------------------------------------
    // command

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> createRfidReader(@RequestBody @NotNull @Valid CreateRfidReaderRequestBody requestBody) {
        RfidReaderId rfidReaderId = this.rfidReaderCreate.process(requestBody.macAddress());
        URI location = URI.create("/api/v1/rfid-readers/" + rfidReaderId.getValue());

        return ResponseEntity.created(location).build();
    }

    @PutMapping(value = "/{rfidReaderId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> updateRfidReader( //
            @PathVariable @NotNull @Valid RfidReaderId rfidReaderId, //
            @RequestBody @NotNull @Valid UpdateRfidReaderRequestBody requestBody //
    ) {
        UpdateRfidReaderCommand command = RfidReaderApiModel.MAPPER.toUpdateRfidReaderCommand(rfidReaderId, requestBody);
        this.rfidReaderUpdate.process(command);

        return ResponseEntity.noContent().build();
    }

}
```

### Where and what

- **One controller per aggregate**, `domain/<aggregate>/web/<Aggregate>Controller`, path `/api/v1/<aggregate in kebab
  case, plural>` (`/api/v1/rfid-readers`, `/api/v1/tool-certifications`). An inner entity gets a sub-path of its
  aggregate and a query in the same read facade – no controller of its own (`GET /rfid-tag-assignments/history`).
- A controller of **no aggregate** is `web/<object>/<Object>Controller`, path `/api/v1/<object>`
  (`DashboardController`, `/api/v1/dashboard`). A new one that the portal calls is added to `basePackages` of
  `ApiExceptionHandler`, else its errors become Spring's default answers.
- `@Validated` `@RestController` `@RequiredArgsConstructor` `@RequestMapping(path = …)`, `public class`; the
  dependencies are `private final @NonNull` fields – **its read facade and its use cases, no other bean**.
- Sections `// query` and `// command`; a section only exists when it has content.
- Every handler returns `ResponseEntity<…>` and names its media type: `produces = MediaType.APPLICATION_JSON_VALUE`
  on a query, `consumes = …` on a command with a body.

### Paths and methods

- **An endpoint is a query or a command, never both** (no "get or create" – the device endpoints are the exception,
  below).
- **Queries:** `GET /` (the list, with optional filters), `GET /{id}`, `GET /{id}/<part>` or `GET /<sub-resource>`
  (`/history`). A state of an aggregate that the portal knows by another id is a path of its own:
  `GET /workshop-sessions/{customerId}/open` – the counter knows the person, not the session.
- **Commands:**

  | Change | Method and path | Example |
  | --- | --- | --- |
  | create one | `POST /` with body | `POST /rfid-readers` |
  | create several at once (all or nothing) | `POST /batch` with body | `POST /tool-certifications/batch` |
  | change the aggregate's own fields | `PUT /{id}` with body | `PUT /customers/{customerId}` |
  | set a part | `PUT /{id}/<part>` with body, or the reference in the path | `PUT /customers/{customerId}/contact`, `PUT /tools/{toolId}/rfid-reader/{rfidReaderId}` |
  | an action without values | `PUT /{id}/<verb>`, no body | `PUT /workshop-sessions/{customerId}/close`, `…/pause` |
  | change one field with a rule of its own | `PATCH /{id}/<field>` with body – the one `PATCH`, it stays (decided 23.11.2026) | `PATCH /rfid-readers/{rfidReaderId}/type` |
  | add entries to a list | `POST /{id}/<list>` with body | `POST /tools/{toolId}/instructors` |
  | remove entries, clear a part | `DELETE /{id}/<list>` with body, `DELETE /{id}/<part>` | `DELETE /tools/{toolId}/instructors`, `DELETE /tools/{toolId}/wlan-relais` |
  | delete | `DELETE /{id}` | `DELETE /rfid-readers/{rfidReaderId}` |

- An aggregate addressed by two references instead of an id has both in the path:
  `PUT /tool-certifications/customers/{customerId}/tools/{toolId}/pause`.
- **Query parameters in camelCase** (`?customerId=`, `?rfidTagId=`), path segments in kebab case.

### Parameters

- **`@PathVariable` and `@RequestParam` are value objects** – `@PathVariable @NotNull @Valid RfidReaderId
  rfidReaderId`, never `UUID` or `String` converted by hand. Each value object used so has a
  `StringTo<ValueObject>Converter` (`@Component`) in `shared/util/`; nothing is added for later.
- **An optional filter** is `@RequestParam(required = false) @Valid MacAddress macAddress`, the only parameter that
  may be `null`; the controller chooses the read facade method with `nonNull(…)`. A list filtered to one entry is
  still a list: `getRfidReader(macAddress).stream().toList()`.
- **The body** is `@RequestBody @NotNull @Valid <Name>RequestBody`. The id comes from the path, never from the body;
  the API model puts both into the command (`toUpdateRfidReaderCommand(rfidReaderId, requestBody)`). A use case that
  takes value objects gets them straight from the body (`requestBody.macAddress()`).

### Status codes

| Answer | When |
| --- | --- |
| `200` with body | a query; a list may be empty (`[]`); a state that may not exist answers `200` with an empty body (`getOpenWorkshopSession`, a comment says so) |
| `201` with `Location` | a create whose new resource has a `GET /{id}` – `Location` points there |
| `204` | every other command; a create without such a `GET` (found by something else: `POST /rfid-tag-assignments`; several: `/batch`) |
| `404` | the resource of the path does not exist: the read facade's `Optional` → `orElseThrow(() -> ResourceNotFoundException.<resource>(id))` in the controller, or a use case that does not find a referenced aggregate |
| `409` | a rule – thrown by the use case, the service or the entity |
| `400` | a format: a value object the converter or Jackson rejects, a constraint of the request body, a missing parameter |
| `500` | a bug or broken data (`IllegalStateException`), without details |

## API model

```java
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface RfidReaderApiModel extends ValueObjectMapper {

    RfidReaderApiModel MAPPER = Mappers.getMapper(RfidReaderApiModel.class);

    // ------------------------------------------------------------------------
    // mapping view to response

    @Mapping(target = "id", source = "id.value")
    @Mapping(target = "macAddress", source = "macAddress.value")
    RfidReaderApiDto toRfidReaderApiDto(RfidReaderView rfidReader);

    // ------------------------------------------------------------------------
    // mapping request body to command

    UpdateRfidReaderCommand toUpdateRfidReaderCommand(RfidReaderId id, UpdateRfidReaderRequestBody requestBody);

    // ------------------------------------------------------------------------
    // response

    record RfidReaderApiDto( //
            @NotBlank String id, //
            @NotBlank String macAddress, //
            RfidReaderType type, //
            String name, //
            String socketName, //
            @NotNull Instant createdAt //
    ) {}

    // ------------------------------------------------------------------------
    // request body

    record UpdateRfidReaderRequestBody( //
            String name, //
            String socketName //
    ) {}

}
```

- `<Aggregate>ApiModel` (or `<Object>ApiModel`) next to the controller: a MapStruct `@Mapper(unmappedTargetPolicy =
  ReportingPolicy.ERROR)` interface extending `ValueObjectMapper`, with `MAPPER`, the mapping methods and the records.
  Sections `// mapping view to response`, `// mapping request body to command`, `// response`, `// request body`.
- **A response** is a record `<What>ApiDto`. It carries **no value object and no `UUID`**: ids and values are
  `String` (`@Mapping(target = "id", source = "id.value")`; a `Set<CustomerId>` maps with `ValueObjectMapper.toValue`),
  enums their enum type, dates `Instant` / `LocalDate` – Jackson writes them as ISO strings (`"2026-11-22T09:15:00Z"`,
  `"1990-01-31"`). A nested part is a record of its own (`CustomerAddressApiDto`).
- **A request** is a record `<Change>RequestBody`. It takes value objects (their `@JsonCreator` reads and checks the
  JSON value – a broken one answers `400`) and enums, and carries the same constraints as the command, so the
  controller answers `400` before a use case runs: a shared constraint (`@PostalCode`, `@ToolDescription`) is used on
  both, never copied.
- **The annotations are the contract.** A response field without `@NotNull` / `@NotBlank` (or a primitive) can be
  `null`, and Jackson writes it as `null` – the portal's schema has it `.nullable()`. Making a field nullable,
  renaming or removing one is a change of the contract (below).
- MapStruct maps by name; a field the mapping cannot fill is a compile error (`ReportingPolicy.ERROR`), so a new view
  field cannot silently be missing in the response.

## Errors

Where a rule lives and how its exception is written (message, `CODE`, static factories): skill `aggregate`, "Where a
rule lives". This is what the portal gets:

| Situation | Thrown | Answer |
| --- | --- | --- |
| an id or reference that does not exist | `ResourceNotFoundException.<resource>(id)` | `404`, `code: RESOURCE_NOT_FOUND`, `resource` (`RFID_READER`) |
| a violated rule | a subclass of `BusinessRuleViolationException` (`RfidReaderHasToolAssignedException.delete(id)`) | `409`, its `code` (`RFID_READER_HAS_TOOL_ASSIGNED`) |
| a format | Bean Validation, a value object (`IllegalArgumentException`), Spring (unreadable body, wrong parameter) | `400`, no `code` |
| a bug, broken data | `IllegalStateException` with the ids | `500`, `detail: "Unexpected error"` |

- **`shared/web/ApiExceptionHandler`** (`@RestControllerAdvice` for `domain` and `web.dashboard`, extends Spring's
  `ResponseEntityExceptionHandler`, which answers Spring's own `400`s) turns them into `ProblemDetail`
  (`application/problem+json`). It logs `404`, `409` and its own `400`s at `info`, a `500` at `error` with the stack
  trace; the `500` answer carries no detail of the exception.
- **The `code` is part of the API.** The portal's error interceptor shows the translation `shared.errors.<CODE>` from
  `de.json` and `en.json` (portal skill `api`, "Errors from the backend"); without one it shows a general message.
  **A new or renamed code gets its translation in the portal in the same change** – a text that says what blocks the
  action and what to do, fitting every variant of the exception.
- `detail` is English and for the log – the portal never shows it.
- Never a generic exception (`RuntimeException`, `Exception`) and never a `ResponseEntity` with an error status built
  by hand – the exception decides the answer.

## Device endpoints (`web/device/`)

`/device/init`, `/device/counter`, `/device/card` – the firmware of the readers calls them; the firmware cannot be
changed together with the backend, so **the fields and answers are a contract**:

- **The firmware's names**, as `String`: requests `InitRequestBody(MACADDR)`, `RfidTagRequestBody(MACADDR, RFID)`,
  `DeviceCardRequestBody(DEVUSECASE, MACADDR, RFID)`; responses `InitResponse`, `CounterResponse`,
  `DeviceCardResponse` with the firmware's fields (`STATE`, `ERROR`, `ICON`, `CUSTOMERNAME`, …) – `*Response`
  records with private `@Builder` and static factories named after the answer (`enterOk`, `errorNoAccess`,
  `errorWithoutUseCase`), not `…ApiDto`.
- The controller converts the strings to value objects (`MacAddress.of(requestBody.MACADDR())`) **inside its `try`**.
- **Always `200`**, also for a refusal and for an error: a refusal answers with a text from **`DeviceMessages`**
  (German, short – it fits the display); an unexpected error or a broken request (a MAC address the value object
  rejects, a blank tag) answers `DeviceMessages.UNEXPECTED`, never Spring's `500`. So `web/device/` is not in
  `ApiExceptionHandler`.
- **`DeviceMessages.BY_CODE`** has a text for every code a use case behind `/device/*` can throw; a new rule that a
  device can hit gets its entry and its device test in the same change. Fixed texts (`UNKNOWN_RFID_READER`,
  `WRONG_RFID_READER`, `RFID_READER_WITHOUT_TYPE`, `NO_TOOL`, …) are constants there, never literals in the controller.
- **One endpoint may change and answer** – `/device/card` checks in and returns the stay. The change is a use case,
  the answer comes from `DeviceReadFacade` after it (skill `use-case`): a use case returns no view.
- Logging: a refusal at `info`, an unexpected error at `error` with the stack trace.
- Times on the display are those of the workshop (`WorkshopTime.ZONE`), never of the JVM.

## The contract with the portal

A change of an endpoint or an `*ApiModel` is a change of the portal too – in the same step:

- the portal's `shared/api/<resource>/` (zod schema, `…ApiModel`, `…Request`, API service) and its feature (portal
  skills `api`, `feature`);
- a new error code: its translation (`shared.errors.<CODE>`, `de.json` and `en.json`);
- the request in the Bruno collection (`core-pos/api/<folder>/`, one folder per controller, also `dashboard/` and
  `device/`) for manual testing – every endpoint has one;
- a gap of `docs/aufgaben.md` ("Backend-Funktionen für das Portal") that is closed is deleted there.

Backend and portal are deployed together; an agent does not deploy – it says that both need it.

## Tests

The details are in skill `backend-testing` ("Controller", "Integration tests"); what an endpoint needs:

- **A controller test for every endpoint** (`domain/<aggregate>/web/<Aggregate>ControllerTest`, standalone MockMvc
  with the converters, `Json.messageConverter()` and `ApiExceptionHandler`): the success with status and every JSON
  field the portal reads (dates compared with their value), a filter with and without a match, `404` with
  `ResourceNotFoundException.CODE` and `resource`, `400` for an invalid value object and per constraint of the body
  (`<endpoint>_with<WhatIsWrong>_is400`), and the code of each rejection the use case can throw.
- **The device endpoints** are tested in `integration/device/` – one class per reader type, the whole answer compared
  field by field; a changed answer changes these tests in the same step.
- `shared/web/ApiExceptionHandlerTest` proves the mapping of each kind, with the literal `RESOURCE_NOT_FOUND` as the
  contract with the portal.

## Tests that enforce it

ArchUnit in `core-pos/src/test/java/de/schaffbar/core_pos/architecture/`, part of `mvn -q test`:

| Rule | Test |
| --- | --- |
| controllers live in `domain.<aggregate>.web` or `web.<object>` | `DependencyArchitectureTest.controllersLiveInAWebPackage` |
| a controller depends on no bean but use cases and read facades; one read facade, named after it | `DependencyArchitectureTest.controllersKnowOnlyUseCasesAndReadFacades`, `ReadFacadeArchitectureTest` |
| controllers are public and `@Validated`, fields `private final`, no `UUID` / `String` parameters, every object parameter `@NotNull` (except an optional filter) | `BeanArchitectureTest` |
| `@PathVariable` / `@RequestParam` are never `UUID` or `String` | `BeanArchitectureTest.pathVariablesAndRequestParamsAreValueObjects` |
| responses carry values as `String`, no value object | `ValueObjectArchitectureTest.responsesCarryValuesAsStrings` |
| every MapStruct mapper (also the API models) fails on an unmapped target | `DomainArchitectureTest.mappersFailOnUnmappedTargets` |
| the domain does not know the API models | `DependencyArchitectureTest.domainDoesNotKnowTheApi` |
| an exception is a `ResourceNotFoundException` or a `BusinessRuleViolationException` and declares its `CODE`; no generic exceptions | `ExceptionArchitectureTest` |

Not checked by ArchUnit: paths, methods and status codes, the sections, the Bruno collection, the portal's
translation – they are in this skill and in the review.

## Checklist

- [ ] Endpoint in the controller of its aggregate (or `web/<object>/`), a query or a command, method and path as in
      the table; query parameters in camelCase.
- [ ] Path variables and parameters as value objects with converter; body `@NotNull @Valid`, the id from the path.
- [ ] Status: `200` / `201` + `Location` / `204`; `404` from the controller or the use case, `409` from the rule.
- [ ] API model: `…ApiDto` with strings, enums, `Instant` / `LocalDate`, nullability by annotation; `…RequestBody`
      with value objects and the command's constraints; mapping in MapStruct.
- [ ] New error code: translation in the portal (`de.json`, `en.json`); reachable from a device: `DeviceMessages`.
- [ ] Device endpoint: firmware names, always `200`, texts from `DeviceMessages`, device tests changed with it.
- [ ] Portal `shared/api` and feature, Bruno, `docs/aufgaben.md` in the same step; controller tests;
      `mvn -q test` passes.

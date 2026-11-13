# schaffbar-backend

The backend of the SchaffBar workshop (`core-pos/`): customers, RFID readers, tags and their assignment, tools with
switch box and WLAN relay, certifications, workshop sessions with usages and billing. Spring Boot 3.5, Java 25, JPA on
PostgreSQL with Flyway, MapStruct, Lombok, Bean Validation. Project overview, glossary (UI ↔ code) and git rules:
`../CLAUDE.md`.

## Commands

From `core-pos/` – there is no Maven wrapper:

```bash
mvn -q test                                  # all tests
mvn -q test -Dtest=CustomerDeleteTest        # one class
mvn -q compile                               # also runs the MapStruct and Lombok processors
```

The backend runs on port 5000 (`/api/v1/...`) against PostgreSQL: either `infrastructure/docker-compose.yml`
(postgres + backend + frontend) or from the IDE with `SCHAFFBAR_DS_URL`, `SCHAFFBAR_DS_USERNAME`,
`SCHAFFBAR_DS_PASSWORD` and a git-ignored `application-local.yaml`. **An agent does not start, restart or migrate
the running backend or touch its database** – it says what needs a restart. Requests for manual testing: the Bruno
collection in `core-pos/api/`.

## Architecture

The structure of an aggregate, the use cases, read models, value objects and events: `core-pos/docs/ddd-overview.md`
(German: `ddd-uebersicht.md`). The rules of each aggregate: the `README.md` in its package
(`core-pos/src/main/java/de/schaffbar/core_pos/domain/<aggregate>/README.md`).

```
core_pos/
  domain/<aggregate>/   entity (aggregate root), *Commands, *Service, *Repository (package-private), *Views,
                        *EventFactory, *PayloadMapper, enums, README.md, web/ (*Controller, *ApiModel)
  domain/dashboard/     read models across aggregates (who is in the workshop, which tools run)
  use_case/             one class per action that touches more than one aggregate (`CustomerDelete`, `CloseSession`)
  device/               endpoints the readers and switch boxes call (`/device/init`, `/counter`, `/card`)
  shared/               event + outbox, exceptions, id value objects, billing (`BillableDuration`)
```

## Rules

- **An aggregate changes only itself.** A service validates, calls the entity's command method and saves the entity
  together with its events. Everything that needs a second aggregate – a check, a cleanup, an orchestration – is a
  **use case** in `use_case/`; services never call each other – except the read models in `domain/dashboard/`,
  which only read. A controller calls a service for its own aggregate and
  a use case for everything else.
- **Every state change produces events.** Command methods of the entity return `List<SchaffbarEvent>`, the service
  saves them to the outbox in the same transaction. A new event: `EventType`, payload record in
  `shared/event/payload/`, method in the `*PayloadMapper` and the `*EventFactory`, row in the README's "Domain Events".
- **No foreign keys between aggregates** (`V6__remove_foreign_keys.sql`, DDD): the database does not protect
  references. Deleting something that others point to is a use case that cleans up or rejects (`CustomerDelete`,
  `RfidReaderDelete`, `ToolDelete`).
- **A rule is documented where it lives:** each aggregate README has "Business Rules / Invariants" (with the class
  that enforces it) and "Open Rules" (enforced only in the portal / planned). Implementing a rule moves it from
  "Open Rules" to "Business Rules" – the same change.
- **A violated rule throws a dedicated exception** in `shared/exception/` (`CustomerHasOpenWorkshopSessionException`),
  English message with the ids, static factories for variants (`RfidReaderHasToolAssignedException.delete(id)`).
  There is no `@ControllerAdvice` yet – exceptions reach the portal as `500` (Problems API: `docs/roadmap.md`, A1).
- **Database changes** only through a new Flyway migration `core-pos/src/main/resources/db/migration/V<next>__<what>.sql`
  – never edit an applied one. `ddl-auto: validate`: entity and migration must match.
- **The API is a contract with the portal.** A field's nullability is its Bean Validation (`@NotNull`, `@NotBlank`)
  – the portal's zod schemas mirror it (skill `api` of the portal). A changed `*ApiModel` or endpoint needs the
  portal's `shared/api` changed too, and a gap from `../docs/backend-features.md` that is closed is marked there.

## Code

- Ids are value objects (`CustomerId`, `ToolId`, … in `shared/id/`) in signatures and views; entities store the raw
  `UUID` / `String`.
- Spring beans: `@Service` / `@RestController` + `@Validated` + `@RequiredArgsConstructor`, dependencies as
  `private final @NonNull` fields; public methods validate their parameters (`@NotNull @Valid CustomerId customerId`).
- Commands and views are records (`*Commands`, `*Views` with a MapStruct `MAPPER`); no public setters on entities.
- Style of the surrounding code: `this.` for fields, `//` at the end of a line in a call chain or record header (keeps
  the formatter from joining lines), section comments `// ------…` + `// query` / `// command` / `// helper`.
- `@Transactional` is mixed today (`jakarta.transaction` and Spring's) – follow the file you change; the choice is
  open (`../TODO.md`).
- Comments only for what the code can't say (why, a non-obvious rule); no JavaDoc that repeats a name or signature.

## Tests

- JUnit 5, Mockito (`@ExtendWith(MockitoExtension.class)`, `@Mock`, `@InjectMocks`), AssertJ.
- **A use case** is tested with mocked services: one test per path – allowed, each rejection with its exception, the
  order of side effects where it matters (`InOrder`) – see `use_case/CustomerDeleteTest.java`.
- **Entity logic** (billing, state transitions) is tested on the entity without mocks, one class per method:
  `<Entity>_<Method>Test` (`ToolUsage_GetBillableDurationTest`, `WorkshopSession_CloseTest`).
- Mockito runs strict: a stub a test does not use fails it – stub per test, not in a shared `@BeforeEach`.
- Test data for views: `builder/ViewBuilder.java`, records via their Lombok builders.
- There are no tests with a Spring context or a database: repository queries, migrations and the mapping are not
  covered – say so when a change depends on them.

## Definition of done

1. Rule or behaviour in the right place (entity, service or use case), dedicated exception, tests for each path;
   `mvn -q test` passes.
2. The aggregate README updated (rules moved, new events); `core-pos/docs/ddd-overview.md` (and `ddd-uebersicht.md`)
   when a use case, event or aggregate is added.
3. Portal impact named: API change → portal `shared/api`; rule the portal checked so far → it may drop its
   pre-check later (only with the Problems API).
4. A finished task ticked off in `../docs/roadmap.md`; nothing committed (`../CLAUDE.md`).
5. The report says what was tested, what needs a restart or a migration, and what was not checked.

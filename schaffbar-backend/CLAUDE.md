# schaffbar-backend

The backend of the SchaffBar workshop (`core-pos/`): customers, RFID readers, tags and their assignment, tools with
switch box and WLAN relay, certifications, workshop sessions with usages and billing. Spring Boot 3.5, Java 25, JPA on
PostgreSQL with Flyway, MapStruct, Lombok, Bean Validation. Project overview, glossary (UI ↔ code) and git rules:
`../CLAUDE.md`.

## Commands

From `core-pos/` – there is no Maven wrapper:

```bash
mvn -q test                                  # import and line ending check (Spotless) + all tests
mvn -q test -Dtest=CustomerDeleteTest        # one class
mvn -q compile                               # also runs the MapStruct and Lombok processors
mvn -q spotless:apply                        # sorts the imports, removes unused ones, converts CRLF to LF
```

The backend runs on port 5000 (`/api/v1/...`) against PostgreSQL: either `infrastructure/docker-compose.yml`
(postgres + backend + frontend) or from the IDE with `SCHAFFBAR_DS_URL`, `SCHAFFBAR_DS_USERNAME`,
`SCHAFFBAR_DS_PASSWORD` and a git-ignored `application-local.yaml`. **An agent does not start, restart or migrate
the running backend or touch its database** – it says what needs a restart. Requests for manual testing: the Bruno
collection in `core-pos/api/`.

## Skills

The rules below are the short form; the skills in `.claude/skills/` have all of them, with examples. Load the skill
before working on its topic.

| Task                                                                                                            | Skill             |
| --------------------------------------------------------------------------------------------------------------- | ----------------- |
| an aggregate in `domain/<aggregate>/`: entity, commands, events, repository, service, views, where a rule lives | `aggregate`       |
| tests: which level proves what, naming, service / use case / controller tests, integration fixture, ArchUnit    | `backend-testing` |

Skills for use cases, error handling and the REST API follow (roadmap K5). A skill holds every rule of its topic – also those the ArchUnit tests check; a changed ArchUnit rule changes its skill in the same step.

## Architecture

The structure of an aggregate, the use cases, read models, value objects and events: `core-pos/docs/ddd-overview.md`
(German: `ddd-uebersicht.md`). The rules of each aggregate: the `README.md` in its package
(`core-pos/src/main/java/de/schaffbar/core_pos/domain/<aggregate>/README.md`).

```txt
core_pos/
  domain/<aggregate>/   entity (aggregate root, named after the package), inner entities (`RfidTagAssignmentHistory`),
                        *Commands, *Service, *Repository (package-private), *Views, *EventFactory, *PayloadMapper,
                        enums, README.md, web/ (*Controller, *ApiModel)
  use_case/             one class per change (`RfidReaderCreate`, `CustomerDelete`, `CloseSession`)
  read_facade/          one class per controller for its queries (`RfidReaderReadFacade`, `WorkshopDashboardReadFacade`)
  dashboard/web/        controllers of the overview (who is in the workshop, which tools run)
                        – until the rollout still `domain/dashboard/` with services instead of read facades (E17)
  device/               endpoints the readers and switch boxes call (`/device/init`, `/counter`, `/card`)
  shared/               event + outbox, exceptions, id value objects, billing (`BillableDuration`)
```

## Rules

- **An aggregate changes only itself.** A service validates, calls the entity's command method and saves the entity
  together with its events. Everything that needs a second aggregate – a check, a cleanup, an orchestration – is a
  **use case** in `use_case/`; services never call each other. **A controller knows only use cases and read
  facades:** every change goes through a use case, every query through the controller's read facade – also when it
  only forwards to one service (E2, E17 in `../docs/backend-consistency.md`).
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
  English message with the ids, static factories for variants (`RfidReaderHasToolAssignedException.delete(id)`). It
  extends `BusinessRuleViolationException` (→ `409` with a stable `code` the portal translates: `public static final String CODE`,
  the class name in UPPER_SNAKE_CASE without `Exception`, shared by its variants) or is a
  `ResourceNotFoundException` (→ `404`); `shared/web/ApiExceptionHandler` turns them into `ProblemDetail`. Impossible
  states (broken data) are an `IllegalStateException` (→ `500`). Many exceptions do not have a base class yet (E8).
- **Database changes** only through a new Flyway migration `core-pos/src/main/resources/db/migration/V<next>__<what>.sql`
  – never edit an applied one. `ddl-auto: validate`: entity and migration must match.
- **The API is a contract with the portal.** A field's nullability is its Bean Validation (`@NotNull`, `@NotBlank`)
  – the portal's zod schemas mirror it (skill `api` of the portal). A changed `*ApiModel` or endpoint needs the
  portal's `shared/api` changed too, and a gap from `../docs/backend-features.md` that is closed is marked there.

### Use cases (until the skill `use-case`)

- One class per action in `use_case/`, named `<Aggregate><Action>` (`RfidReaderChangeType`, `ToolAddInstructors`) –
  also for a plain create, update or delete, so every controller looks the same.
- `@Service` `@Validated` `@RequiredArgsConstructor`, one public method `process(…)` annotated `@Command`.
- Checks that a referenced aggregate exists (`customerService.getCustomer(id).orElseThrow(() -> ResourceNotFoundException.customer(id))`) –
  for a list all entries before the change –, then the rules with a second
  aggregate, then the service call(s). A batch is all or nothing (E11).

### Read facades (until their skill)

- One class per controller in `read_facade/`, named after it (`RfidReaderReadFacade`, `WorkshopSessionReadFacade`,
  `WorkshopDashboardReadFacade`): `@Service` `@Validated` `@RequiredArgsConstructor`,
  `@Query` on every public method.
- Public methods are queries (`get…`) and return views – of one aggregate, or a view of its own that combines several
  (`WorkshopDashboardViews`, next to the facade). It reads services (any number) and never changes anything.

### Controller and API model (until the skill `rest-api`)

- `domain/<aggregate>/web/<Aggregate>Controller`: `@RestController` `@Validated` `@RequiredArgsConstructor`, path
  `/api/v1/<aggregate-in-kebab-case>`. Commands call a use case, queries its read facade – never a service.
- **An endpoint is a query or a command**, never both (no "get or create").
- `@PathVariable` and `@RequestParam` are value objects (`@PathVariable RfidReaderId rfidReaderId`), never `UUID` or
  `String` converted by hand – each needs its `StringTo<ValueObject>Converter` in `shared/util/`. An optional filter is
  `@RequestParam(required = false)` with a `nonNull(…)` check.
- `<Aggregate>ApiModel`: a MapStruct `@Mapper` extending `ValueObjectMapper` with the response records (`…ApiDto`) and
  request records (`…RequestBody`). Ids in responses are `String` (`@Mapping(target = "id", source = "id.value")`),
  request bodies take value objects (their `@JsonCreator` reads them).

## Code

- Ids are value objects (`CustomerId`, `ToolId`, … in `shared/id/`) in signatures and views; entities store the raw
  `UUID` / `String`.
- Spring beans: `@Service` / `@RestController` + `@Validated` + `@RequiredArgsConstructor`, dependencies as
  `private final @NonNull` fields; public methods validate their parameters (`@NotNull @Valid CustomerId customerId`).
- Commands and views are records (`*Commands`, `*Views` with a MapStruct `MAPPER`); no public setters on entities.
- `this.` for fields; section comments `// ------…` (ending at column 79) + `// query` / `// command` / `// helper`.
- **Every public method of a service, use case or read facade is `@Query` or `@Command`** (`shared/cqrs/`, E21): they
  bring the transaction (`@Query` = `@Transactional(readOnly = true)`, `@Command` = `@Transactional`), so these
  classes never use `@Transactional` themselves. Spring Data's `@Query` belongs only to repositories. Older code still
  has `@Transactional` (also jakarta's) – replace it when you touch the class.
- Comments only for what the code can't say (why, a non-obvious rule); no JavaDoc that repeats a name or signature.

### Formatting

The IntelliJ code style of the project is `core-pos/.idea/codeStyles/Project.xml` – code written outside the IDE must
look as if it had been formatted with it. The build checks imports and line endings (Spotless, `process-sources`); fix
them with `mvn -q spotless:apply`, the rest of the style is not checked.

- **Line endings LF**, never CRLF – also for files saved by IntelliJ on Windows (`.gitattributes`: `eol=lf`).

- **Imports:** single-class imports, never `*`. Four groups, each sorted alphabetically, separated by one blank line:

  ```java
  import static org.assertj.core.api.Assertions.assertThat;      // 1. static imports

  import java.util.List;                                          // 2. java.*, javax.*

  import jakarta.validation.constraints.NotNull;                  // 3. everything else
  import lombok.RequiredArgsConstructor;                          //    (com, jakarta, lombok, org, …)
  import org.springframework.stereotype.Service;

  import de.schaffbar.core_pos.shared.id.CustomerId;              // 4. de.schaffbar.*
  ```

  No unused imports.

- **Line length 160.** The formatter does not keep line breaks: it joins lines up to 160 characters. A `//` at the end
  of a line keeps a call chain or a record header wrapped – one element per line:

  ```java
  return this.rfidReaderRepository.findById(id.getValue()) //
          .map(RfidReaderViews.MAPPER::toRfidReaderView);
  ```

- Braces always (`if`, `for`, `while`, `do`), also around a single statement; `else`, `catch`, `finally` on a new line.
- Blank lines: at most one in a row; one around every field; one after the class header and before the closing brace.
- Parameters and arguments wrap only when the line is too long, not aligned; continuation indent 8.
- A body without content stays on the line: `record CustomerAddressView(…) {}`.
- Member order (IntelliJ "Rearrange code"):
  1. `static final` fields: private, package-private, protected, public; then other static fields: public →
     private; static initializers;
  2. instance fields: `final` before non-`final`, each public → protected → package-private → private; initializers;
  3. constructors;
  4. methods: public static, protected static, package-private static, public, protected, private static,
     package-private, private;
  5. nested enums, interfaces, static classes, classes.

  The sections (`// static constructor`, `// query`, `// command`, `// helper`) fit this order.

## Tests

Skill `backend-testing` has all rules; the short form:

- **Each rule on the lowest level that proves it completely:** value object / entity (no mocks), service (Mockito for
  repository and outbox, real entity and EventFactory), use case (mocked services, one test per path), controller
  (standalone MockMvc), integration, ArchUnit.
- **A rule whose check reads the database** also gets an acceptance test `r<number>_…` in
  `integration/<Aggregate>AcceptanceTest`: `@IntegrationTest`, `SchaffbarFixture` (`given()` / `api()` / `then()`),
  random values from `Scenario`, no cleanup. One PostgreSQL container (Testcontainers) for the whole run – `mvn test`
  needs Docker; the real database is never touched.
- A rejection is checked with its exception, its `code` and that nothing happened. Mockito runs strict – stub per test.
- Every test has `// given` / `// when` / `// then`; stubs live in `given…`, verifications in `expect…` methods.
  Each test creates its own data (no fields): commands from `builder/CommandBuilder`, views from `builder/ViewBuilder`,
  values random (`RandomValues`, `XyzId.random()`) unless the value is the subject of the test.
- **Architecture** is checked by ArchUnit (`architecture/*ArchitectureTest`); frozen violations live in
  `core-pos/archunit_store/` – never add one to make the build pass.

## Definition of done

1. Rule or behaviour in the right place (entity, service or use case), dedicated exception, tests for each path;
   `mvn -q test` passes.
2. The aggregate README updated (rules moved, new events); `core-pos/docs/ddd-overview.md` (and `ddd-uebersicht.md`)
   when a use case, event or aggregate is added.
3. Portal impact named: API change → portal `shared/api`; rule the portal checked so far → it may drop its
   pre-check later (only with the Problems API).
4. A finished task ticked off in `../docs/roadmap.md`; nothing committed (`../CLAUDE.md`).
5. The report says what was tested, what needs a restart or a migration, and what was not checked.

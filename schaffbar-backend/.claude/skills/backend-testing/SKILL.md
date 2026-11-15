---
name: backend-testing
description: How the core-pos backend (Spring Boot, JUnit 5, Mockito, AssertJ, Testcontainers, ArchUnit) is tested – the test levels (value object and entity, service, use case, controller, integration as acceptance and technical test, architecture), which level proves which rule, where a test lives and how it is named, the integration test setup with @IntegrationTest, SchaffbarFixture (given / api / then), Scenario and RandomValues, and how to run the tests. Use when writing, fixing or reviewing a backend test, when adding a business rule or a use case that needs tests, when extending the integration fixture, when a test needs a database or Docker, or when deciding which kind of test a change needs.
---

# Tests in core-pos

**Each rule is tested on the lowest level that proves it completely** (E22 in `../docs/backend-consistency.md`). A
level higher up proves what the lower one cannot see: a mock cannot know whether the repository query finds the right
row, so a rule whose check reads the database also gets an acceptance test.

| Level | What it proves | Test runs with | Example |
| --- | --- | --- | --- |
| Value object | the value checks itself | nothing | `shared/id/MacAddressTest` |
| Entity | state changes, calculations, the events of a command | nothing | `domain/workshop_session/WorkshopSession_CloseTest` |
| Service | rules of one aggregate that need its repository; the events land in the outbox | Mockito for repository and `OutboxEventStore`, real entity and EventFactory | `domain/rfid_reader/RfidReaderServiceTest` |
| Use case | the paths through one action: allowed, each rejection, order of side effects | Mockito for the services | `use_case/RfidReaderDeleteTest` |
| Controller | HTTP: paths, status, JSON, value object conversion, problem codes | standalone MockMvc, mocked use cases and read facade | `domain/rfid_reader/web/RfidReaderControllerTest` |
| Integration – acceptance | a rule or a flow as the portal sees it, through all layers and the database | `@IntegrationTest` (Spring context, Testcontainers) | `integration/RfidReaderAcceptanceTest` |
| Integration – technical | what only the database shows: repository queries, migrations, transaction and outbox | `@IntegrationTest` | none yet – the first one sets the pattern |
| Architecture | the structure rules of the skills | ArchUnit | `architecture/*ArchitectureTest` |

All source paths below are relative to `core-pos/src/test/java/de/schaffbar/core_pos/`.

## Which tests a change needs

- **A rule in an aggregate README** ("Business Rules / Invariants") is tested where it is enforced – the class in the
  "Enforced in" column – with one test for "allowed" and one per rejection.
- **A rule whose check reads the database** – a second aggregate, a uniqueness, a lookup by something other than the
  id – also gets an **acceptance test**: `r<number>_…` in `integration/<Aggregate>AcceptanceTest`. A rule that lives
  completely in a value object or the entity (R-2, R-7 of `rfid_reader`) does not.
- **A new use case** gets its use case test (every path) and, if it has a rule, the acceptance test above.
- **A new endpoint or a changed `*ApiModel`** gets controller tests: status, the JSON fields the portal reads, the
  problem code of each rejection.
- **A new service method** gets a service test when it does more than load, call the entity and save: a rule, a
  lookup, a cleanup – and always the event it saves.
- **A bug** gets a test that fails without the fix, on the level where the bug was – named after the behaviour, not
  after the bug.
- **A changed structure rule** changes its ArchUnit rule and the skill that describes it in the same step.

## Where a test lives and how it is named

- A test lives **in the package of the class it tests** – the service test next to the package-private repository
  and entity (`domain/rfid_reader/`), the controller test in `web/`, the use case test in `use_case/`. Acceptance tests
  live in `integration/`.
- Class: `<Class>Test` (`RfidReaderServiceTest`, `RfidReaderDeleteTest`); an entity method with many cases gets its
  own class `<Entity>_<Method>Test` (`ToolUsage_GetBillableDurationTest`). Acceptance: `<Aggregate>AcceptanceTest`
  or `<Flow>AcceptanceTest` for a flow over several aggregates (check-in to billing). Technical:
  `<Class>IntegrationTest`.
- Method: what happens, in words – `<method>_<situation>_<result>` when the class tests several methods
  (`createRfidReader_rejectsAUsedMacAddress`, `getRfidReader_unknownId_is404`); without the method when the class
  tests one (`process` of a use case, `<Entity>_<Method>Test`): `rejectsReaderAssignedToTool`. Acceptance:
  `r<number>_<the rule as a sentence>` (`r5_aReaderAssignedToAToolCannotBeDeleted`), the number from the README.

## Common

- JUnit 5, AssertJ (`assertThat`, `assertThatThrownBy`), Mockito with `@ExtendWith(MockitoExtension.class)`, `@Mock`,
  `@InjectMocks`, `@Captor`. `this.` for fields, as in the main code; sections `// ---…` per method under test and
  `// helper` at the end.
- **Mockito runs strict:** a stub a test does not use fails it – stub per test or in a helper the test calls
  (`givenReader()`), never in a shared `@BeforeEach`.
- **A rejection is checked completely:** the exception type, its `code` for a `BusinessRuleViolationException`
  (`.hasFieldOrPropertyWithValue("code", RfidReaderHasToolAssignedException.CODE)`), and that nothing happened –
  `verify(…, never())`, `verifyNoInteractions(this.outboxEventStore)`.
- `// given`, `// when`, `// then` where a test has more than one step; a test with one call and one assertion does
  without.
- Unit tests use fixed, readable values (`MacAddress.of("34:CD:B0:D3:A9:30")`); integration tests random ones
  (`Scenario`, below). Views: `builder/ViewBuilder` or the records' Lombok builders. A random device value (MAC
  address, later RFID tag id) comes from `builder/RandomValues` – value objects of devices have no `random()` (skill
  `aggregate`, "Value objects").
- Parameterized tests for a list of inputs with the same result (`@ParameterizedTest`, `@ValueSource`,
  `@NullAndEmptySource`) – `MacAddressTest`.

## Value object and entity

No mocks, no Spring. A value object test covers the accepted formats (with real values from the devices), the
normalisation (`of_lowerCase_isStoredUpperCase`) and every rejection with `IllegalArgumentException`.

An entity test creates the entity with `of(…)` and its commands – never with setters – and checks the new state and
the returned events (type and payload):

```java
SchaffbarEvent event = session.close(workshopTotal, toolTotal).getFirst();
WorkshopSessionClosedPayload payload = event.deserializeAndValidatePayload(WorkshopSessionClosedPayload.class);
assertThat(event.getType()).isEqualTo(EventType.WORKSHOP_SESSION_CLOSED);
assertThat(payload.workshopTotal()).isEqualTo(workshopTotal);
```

## Service

Mockito for the repository and the `OutboxEventStore`; the **entity and the EventFactory are real** – that is the
point of this level: it found that `RFID_READER_UPDATED` could not be created for a reader without a type, which the
use case test with a mocked service could not see.

```java
@ExtendWith(MockitoExtension.class)
class RfidReaderServiceTest {

    @Mock
    private RfidReaderRepository rfidReaderRepository;

    @Mock
    private OutboxEventStore outboxEventStore;

    @Captor
    private ArgumentCaptor<List<SchaffbarEvent>> events;

    @InjectMocks
    private RfidReaderService rfidReaderService;

    @Test
    void changeRfidReaderType_setsTheTypeAndSavesTheEvent() {
        RfidReader rfidReader = givenReader();
        …
        this.rfidReaderService.changeRfidReaderType(command);

        assertThat(rfidReader.getType()).isEqualTo(RfidReaderType.SWITCH_BOX);
        expectSavedEvent(EventType.RFID_READER_TYPE_CHANGED, rfidReader.getId());
    }

    // helper: givenReader() stubs findById with a real RfidReader.of(…);
    // expectSavedEvent(…) captures the list saved to the outbox and checks type and aggregate id
}
```

- Per command: the success path (state of the entity, the one event in the outbox) and every rejection (unknown id →
  `ResourceNotFoundException`, a rule → its exception), each without an outbox call.
- A save is stubbed only where the service uses its result (`save(any())` → `invocation.getArgument(0)`).
- Queries need a test only when they do more than map (a filter, a combination).

## Use case

Mockito for the services; one test per path – see `use_case/RfidReaderDeleteTest`, `CustomerDeleteTest`:

- allowed: the service call happens (`verify(this.rfidReaderService).deleteRfidReader(id)`);
- each rejection: exception and code, and the change does **not** happen (`never()`);
- the order of side effects where it matters (`InOrder`: first clean up, then delete).

## Controller

Standalone MockMvc – no Spring context: the controller is built by hand with mocked use cases and read facade, the
converters of its value objects and the `ApiExceptionHandler`:

```java
DefaultFormattingConversionService conversionService = new DefaultFormattingConversionService();
conversionService.addConverter(new StringToRfidReaderIdConverter());
conversionService.addConverter(new StringToMacAddressConverter());

this.mockMvc = MockMvcBuilders.standaloneSetup(controller) //
        .setConversionService(conversionService) //
        .setControllerAdvice(new ApiExceptionHandler()) //
        .build();
```

Per endpoint: the status (`200`, `201` with `Location`, `204`), the JSON fields the portal reads (`jsonPath`), a
filter with and without a match, `404` with `RESOURCE_NOT_FOUND`, `400` for an invalid value object, and the problem
code of each rejection the use case can throw.

## Integration tests

```java
@IntegrationTest
class RfidReaderAcceptanceTest {

    @Autowired
    private SchaffbarFixture fixture;

    @Test
    void r5_aReaderAssignedToAToolCannotBeDeleted() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        RfidReaderId switchBox = this.fixture.given().switchBox(scenario);
        ToolId toolId = this.fixture.given().toolWithSwitchBox(scenario, switchBox);

        // when
        MvcTestResult result = this.fixture.api().deleteRfidReader(switchBox);

        // then
        this.fixture.then().expectProblem(result, HttpStatus.CONFLICT, RfidReaderHasToolAssignedException.CODE);
        this.fixture.then().expectRfidReader(switchBox, SWITCH_BOX);

        // when
        this.fixture.then().expectSuccess(this.fixture.api().clearRfidReader(toolId));
        result = this.fixture.api().deleteRfidReader(switchBox);

        // then
        this.fixture.then().expectSuccess(result);
        this.fixture.then().expectNoRfidReader(switchBox);
    }

}
```

### Setup

- **`@IntegrationTest`** (`integration/IntegrationTest`): `@SpringBootTest` + `@AutoConfigureMockMvc` + the
  configuration with the container. Every integration test uses exactly this annotation and nothing else on the
  class – no `@MockitoBean`, no `@TestPropertySource`, no `@DirtiesContext`: Spring caches one context for all of them,
  so **one PostgreSQL container (`postgres:16.3`, like the server) runs for the whole build** and Flyway migrates it
  once (E24). Something that would change the context is a reason to talk first.
- The container runs as `schadmin` – the migrations grant rights to it.
- **No cleanup.** All tests share the database; each test creates its own data with random values (`Scenario`), so
  tests never see each other's data and can run in any order. Never assert on "all readers" – filter by the
  scenario's values (`expectRfidReaderCount(scenario.macAddress(), 1)`).
- Integration tests never touch the real database; they run with `mvn test` and need Docker. Without Docker they fail
  at startup – say so instead of skipping them.

### The fixture (`integration/fixture/`)

| Class | Role |
| --- | --- |
| `SchaffbarFixture` | the test bean (`@TestComponent`): `given()`, `api()`, `then()` |
| `SchaffbarApi` | one method per endpoint the tests call, returning `MvcTestResult`; the only place that knows URLs and JSON |
| `GivenFixture` | builds the starting state through the API, asserts each step and returns the created ids |
| `ThenFixture` | expectations: `expectSuccess`, `expectProblem(result, status, code)`, the state afterwards – read through the API |
| `Scenario` | the input values of one test, random by default (`Scenario.randomValues().macAddress(…).build()` to fix one) |

- **The trigger is `api()`** and returns the result unchecked – the test decides what to expect. `given()` fails
  where a step does not work, so a broken setup never looks like a broken rule.
- **Requests are JSON strings** written like the portal sends them, not the `*RequestBody` records: the test sees
  the API as the portal does and catches a changed field name. A method takes value objects
  (`createRfidReader(MacAddress)`); a `String` variant only where a test sends what a value object cannot hold (the
  lower case MAC of R-1).
- **`then()` reads through the API**, never through repositories – the acceptance test proves what the portal would
  see.
- A new aggregate in the fixture: a section `// <aggregate>` in `SchaffbarApi`, `GivenFixture` and `ThenFixture`,
  its values in `Scenario` (random ones from `RandomValues`). Given methods are named after the state they create
  (`switchBox`, `toolWithSwitchBox`), then methods `expect…`.

### Acceptance test

- One class per aggregate (or flow), **one method per README rule** with its number – both sides of a rule in one
  method: rejected while the condition holds, allowed after it is gone (R-3, R-5 above).
- Steps as `// given`, `// when`, `// then`, repeated for the second side; a comment on a step only for what the
  code can't say (`// when – lower case is the same address (R-7)`).

### Technical integration test

For what only a real database shows: a derived or `@Query` repository method, a migration with data, the transaction
around the outbox (events saved or rolled back with the change), optimistic locking. It lives next to the class it
tests (package-private repositories: `domain/<aggregate>/`), uses `@IntegrationTest` and injects the beans directly;
data still from random values. The first one sets the pattern – show it before writing more.

## Architecture tests

`architecture/*ArchitectureTest` (ArchUnit, `ImportOption.DoNotIncludeTests`): one class per topic
(`DomainArchitectureTest`, `DependencyArchitectureTest`, `QueryCommandArchitectureTest`, `ValueObjectArchitectureTest`,
…), shared predicates and conditions in `ArchitectureSupport`.

- A rule is a `static final ArchRule` field with `@ArchTest`, named as the rule in words
  (`valuesFromDevicesAreNeverInvented`), with `.because(…)` where the reason is not obvious.
- **Rules with known violations are frozen** (`freeze(…)`, store in `core-pos/archunit_store/`): only new violations
  fail; a fixed one leaves the store on the next run – keep that change. Never add a violation to the store to make the
  build pass; a renamed frozen rule leaves an orphan in `stored.rules` – remove it.
- **A new rule is tried once against a deliberate violation** – a rule that cannot fail proves nothing.
- A rule in ArchUnit is also written in the skill of its topic (skills are the complete documentation; ArchUnit
  enforces part of it).

## Running

From `core-pos/`:

```bash
mvn -q test                                    # Spotless + all tests, including integration (Docker must run)
mvn -q test -Dtest=RfidReaderServiceTest       # one class
mvn -q test -Dtest='*AcceptanceTest'           # all acceptance tests
mvn clean test                                 # after "Unable to create test class" (IDE and Maven compiling at once)
```

The whole run takes about 25 seconds, the container start about 10 of them.

## Checklist

- [ ] Each changed rule tested where it is enforced: allowed and every rejection, with exception code and "nothing
      happened".
- [ ] A rule that reads the database: `r<number>_…` in its acceptance test, both sides.
- [ ] Service test with real entity and EventFactory for new command methods; the event in the outbox.
- [ ] Controller test for a new or changed endpoint (status, JSON fields, problem codes).
- [ ] Integration data random (`Scenario`), no cleanup, no assertion on all rows, nothing on the test class except
      `@IntegrationTest`.
- [ ] A new ArchUnit rule failed once on purpose; its skill updated.
- [ ] `mvn -q test` passes; the report names what the tests do not cover.

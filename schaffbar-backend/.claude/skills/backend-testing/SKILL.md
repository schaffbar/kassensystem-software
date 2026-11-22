---
name: backend-testing
description: How the core-pos backend (Spring Boot, JUnit 5, Mockito, AssertJ, Testcontainers, ArchUnit) is tested – the test levels (value object and entity, service, use case, controller, integration as acceptance and technical test, architecture), which level proves which rule, where a test lives and how it is named, the structure of a test (given / when / then, given… and expect… methods), test data (CommandBuilder, ViewBuilder, RandomValues, no data in fields), the integration test setup with @IntegrationTest, SchaffbarFixture (given / api / then), Scenario and RandomValues, and how to run the tests. Use when writing, fixing or reviewing a backend test, when adding a business rule or a use case that needs tests, when extending the integration fixture, when a test needs a database or Docker, or when deciding which kind of test a change needs.
---

# Tests in core-pos

**Each rule is tested on the lowest level that proves it completely** (E22 in `docs/backend-consistency.md`). A
level higher up proves what the lower one cannot see: a mock cannot know whether the repository query finds the right
row, so a rule whose check reads the database also gets an acceptance test.

| Level | What it proves | Test runs with | Example |
| --- | --- | --- | --- |
| Value object | the value checks itself | nothing | `shared/id/MacAddressTest` |
| Entity | state changes, calculations, the events of a command | nothing | `domain/tool_usage/ToolUsageTest` |
| Service | rules of one aggregate that need its repository; the events land in the outbox | Mockito for repository and `OutboxEventStore`, real entity and EventFactory | `domain/rfid_reader/RfidReaderServiceTest` |
| Use case | the paths through one action: allowed, each rejection, order of side effects | Mockito for the services | `use_case/RfidReaderDeleteTest` |
| Controller | HTTP: paths, status, JSON, value object conversion, problem codes | standalone MockMvc, mocked use cases and read facade | `domain/rfid_reader/web/RfidReaderControllerTest` |
| Integration – acceptance | a rule or a flow as the portal sees it, through all layers and the database | `@IntegrationTest` (Spring context, Testcontainers) | `integration/RfidReaderAcceptanceTest` |
| Integration – technical | what only the database shows: repository queries, migrations, transaction and outbox | `@IntegrationTest` | `domain/workshop_usage/WorkshopUsageRepositoryIntegrationTest` |
| Architecture | the structure rules of the skills | ArchUnit | `architecture/*ArchitectureTest` |

All source paths below are relative to `core-pos/src/test/java/de/schaffbar/core_pos/`.

## Which tests a change needs

- **A rule in an aggregate README** ("Business Rules / Invariants") is tested where it is enforced – the class in the
  "Enforced in" column – with one test for "allowed" and one per rejection.
- **A rule whose check reads the database** – a second aggregate, a uniqueness, a lookup by something other than the
  id – also gets an **acceptance test**: `r<number>_…` in `integration/<Aggregate>AcceptanceTest`. A rule that lives
  completely in a value object or the entity (R-2, R-7 of `rfid_reader`) does not.
- **A format rule** (Bean Validation, E33): the own logic of a constraint in a unit test of its validator
  (`shared/validation/DateOfBirthValidatorTest`) – a constraint composed of others (`@PostalCode`, `@ToolDescription`)
  has none; per constraint and endpoint whose request body carries it a controller test
  `<endpoint>_with<WhatIsWrong>_is400`; nothing on the command in the service (the same annotations, and a Mockito
  service test does not validate). An acceptance test is not required, but allowed (`c6`, `c7`).
- **A new use case** gets its use case test (every path) and, if it has a rule, the acceptance test above. A use case
  that only forwards to one service (`RfidReaderCreate`, `RfidTagCreate`) has one path and needs no test of its own –
  the service and controller tests cover it.
- **Every endpoint** has controller tests – at least its success (status, the JSON fields the portal reads), and the
  problem code of each rejection. A new endpoint or a changed `*ApiModel` gets them in the same step.
- **The device endpoints** (`/device/init`, `/counter`, `/card`) are tested in `integration/device/` instead – one
  class per reader type with everything that reader can get, also at the endpoints it does not belong to
  (`GateKeeperInAcceptanceTest`, `SwitchBoxAcceptanceTest`, …; `UnconfiguredReaderAcceptanceTest` for a reader the
  backend does not know or that has no type). One method per answer, `<endpoint>_<situation>_<answer>`
  (`card_alreadyCheckedOut_isRefused`), the body as pretty JSON compared field by field with
  `then().expectDeviceAnswer(…)`. A changed answer or a new case changes these tests in the same step: they are the
  contract with the firmware, which cannot be changed together with the backend.
  A README rule that only a device test proves (TU-1 at the switch box, WU-2 at the gate) has its id in a comment
  above the method (`// TU-1`); the aggregate's acceptance test does not repeat it.
- **Every entity command and `of(…)`** gets entity tests: the success (new state, the returned event with its payload)
  and every rejection – also an inner entity's `of(…)`. That the service test runs the same code with the real entity
  does not replace them.
- **A new service method** gets a service test when it does more than load, call the entity and save: a rule, a
  lookup, a cleanup – and always the event it saves. A create command always gets one: `of(…)` returns no event, the
  service builds the created event, so its payload is checked there.
- **A bug** gets a test that fails without the fix, on the level where the bug was – named after the behaviour, not
  after the bug.
- **A changed structure rule** changes its ArchUnit rule and the skill that describes it in the same step.

## Where a test lives and how it is named

- A test lives **in the package of the class it tests** – the service test next to the package-private repository
  and entity (`domain/rfid_reader/`), the controller test in `web/`, the use case test in `use_case/`. Acceptance tests
  live in `integration/`.
- Class: `<Class>Test` (`RfidReaderServiceTest`, `RfidReaderDeleteTest`) – **one test class per class**, also when a
  method has many cases: each tested method is a section (`// getBillableDurationUntil` in `ToolUsageTest`), never a
  class of its own (`ToolUsage_GetBillableDurationTest`). Acceptance: `<Aggregate>AcceptanceTest`
  or `<Flow>AcceptanceTest` for a flow over several aggregates (check-in to billing). Technical:
  `<Class>IntegrationTest`.
- Method: what happens, in words – `<method>_<situation>_<result>` (`createRfidReader_rejectsAUsedMacAddress`,
  `getRfidReader_unknownId_is404`, `of_lowerCase_isStoredUpperCase`); only a use case test leaves the method out, it
  has just `process`: `rejectsReaderAssignedToTool`.
- Acceptance: `<rule id>_<the rule as a sentence>` – the README id in lower case without the dash (`r5_`, `rt3_`,
  `ta1_`, `tc2_`, `ws4_`, `c5_`): `r5_aReaderAssignedToAToolCannotBeDeleted`. A method that proves two rules
  together names both (`t2_t15_toolNamesAreUniqueWithoutRegardToCase`); a rule with separate cases gets one method
  per case, the sentence says which (`c4_deletingACustomerFreesTheirRfidTag`,
  `c4_deletingACustomerCancelsTheirWaitingRequest`).

## Structure of a test

Every test reads as `// given` – `// when` – `// then`, also a short one; `// given` is left out only when the test
prepares nothing. The test itself stays short: what a step means is in the name of a `given…` or `expect…` method.

```java
@Test
void createRfidReader_rejectsAUsedMacAddress() {
    // given
    MacAddress macAddress = RandomValues.macAddress();
    givenReaderWith(macAddress);

    // when
    Throwable thrown = catchThrowable(() -> this.rfidReaderService.createRfidReader(macAddress));

    // then
    assertThat(thrown) //
            .isInstanceOf(MacAddressAlreadyUsedException.class) //
            .hasFieldOrPropertyWithValue("code", MacAddressAlreadyUsedException.CODE);
    expectNothingSaved();
}
```

- **An exception is caught under `// when`** with `catchThrowable(…)` and checked under `// then` – not
  `assertThatThrownBy`, which mixes both. A controller test keeps the `ResultActions` of its request and calls
  `andExpect` under `// then`.
- **A controller test sends every request through a method of its `// when` section**, named after the controller
  method (`updateCustomer(command)`, `getRfidReaders(macAddress)`, no `when…` prefix) and returning the
  `ResultActions` – no test calls `mockMvc.perform` itself, so path, content type and body exist once per endpoint. A
  body the test needs to break (`"{}"`, an invalid MAC address) gets an overload taking the `String body`, which the
  typed one calls.
- **A stub that sets up a state is a `given…` method** named after the state: `givenReader()`, `givenUnknownReader()`
  (returns the id), `givenReaderWith(macAddress)`, `givenNoToolWith(rfidReaderId)`, `givenSaveReturnsTheReader()`. It
  takes what it needs as parameters and returns what the test needs next. No `when(…)` in the test itself – also no
  `doThrow(…)`: a rejection a controller or use case test needs is `given<Action>RejectedWith(…, RuntimeException
  rejection)` – `givenCloseRejectedWith(customerId, new NoOpenWorkshopSessionException(customerId))`, one method for
  all rejections of that call. `TestArchitectureTest` fails on any Mockito stub in a `@Test` method.
- **A verification that says what happened (or did not) is an `expect…` method**: every interaction (`verify(…)`,
  `never()`, `InOrder` – also when it is one line), and every check that appears in more than one test:
  `expectSavedEvent(type, id)`, `expectSavedEvents(type, usages)` (one event per entity, in order),
  `expectNothingSaved()`, `expectNoEvent()`, `expectTypeNotChanged()`.
- **What stays in the test:** the assertions on the result of this test – the new state and the payload of the
  returned or saved event (`assertThat(rfidReader.getType()).isEqualTo(command.type())`,
  `assertThat(payload.status()).isEqualTo(PAUSED)`), and the JSON fields of a controller response.
- A comment on a step only for what the code can't say, also in unit tests:
  `// when – a second request while the first one waits (TA-3)`.
- **The class is in sections** (`// ---…` line + name): the tests grouped by the method under test
  (`// createRfidReader`, `// updateRfidReader`, …) when the class tests several, then `// given` with the `given…`
  methods, `// when` with the request methods of a controller test, `// then` with the `expect…` methods, `// helper`
  for anything else. A section only exists when it has content.

## Test data

- **Every test creates its own data** – ids, values, commands, views are local variables of the test (or created in
  its `given…` methods), never fields of the class. Everything a test uses is visible in it; fields are only the mocks,
  captors, the class under test and infrastructure like `MockMvc`.
- **Commands come from `builder/CommandBuilder`** – never `XyzCommand.builder()` and never `new XyzCommand(…)` in a
  test, also not for an expected value. Each method returns the Lombok builder with valid defaults; a command with an
  id has two – `updateRfidReaderCommand()` (random id) and `updateRfidReaderCommand(rfidReaderId)` – a create command
  has one (`createCustomerCommand()`). The test changes only what matters to it and asserts against the command, not
  against copied literals:

  ```java
  UpdateRfidReaderCommand command = updateRfidReaderCommand(rfidReader.getId()).build();
  …
  assertThat(rfidReader.getName()).isEqualTo(command.name());

  ChangeRfidReaderTypeCommand command = changeRfidReaderTypeCommand(rfidReaderId).type(RfidReaderType.GATE_KEEPER_IN).build();
  ```

  A new command gets its two methods there, in the section of its aggregate.
- **Random values wherever the value itself is not the subject:** ids with `XyzId.random()`, device values from
  `builder/RandomValues` (`RandomValues.macAddress()`) – value objects of devices have no `random()` (skill
  `aggregate`, "Value objects"). A fixed value only where the test is about it: the formats in `MacAddressTest`, an
  invalid input (`"34-CD-B0-D3-A9-30"`). Integration tests take theirs from `Scenario` (below).
- **Views come from `builder/ViewBuilder`**, the same way – never `XyzView.builder()`, `new XyzView(…)` or
  `mock(XyzView.class)` in a test. Two methods for a view with an id (`rfidReaderView()`,
  `rfidReaderView(rfidReaderId)`, `toolView()`, `toolView(toolId)`), one for a view without an id of its own (`rfidTagAssignmentHistoryView()`, the
  views of a read facade), or one named after a state (`openWorkshopSessionView()`, `finishedToolUsageView()`); the
  test sets what it is about:
  `rfidReaderView().type(RfidReaderType.SWITCH_BOX).build()`, `toolView().rfidReaderId(rfidReaderId).build()`. A new
  view gets its methods there.
- **JSON bodies** of controller and fixture requests: `support/Json.write(command)` when a command has the fields of
  the request body (the id in the body is ignored, the path decides); a short body the test writes itself as a string
  (`"{}"`, `"{\"type\":\"COFFEE_MACHINE\"}"`), a longer one as a text block – never an own `ObjectMapper`.

## Common

- JUnit 5, AssertJ (`assertThat`, `catchThrowable`), Mockito with `@ExtendWith(MockitoExtension.class)`, `@Mock`,
  `@InjectMocks`, `@Captor`. `this.` for fields, as in the main code.
- **Mockito runs strict:** a stub a test does not use fails it – stub per test in its `given…` methods, never in a
  shared `@BeforeEach`.
- **A rejection is checked completely:** the exception type, its `code` for a `BusinessRuleViolationException`
  (`.hasFieldOrPropertyWithValue("code", RfidReaderHasToolAssignedException.CODE)`), its `resource` for a
  `ResourceNotFoundException` (`.hasFieldOrPropertyWithValue("resource", Resource.RFID_READER)`), and that nothing
  happened – `verify(…, never())`, `verifyNoInteractions(this.outboxEventStore)`, the state unchanged (an entity
  test: `expectRejected(thrown, certification, unchangedStatus)`; a device test: read the state afterwards).
- Parameterized tests for a list of inputs with the same result (`@ParameterizedTest`, `@ValueSource`,
  `@NullAndEmptySource`) – `MacAddressTest`; for every state that is rejected `@EnumSource(names = {"PAUSED",
  "REVOKED"})` – `ToolCertificationTest.pause_rejectsACertificationThatIsNotActive`.

## Value object and entity

No mocks, no Spring. A value object test covers the accepted formats (with real values from the devices), the
normalisation (`of_lowerCase_isStoredUpperCase`) and every rejection with `IllegalArgumentException`.

An entity test creates the entity with `of(…)` and its commands – never with setters. Per command (and for `of(…)`)
one test for the success – the new state and the returned events (type, aggregate id and payload) – and one per
rejection (the rule's exception, or the `IllegalStateException` of a guard); sections per method, `expectSingleEvent(…)`
and `expectRejected(…)` under `// then` – see `domain/tool_certification/ToolCertificationTest`:

```java
@Test
void pause_pausesAnActiveCertificationAndReturnsTheEvent() {
    // given
    ToolCertification certification = givenCertification(ACTIVE);

    // when
    List<SchaffbarEvent> events = certification.pause();

    // then
    assertThat(certification.getStatus()).isEqualTo(PAUSED);

    ToolCertificationPausedPayload payload = expectSingleEvent(events, EventType.TOOL_CERTIFICATION_PAUSED, certification) //
            .deserializeAndValidatePayload(ToolCertificationPausedPayload.class);
    assertThat(payload.id()).isEqualTo(certification.getId());
    assertThat(payload.status()).isEqualTo(PAUSED);
}

@ParameterizedTest
@EnumSource(value = ToolCertificationStatus.class, names = {"PAUSED", "REVOKED"})
void pause_rejectsACertificationThatIsNotActive(ToolCertificationStatus status) {
    // given
    ToolCertification certification = givenCertification(status);

    // when
    Throwable thrown = catchThrowable(certification::pause);

    // then
    expectRejected(thrown, certification, status);
}
```

`givenCertification(status)` creates it with `of(…)` and brings it into the state with its commands.

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
        // given
        RfidReader rfidReader = givenReader();
        ChangeRfidReaderTypeCommand command = changeRfidReaderTypeCommand(rfidReader.getId()).build();

        // when
        this.rfidReaderService.changeRfidReaderType(command);

        // then
        assertThat(rfidReader.getType()).isEqualTo(command.type());
        expectSavedEvent(EventType.RFID_READER_TYPE_CHANGED, rfidReader.getId());
    }

    // given: givenReader() stubs findById with a real RfidReader.of(…), givenUnknownReader(), …
    // then: expectSavedEvent(…) captures the list saved to the outbox and checks type and aggregate id;
    //       expectNothingSaved(), expectNoEvent()
}
```

- Per command: the success path (what the service does: saved, deleted, the one event in the outbox with its type and
  aggregate id) and every rejection (unknown id → `ResourceNotFoundException`, a rule → its exception), each without
  an outbox call – also a rule the entity enforces (a state transition): the entity test proves the rule, the service
  test that the exception passes through, the state stays and nothing reaches the outbox. A rule the service checks
  because it finds the aggregate by its state (E29: no running visit, no waiting request) is proven here.
- **The payload of every event the service builds itself** – the created and the deleted event – is checked here:
  `expectSavedEvent(…)` returns the event, the test reads `deserializeAndValidatePayload(XyzCreatedPayload.class)` and
  checks the fields. The payload of an event the entity's command returns is checked in the entity test, not again.
- **A create that returns nothing** (`startToolUsage`, `requestRfidTagAssignment`): the test does not know the new id,
  so it takes the saved entity from the repository – `ToolUsage usage = expectSavedUsage()` with
  `verify(this.toolUsageRepository).save(this.savedUsage.capture())` – and checks the event against it:
  `expectSavedEvent(EventType.TOOL_USAGE_STARTED, usage.getId())`. A captor is read in a `verify`, never captured in a
  stub.
- A save is stubbed only where the service uses its result (`givenSaveReturnsTheReader()`: `save(any())` →
  `invocation.getArgument(0)`).
- Queries need a test only when they do more than map (a filter, a combination).

## Use case

Mockito for the services; one test per path – see `use_case/RfidReaderChangeTypeTest`, `RfidReaderDeleteTest`:

- the state of the other aggregates as `given…` methods (`givenReaderOfType(SWITCH_BOX)` returns the id,
  `givenToolWith(rfidReaderId)`, `givenNoToolWith(rfidReaderId)`);
- allowed: the service call happens (`expectTypeChanged(command)` →
  `verify(this.rfidReaderService).changeRfidReaderType(command)`);
- each rejection: exception and code, and the change does **not** happen (`expectTypeNotChanged()` → `never()`);
- the order of side effects where it matters (`InOrder`: first clean up, then delete).

## Controller

Standalone MockMvc – no Spring context: the controller is built by hand with mocked use cases and read facade, the
converters of its value objects, the JSON converter with Boot's Jackson configuration and the `ApiExceptionHandler`:

```java
DefaultFormattingConversionService conversionService = new DefaultFormattingConversionService();
conversionService.addConverter(new StringToRfidReaderIdConverter());
conversionService.addConverter(new StringToMacAddressConverter());

this.mockMvc = MockMvcBuilders.standaloneSetup(controller) //
        .setConversionService(conversionService) //
        .setMessageConverters(Json.messageConverter()) //
        .setControllerAdvice(new ApiExceptionHandler()) //
        .build();
```

`Json.messageConverter()` (`support/Json`) uses the same `ObjectMapper` as `Json.write`: `JavaTimeModule`, dates not
as timestamps – what Boot configures, as long as `application.yml` sets no `spring.jackson.*`. A setting there is
added to `Json` in the same change.

Every endpoint has at least its success test. Per endpoint: the status (`200`, `201` with `Location`, `204` – E32), the
JSON fields the portal reads (`jsonPath`; the portal's zod schema in
`schaffbar-portal/src/app/shared/api/<resource>/*.api-model.ts` lists them), a filter with and without a match, `404`
with `ResourceNotFoundException.CODE` and its `resource`, `400` for an invalid value object and for each format rule
of the request body (E33), and the problem code of each rejection the use case can throw.

- **Dates and times** are compared with the value they come from – `jsonPath("$.createdAt").value(view.createdAt()
  .toString())`, `jsonPath("$.dateOfBirth").value(view.dateOfBirth().toString())`: with `Json.messageConverter()` the
  controller writes ISO strings (`"1990-01-31"`, `"2026-11-22T09:15:00Z"`) like the running backend, so the controller
  test proves the format the portal parses. Never only `exists()`.
- The same rules hold for `shared/web/ApiExceptionHandlerTest`: its test controller stands in for the real ones, and
  every request goes through a method of its `// when` section.

```java
@Test
void changeRfidReaderType_ofASwitchBoxWithTool_is409WithTheCode() throws Exception {
    // given
    ChangeRfidReaderTypeCommand command = changeRfidReaderTypeCommand().type(RfidReaderType.GATE_KEEPER_IN).build();
    givenSwitchBoxWithToolForTypeChange(command);

    // when
    ResultActions result = changeRfidReaderType(command);

    // then
    result.andExpect(status().isConflict()) //
            .andExpect(jsonPath("$.code").value(RfidReaderHasToolAssignedException.CODE));
}

// ------------------------------------------------------------------------
// when

private ResultActions changeRfidReaderType(ChangeRfidReaderTypeCommand command) throws Exception {
    return this.mockMvc.perform(patch("/api/v1/rfid-readers/" + command.id().getValue() + "/type") //
            .contentType(MediaType.APPLICATION_JSON) //
            .content(Json.write(command)));
}
```

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
        RfidReaderId switchBox = this.fixture.given().switchBox(scenario.macAddress());
        ToolId toolId = this.fixture.given().toolWithSwitchBox(scenario.toolName(), switchBox);

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
- **The one exception: a waiting RFID tag request.** Only one may wait in the whole workshop (TA-3), so a request
  left behind makes the next test that requests a tag fail with `409` – in any class. A test that leaves one ends with
  a section `// cleanup` and `this.fixture.endWaitingRfidTagAssignment()` (assigns a new tag to the waiting person),
  after its `// then`; a test whose rule assigns the tag anyway checks that as part of the rule instead.
- Integration tests never touch the real database; they run with `mvn test` and need Docker. Without Docker they fail
  at startup – say so instead of skipping them.

### The fixture (`integration/fixture/`)

| Class | Role |
| --- | --- |
| `SchaffbarFixture` | the test bean (`@TestComponent`): `given()`, `api()`, `then()` |
| `SchaffbarApi` | one method per endpoint the tests call, returning `MvcTestResult`; the only place that builds requests (URLs, JSON bodies) – `GivenFixture` and `ThenFixture` read the responses |
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
  (`switchBox`, `toolWithSwitchBox`) and take the values they need (`switchBox(MacAddress)`), then methods `expect…`.
- **`Scenario` holds one value per kind** – the reader, the tag, the person, the tool of the test. A second one of the
  same kind (the other reader of R-1, the gate at the exit, a second tool) is a local variable from `RandomValues`,
  named after its role: `MacAddress gateKeeperOut = RandomValues.macAddress();`.

### Acceptance test

- One class per aggregate (or flow), **one method per README rule** with its number – both sides of a rule in one
  method: rejected while the condition holds, allowed after it is gone (R-3, R-5 above).
- **The allowed side is triggered with `api()`** like the rejected one and checked with `then()` – not built with
  `given()`, which would hide a failing step as a broken setup. At a device endpoint `expectSuccess` proves nothing
  (`/device/*` always answers `200`, the refusal is in the body): check the answer (`expectDeviceAnswer`) or the state
  afterwards.
- Steps as `// given`, `// when`, `// then`, repeated for the second side; a comment on a step only for what the
  code can't say (`// when – lower case is the same address (R-7)`).
- A side of a rule the API cannot reach (the tool side of WS-5: checking out switches the tools off) is named in a
  comment above the test; its use case test proves it.
- **A flow** (`WorkshopFlowAcceptanceTest`: check-in, tool on and off, check-out, billing, the next visit) is one
  method per flow with a `// when` – `// then` pair per step, each `then` reading the state through the API
  (`expectToolRunning`, `expectCheckedOut`). It proves the steps fit together; the answers of the devices stay in
  `integration/device/`, the rules in the aggregate's acceptance test.

### Technical integration test

For what only a real database shows and no acceptance test already proves: a rule the database guards itself (a
unique constraint or index, V9), a `@Query` or derived repository method no acceptance test reaches, a migration with
data, the transaction around the outbox (events saved or rolled back with the change), optimistic locking. A derived
query an acceptance test runs (`findByMacAddress` in R-1) needs none; nor does the "at most one" check of a `default`
method – the broken data it guards against cannot be created through the API. It lives next to the class it
tests (package-private repositories: `domain/<aggregate>/`), uses `@IntegrationTest` and injects the beans directly;
data still from random values. Each `saveAndFlush` commits on its own (no test transaction), so a test can show a
rejection by the database and then the allowed side – `WorkshopUsageRepositoryIntegrationTest`: a second running
usage of the same person fails on the partial unique index of V9 (`DataIntegrityViolationException`), after
checking out the next one is saved. A comment above the class says which rule the database guards and why the
service's check is not enough.

## Architecture tests

`architecture/*ArchitectureTest` (ArchUnit, `ImportOption.DoNotIncludeTests`): one class per topic
(`DomainArchitectureTest`, `DependencyArchitectureTest`, `QueryCommandArchitectureTest`, `ValueObjectArchitectureTest`,
…), shared predicates and conditions in `ArchitectureSupport`. `TestArchitectureTest` checks the test code itself
(`ImportOption.OnlyIncludeTests`): no Mockito stub directly in a test method.

- A rule is a `static final ArchRule` field with `@ArchTest`, named as the rule in words
  (`valuesFromDevicesAreNeverInvented`), with `.because(…)` where the reason is not obvious.
- **Every rule is strict** – no `freeze(…)`, no store of known violations. A new rule that finds violations comes
  with their fix in the same change; a violation is never excepted to make the build pass.
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

- [ ] One test class per class (`<Class>Test`), one section per tested method.
- [ ] `// given` / `// when` / `// then` in every test; stubs in `given…` (also rejections: `given…RejectedWith`),
      every interaction and every repeated check in `expect…` methods, sections `// given` and `// then`; exceptions
      with `catchThrowable`.
- [ ] Commands from `CommandBuilder`, views from `ViewBuilder` – no `.builder()`, no `new`, also not for expected
      values; no test data in fields; values random (`RandomValues`, `XyzId.random()`) unless the value is the subject.
- [ ] Each changed rule tested where it is enforced: allowed and every rejection, with exception code (`resource` for
      a `404`) and "nothing happened".
- [ ] A rule that reads the database: `r<number>_…` in its acceptance test, both sides.
- [ ] Service test with real entity and EventFactory for new command methods; the event in the outbox, the payload of
      every event the service builds itself (created, deleted).
- [ ] Controller tests for every endpoint: at least the success (status, JSON fields), the problem codes, `400` per
      format rule; every request through a method of the `// when` section.
- [ ] Integration data random (`Scenario`), no cleanup, no assertion on all rows, nothing on the test class except
      `@IntegrationTest`.
- [ ] A new ArchUnit rule failed once on purpose; its skill updated.
- [ ] `mvn -q test` passes; the report names what the tests do not cover.

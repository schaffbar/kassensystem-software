---
name: use-case
description: How the layer between the controllers and the aggregates of the core-pos backend is built – use cases in `use_case/` (one class per change, named after the aggregate whose state changes and its entity's command, one `process(…)` with `@Command`, the order existence → rules with a second aggregate → service calls, a batch all or nothing, an aggregate found by its state, a use case that chooses between two others) and read facades in `read_facade/` (one per controller and named after it, `@Query` only, views of their own that combine several aggregates, a query with a precondition), what both may call and return, their tests and the ArchUnit rules that enforce them. Use when adding or changing a use case or a read facade, when a change or a check needs a second aggregate, when deciding whether something belongs in a use case, a service or a read facade, when a controller needs a new query, or when reviewing any of these.
---

# Use cases and read facades in core-pos

Between the controllers and the aggregates sit two kinds of classes – one for changes, one for queries:

```txt
controller ── a change ──▶ use_case/<Aggregate><EntityCommand>.process(…)  ──▶ services: queries, then command(s)
           ── a query ───▶ read_facade/<Controller>ReadFacade.get…(…)        ──▶ services: queries only
                                                                                   │
                                                          <Aggregate>Service ──────┘ entity, repository, outbox
```

- **A controller knows only use cases and its read facade** – every change goes through a use case, every query
  through the read facade, also when either only forwards to one service;
  so every controller looks the same.
- **Both call services, never the other way round**, and a service never calls another service
  (skill `aggregate`). Neither sees an entity: services give them views, they give services commands and value objects.
- Everything that needs a second aggregate – a check, a cleanup, a value from elsewhere – is a use case. What one
  aggregate can check alone stays in its service or entity (skill `aggregate`, "Where a rule lives").

Project rules: `schaffbar-backend/CLAUDE.md`. Tests: skill `backend-testing`. Controllers and API models: until the
skill `rest-api`, `schaffbar-backend/CLAUDE.md`, "Controller and API model".

Reference implementations:

| What it shows | Class |
| --- | --- |
| a use case that only forwards | `use_case/RfidReaderCreate` |
| a rule with a second aggregate | `use_case/RfidReaderChangeType` |
| an aggregate found by its state, two rules, the command built from other aggregates | `use_case/WorkshopSessionClose` |
| a batch – all or nobody | `use_case/ToolCertificationCreate` |
| a rejection, then the cleanup of several aggregates in one transaction | `use_case/CustomerDelete` |
| choosing between two use cases | `use_case/ToolUsageToggle` |
| a read facade that only forwards | `read_facade/RfidReaderReadFacade` |
| a read facade with a view of its own over several aggregates | `read_facade/WorkshopSessionReadFacade` + `OpenWorkshopSessionViews` |
| queries with a precondition | `read_facade/DeviceReadFacade` |

## Use case

```java
@Service
@Validated
@RequiredArgsConstructor
public class RfidReaderChangeType {

    private final @NonNull RfidReaderService rfidReaderService;

    private final @NonNull ToolService toolService;

    @Command
    public void process(@NotNull @Valid ChangeRfidReaderTypeCommand command) {
        RfidReaderView rfidReader = this.rfidReaderService.getRfidReader(command.id()) //
                .orElseThrow(() -> ResourceNotFoundException.rfidReader(command.id()));

        if (rfidReader.type() == RfidReaderType.SWITCH_BOX) {
            ensureNoToolAssigned(command.id());
        }

        this.rfidReaderService.changeRfidReaderType(command);
    }

    // ------------------------------------------------------------------------
    // helper

    // R-3
    private void ensureNoToolAssigned(RfidReaderId rfidReaderId) {
        this.toolService.getTool(rfidReaderId) //
                .ifPresent(_ -> throwRfidReaderHasToolAssignedException(rfidReaderId));
    }

    private void throwRfidReaderHasToolAssignedException(RfidReaderId rfidReaderId) {
        throw RfidReaderHasToolAssignedException.changeType(rfidReaderId);
    }

}
```

### Name

`<Aggregate><EntityCommand>`:

- **The prefix is the aggregate whose state changes** – the one whose service command the use case calls. When
  several change, it is the one the action is about: `WorkshopUsageStart` also starts a session, `CustomerDelete`
  also frees the tag. It is not the aggregate the action starts from: requesting a tag for a customer changes the
  assignment, so it is `RfidTagAssignmentRequest`, not `CustomerRequestRfidTagAssignment`. The whole name tells two
  aggregates with the same start apart – `ToolUsageStart` belongs to `ToolUsage`, not `Tool`.
- **The action is the entity's command** without the aggregate's name – the table "entity command → service command
  → use case" in skill `aggregate`, "Service": `ToolCertificationPause`, `RfidReaderChangeType`,
  `ToolAssignRfidReader`, `RfidTagAssignmentAssignRfidTag`; create and delete are `ToolCreate`, `ToolDelete`.
- A use case that only chooses between two others is named after what it does: `ToolUsageToggle`.
- **No ArchUnit rule checks the name** (decided 23.11.2026: a check of the prefix alone let the wrong aggregate
  through, a full check needs a list of exceptions) – check these points when writing one and in the review.

### Shape

- `@Service` `@Validated` `@RequiredArgsConstructor`, `public class`. Dependencies are `private final @NonNull`
  fields: **the services of any aggregates**, and other use cases only to choose between whole actions with their
  rules (`ToolUsageToggle` → `ToolUsageStart` or `ToolUsageStop`). Never a repository (they are package-private),
  the `OutboxEventStore` or a read facade.
- **One public method `process(…)` with `@Command`.** It is the transaction of the whole change: all service calls
  commit together or not at all – a check-out that is refused also switches no tool off (`WorkshopUsageStop`), a
  customer is deleted together with the cleanup of the other aggregates (`CustomerDelete`).
- **Input** is a command of the aggregate or value objects, every parameter validated: `@NotNull @Valid CustomerId`,
  `@NotNull @Valid ChangeRfidReaderTypeCommand`, a list `@NotEmpty List<@NotNull @Valid CustomerId>`, an enum
  `@NotNull` – never a raw `UUID` or `String`.
- **It returns `void` or the id of what it created** (`ToolCreate` → `ToolId`) – never a view. Who needs the state
  after the change reads it from the read facade afterwards: the device controller checks in, then reads the stay
  (`DeviceReadFacade.getWorkshopStay`); it toggles a tool, then asks `isToolInUse`.
- Only the section `// helper` with the `ensure…`, `throw…` and other private helpers.

### What `process` does, in this order

1. **Load or check what it refers to** – every referenced aggregate exists, else
   `orElseThrow(() -> ResourceNotFoundException.customer(customerId))` (`404`). For a list, **all entries before any
   rule**: a batch with an unknown customer answers `404`, not the `409` of a rule that one of the others breaks
   (TC-4 in `ToolCertificationCreate`). What it loads it keeps as a view – the rules and the command need it.
   - **An aggregate found by its state** ("the open session of the customer", "the running visit") is found by a
     query in that state, and `orElseThrow` throws the rule's exception – then the service command gets its
     id: `getOpenWorkshopSession(customerId).map(WorkshopSessionView::id).orElseThrow(() -> new
     NoOpenWorkshopSessionException(customerId))` in `WorkshopSessionClose`, with the rule id in a comment.
2. **The rules with a second aggregate** – one `ensure…(…)` each, with the rule id above it and the `throw` in a
   `throw…` helper; the same as in a service (skill `aggregate`, "How a check is written"). A rule that only one
   aggregate needs is not repeated here – the service or entity checks it, the exception passes through.
3. **The change** – the service command(s). A use case builds the command a service needs from what it gathered
   (`WorkshopSessionClose`: `CloseWorkshopSessionCommand` with both totals; `ToolUsageStart`: `StartToolUsageCommand`
   with the session of the running visit). A cleanup of other aggregates comes before the change it makes possible
   (`CustomerDelete`: free the tag, delete the certifications, remove the instructor, then delete the customer); a
   helper that acts is named with a verb of its own (`releaseRfidTag`), not `ensure…`.

- **A batch is all or nothing**: every entry is checked before the first change, and one exception rolls the
  whole transaction back – `ToolCertificationCreate` certifies all customers or none.
- **A use case that only forwards** (`RfidReaderCreate`, `ToolUpdate`) is written anyway – one class per change, so
  every controller looks the same. It has one path and no test of its own.
- A rule a use case enforces is a row in the README of the aggregate it belongs to, "Enforced in"
  `` `RfidReaderChangeType` use case ``; implementing an open rule moves it from "Open Rules" to "Business Rules".

## Read facade

```java
@Service
@Validated
@RequiredArgsConstructor
public class RfidReaderReadFacade {

    private final @NonNull RfidReaderService rfidReaderService;

    @Query
    public List<RfidReaderView> getRfidReaders() {
        return this.rfidReaderService.getRfidReaders();
    }

    @Query
    public Optional<RfidReaderView> getRfidReader(@NotNull @Valid RfidReaderId rfidReaderId) {
        return this.rfidReaderService.getRfidReader(rfidReaderId);
    }

}
```

### Shape

- **One per controller, named after it and used only by it** – `CustomerController` → `CustomerReadFacade`,
  `DashboardController` → `DashboardReadFacade`, `DeviceController` → `DeviceReadFacade`. A new query of a controller
  is a method of its read facade, never a second facade.
- `@Service` `@Validated` `@RequiredArgsConstructor`, `public class`; dependencies are `private final @NonNull`
  services – any number, never a use case or a repository.
- **Every public method is `@Query`** – a read-only transaction. It never calls a `@Command`, an entity command, a
  `save` or the outbox: in a read-only transaction a change would not be written.
- **Names and results** like a service query: `get…` returns views – `Optional<View>` when there may be nothing,
  `List<View>` –, `is…` / `has…` a `boolean`. Parameters are value objects, `@NotNull @Valid`. A filter of the API is
  a method of its own (`getToolCertifications(CustomerId)`, `getToolCertifications(ToolId)`) – the controller decides
  which one to call.
- Only the section `// helper`.

### Views of its own

A query that combines several aggregates returns a view of its own, in `<What it shows>Views` next to the facade:
`DashboardViews`, `DeviceViews`, `OpenWorkshopSessionViews` – named after what it shows, not after an aggregate
(`OpenWorkshopSessionViews`, because `WorkshopSessionViews` is the aggregate's).

- The same rules as an aggregate's views: records nested in `*Views` with `@Builder` and `@Validated`, every field
  validated (`@Valid` on value objects and on nested views, also as list elements: `List<@Valid @NotNull
  WorkshopUsageView>`), ids as value objects – never `UUID`. They may contain the views of the aggregates.
- They are no MapStruct mapper: the facade builds them in private `to<View>(…)` helpers
  (`toOpenWorkshopSessionView`).
- What a field means when it is `null` is a comment (`String toolName, // null when the tool was deleted`); a value
  computed from "now" uses one point in time for the whole view (`Instant now` once in `WorkshopSessionReadFacade`),
  so the parts fit together.

### A query with a precondition

A query throws, like a command, when it cannot answer:

- a referenced aggregate does not exist → `ResourceNotFoundException` (`404`):
  `WorkshopSessionReadFacade.getOpenWorkshopSession` for an unknown customer – while "no open session" is an empty
  `Optional`, not an error;
- a rule → its exception (`409`): `DeviceReadFacade.getCustomer(rfidTagId)` without an assignment →
  `NoCustomerAssignedException`;
- broken data → `IllegalStateException` with the ids (`500`): a running usage whose customer is gone
  (`DashboardReadFacade`), an assignment whose customer does not exist (`DeviceReadFacade.getCustomer`) – with a
  comment why it cannot happen (`// a customer with an open session cannot be deleted (CustomerDelete)`).

## Tests

The details are in skill `backend-testing`; which tests these classes need:

- **A use case with more than one path** – a rule, a lookup, a cleanup – gets a use case test (`use_case/<Name>Test`,
  Mockito for the services): the allowed path, each rejection with its exception, code and "nothing changed", and the
  order of side effects where it matters (`InOrder` in `CustomerDeleteTest`). A use case that only forwards has none.
- A rule a use case enforces reads the database – it also gets an acceptance test `<rule id>_…` in
  `integration/<Aggregate>AcceptanceTest`, both sides of the rule.
- **A read facade that combines, filters or has a precondition** gets a test (`read_facade/<Name>Test`, Mockito for
  the services): what the view contains, the order, each precondition – `DashboardReadFacadeTest`,
  `DeviceReadFacadeTest`, `WorkshopSessionReadFacadeTest`. One that only forwards has none; the controller test
  covers it.

## Tests that enforce it

ArchUnit in `core-pos/src/test/java/de/schaffbar/core_pos/architecture/`, part of `mvn -q test`:

| Rule | Test |
| --- | --- |
| use cases are public `@Service` + `@Validated`, have only `process`, which is `@Command` | `UseCaseArchitectureTest` |
| a use case uses no read facade and no `OutboxEventStore` | `UseCaseArchitectureTest.useCasesChangeThroughServices` |
| read facades: only `*ReadFacade` and `*Views` in `read_facade/`, public `@Service` + `@Validated`, every public method `@Query`, no `@Command` called, no use case used | `ReadFacadeArchitectureTest` |
| one read facade per controller, named after it, used only by it | `ReadFacadeArchitectureTest` (`controllersUseTheReadFacadeNamedAfterThem`, `readFacadesServeOnlyTheirController`) |
| a controller depends on no bean but use cases and read facades | `DependencyArchitectureTest.controllersKnowOnlyUseCasesAndReadFacades` |
| the domain uses no use case and no read facade | `DependencyArchitectureTest.domainDoesNotUseUseCases` |
| fields `private final`, no `UUID` / `String` in public methods, every object parameter `@NotNull` / `@NotEmpty` / `@NotBlank` | `BeanArchitectureTest` |
| a `@Query` writes nothing (also in a lambda) | `QueryCommandArchitectureTest.queriesWriteNothing` |
| the views of a read facade follow the view rules | `DomainArchitectureTest` (`views…`) |

Not checked by ArchUnit: the name of a use case and the order in `process` – they are in this skill and checked when
writing and in the review.

## Checklist

- [ ] A change → a use case `<Aggregate><EntityCommand>`, the prefix the aggregate whose state changes; a query → a
      method of the controller's read facade.
- [ ] Use case: `@Service @Validated @RequiredArgsConstructor`, services as `private final @NonNull` fields, one
      `process(…)` with `@Command`, validated value objects or a command in, `void` or the new id out.
- [ ] `process`: existence first (a list: all entries), an aggregate found by its state throws the rule's exception,
      then one `ensure…` per rule with its id, then the service call(s); a batch is all or nothing.
- [ ] Read facade: `@Query` on every public method, views out, never a change; a view of its own in
      `<What it shows>Views`, validated, built in `to<View>` helpers; a precondition throws (`404`, `409`, `500` for
      broken data).
- [ ] README row "Enforced in … use case" for a new rule; tests as in "Tests"; `mvn -q test` passes.

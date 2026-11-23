---
name: aggregate
description: How an aggregate of the core-pos backend (Spring Boot, JPA, MapStruct, Lombok) is built in `domain/<aggregate>/` – the aggregate root entity and inner entities, *Commands, *EventFactory, *PayloadMapper, *Repository, *Service and *Views, which rule lives where (entity, service or use case), domain events and the outbox, value objects, the aggregate README, and the ArchUnit tests that enforce part of it. Use when adding or changing an aggregate, an entity, a command, a domain event, a repository query, a service method or a view, when adding a business rule to the domain, or when reviewing any of these or an ArchUnit failure in DomainArchitectureTest.
---

# Aggregates in core-pos

An aggregate is a folder in `core-pos/src/main/java/de/schaffbar/core_pos/domain/` – `customer`, `tool`,
`rfid_reader`, … It owns its table, its state changes and its events. Nothing outside the folder sees the entity:
others get **views** from the service, and changes come in as **commands** or value objects.

Everything that needs a second aggregate is a **use case** in `use_case/`; the REST controller and its API model live in
`domain/<aggregate>/web/`. Project rules, commands and the Definition of Done: `schaffbar-backend/CLAUDE.md`.

Reference implementation: `domain/rfid_reader/` – small, complete, and the pilot of the backend consistency work
(`docs/backend-consistency.md`).

## Folder

```
domain/<aggregate>/
  <Aggregate>.java                 aggregate root (@Entity)                     always
  <Aggregate>Commands.java         command records                              optional
  <Aggregate>EventFactory.java     creates the domain events                    always
  <Aggregate>PayloadMapper.java    entity → event payload (MapStruct)           always
  <Aggregate>Repository.java       Spring Data repository                       always
  <Aggregate>Service.java          queries and commands of the aggregate        always
  <Aggregate>Views.java            view records + entity → view (MapStruct)     always
  <Inner>.java + <Inner>Repository  inner entity (@Entity) with its repository   optional
  <Enum>.java                      enums of the aggregate (RfidReaderType) – public: views, API and payloads use them
  <Part>.java                      @Embeddable part of the entity (CustomerAddress) – package-private
  README.md                        attributes, rules, events
  web/                             controller + API model, only when the portal calls it
```

Nothing else: no `*Helper`, `*Utils`, `*Manager`. Something that does not fit one of these is either part of one of
them, a use case, or in `shared/`.

## Where a rule lives

| The rule needs | Lives in | Example |
| --- | --- | --- |
| one field's format without a value of its own | Bean Validation on the command (`@NotBlank`, `@Email`) | a valid email address |
| one field's value range Bean Validation has no annotation for | an own constraint in `shared/validation/` (annotation + `ConstraintValidator`), on the command | date of birth within the last 100 years (`@DateOfBirth`) |
| one field's format with a value (a length, a pattern) | an own constraint in `shared/validation/` composed of Bean Validation annotations (`@Size` + `@ReportAsSingleViolation`, `validatedBy = {}`), so the value is written once and command and request body share it – no constant | postal code has 5 characters (`@PostalCode`) |
| the aggregate's own state, the aggregate loaded by its id | the entity's command method, throws a dedicated exception | a paused certification cannot be paused again |
| the aggregate's own state, the aggregate found **by that state** ("the open session of the customer", "the waiting request", "the running visit") | whoever finds it – the service, or the use case that needs its id: the query finds the row in that state, `orElseThrow` throws the dedicated exception; the entity's command keeps a guard against a bug (see "How a check is written") | leaving without a running visit (WU-2), assigning a tag without a waiting request (TA-4) |
| other rows of the same aggregate | the service command | MAC address is unique |
| a second aggregate | a use case | a reader assigned to a tool cannot be deleted |
| a referenced aggregate exists | a use case, before the change; for a list, all entries first | the customers of a batch certification exist |

A violated rule throws a dedicated exception from `shared/exception/` that extends `BusinessRuleViolationException`
(→ `409`) or uses `ResourceNotFoundException` (→ `404`). Every rule gets a row in the README.

```java
public class MacAddressAlreadyUsedException extends BusinessRuleViolationException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String MESSAGE = "MAC address '%s' is already used by RFID reader [id: %s]";

    public static final String CODE = "MAC_ADDRESS_ALREADY_USED";

    public MacAddressAlreadyUsedException(MacAddress macAddress, RfidReaderId rfidReaderId) {
        super(CODE, String.format(MESSAGE, macAddress.getValue(), rfidReaderId.getValue()));
    }

}
```

- The **message** is English with the ids – for the log, the portal never shows it.
- The **code** is part of the API: the portal shows its translation (`shared.errors.<CODE>` in `de.json` and
  `en.json`). `public static final String CODE`, the class name in UPPER_SNAKE_CASE without `Exception`; one code per
  class – variants from static factories (`RfidReaderHasToolAssignedException.delete(id)`, `.changeType(id)`) share it,
  so its translation fits all of them. A new or renamed code needs the portal's translation in the same change.

### How a check is written

The same in services and use cases – the command reads as the list of its rules, then the change:

```java
@Command
public void startToolUsage(@NotNull @Valid StartToolUsageCommand command) {
    ensureToolNotInUseByAnotherCustomer(command.toolId(), command.customerId());
    ensureCustomerNotAlreadyUsingTool(command.customerId(), command.toolId());
    ensureMaxSimultaneousUsagesNotExceeded(command.customerId());

    ToolUsage toolUsage = ToolUsage.of(command);
    …
}

// ------------------------------------------------------------------------
// helper

// TU-9
private void ensureToolNotInUseByAnotherCustomer(ToolId toolId, CustomerId customerId) {
    this.toolUsageRepository.findByToolIdAndEndTimeIsNull(toolId) //
            .filter(existing -> !existing.getCustomerId().sameValueAs(customerId)) //
            .ifPresent(this::throwToolAlreadyInUseByAnotherCustomerException);
}

// TU-1
private void ensureMaxSimultaneousUsagesNotExceeded(CustomerId customerId) {
    long runningCount = this.toolUsageRepository.countByCustomerIdAndEndTimeIsNull(customerId);
    if (runningCount >= MAX_SIMULTANEOUS_TOOL_USAGES) {
        throw new MaxToolUsageExceededException(customerId, MAX_SIMULTANEOUS_TOOL_USAGES);
    }
}

private void throwToolAlreadyInUseByAnotherCustomerException(ToolUsage existing) {
    throw new ToolAlreadyInUseByAnotherCustomerException(existing.getToolId());
}
```

- **Every check before the change is a private `ensure<what must hold>(…)`** in `// helper` – also a one-liner, so
  the name says the rule. Above it the rule id from the README or why (`// TU-9`,
  `// the usage of the person at the tool would never end (T-17)`).
- A row that violates the rule: `find…(…).ifPresent(this::throwXyzException)` or
  `.ifPresent(existing -> throwXyzException(id, existing))` – **the `throw` lives in a private `throw<Exception>`
  helper, never `throw new` inside a lambda**. A helper that decides between two exceptions decides there
  (`throwCertificationExistsException`).
- A check without an `Optional` (`count >= MAX`, `anyMatch`, a `boolean` query) is a plain `if (…) throw new …;`
  inside the `ensure…` method.
- `ensure…` only checks, it changes nothing – an `ifPresent` that acts (unassign or cancel in `CustomerDelete`) is
  a helper with a verb of its own (`releaseRfidTag`).
- Loading stays inline: `orElseThrow(() -> ResourceNotFoundException.tool(toolId))` or a `findOrThrow` helper – it
  returns what the command works with. An aggregate found by its state throws the rule's exception instead
  (`findByCustomerIdAndExitTimeIsNull(customerId).orElseThrow(() -> new NoRunningWorkshopUsageFoundException(customerId))`,
  E29 in `docs/backend-consistency.md`).
- A guard against a bug (the caller already checked) is an `ensure…` too, throws `IllegalStateException` and says
  in its comment who checks before (`ensureNoOpenWorkshopSession` in `WorkshopSessionService`). The same in an
  entity's command whose aggregate was found by its state – the guard stays, the comment says who found it:

  ```java
  public List<SchaffbarEvent> stop() {
      // the service finds only a running visit (WU-2)
      if (nonNull(this.exitTime)) {
          throw new IllegalStateException("Workshop usage [id: " + this.id + "] has already been stopped");
      }
      …
  ```

## Aggregate root

```java
@Getter
@Setter(AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
@Entity
@Table(name = "RFID_READER", schema = "SCHAFFBAR")
class RfidReader {

    @Id
    private UUID id;

    @NotBlank
    @Column(unique = true)
    private String macAddress;

    @Enumerated(EnumType.STRING)
    private RfidReaderType type;

    @NotNull
    private Instant createdAt;

    @Version
    private Instant updatedAt;

    // ------------------------------------------------------------------------
    // static constructor

    public static RfidReader of(MacAddress macAddress) {
        RfidReader rfidReader = new RfidReader();
        rfidReader.setId(RfidReaderId.random().getValue());
        rfidReader.setMacAddress(macAddress.getValue());
        rfidReader.setCreatedAt(Instant.now());

        return rfidReader;
    }

    // ------------------------------------------------------------------------
    // query

    public RfidReaderId getId() {
        return RfidReaderId.of(this.id);
    }

    public MacAddress getMacAddress() {
        return MacAddress.of(this.macAddress);
    }

    // ------------------------------------------------------------------------
    // command

    public List<SchaffbarEvent> changeType(ChangeRfidReaderTypeCommand command) {
        setType(command.type());

        return List.of(RfidReaderEventFactory.rfidReaderTypeChanged(this));
    }

}
```

- **Package-private class** – only its own service sees it; everyone else gets views.
- **No public constructor.** The protected no-args constructor is for JPA; an aggregate is created by
  `public static <Aggregate> of(…)`, which takes a command or value objects, sets a new id
  (`<Aggregate>Id.random().getValue()`, not `UUID.randomUUID()`) and `createdAt` when the entity has one, and returns
  the entity – the creation event is created by the service after saving.
- **Fields store raw values**: the id and references to other aggregates as `UUID` (or `String` for RFID tags), value
  objects as their value, enums with `@Enumerated(EnumType.STRING)`. Constraints on the fields (`@NotNull`,
  `@NotBlank`) mirror the migration. Every entity has `@Version private Instant updatedAt`.
- **Getters are public, setters private** (Lombok `@Getter`, `@Setter(AccessLevel.PRIVATE)`).
- **`getId()` and every getter of a reference or of a value with a value object return the value object**, never
  `UUID` or `String`: `getCustomerId()` → `CustomerId.of(this.customerId)`, `getMacAddress()`, `getIpAddress()`; an
  optional one returns `null` when unset (`getCertifiedBy()`, `getIpAddress()`). The own id is always `getId()`, never
  `get<Aggregate>Id()`. Payload mapper and views then take the value object from the getter without a conversion.
- **Further public methods are queries or commands.** A query starts with `get`, `is` or `has` and may take parameters
  (`isInstructor(CustomerId)`, `getBillableDurationUntil(Instant)`). A command changes the state through the private
  setters and returns the events of the change as `List<SchaffbarEvent>`, created by the EventFactory. A command first
  checks the rules that need only the aggregate's own state and throws their exception
  (`ToolCertification.pause()`) – or, when the aggregate was found by that state, keeps a guard against a bug
  (`WorkshopUsage.stop()`, table "Where a rule lives").
- **A command is named after the change, without the aggregate's name:** a change of the aggregate itself is the
  verb alone (`session.close()`, `certification.pause()`, `assignment.cancel()`); a change concerning something else
  names it, and opposite changes are named as a pair (`tool.assignRfidReader()` / `clearRfidReader()`,
  `assignment.assignRfidTag()` / `unassignRfidTag()`). Never `certification.pauseCertification()`. The service command
  and the use case take their names from it (section "Service").
- **A state without an end is named as in the glossary (E6):** a session is *open* (`OPEN`), a workshop or tool usage
  is *running* (no `exitTime` / `endTime`) – in methods, exceptions and their codes, views, fields and API paths:
  `getOpenWorkshopSession`, `hasRunningToolUsage`, `RunningToolUsageExistsForSessionException`; never `active`. A
  certification's status `ACTIVE` is not such a state – it stays. The one exception, decided 23.11.2026: the
  dashboard keeps its names (`/dashboard/active-users`, `/dashboard/active-tools`, `Active…View` / `…ApiDto`,
  `getActive…`) – they name what the dashboard shows, not a usage.
- Null checks with `nonNull(…)` / `isNull(…)` (static import from `java.util.Objects`), not `!= null` / `== null`.
- Sections `// static constructor`, `// query`, `// command`, `// helper`.

## Inner entity

The aggregate root is the entity **named after its package** (`rfid_tag_assignment` → `RfidTagAssignment`). Another
entity in the package is an **inner entity**: it belongs to the aggregate and changes only together with it –
`RfidTagAssignmentHistory`, the entry an unassignment leaves behind (E16 in `docs/backend-consistency.md`).

- Package-private like the root, created by a **package-private** `static of(…)` that the service calls, private
  setters; it has its own package-private `<Inner>Repository`, but **no service, views, commands, event factory or
  payload mapper** of its own – the aggregate's building blocks cover it (its view is a record in `<Aggregate>Views`).
  Also on the web side: no controller or read facade of its own – a sub-path of the aggregate's controller
  (`GET /rfid-tag-assignments/history`) with a query in the aggregate's read facade (E16).
- **Its change is a change of the aggregate:** the service writes it in the same command, and the aggregate's event
  describes it. `unassignRfidTag` finds the customer's assignment in the state `ASSIGNED` (or throws
  `NoRfidTagAssignedException`), only then creates the history entry with `RfidTagAssignmentHistory.of(assignment)`,
  passes it to the entity's command `assignment.unassignRfidTag(historyEntry)`, saves it and deletes the assignment;
  `RFID_TAG_UNASSIGNED` carries the whole history entry. One change, one event – never a second event for the inner
  entity.
- **The inner entity is created after the aggregate's state is checked** – it is built from that state
  (`of(assignment)` reads the tag), so the service creates it only after it found the aggregate in the right state
  (E29); the guard in the root's command then protects against a bug.
- Its id stays inside (`UUID`, no value object) – nobody outside refers to it.
- Use it only when the rows exist because of the aggregate and are never changed alone. Something with its own
  lifecycle is an aggregate.

## Commands

```java
public interface RfidReaderCommands {

    @Builder
    @Validated
    record ChangeRfidReaderTypeCommand( //
            @NotNull @Valid RfidReaderId id, //
            @NotNull RfidReaderType type //
    ) {}

}
```

- **Optional.** A command is worth it when a change carries more than one or two values. A change with one or two
  value objects takes them directly (`createRfidReader(MacAddress)`, `deleteRfidReader(RfidReaderId)`). When no change
  of an aggregate needs a command, it has no `*Commands`.
- `public interface <Aggregate>Commands` with one nested record per command, named after the change:
  `CreateCustomerCommand`, `ChangeCustomerContactCommand`, `ChangeRfidReaderTypeCommand`, `SetWlanRelaisCommand`.
- Every record has `@Builder` and `@Validated`.
- **The own aggregate's id is `id`** (as in the entity and the view), a reference to another aggregate is
  `<aggregate>Id` (`customerId`, `toolId`) – both as value objects, never `UUID` or `String`.
- **Every field is validated:** a value object `@NotNull @Valid` (optional: only `@Valid`), a required string
  `@NotBlank`, other required fields `@NotNull`, formats `@Size`, `@Email`, `@Pattern`. A field without annotation is
  optional. The service validates the command with `@NotNull @Valid`. The request body of the API model carries the
  same constraints, so the controller answers `400` before the use case is called.
- A change of several entries at once takes a list, not one call per entry:
  `@NotEmpty List<@NotNull @Valid CustomerId> customerIds` (`addInstructors`, `removeInstructors`).

## EventFactory

```java
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class RfidReaderEventFactory {

    static SchaffbarEvent rfidReaderTypeChanged(RfidReader rfidReader) {
        var payload = RfidReaderPayloadMapper.MAPPER.toRfidReaderTypeChangedPayload(rfidReader);
        payload.validate();

        return SchaffbarEvent.rfidReaderEvent(EventType.RFID_READER_TYPE_CHANGED, rfidReader.getId(), payload);
    }

    static SchaffbarEvent rfidReaderDeleted(RfidReaderId rfidReaderId) {
        var payload = new RfidReaderDeletedPayload(rfidReaderId);
        payload.validate();

        return SchaffbarEvent.rfidReaderEvent(EventType.RFID_READER_DELETED, rfidReaderId, payload);
    }

}
```

- **The only place where events are created.** `final`, package-private, private no-args constructor, only static
  methods named `<aggregate><PastTense>` (`toolCertificationPaused`).
- Each method builds the payload – with the PayloadMapper from the entity, or `new …Payload(id)` when it only carries
  ids – **validates it right after building it** (`payload.validate()`), and creates the event with the
  aggregate's factory method `SchaffbarEvent.<aggregate>Event(EventType, id, payload)`.
- Called by the entity's commands and by the service for create and delete.
- **Every action with its own meaning has its own event** – `TOOL_WLAN_RELAIS_SET` and `TOOL_WLAN_RELAIS_CLEARED`, not
  one `TOOL_WLAN_RELAIS_UPDATED` for both.

**A new event:**

1. Constant in `shared/event/EventType`.
2. Payload record in `shared/event/payload/<Aggregate>EventPayload` – nested in the interface, implements it, fields
   validated like a command. Payloads contain **enums and values**: an enum as the enum, never its `String` (the
   stored JSON is the same); a value as its value object (`MacAddress`, `IpAddress`, the ids), also a value record
   from `shared/` (`BillingTotal`) – E31. Never an entity or an `@Embeddable`: a part of the entity gets its own record
   in the payload interface (`CustomerAddressPayload`, with `@Valid` on the field), so a change of the entity does not
   change the format of the stored events.
3. Method in the PayloadMapper (when mapped from the entity).
4. Method in the EventFactory.
5. Row in the README's "Domain Events".

## PayloadMapper

```java
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
interface RfidReaderPayloadMapper {

    RfidReaderPayloadMapper MAPPER = Mappers.getMapper(RfidReaderPayloadMapper.class);

    // ------------------------------------------------------------------------
    // mapper

    RfidReaderTypeChangedPayload toRfidReaderTypeChangedPayload(RfidReader rfidReader);

}
```

Package-private interface, `@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)` – a payload field the entity cannot
fill breaks the build instead of being `null` in the event. One method `to<Payload>(<Aggregate>)` per payload. Payload
and entity getters share the value objects, so no conversion is needed; a `default` method only where a type really
differs.

## Repository

```java
@Repository
interface ToolRepository extends JpaRepository<Tool, UUID> {

    List<Tool> findByNameIgnoreCase(String name);

    List<Tool> findByRfidReaderId(UUID rfidReaderId);

    default Optional<Tool> findByRfidReaderId(RfidReaderId rfidReaderId) {
        List<Tool> tools = findByRfidReaderId(rfidReaderId.getValue());
        if (tools.size() > 1) {
            throw new IllegalStateException("More than one tool with RFID reader [id: " + rfidReaderId.getValue() + "]");
        }

        return tools.stream().findFirst();
    }

}
```

- **Package-private interface** extending `JpaRepository<Aggregate, UUID>` (`String` for RFID tags), `@Repository`.
- Spring Data derives queries from methods with raw types. **For every method that takes the raw value of a value
  object** – a `UUID` id or reference, the `String` of an `RfidTagId`, a `MacAddress`, an `IpAddress` – **there is a
  `default` method with the same name directly below it that takes the value object** and calls it with `getValue()`.
  A plain value without a value object (a name, a status) needs none. The service only calls the value object variants
  and, of the inherited methods, `findById(id.getValue())` and `delete(entity)`; another inherited method it needs gets
  a value object overload too (`default boolean existsById(RfidTagId id)`). ArchUnit checks both:
  `servicesUseTheValueObjectVariants` for a method taking a `UUID`, `nothingDeletesOrChecksByARawId` for the
  inherited `deleteById`, `deleteAllById`, `deleteAllByIdInBatch` and `existsById` (their id is erased to `Object`) –
  outside the repository only the value object overload is allowed (E30).
- **`Optional` straight from Spring Data only on a unique column** (the id, a unique constraint, a partial unique
  index like V9). A query that should find at most one row on a column without such a constraint returns a `List` and
  checks "at most one" in the default method (`findByRfidReaderId` above); more than one is broken data →
  `IllegalStateException` instead of Spring's `IncorrectResultSizeDataAccessException`.
- A query the service needs under a name of its own wraps the derived one in a named `default` method
  (`findWaitingForAssignment()` → `findByStatus(WAITING_FOR_ASSIGNMENT)` in `RfidTagAssignmentRepository`).

## Service

```java
@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class RfidReaderService {

    private final @NonNull RfidReaderRepository rfidReaderRepository;

    private final @NonNull OutboxEventStore outboxEventStore;

    // ------------------------------------------------------------------------
    // query

    @Query
    public Optional<RfidReaderView> getRfidReader(@NotNull @Valid RfidReaderId id) {
        return this.rfidReaderRepository.findById(id.getValue()) //
                .map(RfidReaderViews.MAPPER::toRfidReaderView);
    }

    // ------------------------------------------------------------------------
    // command

    @Command
    public void changeRfidReaderType(@NotNull @Valid ChangeRfidReaderTypeCommand command) {
        List<SchaffbarEvent> events = this.rfidReaderRepository.findById(command.id().getValue()) //
                .map(rfidReader -> rfidReader.changeType(command)) //
                .orElseThrow(() -> ResourceNotFoundException.rfidReader(command.id()));

        this.outboxEventStore.save(events);

        log.info("Updated type of RFID reader with id {} and published events {}", command.id(), events);
    }

}
```

- `@Slf4j @Service @Validated @RequiredArgsConstructor`, `public class <Aggregate>Service`. Dependencies are
  `private final @NonNull` fields: **its own repository and the `OutboxEventStore` – never another service or a use
  case or a read facade.** Use cases (changes) and read facades (queries) call the services, never the other way round.
- **Public queries and public commands, everything else `private`** (helpers like `ensureMacAddressUnused`,
  `throwMacAddressAlreadyUsedException`, `findOrThrow` in `// helper`).
- **Input is a command or value objects**, never a raw `UUID` or `String`; every object parameter is validated
  (`@NotNull @Valid`, `@NotEmpty` for lists).
- **Every public method is `@Query` or `@Command`** (`de.schaffbar.core_pos.shared.cqrs` – not Spring Data's
  `@Query`). They bring the transaction: `@Query` is `@Transactional(readOnly = true)`, `@Command` is
  `@Transactional`. A service never uses `@Transactional` itself – what all queries or all commands need is added to
  the annotation, in one place.
- **Queries** (`@Query`) are named `get…` (`is…` / `has…` for a yes/no answer) and return views – `Optional<View>`,
  `List<View>` – or a plain answer (`boolean`, value object), never `void`. Never an entity, also not inside
  `Optional` or `List`. They map with `<Aggregate>Views.MAPPER`. Called from a use case they join its writing
  transaction.
- **Commands** are `@Command` and return `void` – only a create returns the new id. **A service command is the
  entity's command; where the entity leaves out the aggregate's name, it stands after the verb** (E20). The use case
  is named `<Aggregate><EntityCommand>` (E3, `schaffbar-backend/CLAUDE.md`):
  - **The prefix is the aggregate whose state changes** – the one whose service command the use case calls. When
    several change, it is the one the action is about (`WorkshopUsageStart` also starts a session). It is not the
    aggregate the action starts from: requesting a tag for a customer changes the assignment, so it is
    `RfidTagAssignmentRequest`, not `CustomerRequestRfidTagAssignment`. Two aggregates with the same start are told
    apart by the whole name – `ToolUsageStart` belongs to `ToolUsage`, not `Tool`.
  - **The action is the entity's command** without the aggregate's name (table below); a use case that only chooses
    between two others is named after what it does (`ToolUsageToggle` → `ToolUsageStart` or `ToolUsageStop`).
  - **No ArchUnit rule checks the name** (decided 23.11.2026: a check of the prefix alone let the wrong aggregate
    through, a full check needs a list of exceptions) – a new or renamed use case is checked against these two
    points when it is written and in the review.

  | Entity command | Service command | Use case |
  | --- | --- | --- |
  | the verb alone: `pause()`, `close()`, `stop()`, `update(…)` | verb + aggregate: `pauseToolCertification`, `closeWorkshopSession` | `ToolCertificationPause`, `WorkshopSessionClose` |
  | verb + property: `changeType(…)`, `changeContact(…)` | the aggregate after the verb: `changeRfidReaderType`, `changeCustomerContact` | `RfidReaderChangeType`, `CustomerChangeContact` |
  | names something else: `assignRfidReader(…)`, `setWlanRelais(…)`, `assignRfidTag(…)` | the same: `assignRfidReader`, `setWlanRelais`, `assignRfidTag` | `ToolAssignRfidReader`, `RfidTagAssignmentAssignRfidTag` |
  | create (`of(…)` + `save`), delete | verb + aggregate: `createTool`, `deleteTool` | `ToolCreate`, `ToolDelete` |

  The flow:
  - load the entity, or throw `ResourceNotFoundException.<aggregate>(id)` – or, found by its state, the rule's
    exception (E29);
  - check the rules that need other rows of the aggregate (uniqueness) – one `ensure…` each ("How a check is
    written");
  - call the entity's command (or `of(…)` + `save` + the created event from the EventFactory; for delete: the loaded
    entity to `delete(entity)` + the deleted event – never a view and `deleteById`, E30);
  - save the events with `this.outboxEventStore.save(events)` – in the same transaction, or not at all. **Every
    database change produces an event** – a create, an update, a delete, the write of an inner entity; a command that
    changes rows without an event is a bug;
  - `log.info("<What happened> … and published events {}", id, events)`.
- A loaded entity needs no `save` after a command – JPA writes the change at commit.

## Views

```java
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface RfidReaderViews {

    RfidReaderViews MAPPER = Mappers.getMapper(RfidReaderViews.class);

    // ------------------------------------------------------------------------
    // mapper

    RfidReaderView toRfidReaderView(RfidReader rfidReader);

    // ------------------------------------------------------------------------
    // views

    @Builder
    @Validated
    record RfidReaderView( //
            @NotNull @Valid RfidReaderId id, //
            @NotNull @Valid MacAddress macAddress, //
            RfidReaderType type, //
            String name, //
            String socketName, //
            @NotNull Instant createdAt //
    ) {}

}
```

- **The only form in which the aggregate leaves its package.** `public interface <Aggregate>Views`, which is itself the
  MapStruct mapper: `@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)` and `MAPPER`, then `// mapper` and
  `// views`.
- One record `<Aggregate>View` (and nested parts like `CustomerAddressView`), each with `@Builder` and `@Validated`.
- Ids, references and values with a value object as value objects (`MacAddress`, `IpAddress`), fields validated like
  a command: the annotations say which values can be missing – the API model and the portal's zod schema follow them.

## Value objects

Ids and checked values live in `shared/id/` (`CustomerId`, `RfidTagId`, `MacAddress`, `IpAddress`), and every class
there is one:

```java
@Value(staticConstructor = "of")
public class ToolId implements ValueObject<ToolId> {

    @JsonValue
    UUID value;

    @JsonCreator
    private ToolId(UUID id) {
        ValueObjectAssert.notNull(id, ValueObjectName.TOOL_ID);
        this.value = id;
    }

    public static ToolId random() {
        return ToolId.of(UUID.randomUUID());
    }

}
```

- **`implements ValueObject<Self>`:** `sameValueAs(…)` comes from the interface – never written again; the type
  parameter makes `toolId.sameValueAs(customerId)` a compile error.
- **They check themselves when created** in the private `@JsonCreator` constructor (`ValueObjectAssert` with the
  object's own `ValueObjectName`) – a value object that exists is valid.
- **`random()` only for UUID ids** – the entity's `of(…)` creates its id with it. A value that comes from a device
  (`MacAddress`, `RfidTagId`, `IpAddress`) has none: our system never invents one. Tests take random values from
  `RandomValues` in the test sources.
- **An `@Embeddable` is not a value object** – `CustomerAddress` is a part of the entity: package-private, without
  its own check on creation, stored in the entity's table. The README calls it a part (or embeddable), not a value
  object.

## README

Each aggregate has a `README.md`: aggregate root and identity, attributes (field, type, constraints), enums, commands,
**Business Rules / Invariants** (`#`, rule, where it is enforced), **Open Rules** (enforced only in the portal /
planned) and **Domain Events**. Changed together with the code: a new rule, event or field is a new row; implementing
an open rule moves it to "Business Rules".

## A new aggregate

Besides the folder, a new aggregate touches `shared/`, the migrations and the docs – in this order:

1. **Id** in `shared/id/`: `<Aggregate>Id` like `ToolId` (section "Value objects"), plus its name in
   `ValueObjectAssert.ValueObjectName`.
2. **Converter** in `shared/util/`: `StringTo<ValueObject>Converter implements Converter<String, …>` (`@Component`) for
   every value object used as `@PathVariable` or `@RequestParam` – ids and others (`MacAddress`).
3. **Events** in `shared/event/`: a value in `AggregateType`, the event types in `EventType`, the payload interface
   `<Aggregate>EventPayload` in `payload/`, and the factory method `SchaffbarEvent.<aggregate>Event(EventType,
   <Aggregate>Id, <Aggregate>EventPayload)`.
4. **Exceptions** in `shared/exception/`: the rule exceptions of the aggregate; a `Resource` value and a factory in
   `ResourceNotFoundException` only when something looks it up by its id (a `404`). Like the converter: nothing is
   added for later.
5. **The folder** (above): enums, commands, entity, repository, payload mapper, event factory, views, service, README.
6. **Migration** `core-pos/src/main/resources/db/migration/V<next>__<aggregate>.sql`: `CREATE TABLE
   schaffbar.<aggregate> (…)` with primary key, constraints and indexes, then
   `GRANT ALL ON TABLE schaffbar.<aggregate> TO schadmin;`. **No foreign keys** – not to other aggregates (they are
   checked in use cases) and not inside the aggregate either (`@ElementCollection` tables are managed by JPA).
7. **Use cases, controller and API model** – `schaffbar-backend/CLAUDE.md` until their skills exist.
8. **Docs**: the README of the aggregate; in `core-pos/docs/ddd-overview.md` and `ddd-uebersicht.md` the domain
   overview, the aggregate catalog (link to the README), the use cases, value objects and the event list.

## Code style

- Formatting and import order: `schaffbar-backend/CLAUDE.md` ("Formatting").
- `this.` for fields; `//` at the end of a line in a call chain or a record header, one element per line.
- Section separators `// ------…` (ending at column 79, indentation counts) followed by the section name, as in the
  examples.
- Comments only for what the code can't say; no JavaDoc that repeats a name.

## Not everywhere yet

The rules above are the target. Some code predates them and is fixed aggregate by aggregate (roadmap K4) – **follow
this skill, not the code next to you.** Known deviations:

- Value objects without `@Valid` in commands and views (`@NotNull CustomerId customerId` in `WorkshopUsageViews`,
  `ToolViews`, …).

Every ArchUnit rule is strict – the freeze store is gone (22.11.2026).

## Tests that enforce it

Most of these rules are checked by the ArchUnit tests in `core-pos/src/test/java/de/schaffbar/core_pos/architecture/`
(`DomainArchitectureTest`, `DependencyArchitectureTest`, `ValueObjectArchitectureTest`, …) – part of `mvn -q test`.
Every rule is strict (no `freeze(…)`): a violation is fixed, never excepted to make a build pass. A rule in this skill and its ArchUnit rule change together.

## Checklist

- [ ] Only the known building blocks in the folder; entity and embeddable package-private, created by `of(…)`.
- [ ] Every state change is an entity command returning its events from the EventFactory; every database change has
      its event, an inner entity's change is in the aggregate's event.
- [ ] Ids and references are value objects everywhere outside the entity's fields and the repository's derived queries.
- [ ] Commands and views validated field by field; `@Builder` + `@Validated`.
- [ ] Service: own repository + `OutboxEventStore` only; queries `@Query` returning views; commands `@Command`, save their
      events, log them.
- [ ] Rule in the right place (table "Where a rule lives") with a dedicated exception and a test; a guard against a
      bug says who checks before.
- [ ] Each check an `ensure…` with its rule id; `throw` in a `throw…` helper, never inside a lambda.
- [ ] Names: service command = entity command (+ aggregate where the entity leaves it out); use case
      `<Aggregate><EntityCommand>` with the aggregate whose state changes (no ArchUnit rule – check it by hand); open /
      running, never active.
- [ ] Delete: load the entity, `delete(entity)`, the deleted event – no `deleteById` (ArchUnit).
- [ ] New event: EventType, payload (enums as enum, values as value objects, no entity or embeddable inside), mapper,
      factory, README.
- [ ] README updated; `mvn -q test` passes.

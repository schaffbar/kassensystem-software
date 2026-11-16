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
  <Enum>.java, @Embeddable          types of the aggregate (RfidReaderType, CustomerAddress)
  README.md                        attributes, rules, events
  web/                             controller + API model, only when the portal calls it
```

Nothing else: no `*Helper`, `*Utils`, `*Manager`. Something that does not fit one of these is either part of one of
them, a use case, or in `shared/`.

## Where a rule lives

| The rule needs | Lives in | Example |
| --- | --- | --- |
| one field's format | Bean Validation on the command (`@NotBlank`, `@Size`, `@Email`, `@Pattern`) | postal code has 5 characters |
| the aggregate's own state | the entity's command method, throws a dedicated exception | a paused certification cannot be paused again |
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

    @NotNull
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
  (`<Aggregate>Id.random().getValue()`, not `UUID.randomUUID()`) and `createdAt`, and returns the entity – the creation
  event is created by the service after saving.
- **Fields store raw values**: the id and references to other aggregates as `UUID` (or `String` for RFID tags), value
  objects as their value, enums with `@Enumerated(EnumType.STRING)`. Constraints on the fields (`@NotNull`,
  `@NotBlank`) mirror the migration. Every entity has `@Version private Instant updatedAt`.
- **Getters are public, setters private** (Lombok `@Getter`, `@Setter(AccessLevel.PRIVATE)`).
- **`getId()` and every getter of a reference return the value object**, never `UUID` or `String`:
  `getCustomerId()` → `CustomerId.of(this.customerId)`; an optional reference returns `null` when unset
  (`getCertifiedBy()`). The own id is always `getId()`, never `get<Aggregate>Id()`.
- **Further public methods are queries or commands.** A query starts with `get`, `is` or `has` and may take parameters
  (`isInstructor(CustomerId)`, `getBillableDurationUntil(Instant)`). A command changes the state through the private
  setters and returns the events of the change as `List<SchaffbarEvent>`, created by the EventFactory. A command first
  checks the rules that need only the aggregate's own state and throws their exception
  (`ToolCertification.pause()`).
- **A command is named after the change, without the aggregate's name:** a change of the aggregate itself is the
  verb alone (`session.close()`, `certification.pause()`, `assignment.cancel()`); a change concerning something else
  names it, and opposite changes are named as a pair (`tool.assignRfidReader()` / `clearRfidReader()`,
  `assignment.assignRfidTag()` / `unassignRfidTag()`). Never `certification.pauseCertification()`.
- Null checks with `nonNull(…)` / `isNull(…)` (static import from `java.util.Objects`), not `!= null` / `== null`.
- Sections `// static constructor`, `// query`, `// command`, `// helper`.

## Inner entity

The aggregate root is the entity **named after its package** (`rfid_tag_assignment` → `RfidTagAssignment`). Another
entity in the package is an **inner entity**: it belongs to the aggregate and changes only together with it –
`RfidTagAssignmentHistory`, the entry an unassignment leaves behind (E16 in `docs/backend-consistency.md`).

- Package-private like the root, created by a **package-private** `static of(…)` that the service calls, private
  setters; it has its own package-private `<Inner>Repository`, but **no service, views, commands, event factory or
  payload mapper** of its own – the aggregate's building blocks cover it (its view is a record in `<Aggregate>Views`).
- **Its change is a change of the aggregate:** the service writes it in the same command, and the aggregate's event
  describes it. `unassignRfidTag` creates the history entry with `RfidTagAssignmentHistory.of(assignment)`, passes it
  to the entity's command `assignment.unassignRfidTag(historyEntry)`, saves it and deletes the assignment;
  `RFID_TAG_UNASSIGNED` carries the whole history entry. One change, one event – never a second event for the inner
  entity.
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
  `CreateCustomerCommand`, `UpdateCustomerContactCommand`, `ChangeRfidReaderTypeCommand`, `SetWlanRelaisCommand`.
- Every record has `@Builder` and `@Validated`.
- **The own aggregate's id is `id`** (as in the entity and the view), a reference to another aggregate is
  `<aggregate>Id` (`customerId`, `toolId`) – both as value objects, never `UUID` or `String`.
- **Every field is validated:** a value object `@NotNull @Valid` (optional: only `@Valid`), a required string
  `@NotBlank`, other required fields `@NotNull`, formats `@Size`, `@Email`, `@Pattern`. A field without annotation is
  optional. The service validates the command with `@NotNull @Valid`.
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
   validated like a command. Payloads may use the enums and value objects of their aggregate.
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

    Optional<Tool> findByName(String name);

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
- Spring Data derives queries from methods with raw types. **For every method that takes a raw id (`UUID`) there is a
  `default` method with the same name that takes the value objects** and calls it with `getValue()`. The service only
  calls the value object variants (and the inherited `findById(id.getValue())`, `deleteById(…)`).
- A query for one row that the database returns as a list checks "at most one" in the default method; more than one
  is broken data → `IllegalStateException`.

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
- **Public queries and public commands, everything else `private`** (helpers like `throwMacAddressAlreadyUsedException`,
  `findByCustomerAndToolOrThrow` in `// helper`).
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
- **Commands** are named verb + aggregate (`createTool`, `changeRfidReaderType`, `pauseToolCertification`),
  `@Command`, and return `void` – only a create returns the new id. The flow:
  - load the entity, or throw `ResourceNotFoundException.<aggregate>(id)`;
  - check the rules that need other rows of the aggregate (uniqueness) – with a private `throw…` helper, no
    `throw new` inside a lambda:
    `findByMacAddress(…).ifPresent(existing -> throwMacAddressAlreadyUsedException(macAddress, existing))`;
  - call the entity's command (or `of(…)` + `save` + the created event from the EventFactory; for delete:
    `deleteById` + the deleted event);
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
            @NotBlank String macAddress, //
            RfidReaderType type, //
            String name, //
            @NotNull Instant createdAt //
    ) {}

}
```

- **The only form in which the aggregate leaves its package.** `public interface <Aggregate>Views`, which is itself the
  MapStruct mapper: `@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)` and `MAPPER`, then `// mapper` and
  `// views`.
- One record `<Aggregate>View` (and nested parts like `CustomerAddressView`), each with `@Builder` and `@Validated`.
- Ids and references as value objects, fields validated like a command: the annotations say which values can be
  missing – the API model and the portal's zod schema follow them.

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
   every value object used as `@PathVariable` or `@RequestParam` – ids and others (`MacAddress`, `IpAddress`).
3. **Events** in `shared/event/`: a value in `AggregateType`, the event types in `EventType`, the payload interface
   `<Aggregate>EventPayload` in `payload/`, and the factory method `SchaffbarEvent.<aggregate>Event(EventType,
   <Aggregate>Id, <Aggregate>EventPayload)`.
4. **Exceptions** in `shared/exception/`: a `Resource` value and a factory in `ResourceNotFoundException`; the rule
   exceptions of the aggregate.
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

- `@Transactional` (Spring's or jakarta's) instead of `@Query` / `@Command`, queries without any transaction (E9,
  E21) – `rfid_reader`, `rfid_tag` and `rfid_tag_assignment` are converted.
- `RuntimeException` and exceptions without a base class → dedicated exceptions / `IllegalStateException` (E8).
- Repository default methods with other names (`findActiveByCustomerId`) or missing
  (`ToolCertificationRepository`); services calling the `UUID` variants.
- Short command names in `ToolCertificationService` (`create`, `pause`), `startUsage`, `enterWorkshop`.
- Value objects without `@Valid` in commands and views.
- Controllers of the not yet converted aggregates read the services directly; `domain/dashboard/` still holds services that read other services – both
  become read facades (E17).

The full list is the ArchUnit freeze store `core-pos/archunit_store/`.

## Tests that enforce it

Most of these rules are checked by the ArchUnit tests in `core-pos/src/test/java/de/schaffbar/core_pos/architecture/`
(`DomainArchitectureTest`, `DependencyArchitectureTest`, `ValueObjectArchitectureTest`, …) – part of `mvn -q test`. Rules with known violations are
frozen: only new violations fail; a fixed one disappears from `archunit_store/` on the next run – keep that change.
Never add a violation to the store to make a build pass. A rule in this skill and its ArchUnit rule change together.

## Checklist

- [ ] Only the known building blocks in the folder; entity package-private, created by `of(…)`.
- [ ] Every state change is an entity command returning its events from the EventFactory; every database change has
      its event, an inner entity's change is in the aggregate's event.
- [ ] Ids and references are value objects everywhere outside the entity's fields and the repository's derived queries.
- [ ] Commands and views validated field by field; `@Builder` + `@Validated`.
- [ ] Service: own repository + `OutboxEventStore` only; queries `@Query` returning views; commands `@Command`, save their
      events, log them.
- [ ] Rule in the right place (table "Where a rule lives") with a dedicated exception and a test.
- [ ] New event: EventType, payload, mapper, factory, README.
- [ ] README updated; `mvn -q test` passes.

# Schaffbar — Documentation

An overview of the core-pos backend for someone new to it. The rules themselves – how an aggregate, a use case or a
test is written – are in `schaffbar-backend/CLAUDE.md` and the skills `aggregate` and `backend-testing`; the rules of
each aggregate are in its `README.md`. When this overview and they differ, they are right.

## Table of Contents

1. [Domain Overview](#1-domain-overview)
2. [Aggregate Structure Pattern](#2-aggregate-structure-pattern)
3. [Aggregate Catalog](#3-aggregate-catalog)
4. [Use Cases](#4-use-cases)
5. [Read Facades](#5-read-facades)
6. [Value Objects](#6-value-objects)
7. [Domain Events](#7-domain-events)

---

## 1. Domain Overview

The **Schaffbar Core-POS** system manages a **workshop** (Werkstatt) where customers check in, use tools, and are
billed for their time. The core domain revolves around:

- **Customer management** — personal data, contact info, addresses, age group
- **RFID infrastructure** — readers, tags, and their assignment to customers
- **Tool management** — workshop tools with their switch box (RFID reader) and WLAN relais (Shelly), instructors
- **Tool certification management** — per-customer, per-tool certifications (Einweisungen) that gate access to
  machines (ACTIVE/PAUSED/REVOKED lifecycle)
- **Workshop access tracking** — sessions (what is billed, `OPEN` → `PAID`) and workshop usages (one stay from the
  gate at the entrance to the gate at the exit)
- **Tool usage tracking** — which customer uses which tool, for billing and safety enforcement
- **Devices** — the RFID readers at the counter, at the gates and at the tools talk to `/device/*`
- **Workshop dashboard** — who is in the workshop right now and which tools run

### Bounded Context

All aggregates live in a single bounded context: **Core-POS**. An aggregate changes only itself; its service knows
only its own repository and the outbox. **Every change is a use case** in `use_case/` – one class per action, also for
a plain create, update or delete, so every controller looks the same. A use case checks what needs a second aggregate
(it exists, a rule across aggregates) and then calls the service command(s). **Every query goes through the read
facade** of its controller in `read_facade/`. Controllers never call a service.

### Event-Driven Architecture

Every database change produces **`SchaffbarEvent`** instances – a create, an update, a delete, the write of an inner
entity. They are saved to the **transactional outbox** (`outbox_event` table) in the transaction of the change, or not
at all. Today the outbox is the **change log** of the system: no process reads or publishes it yet. The columns
`processed`, `processed_at` and `owner` are prepared for a later relay and stay empty.

---

## 2. Aggregate Structure Pattern

Every aggregate is a folder in `domain/` with the same building blocks (skill `aggregate`, "Folder"):

```txt
domain/<aggregate>/
│
├── <Aggregate>.java              # aggregate root (@Entity)
├── <Aggregate>Commands.java      # command records (optional)
├── <Aggregate>EventFactory.java  # creates the domain events
├── <Aggregate>PayloadMapper.java # entity → event payload (MapStruct)
├── <Aggregate>Repository.java    # Spring Data repository, package-private
├── <Aggregate>Service.java       # queries and commands of the aggregate
├── <Aggregate>Views.java         # view records + entity → view (MapStruct)
├── <Inner>.java + <Inner>Repository.java  # inner entity with its repository (optional, RfidTagAssignmentHistory)
├── <Enum>.java                   # enums of the aggregate (RfidReaderType, AgeGroup) – public
├── <Part>.java                   # @Embeddable part of the entity (CustomerAddress) – package-private
├── README.md                     # attributes, rules, events
│
└── web/                          # only when the portal calls it
    ├── <Aggregate>Controller.java   # REST controller
    └── <Aggregate>ApiModel.java     # response and request records, MapStruct mapping
```

### Component Responsibilities

| Component                 | Responsibility                                                                                                                                                                                                            |
|---------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Aggregate Root**        | Encapsulates state and invariants. Created by a static `of(…)`; every state change is a command method that returns `List<SchaffbarEvent>`. Nothing outside the folder sees the entity.                                   |
| **Commands**              | Immutable `record` types with Bean Validation, grouped in a `*Commands` interface.                                                                                                                                        |
| **Service**               | Loads the entity (or `404`), checks the rules that need other rows of the aggregate (uniqueness), calls the entity's command and saves its events with `OutboxEventStore`. Every public method is `@Query` or `@Command`. |
| **Repository**            | Spring Data `JpaRepository`, package-private; every method taking a raw value has an overload taking the value object.                                                                                                    |
| **Views**                 | Read-only `record` projections, mapped with MapStruct (`MAPPER`) – what the outside world gets instead of the entity.                                                                                                     |
| **EventFactory**          | Creates the `SchaffbarEvent` of each event type: maps the entity to its payload (`PayloadMapper`), validates the payload, wraps it.                                                                                       |
| **PayloadMapper**         | MapStruct interface from the entity to the event payload records.                                                                                                                                                         |
| **Controller / ApiModel** | REST layer: a command calls a use case, a query the controller's read facade.                                                                                                                                             |

### Aggregate Root Internal Structure

```java
class Aggregate {

    // fields: JPA @Id (raw UUID), attributes, references as raw ids, @Version Instant updatedAt

    // --- static constructor ---
    // static Aggregate of(<CreateCommand>) { ... }   – the service saves it and builds the created event

    // --- query ---
    // getId() and the getters of references return value objects
    // derived queries: is…, has…, get…

    // --- command ---
    // List<SchaffbarEvent> update(...) { ... }
    // List<SchaffbarEvent> <domainAction>(...) { ... }

    // --- helper ---
}
```

**Key conventions:**

- **No public setters** — state changes only through named command methods
- **Every command method returns `List<SchaffbarEvent>`** — the service saves them to the outbox
- **`of(…)` returns the new entity** — the service saves it and creates the created event with the EventFactory
- **`@Version` field** (`Instant updatedAt`) for optimistic locking
- **Ids stored as raw types** (`UUID`, `String`) internally, exposed as **value objects** by the getters

---

## 3. Aggregate Catalog

Each aggregate's detailed documentation (attributes, commands, business rules, domain events) lives in a `README.md`
inside its package. This keeps the documentation close to the code it describes.

| #   | Aggregate         | Package                                            | Identity                     | README                                                                                                       |
|-----|-------------------|----------------------------------------------------|------------------------------|--------------------------------------------------------------------------------------------------------------|
| 3.1 | Customer          | `de.schaffbar.core_pos.domain.customer`            | `CustomerId` (UUID)          | [customer/README.md](../src/main/java/de/schaffbar/core_pos/domain/customer/README.md)                       |
| 3.2 | Tool              | `de.schaffbar.core_pos.domain.tool`                | `ToolId` (UUID)              | [tool/README.md](../src/main/java/de/schaffbar/core_pos/domain/tool/README.md)                               |
| 3.3 | RfidReader        | `de.schaffbar.core_pos.domain.rfid_reader`         | `RfidReaderId` (UUID)        | [rfid_reader/README.md](../src/main/java/de/schaffbar/core_pos/domain/rfid_reader/README.md)                 |
| 3.4 | RfidTag           | `de.schaffbar.core_pos.domain.rfid_tag`            | `RfidTagId` (String)         | [rfid_tag/README.md](../src/main/java/de/schaffbar/core_pos/domain/rfid_tag/README.md)                       |
| 3.5 | RfidTagAssignment | `de.schaffbar.core_pos.domain.rfid_tag_assignment` | `RfidTagAssignmentId` (UUID) | [rfid_tag_assignment/README.md](../src/main/java/de/schaffbar/core_pos/domain/rfid_tag_assignment/README.md) |
| 3.6 | WorkshopSession   | `de.schaffbar.core_pos.domain.workshop_session`    | `WorkshopSessionId` (UUID)   | [workshop_session/README.md](../src/main/java/de/schaffbar/core_pos/domain/workshop_session/README.md)       |
| 3.7 | WorkshopUsage     | `de.schaffbar.core_pos.domain.workshop_usage`      | `WorkshopUsageId` (UUID)     | [workshop_usage/README.md](../src/main/java/de/schaffbar/core_pos/domain/workshop_usage/README.md)           |
| 3.8 | ToolUsage         | `de.schaffbar.core_pos.domain.tool_usage`          | `ToolUsageId` (UUID)         | [tool_usage/README.md](../src/main/java/de/schaffbar/core_pos/domain/tool_usage/README.md)                   |
| 3.9 | ToolCertification | `de.schaffbar.core_pos.domain.tool_certification`  | `ToolCertificationId` (UUID) | [tool_certification/README.md](../src/main/java/de/schaffbar/core_pos/domain/tool_certification/README.md)   |

---

## 4. Use Cases

One class per action in `use_case/`, named `<Aggregate><EntityCommand>`: the prefix is the aggregate whose state
changes, the action is the command of its entity (skill `aggregate`, "Service"). The rule ids are those of
the aggregate READMEs.

**With a second aggregate** – the use case reads or changes more than its own aggregate:

| Use Case                           | Also reads / changes                                                | What it checks and does                                                                                                                                                                                                                               |
|------------------------------------|---------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `CustomerDelete`                   | WorkshopSession; changes RfidTagAssignment, ToolCertification, Tool | no open session (C-5); frees the tag – an assigned one is unassigned, a waiting request cancelled (C-4) –, deletes the customer's certifications and removes them as instructor (C-9); paid sessions, usages and certifications they gave stay (C-11) |
| `RfidReaderChangeType`             | Tool                                                                | a switch box with a tool keeps its type (R-3)                                                                                                                                                                                                         |
| `RfidReaderDelete`                 | Tool                                                                | a reader assigned to a tool is not deleted (R-5)                                                                                                                                                                                                      |
| `RfidTagAssignmentRequest`         | Customer                                                            | the customer exists, then the request waits for a tag                                                                                                                                                                                                 |
| `RfidTagAssignmentAssignRfidTag`   | RfidTag                                                             | the tag is registered, then it is assigned to the waiting request (the reader at the counter)                                                                                                                                                         |
| `RfidTagAssignmentUnassignRfidTag` | Customer                                                            | the customer exists, then their tag is unassigned (history entry)                                                                                                                                                                                     |
| `RfidTagDelete`                    | RfidTagAssignment                                                   | an assigned tag is not deleted (RT-3)                                                                                                                                                                                                                 |
| `ToolAddInstructors`               | Customer                                                            | the instructors exist as customers (T-22)                                                                                                                                                                                                             |
| `ToolRemoveInstructors`            | Customer                                                            | the instructors exist as customers (T-22)                                                                                                                                                                                                             |
| `ToolAssignRfidReader`             | RfidReader                                                          | the reader exists and is a switch box (T-9)                                                                                                                                                                                                           |
| `ToolClearRfidReader`              | ToolUsage                                                           | not while the tool runs – the usage could no longer be stopped at the switch box (T-14)                                                                                                                                                               |
| `ToolDelete`                       | ToolUsage; changes ToolCertification                                | not while the tool runs (T-17); its certifications are deleted with it                                                                                                                                                                                |
| `ToolCertificationCreate`          | Tool, Customer                                                      | all customers exist – all or nobody (TC-4); the one who certifies is an instructor of the tool (TC-8); instructors of the tool are not certified (TC-5)                                                                                               |
| `ToolUsageStart`                   | Customer, Tool, ToolCertification, WorkshopUsage                    | a `RED` tool needs an active certification, `YELLOW` only recommends one (TU-10); the customer is in the workshop, the usage belongs to the session of the running visit (TU-2)                                                                       |
| `ToolUsageToggle`                  | – (calls `ToolUsageStart` or `ToolUsageStop`)                       | the one action of the switch box: switches the tool off when the customer uses it, else on (TU-11)                                                                                                                                                    |
| `WorkshopSessionClose`             | Customer, WorkshopUsage, ToolUsage                                  | the customer's open session (WS-4); no visit and no tool usage runs (WS-5); closes it with the totals of both                                                                                                                                         |
| `WorkshopUsageStart`               | Customer; changes WorkshopSession                                   | the first check-in starts the session, every later one joins it (WU-4)                                                                                                                                                                                |
| `WorkshopUsageStop`                | Customer, WorkshopSession; changes ToolUsage                        | an open session – "never checked in" apart from "already checked out" (WU-9, WU-2); the running tools are switched off (WU-5, TU-4)                                                                                                                   |

**Only their own aggregate** – they forward to the service, which checks the aggregate's own rules: `CustomerCreate`,
`CustomerUpdate`, `CustomerChangeContact`, `CustomerChangeAddress`, `RfidReaderCreate`, `RfidReaderUpdate`,
`RfidTagCreate`, `ToolCreate`, `ToolUpdate`, `ToolSetWlanRelais`, `ToolClearWlanRelais`, `ToolCertificationPause`,
`ToolCertificationReactivate`, `ToolCertificationRevoke`, `ToolCertificationDelete`, `ToolUsageStop`.

---

## 5. Read Facades

Every controller reads through the one read facade named after it in `read_facade/`; a read facade only reads.
Those that combine several aggregates have views of their own (`DashboardViews`, `DeviceViews`,
`OpenWorkshopSessionViews`):

| Read facade                   | Reads                                                                                             | What for                                                                                                                                                                                     |
|-------------------------------|---------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `DashboardReadFacade`         | WorkshopUsage, ToolUsage, Tool, Customer                                                          | `GET /dashboard/active-users`: the running workshop usages with their customer, the longest first; `GET /dashboard/active-tools`: the running tool usages with tool and customer             |
| `DeviceReadFacade`            | RfidReader, RfidTag, RfidTagAssignment, Customer, Tool, ToolUsage, WorkshopSession, WorkshopUsage | `/device/*`: the reader by its MAC address, the customer of a tag (tag → assignment → customer), the tool of a switch box, whether a tool runs, the stay shown after a check-in or check-out |
| `WorkshopSessionReadFacade`   | WorkshopSession, WorkshopUsage, ToolUsage, Tool, Customer                                         | `GET /workshop-sessions/{customerId}/open`: the open session with its visits and one summary per tool, with the totals                                                                       |
| `RfidTagAssignmentReadFacade` | RfidTagAssignment, Customer                                                                       | the assignments (the customer must exist) and their history                                                                                                                                  |
| the others                    | their aggregate                                                                                   | `CustomerReadFacade`, `RfidReaderReadFacade`, `RfidTagReadFacade`, `ToolReadFacade`, `ToolCertificationReadFacade`                                                                           |

---

## 6. Value Objects

All value objects are located in `de.schaffbar.core_pos.shared.id`. They check themselves when created; the ids have
`random()`, the values that come from a device (`MacAddress`, `RfidTagId`, `IpAddress`) have not – the system never
invents one.

| Value Object          | Wraps    | Used for                                      |
|-----------------------|----------|-----------------------------------------------|
| `CustomerId`          | `UUID`   | Customer identity                             |
| `ToolId`              | `UUID`   | Tool identity                                 |
| `ToolUsageId`         | `UUID`   | Tool Usage identity                           |
| `ToolCertificationId` | `UUID`   | Tool Certification identity                   |
| `RfidReaderId`        | `UUID`   | RFID Reader identity                          |
| `RfidTagId`           | `String` | RFID Tag identity (hardware id, `04A31F9C`)   |
| `RfidTagAssignmentId` | `UUID`   | RFID Tag Assignment identity                  |
| `WorkshopSessionId`   | `UUID`   | Workshop Session identity                     |
| `WorkshopUsageId`     | `UUID`   | Workshop Usage identity                       |
| `MacAddress`          | `String` | RFID Reader MAC address (`34:CD:B0:D3:A9:30`) |
| `IpAddress`           | `String` | IPv4 address of a tool's WLAN relais          |

---

## 7. Domain Events

### Event Infrastructure

- **`SchaffbarEvent`** — immutable envelope with `id`, `type`, `version`, `aggregateType`, `aggregateId`,
  `timestamp`, `payload` (JSON) and `owner` (not set yet); built only by its static factories, which the EventFactory
  of each aggregate calls
- **`EventPayload`** — the interface of all payload records, with `validate()` (Bean Validation)
- **`OutboxEventStore`** — saves the events of a command (`Propagation.MANDATORY`: outside the command's transaction
  it fails, so an event is never saved without its change)
- **`OutboxEvent`** — JPA entity of the `schaffbar.outbox_event` table, the whole event as JSON in `payload`
- **`AggregateType`** — enum identifying the source aggregate

### Event Flow

```txt
Command method of the aggregate root (or of(…) + the created event of the EventFactory)
  └── returns List<SchaffbarEvent>
        └── the service calls OutboxEventStore.save(events) – in the command's transaction
              └── outbox_event table – the change log; no relay reads it yet
```

### Event Creation Pattern

```txt
EventFactory.someEvent(aggregate)
  ├── PayloadMapper.MAPPER.toSomePayload(aggregate)           // MapStruct mapping
  ├── payload.validate()                                      // Bean Validation
  └── SchaffbarEvent.<aggregateType>Event(type, id, payload)  // envelope
```

### Complete Event List

| Aggregate         | Event Type                                                                                                                                                                                                        |
|-------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Customer          | `CUSTOMER_CREATED`, `CUSTOMER_UPDATED`, `CUSTOMER_CONTACT_CHANGED`, `CUSTOMER_ADDRESS_CHANGED`, `CUSTOMER_DELETED`                                                                                                |
| Tool              | `TOOL_CREATED`, `TOOL_UPDATED`, `TOOL_WLAN_RELAIS_SET`, `TOOL_WLAN_RELAIS_CLEARED`, `TOOL_RFID_READER_ASSIGNED`, `TOOL_RFID_READER_CLEARED`, `TOOL_INSTRUCTORS_ADDED`, `TOOL_INSTRUCTORS_REMOVED`, `TOOL_DELETED` |
| ToolUsage         | `TOOL_USAGE_STARTED`, `TOOL_USAGE_STOPPED`                                                                                                                                                                        |
| ToolCertification | `TOOL_CERTIFICATION_CREATED`, `TOOL_CERTIFICATION_PAUSED`, `TOOL_CERTIFICATION_REACTIVATED`, `TOOL_CERTIFICATION_REVOKED`, `TOOL_CERTIFICATION_DELETED`                                                           |
| RfidReader        | `RFID_READER_CREATED`, `RFID_READER_UPDATED`, `RFID_READER_TYPE_CHANGED`, `RFID_READER_DELETED`                                                                                                                   |
| RfidTag           | `RFID_TAG_CREATED`, `RFID_TAG_DELETED`                                                                                                                                                                            |
| RfidTagAssignment | `RFID_TAG_ASSIGNMENT_REQUESTED`, `RFID_TAG_ASSIGNMENT_CANCELLED`, `RFID_TAG_ASSIGNED`, `RFID_TAG_UNASSIGNED`                                                                                                      |
| WorkshopSession   | `WORKSHOP_SESSION_STARTED`, `WORKSHOP_SESSION_CLOSED`                                                                                                                                                             |
| WorkshopUsage     | `WORKSHOP_USAGE_ENTERED`, `WORKSHOP_USAGE_LEFT`                                                                                                                                                                   |

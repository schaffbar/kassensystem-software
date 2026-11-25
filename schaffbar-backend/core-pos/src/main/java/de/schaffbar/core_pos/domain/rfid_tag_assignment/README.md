# RfidTagAssignment

**Aggregate Root:** `RfidTagAssignment`

**Inner entity:** `RfidTagAssignmentHistory` – one entry per finished assignment, written by the unassignment; read
through `GET /api/v1/rfid-tag-assignments/history` (a sub-path of the aggregate)

**Identity:** `RfidTagAssignmentId` (UUID)

## Attributes

### Aggregate root `RfidTagAssignment`

| Field            | Type                      | Constraints                                         |
|------------------|---------------------------|-----------------------------------------------------|
| `id`             | `UUID`                    | PK                                                  |
| `customerId`     | `UUID`                    | `@NotNull`, `@Column(unique = true)`                |
| `assignmentType` | `RfidTagAssignmentType`   | `@NotNull` (`FIXED` or `TEMPORARY`)                 |
| `status`         | `RfidTagAssignmentStatus` | `@NotNull` (`WAITING_FOR_ASSIGNMENT` or `ASSIGNED`) |
| `rfidTagId`      | `String`                  | `@Column(unique = true)`, set upon assignment       |
| `assignmentDate` | `Instant`                 | set upon assignment                                 |
| `updatedAt`      | `Instant`                 | `@Version`                                          |

**Lifecycle:** `WAITING_FOR_ASSIGNMENT` → `ASSIGNED` → _(deleted on unassign, history entry written)_;
`WAITING_FOR_ASSIGNMENT` → _(deleted on cancel, no history)_

### Inner entity `RfidTagAssignmentHistory`

| Field              | Type                    | Constraints                  |
|--------------------|-------------------------|------------------------------|
| `id`               | `UUID`                  | PK, not visible outside      |
| `customerId`       | `UUID`                  | `@NotNull`                   |
| `rfidTagId`        | `String`                | `@NotNull`                   |
| `assignmentType`   | `RfidTagAssignmentType` | `@NotNull`                   |
| `assignmentDate`   | `Instant`               | `@NotNull`, `@Past`          |
| `unassignmentDate` | `Instant`               | `@NotNull`, `@PastOrPresent` |
| `updatedAt`        | `Instant`               | `@Version`                   |

## Commands

No command records – each change takes one or two values:

| Operation          | Service command            | Input                      | Use case                                          |
|--------------------|----------------------------|----------------------------|---------------------------------------------------|
| Request assignment | `requestRfidTagAssignment` | customerId, assignmentType | `RfidTagAssignmentRequest`                        |
| Assign RFID tag    | `assignRfidTag`            | rfidTagId                  | `RfidTagAssignmentAssignRfidTag`                  |
| Unassign           | `unassignRfidTag`          | customerId                 | `RfidTagAssignmentUnassignRfidTag`                |
| Cancel request     | `cancelRfidTagAssignment`  | customerId                 | `RfidTagAssignmentCancel`, `CustomerDelete` (C-4) |

## Business Rules / Invariants

| #     | Rule                                                                                                                             | Enforced in                                                                                                                                                                                                                             |
|-------|----------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| TA-1  | A customer can have at most one RFID tag assignment (waiting or assigned)                                                        | `RfidTagAssignmentService.requestRfidTagAssignment()` (`CustomerHasRfidTagAssignmentException`, `409`), unique `customerId`                                                                                                             |
| TA-2  | An RFID tag can be assigned to at most one customer                                                                              | `RfidTagAssignmentService.assignRfidTag()` (`RfidTagAlreadyAssignedException`, `409`), unique `rfidTagId`                                                                                                                               |
| TA-3  | Only one request (`WAITING_FOR_ASSIGNMENT`) may wait at a time – in the whole workshop                                           | `RfidTagAssignmentService.requestRfidTagAssignment()` (`RfidTagAssignmentAlreadyWaitingException`, `409`)                                                                                                                               |
| TA-4  | A tag is assigned only to the waiting request; without one the tag is rejected                                                   | `RfidTagAssignmentService.assignRfidTag()` finds the waiting request (`NoRfidTagAssignmentWaitingException`, `409`); the entity guards against a bug                                                                                    |
| TA-5  | Only an `ASSIGNED` assignment can be unassigned                                                                                  | `RfidTagAssignmentService.unassignRfidTag()` finds the assigned one (`NoRfidTagAssignedException`, `409`); the entity guards against a bug                                                                                              |
| TA-6  | Unassigning deletes the assignment and writes its history entry – one change, one event                                          | `RfidTagAssignmentService.unassignRfidTag()`; `RFID_TAG_UNASSIGNED` carries the history entry                                                                                                                                           |
| TA-7  | Customer must exist before requesting assignment                                                                                 | `RfidTagAssignmentRequest` use case (`404`)                                                                                                                                                                                             |
| TA-8  | RFID tag must exist before it can be assigned                                                                                    | `RfidTagAssignmentAssignRfidTag` use case (`404`)                                                                                                                                                                                       |
| TA-10 | A waiting request (`WAITING_FOR_ASSIGNMENT`) can be cancelled – it is deleted without history                                    | `RfidTagAssignmentService.cancelRfidTagAssignment()` finds the waiting request (`NoRfidTagAssignmentWaitingException`, `409`), used by `RfidTagAssignmentCancel` (`PUT /rfid-tag-assignments/{customerId}/cancel`) and `CustomerDelete` |
| TA-11 | History entries are never changed                                                                                                | `RfidTagAssignmentHistory` has no command methods                                                                                                                                                                                       |
| TA-12 | At the counter (`RFID_TAG_ASSIGNER`) an assigned tag shows its owner instead of being assigned – a waiting request keeps waiting | `DeviceController` – `/device/counter` (`assignRfidTagOrShowOwner`)                                                                                                                                                                     |

## Open Rules

Rules the portal already follows or the UI redesign plans, but the backend does not enforce yet. When one is
implemented, move it to "Business Rules / Invariants".

### Planned (not implemented yet)

| #    | Rule                                                                                   | Status                                              |
|------|----------------------------------------------------------------------------------------|-----------------------------------------------------|
| TA-9 | Unassigning can deactivate the tag at the same time (lost tag) instead of releasing it | New option for unassigning; needs RT-4 in `RfidTag` |

## Domain Events

| Event                           | Payload                                                                                         |
|---------------------------------|-------------------------------------------------------------------------------------------------|
| `RFID_TAG_ASSIGNMENT_REQUESTED` | id, customerId, assignmentType                                                                  |
| `RFID_TAG_ASSIGNMENT_CANCELLED` | id, customerId                                                                                  |
| `RFID_TAG_ASSIGNED`             | id, customerId, rfidTagId, assignmentDate                                                       |
| `RFID_TAG_UNASSIGNED`           | id, customerId, rfidTagId, assignmentType, assignmentDate, unassignmentDate (the history entry) |

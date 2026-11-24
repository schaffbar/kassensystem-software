# ToolCertification

**Aggregate Root:** `ToolCertification`

**Identity:** `ToolCertificationId` (UUID)

## Attributes

| Field         | Type                      | Constraints                                  |
|---------------|---------------------------|----------------------------------------------|
| `id`          | `UUID`                    | PK                                           |
| `customerId`  | `UUID`                    | `@NotNull`                                   |
| `toolId`      | `UUID`                    | `@NotNull`                                   |
| `certifiedBy` | `UUID`                    | optional; can name a deleted customer (TC-9) |
| `status`      | `ToolCertificationStatus` | `@NotNull`                                   |
| `certifiedAt` | `Instant`                 | `@NotNull`                                   |
| `updatedAt`   | `Instant`                 | `@Version`                                   |

**Status Enum:** `ToolCertificationStatus` — `ACTIVE`, `PAUSED`, `REVOKED`

**Lifecycle:** `ACTIVE` ↔ `PAUSED`, `ACTIVE`/`PAUSED` → `REVOKED` (terminal state)

## Commands

| Command                          | Fields                          | Use case                      |
|----------------------------------|---------------------------------|-------------------------------|
| `CreateToolCertificationCommand` | customerId, toolId, certifiedBy | `ToolCertificationCreate`     |
| _(pause, no command)_            | customerId, toolId              | `ToolCertificationPause`      |
| _(reactivate, no command)_       | customerId, toolId              | `ToolCertificationReactivate` |
| _(revoke, no command)_           | customerId, toolId              | `ToolCertificationRevoke`     |
| _(delete, no command)_           | customerId, toolId              | `ToolCertificationDelete`     |

## Business Rules / Invariants

| #    | Rule                                                                                                                                                     | Enforced in                                                                                                                                       |
|------|----------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------|
| TC-1 | A customer can have at most one certification per tool (unique constraint on `customer_id, tool_id`)                                                     | DB unique constraint + `ToolCertificationService.createToolCertification()` — `CertificationAlreadyExistsException` (409)                         |
| TC-2 | REVOKED is a terminal state — the customer can never be re-certified for that tool                                                                       | `ToolCertificationService.createToolCertification()` — `CertificationPermanentlyRevokedException` (409)                                           |
| TC-3 | Only valid state transitions: ACTIVE→PAUSED, PAUSED→ACTIVE, ACTIVE/PAUSED→REVOKED                                                                        | `ToolCertification.pause()`, `reactivate()`, `revoke()` — `InvalidCertificationStateTransitionException` (409)                                    |
| TC-4 | A batch certifies all customers or nobody — the first rejection answers for the whole batch                                                              | `ToolCertificationCreate` — one transaction; `204`, or the first rejection: unknown tool or customer (`404`), then TC-8, TC-5, TC-1, TC-2 (`409`) |
| TC-5 | Instructors of a tool are not certified for it – they need no certification                                                                              | `ToolCertificationCreate` — `CustomerIsInstructorException` (409)                                                                                 |
| TC-8 | Only an instructor of the tool certifies customers for it                                                                                                | `ToolCertificationCreate` — `CustomerNotInstructorException` (409)                                                                                |
| TC-9 | `certifiedBy` can name a customer who was deleted – the certification stays valid; whoever shows its instructor finds no customer (the portal shows „–“) | `CustomerDelete` use case leaves the certifications the customer gave unchanged (C-13)                                                            |

A certification that does not exist answers `404` (`ResourceNotFoundException.toolCertification(customerId, toolId)`)
when it is paused, reactivated, revoked or deleted.

## Open Rules

Rules the portal already follows or the UI redesign plans, but the backend does not enforce yet. When one is
implemented, move it to "Business Rules / Invariants".

### Enforced only in the portal

None.

### Planned (not implemented yet)

| #    | Rule                                                                     | Status                                                                            |
|------|--------------------------------------------------------------------------|-----------------------------------------------------------------------------------|
| TC-6 | The date of the certification can be set when certifying (default today) | `certifiedAt` is always the current time; `ToolCertificationCreate` takes no date |
| TC-7 | A reason when pausing (optional) and revoking (required)                 | No field yet                                                                      |

## Domain Events

| Event                            | Payload                                                  |
|----------------------------------|----------------------------------------------------------|
| `TOOL_CERTIFICATION_CREATED`     | id, customerId, toolId, status, certifiedAt, certifiedBy |
| `TOOL_CERTIFICATION_PAUSED`      | id, customerId, toolId, status                           |
| `TOOL_CERTIFICATION_REACTIVATED` | id, customerId, toolId, status                           |
| `TOOL_CERTIFICATION_REVOKED`     | id, customerId, toolId, status                           |
| `TOOL_CERTIFICATION_DELETED`     | id, customerId, toolId                                   |

# ToolCertification

**Aggregate Root:** `ToolCertification`

**Identity:** `ToolCertificationId` (UUID)

## Attributes

| Field         | Type                      | Constraints |
|---------------|---------------------------|-------------|
| `id`          | `UUID`                    | PK          |
| `customerId`  | `UUID`                    | `@NotNull`  |
| `toolId`      | `UUID`                    | `@NotNull`  |
| `certifiedBy` | `UUID`                    | optional    |
| `status`      | `ToolCertificationStatus` | `@NotNull`  |
| `certifiedAt` | `Instant`                 | `@NotNull`  |
| `updatedAt`   | `Instant`                 | `@Version`  |

**Status Enum:** `ToolCertificationStatus` — `ACTIVE`, `PAUSED`, `REVOKED`

**Lifecycle:** `ACTIVE` ↔ `PAUSED`, `ACTIVE`/`PAUSED` → `REVOKED` (terminal state)

**Note:** `of(CreateToolCertificationCommand)` returns `ToolCertification` directly (no `WithEvents` record). The
creation event is produced in the Service after persisting. Command methods (`pause()`, `reactivate()`, `revoke()`)
return `List<SchaffbarEvent>`.

## Commands

| Command                          | Fields                          |
|----------------------------------|---------------------------------|
| `CreateToolCertificationCommand` | customerId, toolId, certifiedBy |

## Business Rules / Invariants

| #    | Rule                                                                                                 | Enforced in                                                                                                               |
|------|------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------|
| TC-1 | A customer can have at most one certification per tool (unique constraint on `customer_id, tool_id`) | DB unique constraint + `ToolCertificationService.createToolCertification()` — `CertificationAlreadyExistsException` (409) |
| TC-2 | REVOKED is a terminal state — the customer can never be re-certified for that tool                   | `ToolCertificationService.createToolCertification()` — `CertificationPermanentlyRevokedException` (409)                   |
| TC-3 | Only valid state transitions: ACTIVE→PAUSED, PAUSED→ACTIVE, ACTIVE/PAUSED→REVOKED                    | `ToolCertification.pause()`, `reactivate()`, `revoke()` — `InvalidCertificationStateTransitionException` (409)            |
| TC-4 | A batch certifies all customers or nobody — the first rejection answers for the whole batch          | `ToolCertificationCreate` — one transaction; `204`, or the `404` / `409` of the first rejection                           |
| TC-5 | Instructors of a tool are not certified for it – they need no certification                          | `ToolCertificationCreate` — `CustomerIsInstructorException` (409)                                                         |
| TC-8 | Only an instructor of the tool certifies customers for it                                            | `ToolCertificationCreate` — `CustomerNotInstructorException` (409)                                                        |

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

| Event                            | Payload                            |
|----------------------------------|------------------------------------|
| `TOOL_CERTIFICATION_CREATED`     | Full certification snapshot        |
| `TOOL_CERTIFICATION_PAUSED`      | Certification id + status          |
| `TOOL_CERTIFICATION_REACTIVATED` | Certification id + status          |
| `TOOL_CERTIFICATION_REVOKED`     | Certification id + status          |
| `TOOL_CERTIFICATION_DELETED`     | Certification id + customer + tool |

# ToolUsage

**Aggregate Root:** `ToolUsage`

**Identity:** `ToolUsageId` (UUID)

## Attributes

| Field               | Type      | Constraints                |
|---------------------|-----------|----------------------------|
| `id`                | `UUID`    | PK                         |
| `customerId`        | `UUID`    | `@NotNull`                 |
| `toolId`            | `UUID`    | `@NotNull`                 |
| `workshopSessionId` | `UUID`    | `@NotNull`                 |
| `startTime`         | `Instant` | `@NotNull`                 |
| `endTime`           | `Instant` | optional, set when stopped |
| `updatedAt`         | `Instant` | `@Version`                 |

## Commands

| Command                 | Fields                                |
|-------------------------|---------------------------------------|
| `StartToolUsageCommand` | customerId, toolId, workshopSessionId |

## Business Rules / Invariants

| #     | Rule                                                                                                                       | Enforced in                                                                                                              |
|-------|----------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------|
| TU-1  | A customer can use at most 2 tools simultaneously                                                                          | `ToolUsageService.startToolUsage()` — `MaxToolUsageExceededException` (409)                                              |
| TU-2  | A customer can only use tools when actively in the workshop (has active WorkshopUsage)                                     | `ToolUsageStart` use case — `WorkshopUsageService.getActiveWorkshopUsage()`, else `CustomerNotInWorkshopException` (409) |
| TU-3  | A customer cannot start the same tool twice simultaneously                                                                 | `ToolUsageService.startToolUsage()` — `CustomerAlreadyUsingToolException` (409)                                          |
| TU-4  | When a customer leaves the workshop, all active tool usages are automatically stopped                                      | `WorkshopUsageStop` use case — calls `ToolUsageService.stopToolUsages()`                                                 |
| TU-5  | Tool on/off detection happens via RFID reader (type SWITCH_BOX) assigned to the tool                                       | `DeviceController` — resolves tool via `ToolService.getTool(RfidReaderId)` and toggles start/stop                        |
| TU-6  | Customer identification happens via RFID tag assignment                                                                    | `DeviceController` — resolves customer via `RfidTagAssignmentService`                                                    |
| TU-7  | Duration is computed as `endTime - startTime` (truncated to seconds)                                                       | `ToolUsage.getDuration()` — returns `null` if either time is missing                                                     |
| TU-8  | Customer must have an open workshop session to start a tool usage                                                          | `ToolUsageStart` use case — takes the session of the running workshop usage, which is open (WS-5)                        |
| TU-9  | A tool can only be used by one customer at a time                                                                          | `ToolUsageService.startToolUsage()` — `ToolAlreadyInUseByAnotherCustomerException` (409)                                 |
| TU-10 | A customer needs an active ToolCertification for a tool with certification requirement `RED`; `YELLOW` only recommends one | `ToolUsageStart` use case — `CustomerNotCertifiedForToolException` (409) for `RED` only                                  |
| TU-11 | Billing: minutes rounded up, one unit per started 6 minutes; a running usage counts until now                              | `BillableDuration`, `ToolUsage.getBillableDurationUntil()`                                                               |

## Domain Events

| Event                | Payload             |
|----------------------|---------------------|
| `TOOL_USAGE_STARTED` | Tool usage snapshot |
| `TOOL_USAGE_STOPPED` | Tool usage snapshot |

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

| Command                       | Fields                                | Use case                                                   |
|-------------------------------|---------------------------------------|------------------------------------------------------------|
| `StartToolUsageCommand`       | customerId, toolId, workshopSessionId | `ToolUsageStart` (session from the running workshop usage) |
| _(Stop, without a command)_   | customerId, toolId                    | `ToolUsageStop`                                            |
| _(Toggle, without a command)_ | customerId, toolId                    | `ToolUsageToggle` – the switch box                         |

## Business Rules / Invariants

| #     | Rule                                                                                                                       | Enforced in                                                                                                                                                                |
|-------|----------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| TU-1  | A customer can use at most 2 tools simultaneously                                                                          | `ToolUsageService.startToolUsage()` — `MaxToolUsageExceededException` (409)                                                                                                |
| TU-2  | A customer can only use tools while in the workshop (has a running workshop usage)                                         | `ToolUsageStart` use case — `WorkshopUsageService.getRunningWorkshopUsage()`, else `CustomerNotInWorkshopException` (409)                                                  |
| TU-3  | A customer cannot start the same tool twice simultaneously                                                                 | `ToolUsageService.startToolUsage()` — `CustomerAlreadyUsingToolException` (409); in the database covered by the index of TU-9                                              |
| TU-4  | When a customer leaves the workshop, all their running tool usages are stopped                                             | `WorkshopUsageStop` use case — calls `ToolUsageService.stopToolUsages()`                                                                                                   |
| TU-5  | Tool on/off detection happens via RFID reader (type SWITCH_BOX) assigned to the tool                                       | `DeviceController` – `/device/card` finds the tool via `DeviceReadFacade.getTool(rfidReaderId)` and calls `ToolUsageToggle`                                                |
| TU-6  | Customer identification happens via RFID tag assignment                                                                    | `DeviceController` – `DeviceReadFacade.getCustomer(rfidTagId)`, an unknown or unassigned tag is not recognized                                                             |
| TU-8  | Customer must have an open workshop session to start a tool usage                                                          | `ToolUsageStart` use case — takes the session of the running workshop usage, which is open (WS-5)                                                                          |
| TU-9  | A tool can only be used by one customer at a time                                                                          | `ToolUsageService.startToolUsage()` — `ToolAlreadyInUseByAnotherCustomerException` (409); two requests at the same time: partial unique index `uq_tool_usage_running` (V9) |
| TU-10 | A customer needs an active ToolCertification for a tool with certification requirement `RED`; `YELLOW` only recommends one | `ToolUsageStart` use case — `CustomerNotCertifiedForToolException` (409) for `RED` only                                                                                    |
| TU-11 | The switch box has one action: the tag switches the tool off when the person uses it, else on                              | `ToolUsageToggle` use case – `ToolUsageStop` or `ToolUsageStart` with their rules                                                                                          |
| TU-12 | Only the person using a tool can stop its usage                                                                            | `ToolUsageService.stopToolUsage()` — `ToolNotInUseByCustomerException` (409); the switch box never gets there (TU-11)                                                      |

TU-3, TU-8 and TU-12 cannot be reached through the API or a device – the switch box only toggles (TU-11), and a
running workshop usage always belongs to an open session (WS-5) – so the service and use case tests prove them.

## Domain Events

| Event                | Payload             |
|----------------------|---------------------|
| `TOOL_USAGE_STARTED` | Tool usage snapshot |
| `TOOL_USAGE_STOPPED` | Tool usage snapshot |

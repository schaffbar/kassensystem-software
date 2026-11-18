# WorkshopUsage

**Aggregate Root:** `WorkshopUsage`

**Identity:** `WorkshopUsageId` (UUID)

_Note: Code suggests considering renaming to `WorkshopSlot` or `UsageSlot`._

## Attributes

| Field               | Type      | Constraints           |
|---------------------|-----------|-----------------------|
| `id`                | `UUID`    | PK                    |
| `customerId`        | `UUID`    | `@NotNull`            |
| `workshopSessionId` | `UUID`    | `@NotNull`            |
| `entryTime`         | `Instant` | `@NotNull`            |
| `exitTime`          | `Instant` | optional, set on exit |
| `updatedAt`         | `Instant` | `@Version`            |

## Commands

| Operation      | Input                         |
|----------------|-------------------------------|
| Enter workshop | customerId, workshopSessionId |
| Leave workshop | customerId                    |

## Business Rules / Invariants

| #    | Rule                                                                                          | Enforced in                                                                                                  |
|------|-----------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------|
| WU-1 | A customer can have at most one active (no `exitTime`) workshop usage at a time               | `WorkshopUsageService.startWorkshopUsage()` — `CustomerAlreadyInWorkshopException` (409)                     |
| WU-2 | Customer must have an active usage to leave                                                   | `WorkshopUsageService.stopWorkshopUsage()` — `NoActiveWorkshopUsageFoundException` (409)                     |
| WU-3 | Duration is computed as `exitTime - entryTime` (truncated to seconds)                         | `WorkshopUsage.getDuration()` — returns `null` if either time is missing                                     |
| WU-4 | The first check-in starts the session, every later one joins the open session                 | `WorkshopUsageStart` use case — `WorkshopSessionService.startWorkshopSession()` only without an open session |
| WU-5 | Active tool usages are stopped when leaving the workshop                                      | `WorkshopUsageStop` use case — calls `ToolUsageService.stopToolUsages()`                                     |
| WU-7 | Billing: minutes rounded up, one unit per started 6 minutes; a running usage counts until now | `BillableDuration`, `WorkshopUsage.getBillableDurationUntil()`                                               |

## Domain Events

| Event                    | Payload        |
|--------------------------|----------------|
| `WORKSHOP_USAGE_ENTERED` | Usage snapshot |
| `WORKSHOP_USAGE_LEFT`    | Usage snapshot |

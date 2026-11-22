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

| Command                          | Fields                        | Use case             |
|----------------------------------|-------------------------------|----------------------|
| _(Check in, without a command)_  | customerId, workshopSessionId | `WorkshopUsageStart` |
| _(Check out, without a command)_ | customerId                    | `WorkshopUsageStop`  |

## Business Rules / Invariants

| #    | Rule                                                                                            | Enforced in                                                                                                                                                                                                       |
|------|-------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| WU-1 | A customer can have at most one active (no `exitTime`) workshop usage at a time                 | `WorkshopUsageService.startWorkshopUsage()` — `CustomerAlreadyInWorkshopException` (409); two requests at the same time: partial unique index `uq_workshop_usage_running` (V9)                                    |
| WU-2 | Customer must have an active usage to leave                                                     | `WorkshopUsageService.stopWorkshopUsage()` — `NoActiveWorkshopUsageFoundException` (409)                                                                                                                          |
| WU-3 | Duration is computed as `exitTime - entryTime` (truncated to seconds)                           | `WorkshopUsage.getDuration()` — returns `null` if either time is missing                                                                                                                                          |
| WU-4 | The first check-in starts the session, every later one joins the open session                   | `WorkshopUsageStart` use case — `WorkshopSessionService.startWorkshopSession()` only without an open session                                                                                                      |
| WU-5 | Active tool usages are stopped when leaving the workshop                                        | `WorkshopUsageStop` use case — calls `ToolUsageService.stopToolUsages()`                                                                                                                                          |
| WU-7 | Billing: minutes rounded up, one unit per started 6 minutes; a running usage counts until now   | `BillableDuration`, `WorkshopUsage.getBillableDurationUntil()`                                                                                                                                                    |
| WU-8 | Check-in and check-out happen with the RFID tag at the gate keepers (`GATE_KEEPER_IN` / `_OUT`) | `DeviceController` – `/device/card` finds the person via `DeviceReadFacade.getCustomer(rfidTagId)` and calls `WorkshopUsageStart` / `WorkshopUsageStop`; a refusal is `STATE: ERROR` with the message of the code | 

## Domain Events

| Event                    | Payload        |
|--------------------------|----------------|
| `WORKSHOP_USAGE_ENTERED` | Usage snapshot |
| `WORKSHOP_USAGE_LEFT`    | Usage snapshot |

# WorkshopSession

**Aggregate Root:** `WorkshopSession`

**Identity:** `WorkshopSessionId` (UUID)

## Attributes

| Field             | Type                    | Constraints                                       |
|-------------------|-------------------------|---------------------------------------------------|
| `id`              | `UUID`                  | PK                                                |
| `customerId`      | `UUID`                  | `@NotNull`                                        |
| `startTime`       | `Instant`               | `@NotNull`                                        |
| `closeTime`       | `Instant`               | optional, set on close                            |
| `status`          | `WorkshopSessionStatus` | `@NotNull` (`OPEN` or `PAID`)                     |
| `workshopMinutes` | `Long`                  | optional, set on close (billed workshop time)     |
| `workshopUnits`   | `Long`                  | optional, set on close                            |
| `toolMinutes`     | `Long`                  | optional, set on close (billed tool usage, total) |
| `toolUnits`       | `Long`                  | optional, set on close                            |
| `updatedAt`       | `Instant`               | `@Version`                                        |

**Lifecycle:** `OPEN` → `PAID`

## Commands

| Operation     | Command                       | Fields                                        | Use case                                     |
|---------------|-------------------------------|-----------------------------------------------|----------------------------------------------|
| Start session | _(no command)_                | customerId                                    | – (`WorkshopUsageStart`, the first check-in) |
| Close session | `CloseWorkshopSessionCommand` | id, workshopTotal, toolTotal (`BillingTotal`) | `WorkshopSessionClose`                       |

## Business Rules / Invariants

| #    | Rule                                                                                           | Enforced in                                                                                                                                                                                                                              |
|------|------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| WS-1 | A customer can have at most one `OPEN` session at a time                                       | `WorkshopUsageStart` starts a session only without an open one; `WorkshopSessionService.startWorkshopSession()` guards it (`IllegalStateException`); two requests at the same time: partial unique index `uq_workshop_session_open` (V9) |
| WS-2 | Closing sets status to `PAID` and records `closeTime`; a closed session cannot be closed again | `WorkshopSession.close()` – a guard against a bug (`IllegalStateException`): the use case finds only an open session (WS-4)                                                                                                              |
| WS-3 | Customer must exist before closing a session                                                   | `WorkshopSessionClose` use case (404)                                                                                                                                                                                                    |
| WS-4 | Only an open session can be closed                                                             | `WorkshopSessionClose` use case – finds the open session of the customer (`NoOpenWorkshopSessionException`, `409`) and closes it by its id                                                                                               |
| WS-5 | Cannot close while a workshop or tool usage is running                                         | `WorkshopSessionClose` use case — `RunningWorkshopUsageExistsForSessionException` (checked first: checking out stops the tools too, so through the API only this one occurs) / `RunningToolUsageExistsForSessionException` (409)         |
| WS-6 | Closing stores the final billing totals (workshop, tools)                                      | `WorkshopSessionClose` use case reads them (`getBillingTotal()` of both usage services, without `now`), `WorkshopSession.close()` stores them                                                                                            |

WS-6 cannot be read through the API – the response shows the totals until now, not the stored ones – so
`WorkshopSessionTest` (stored by `close()`) and `WorkshopSessionCloseTest` (read without `now`) prove it.

## Domain Events

| Event                      | Payload                                                                                  |
|----------------------------|------------------------------------------------------------------------------------------|
| `WORKSHOP_SESSION_STARTED` | Session snapshot                                                                         |
| `WORKSHOP_SESSION_CLOSED`  | Session snapshot incl. billing totals (`workshopTotal`, `toolTotal`: `minutes`, `units`) |

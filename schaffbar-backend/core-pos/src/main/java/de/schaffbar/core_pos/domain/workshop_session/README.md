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

| Operation     | Input                                                   |
|---------------|---------------------------------------------------------|
| Start session | customerId                                              |
| Close session | customerId, workshop total, tool total (`BillingTotal`) |

## Business Rules / Invariants

| #    | Rule                                                     | Enforced in                                                                |
|------|----------------------------------------------------------|----------------------------------------------------------------------------|
| WS-1 | A customer can have at most one `OPEN` session at a time | `WorkshopSessionService.startSession()` — checks for existing open session |
| WS-2 | Closing sets status to `PAID` and records `closeTime`    | `WorkshopSession.close()`                                                  |
| WS-3 | Customer must exist before closing a session             | `CloseSession` use case                                                    |
| WS-4 | Cannot close a session that doesn't exist                | `WorkshopSessionService.closeSession()`                                    |
| WS-5 | Cannot close while a workshop or tool usage is running   | `CloseSession` use case                                                    |
| WS-6 | Closing stores the final billing totals (workshop, tools) | `CloseSession` use case computes them, `WorkshopSession.close()` stores them |

## Domain Events

| Event                      | Payload                                                                                  |
|----------------------------|------------------------------------------------------------------------------------------|
| `WORKSHOP_SESSION_STARTED` | Session snapshot                                                                         |
| `WORKSHOP_SESSION_CLOSED`  | Session snapshot incl. billing totals (`workshopTotal`, `toolTotal`: `minutes`, `units`) |

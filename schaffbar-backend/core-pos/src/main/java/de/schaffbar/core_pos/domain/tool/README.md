# Tool

**Aggregate Root:** `Tool`

**Identity:** `ToolId` (UUID)

## Attributes

| Field                      | Type                       | Constraints                                          |
|----------------------------|----------------------------|------------------------------------------------------|
| `id`                       | `UUID`                     | PK                                                   |
| `name`                     | `String`                   | `@NotBlank`, unique                                  |
| `description`              | `String`                   | optional                                             |
| `area`                     | `ToolArea`                 | `@NotNull` enum (`HOLZ`, `METALL`, `KREATIV`, `FABLAB`) |
| `certificationRequirement` | `CertificationRequirement` | `@NotNull` enum (`YELLOW`, `RED`)                    |
| `rfidReaderId`             | `UUID`                     | optional, FK-like reference                          |
| `wlanRelaisType`           | `WlanRelaisType`           | optional enum (`SHELLY_1`, `SHELLY_2`, `SHELLY_PRO`) |
| `ipAddress`                | `String`                   | required when `wlanRelaisType` is set                |
| `httpStartCommand`         | `String`                   | auto-derived from `WlanRelaisType` template          |
| `onCommand`                | `String`                   | auto-derived from `WlanRelaisType` template          |
| `offCommand`               | `String`                   | auto-derived from `WlanRelaisType` template          |
| `createdAt`                | `Instant`                  | `@NotNull`                                           |
| `updatedAt`                | `Instant`                  | `@Version`                                           |

## Commands

| Command                 | Fields                                             |
|-------------------------|----------------------------------------------------|
| `CreateToolCommand`     | name, description, area, certificationRequirement  |
| `UpdateToolCommand`     | name, description, area, certificationRequirement  |
| `SetWlanRelaisCommand`  | wlanRelaisType, ipAddress                          |

## Business Rules / Invariants

| #    | Rule                                                                                                                                    | Enforced in                                                          |
|------|-----------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------|
| T-1  | `name` must not be blank                                                                                                                | Bean Validation                                                      |
| T-2  | Tool name must be unique across all tools                                                                                               | `ToolService.createTool()`, `ToolService.updateTool()`               |
| T-3  | An RFID reader can only be assigned to one tool at a time                                                                               | `ToolService.assignRfidReader()`                                     |
| T-4  | An IP address can only be used by one tool at a time                                                                                    | `ToolService.setWlanRelais()`                                        |
| T-5  | When `wlanRelaisType` is set, `ipAddress` is required                                                                                   | `SetWlanRelaisCommand` — Bean Validation `@NotNull`                  |
| T-6  | WLAN-Relais commands (`httpStartCommand`, `onCommand`, `offCommand`) are derived from the `WlanRelaisType` template — not user-settable | `Tool.setWlanRelais()`                                               |
| T-7  | Clearing WLAN-Relais resets all relais-related fields to null                                                                           | `Tool.clearWlanRelais()`                                             |
| T-8  | If assigning an RFID reader that is already assigned to the same tool → no-op (idempotent)                                              | `ToolService.assignRfidReader()`                                     |
| T-9  | Only RFID readers of type `SWITCH_BOX` can be assigned to tools                                                                         | `ToolAssignRfidReader` use case                                      |
| T-10 | `area` and `certificationRequirement` are mandatory and set at creation                                                                 | Bean Validation `@NotNull`                                           |
| T-11 | `area` and `certificationRequirement` can only be changed together with name and description via `UpdateToolCommand`                    | `Tool.update()`, `UpdateToolCommand`                                 |
| T-12 | When `certificationRequirement` is `RED`, tool usage requires active certification                                                      | `StartToolUsage` use case                                            |
| T-13 | When `certificationRequirement` is `YELLOW`, tool usage is allowed without certification (advisory only)                                | `StartToolUsage` use case                                            |

## Open Rules

Rules the portal already follows or the UI redesign plans, but the backend does not enforce yet. When one is
implemented, move it to "Business Rules / Invariants".

### Enforced only in the portal

| #    | Rule                                                                                                                                      | Status                                                                                                                                        |
|------|-------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------|
| T-14 | The RFID reader (switch box) cannot be cleared while the tool is in use – the session of the person using it would not be closed properly | Portal: "Schaltbox trennen" disabled with a hint (`canClearRfidReader` in `tool.model.ts`). `ToolClearRfidReader` does not check              |
| T-15 | Tool names are unique case-insensitively (extends T-2)                                                                                    | Portal: `validateUniqueToolName` compares in lower case. `ToolRepository.findByName` is case-sensitive, "Kreissäge" and "kreissäge" both pass |
| T-16 | `description` has at most 255 characters                                                                                                  | Portal: `maxLength(255)`. Column is `VARCHAR(255)`, no `@Size` on the commands → database error (500)                                         |

### Planned (not implemented yet)

| #    | Rule                                                                                          | Status                                                                                    |
|------|-----------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------|
| T-17 | A tool cannot be deleted while it is in use                                                   | Neither portal nor backend. `ToolDelete` deletes certifications and tool without checking |
| T-18 | Removing the WLAN relais can switch it off first ("Relais vorher ausschalten", on by default) | New option for clearing the WLAN relais. The portal only asks for confirmation            |
| T-19 | Test the connection to the WLAN relais ("Verbindung testen")                                  | New endpoint                                                                              |

### To decide

- Only persons with an active certification for the tool can become instructors? The portal's "Einweiser hinzufügen"
  dialog filters this way by default, but the filter can be switched off. `ToolAddInstructors` accepts anyone.

## Domain Events

| Event                         | Payload                                             |
|-------------------------------|-----------------------------------------------------|
| `TOOL_CREATED`                | Full tool snapshot (incl. area, certReq)            |
| `TOOL_UPDATED`                | Updated name, description, area, certificationReq   |
| `TOOL_WLAN_RELAIS_SET`        | WLAN-Relais config (type, ipAddress)                |
| `TOOL_WLAN_RELAIS_CLEARED`    | toolId                                              |
| `TOOL_RFID_READER_ASSIGNED`   | toolId, rfidReaderId                                |
| `TOOL_RFID_READER_CLEARED`    | toolId                                              |
| `TOOL_INSTRUCTORS_ADDED`      | toolId, instructorIds                               |
| `TOOL_INSTRUCTORS_REMOVED`    | toolId, instructorIds                               |
| `TOOL_DELETED`                | toolId                                              |

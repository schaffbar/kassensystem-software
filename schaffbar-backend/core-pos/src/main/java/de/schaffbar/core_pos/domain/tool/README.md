# Tool

**Aggregate Root:** `Tool`

**Identity:** `ToolId` (UUID)

## Attributes

| Field                      | Type                       | Constraints                                             |
| -------------------------- | -------------------------- | ------------------------------------------------------- |
| `id`                       | `UUID`                     | PK                                                      |
| `name`                     | `String`                   | `@NotBlank`, unique                                     |
| `description`              | `String`                   | optional                                                |
| `area`                     | `ToolArea`                 | `@NotNull` enum (`HOLZ`, `METALL`, `KREATIV`, `FABLAB`) |
| `certificationRequirement` | `CertificationRequirement` | `@NotNull` enum (`YELLOW`, `RED`)                       |
| `rfidReaderId`             | `UUID`                     | optional, FK-like reference                             |
| `wlanRelaisType`           | `WlanRelaisType`           | optional enum (`SHELLY_1`, `SHELLY_2`, `SHELLY_PRO`)    |
| `ipAddress`                | `String`                   | required when `wlanRelaisType` is set                   |
| `httpStartCommand`         | `String`                   | auto-derived from `WlanRelaisType` template             |
| `onCommand`                | `String`                   | auto-derived from `WlanRelaisType` template             |
| `offCommand`               | `String`                   | auto-derived from `WlanRelaisType` template             |
| `createdAt`                | `Instant`                  | `@NotNull`                                              |
| `updatedAt`                | `Instant`                  | `@Version`                                              |

## Commands

| Command                | Fields                                                |
| ---------------------- | ----------------------------------------------------- |
| `CreateToolCommand`    | name, description, area, certificationRequirement     |
| `UpdateToolCommand`    | id, name, description, area, certificationRequirement |
| `SetWlanRelaisCommand` | id, wlanRelaisType, ipAddress                         |

## Business Rules / Invariants

| #    | Rule                                                                                                                                    | Enforced in                                                                                                                        |
|------|-----------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------|
| T-1  | `name` must not be blank                                                                                                                | Bean Validation                                                                                                                    |
| T-2  | Tool name must be unique across all tools                                                                                               | `ToolService.createTool()`, `ToolService.updateTool()` (`ToolNameAlreadyUsedException`, `409`)                                     |
| T-3  | An RFID reader can only be assigned to one tool at a time                                                                               | `ToolService.assignRfidReader()` (`RfidReaderAlreadyAssignedToToolException`, `409`)                                               |
| T-4  | An IP address can only be used by one tool at a time                                                                                    | `ToolService.setWlanRelais()` (`IpAddressAlreadyUsedException`, `409`)                                                             |
| T-5  | When `wlanRelaisType` is set, `ipAddress` is required                                                                                   | `SetWlanRelaisCommand` — Bean Validation `@NotNull`                                                                                |
| T-6  | WLAN-Relais commands (`httpStartCommand`, `onCommand`, `offCommand`) are derived from the `WlanRelaisType` template — not user-settable | `Tool.setWlanRelais()`                                                                                                             |
| T-7  | Clearing WLAN-Relais resets all relais-related fields to null                                                                           | `Tool.clearWlanRelais()`                                                                                                           |
| T-8  | If assigning an RFID reader that is already assigned to the same tool → no-op (idempotent)                                              | `Tool.assignRfidReader()` (no event)                                                                                               |
| T-9  | Only RFID readers of type `SWITCH_BOX` can be assigned to tools                                                                         | `ToolAssignRfidReader` use case (`InvalidRfidReaderTypeForToolException`, `409`)                                                   |
| T-10 | `area` and `certificationRequirement` are mandatory and set at creation                                                                 | Bean Validation `@NotNull`                                                                                                         |
| T-11 | `area` and `certificationRequirement` can only be changed together with name and description via `UpdateToolCommand`                    | `Tool.update()`, `UpdateToolCommand`                                                                                               |
| T-12 | When `certificationRequirement` is `RED`, tool usage requires active certification                                                      | `ToolUsageStart` use case (TU-10)                                                                                                  |
| T-13 | When `certificationRequirement` is `YELLOW`, tool usage is allowed without certification (advisory only)                                | `ToolUsageStart` use case (TU-10)                                                                                                  |
| T-14 | The RFID reader (switch box) cannot be cleared while the tool is in use – the person at the tool could no longer stop the usage         | `ToolClearRfidReader` use case (`ToolInUseException`, `409`); portal: `canClearRfidReader`                                         |
| T-15 | Tool names are unique case-insensitively (extends T-2) – two existing names differing only in case: the next edit renames one           | `ToolService.createTool()`, `ToolService.updateTool()` (`ToolNameAlreadyUsedException`, `409`)                                     |
| T-16 | `description` has at most 255 characters, the size of its column                                                                        | `@ToolDescription` on the commands (`400`); portal: `maxLength(255)`                                                               |
| T-17 | A tool cannot be deleted while it is in use – the usage of the person at the tool would never end                                       | `ToolDelete` use case (`ToolInUseException`, `409`); portal: `canDeleteTool`                                                       |
| T-20 | A person is an instructor of a tool only once – adding them again is rejected                                                           | `Tool.addInstructors()` (`CustomerAlreadyInstructorException`, `409`)                                                              |
| T-21 | Only an instructor of the tool can be removed as instructor                                                                             | `Tool.removeInstructors()` (`CustomerNotInstructorException`, `409`)                                                               |
| T-22 | Instructors must exist as customers                                                                                                     | `ToolAddInstructors`, `ToolRemoveInstructors` use cases (`404`)                                                                    |
| T-23 | Every tool is switched over its WLAN relais – a switch box whose tool has none does not start                                           | `DeviceController` – `/device/init` answers `STATE: ERROR` "Kein WLAN-Relais"; creating or editing a tool does not require one yet |

## Open Rules

Rules the portal already follows or the UI redesign plans, but the backend does not enforce yet. When one is
implemented, move it to "Business Rules / Invariants".

### Enforced only in the portal

None.

### Planned (not implemented yet)

| #    | Rule                                                                                          | Status                                                                         |
| ---- | --------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------ |
| T-18 | Removing the WLAN relais can switch it off first ("Relais vorher ausschalten", on by default) | New option for clearing the WLAN relais. The portal only asks for confirmation |
| T-19 | Test the connection to the WLAN relais ("Verbindung testen")                                  | New endpoint                                                                   |

## Domain Events

| Event                       | Payload                                           |
| --------------------------- | ------------------------------------------------- |
| `TOOL_CREATED`              | Full tool snapshot (incl. area, certReq)          |
| `TOOL_UPDATED`              | Updated name, description, area, certificationReq |
| `TOOL_WLAN_RELAIS_SET`      | WLAN-Relais config (type, ipAddress)              |
| `TOOL_WLAN_RELAIS_CLEARED`  | toolId                                            |
| `TOOL_RFID_READER_ASSIGNED` | toolId, rfidReaderId                              |
| `TOOL_RFID_READER_CLEARED`  | toolId                                            |
| `TOOL_INSTRUCTORS_ADDED`    | toolId, instructorIds                             |
| `TOOL_INSTRUCTORS_REMOVED`  | toolId, instructorIds                             |
| `TOOL_DELETED`              | toolId                                            |

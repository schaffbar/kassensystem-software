# RfidTag

**Aggregate Root:** `RfidTag`

**Identity:** `RfidTagId` (String — hardware ID)

## Attributes

| Field       | Type      | Constraints                    |
|-------------|-----------|--------------------------------|
| `id`        | `String`  | PK (hardware tag ID)           |
| `active`    | `boolean` | defaults to `true` on creation |
| `createdAt` | `Instant` | `@NotNull`                     |
| `updatedAt` | `Instant` | `@Version`                     |

## Commands

| Command                | Fields             |
|------------------------|--------------------|
| `CreateRfidTagCommand` | rfidTagId (String) |

## Business Rules / Invariants

| #    | Rule                                 | Enforced in                                     |
|------|--------------------------------------|-------------------------------------------------|
| RT-1 | RFID Tag ID must not be blank        | `@NotBlank` on `CreateRfidTagCommand.rfidTagId` |
| RT-2 | Newly created tags are always active | `RfidTag.of()` sets `active = true`             |

## Open Rules

Rules the portal already follows or the UI redesign plans, but the backend does not enforce yet. When one is
implemented, move it to "Business Rules / Invariants".

### Enforced only in the portal

| #    | Rule                                                                                     | Status                                                                                                                                                              |
|------|------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| RT-3 | An assigned RFID tag cannot be deleted – it must be unassigned first (reject with `409`) | Portal: "Tag löschen" only without assignment (`canDeleteRfidTag`). `RfidTagService.deleteRfidTag` has a TODO, `rfid_tag_assignment.rfid_tag_id` has no foreign key |

### Planned (not implemented yet)

| #    | Rule                                                                                                      | Status                                                                                                                    |
|------|-----------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------|
| RT-4 | A tag can be deactivated (lost) and reactivated; a deactivated tag cannot be assigned and unlocks nothing | `active` exists, but there is no endpoint, and `CustomerAssignRfidTag`, `EnterWorkshop`, `StartToolUsage` do not check it |

## Domain Events

| Event              | Payload           |
|--------------------|-------------------|
| `RFID_TAG_CREATED` | Full tag snapshot |
| `RFID_TAG_DELETED` | rfidTagId         |

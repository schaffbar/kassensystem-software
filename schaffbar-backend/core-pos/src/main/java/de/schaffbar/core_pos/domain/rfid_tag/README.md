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

No command records – both changes take only the id (E1):

| Change | Input                                |
|--------|--------------------------------------|
| create | rfidTagId (`RfidTagId` value object) |
| delete | rfidTagId (`RfidTagId` value object) |

## Business Rules / Invariants

| #    | Rule                                                                 | Enforced in                                                                   |
|------|----------------------------------------------------------------------|-------------------------------------------------------------------------------|
| RT-1 | RFID Tag ID must not be blank                                        | `RfidTagId` value object, Bean Validation                                     |
| RT-2 | Newly created tags are always active                                 | `RfidTag.of()` sets `active = true`                                           |
| RT-3 | An assigned RFID tag cannot be deleted – it must be unassigned first | `RfidTagDelete` use case (`RfidTagAssignedToCustomerException`, `409`)        |
| RT-5 | A tag (hardware id) is registered only once                          | `RfidTagService.createRfidTag()` (`RfidTagAlreadyRegisteredException`, `409`) |

## Open Rules

Rules the portal already follows or the UI redesign plans, but the backend does not enforce yet. When one is
implemented, move it to "Business Rules / Invariants".

### Planned (not implemented yet)

| #    | Rule                                                                                                      | Status                                                                                                                    |
|------|-----------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------|
| RT-4 | A tag can be deactivated (lost) and reactivated; a deactivated tag cannot be assigned and unlocks nothing | `active` exists, but there is no endpoint, and `CustomerAssignRfidTag`, `EnterWorkshop`, `StartToolUsage` do not check it |

## Domain Events

| Event              | Payload           |
|--------------------|-------------------|
| `RFID_TAG_CREATED` | Full tag snapshot |
| `RFID_TAG_DELETED` | rfidTagId         |

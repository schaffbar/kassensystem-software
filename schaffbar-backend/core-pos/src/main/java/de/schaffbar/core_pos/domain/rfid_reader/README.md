# RfidReader

**Aggregate Root:** `RfidReader`

**Identity:** `RfidReaderId` (UUID)

## Attributes

| Field        | Type             | Constraints                           |
|--------------|------------------|---------------------------------------|
| `id`         | `UUID`           | PK                                    |
| `macAddress` | `String`         | `@NotBlank`, `@Column(unique = true)` |
| `type`       | `RfidReaderType` | optional enum                         |
| `name`       | `String`         | optional                              |
| `socketName` | `String`         | optional                              |
| `createdAt`  | `Instant`        | `@NotNull`                            |
| `updatedAt`  | `Instant`        | `@Version`                            |

**Enum:** `RfidReaderType`

| Value               | Code | Purpose                                  |
|---------------------|------|------------------------------------------|
| `RFID_TAG_REGISTER` | `A`  | Registers new RFID tags                  |
| `RFID_TAG_ASSIGNER` | `C`  | Assigns RFID tags to customers           |
| `GATE_KEEPER_IN`    | `GI` | Workshop entry gate                      |
| `GATE_KEEPER_OUT`   | `GO` | Workshop exit gate                       |
| `SWITCH_BOX`        | `S`  | Controls tool power (paired with Tool)   |

## Commands

| Command                       | Fields                                                         |
|-------------------------------|----------------------------------------------------------------|
| _(Creation via MacAddress)_   | macAddress (`MacAddress` value object)                         |
| `UpdateRfidReaderCommand`     | id (`RfidReaderId`), type (`RfidReaderType`), name, socketName |
| `ChangeRfidReaderTypeCommand` | id (`RfidReaderId`), type (`RfidReaderType`)                   |

## Business Rules / Invariants

| #   | Rule                                                                                                     | Enforced in                                                                                                |
|-----|----------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------|
| R-1 | `macAddress` must be unique across all RFID readers                                                      | `RfidReaderService.createRfidReader()` (`MacAddressAlreadyUsedException`, `409`), DB unique constraint     |
| R-2 | `macAddress` must not be blank                                                                           | `MacAddress` value object, Bean Validation                                                                 |
| R-3 | The type of an RFID reader of type `SWITCH_BOX` cannot be changed while a tool is assigned to it         | `ChangeRfidReaderType` use case                                                                            |
| R-5 | An RFID reader that is assigned to a tool cannot be deleted                                              | `RfidReaderDelete` use case                                                                                |
| R-7 | `macAddress` has the format `34:CD:B0:D3:A9:30` (six hex pairs, colons); lower case is stored upper case | `MacAddress` value object (`400`)                                                                          |
| R-8 | A switch box starts only with a tool assigned to it; holding a tag to it without a tool is refused       | `DeviceController` – `/device/init` answers `STATE: ERROR` "Kein Werkzeug", `/device/card` "Kein Werkzeug" |

## Open Rules

Rules the portal already follows or the UI redesign plans, but the backend does not enforce yet. When one is
implemented, move it to "Business Rules / Invariants".

### Enforced only in the portal

| #   | Rule                                                                               | Status                                                                                                                   |
|-----|------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------|
| R-4 | A socket name contains only lowercase letters, digits and hyphens (`^[a-z0-9-]+$`) | Portal: checked only when the socket name changes – existing names like "Socket 43" stay valid. Backend accepts anything |

### Planned (not implemented yet)

| #   | Rule                    | Status                                            |
|-----|-------------------------|---------------------------------------------------|
| R-6 | Reader names are unique | Planned duplicate check in "Lesegerät bearbeiten" |

## Domain Events

| Event                      | Payload              |
|----------------------------|----------------------|
| `RFID_READER_CREATED`      | Full reader snapshot |
| `RFID_READER_UPDATED`      | Updated fields       |
| `RFID_READER_TYPE_CHANGED` | rfidReaderId, type   |
| `RFID_READER_DELETED`      | rfidReaderId         |

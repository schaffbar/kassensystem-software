# Customer

**Aggregate Root:** `Customer`

**Identity:** `CustomerId` (UUID)

## Attributes

| Field         | Type              | Constraints               |
| ------------- | ----------------- | ------------------------- |
| `id`          | `UUID`            | PK                        |
| `firstName`   | `String`          | `@NotBlank`               |
| `lastName`    | `String`          | `@NotBlank`               |
| `dateOfBirth` | `LocalDate`       | `@NotNull`                |
| `clubMember`  | `boolean`         | —                         |
| `email`       | `String`          | `@NotBlank`               |
| `phone`       | `String`          | optional                  |
| `address`     | `CustomerAddress` | `@NotNull`, `@Embeddable` |
| `createdAt`   | `Instant`         | `@NotNull`                |
| `updatedAt`   | `Instant`         | `@Version`                |

**Value Object:** `CustomerAddress` (embedded)

- `addressLine1` (`@NotBlank`), `addressLine2` (optional), `postalCode` (`@NotBlank`), `city` (`@NotBlank`), `country` (
  `@NotBlank`)

## Commands

| Command                        | Fields                                                                                                            |
| ------------------------------ | ----------------------------------------------------------------------------------------------------------------- |
| `CreateCustomerCommand`        | firstName, lastName, dateOfBirth, clubMember, email, phone, addressLine1, addressLine2, postalCode, city, country |
| `UpdateCustomerCommand`        | id (`CustomerId`), firstName, lastName, dateOfBirth, clubMember                                                   |
| `UpdateCustomerContactCommand` | id (`CustomerId`), email, phone                                                                                   |
| `UpdateCustomerAddressCommand` | id (`CustomerId`), addressLine1, addressLine2, postalCode, city, country                                          |

## Business Rules / Invariants

| #    | Rule                                                                                                                                                                                                                          | Enforced in                                                                                            |
| ---- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------ |
| C-1  | `firstName`, `lastName`, `email` must not be blank                                                                                                                                                                            | Bean Validation on Entity + Commands                                                                   |
| C-2  | `dateOfBirth` must not be null                                                                                                                                                                                                | Bean Validation                                                                                        |
| C-3  | Address must always be present and valid                                                                                                                                                                                      | `@NotNull` on embedded `CustomerAddress`                                                               |
| C-4  | Deleting a customer frees their RFID tag: an assigned tag is unassigned (history like TA-6, the tag becomes free), a waiting request is cancelled                                                                             | `CustomerDelete` use case                                                                              |
| C-5  | A customer with an open workshop session cannot be deleted – the session still has to be closed and paid                                                                                                                      | `CustomerDelete` use case (`CustomerHasOpenWorkshopSessionException`); the portal checks before asking |
| C-9  | Deleting a customer deletes their tool certifications and removes them as instructor from all tools                                                                                                                           | `CustomerDelete` use case                                                                              |
| C-11 | Paid sessions, workshop and tool usages and the tag history stay when a customer is deleted (accounting, statistics); so do certifications the customer has given to others (`certifiedBy` then points to a deleted customer) | `CustomerDelete` use case deletes nothing of them – there are no foreign keys (V6)                     |

The deletion concept (C-4, C-5, C-9, C-11) was decided on 2026-11-11; C-10 ("remove the tag assignment") became C-4.

## Open Rules

Rules the portal already follows or the UI redesign plans, but the backend does not enforce yet. When one is
implemented, move it to "Business Rules / Invariants".

### Enforced only in the portal

| #   | Rule                                                                     | Status                                                  |
| --- | ------------------------------------------------------------------------ | ------------------------------------------------------- |
| C-6 | `email` is a valid e-mail address                                        | Portal: `email()` validator. Backend: only `@NotBlank`  |
| C-7 | `postalCode` has exactly 5 characters                                    | Portal: `validatePostalCode`. Backend: only `@NotBlank` |
| C-8 | `dateOfBirth` lies within the last 100 years and before the current year | Portal: `validateDateOfBirth`. Backend: only `@NotNull` |

## Domain Events

| Event                      | Payload                |
| -------------------------- | ---------------------- |
| `CUSTOMER_CREATED`         | Full customer snapshot |
| `CUSTOMER_UPDATED`         | Updated base fields    |
| `CUSTOMER_CONTACT_CHANGED` | email, phone           |
| `CUSTOMER_ADDRESS_CHANGED` | Full address           |
| `CUSTOMER_DELETED`         | CustomerId             |

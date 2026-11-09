# Customer

**Aggregate Root:** `Customer`

**Identity:** `CustomerId` (UUID)

## Attributes

| Field         | Type              | Constraints               |
|---------------|-------------------|---------------------------|
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
|--------------------------------|-------------------------------------------------------------------------------------------------------------------|
| `CreateCustomerCommand`        | firstName, lastName, dateOfBirth, clubMember, email, phone, addressLine1, addressLine2, postalCode, city, country |
| `UpdateCustomerCommand`        | id (`CustomerId`), firstName, lastName, dateOfBirth, clubMember                                                   |
| `UpdateCustomerContactCommand` | id (`CustomerId`), email, phone                                                                                   |
| `UpdateCustomerAddressCommand` | id (`CustomerId`), addressLine1, addressLine2, postalCode, city, country                                          |

## Business Rules / Invariants

| #   | Rule                                                    | Enforced in                                            |
|-----|---------------------------------------------------------|--------------------------------------------------------|
| C-1 | `firstName`, `lastName`, `email` must not be blank      | Bean Validation on Entity + Commands                   |
| C-2 | `dateOfBirth` must not be null                          | Bean Validation                                        |
| C-3 | Address must always be present and valid                | `@NotNull` on embedded `CustomerAddress`               |
| C-4 | Customer must not be deleted if assigned to an RFID tag | **TODO** — noted in `CustomerService.deleteCustomer()` |

## Open Rules

Rules the portal already follows or the UI redesign plans, but the backend does not enforce yet. When one is
implemented, move it to "Business Rules / Invariants". Deleting needs a concept first (TODO in
`CustomerService.deleteCustomer()`).

### Enforced only in the portal

| #   | Rule                                                                                                     | Status                                                                                                                                           |
|-----|----------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------|
| C-5 | A customer with an open workshop session cannot be deleted – the session still has to be closed and paid | Portal: checks `GET /workshop-sessions/{customerId}/active` before asking (`canDeleteUser` in `users.store.ts`). `CustomerDelete` does not check |
| C-6 | `email` is a valid e-mail address                                                                        | Portal: `email()` validator. Backend: only `@NotBlank`                                                                                           |
| C-7 | `postalCode` has exactly 5 characters                                                                    | Portal: `validatePostalCode`. Backend: only `@NotBlank`                                                                                          |
| C-8 | `dateOfBirth` lies within the last 100 years and before the current year                                 | Portal: `validateDateOfBirth`. Backend: only `@NotNull`                                                                                          |

### Planned (not implemented yet)

| #    | Rule                                                                              | Status                                                                                                                             |
|------|-----------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------|
| C-9  | Deleting a customer deletes their tool certifications                             | Today the foreign key `fk_tool_certification_customer` rejects the delete → database error (500) for everyone with a certification |
| C-10 | Deleting a customer removes their RFID tag assignment, the tag becomes free again | Today the assignment stays and points to the deleted customer. Contradicts C-4 – decide which one applies                          |
| C-11 | Paid sessions stay for the accounting when a customer is deleted                  | Sessions and usages have no foreign key and stay, but nothing defines what they show without the customer                          |

## Domain Events

| Event                      | Payload                |
|----------------------------|------------------------|
| `CUSTOMER_CREATED`         | Full customer snapshot |
| `CUSTOMER_UPDATED`         | Updated base fields    |
| `CUSTOMER_CONTACT_CHANGED` | email, phone           |
| `CUSTOMER_ADDRESS_CHANGED` | Full address           |
| `CUSTOMER_DELETED`         | CustomerId             |

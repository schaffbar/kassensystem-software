# TODO

## Important

- [Frontend] RFID reader after init shows type.null in the list and detail page
- [Frontend] RFID-Reader detail page trennen (mac + type / name + socket)

- [Backend] close all tool sessions when restarting server/backend
- [Frontend] Fix Geburtsdatum
- [PostgreSql] init.sql Add NOT NULL constraints to customer
- [Frontend] Validate Session before closing it
- [Backend] Scheduler to clean up waiting for assignment
- [Backend] Genauer definieren was erlaubt ist in WaitingForAssignment und was in Assigned status
- [Frontend] Action feedback - Snackbar?
- [Frontend] handle API errors
- [Frontend] check required properties in all entities and forms (update)

## Maintenance

- [Backend] put bruno collection to backend
- [Frontend] angular 22 - migrate to new forms
- [Backend] Migrate to Spring Boot 4
- [Backend] fix/implement/remove TODOs

- [Frontend] users and tools store should be global?
- [Frontend] tools store is used in tool list and tool details. Is it the same instance?
- [Frontend] dashboard store - withEntities vs withState
- [Backend] Enter and leave time as LocalDateTime instead of Instant?
- [Backend] Beim Löschen immer überprüfen ob Entity nicht irgendwo referenziert wird
- [Backend] Facade vs Use Case and move all mutations to use cases
- [Backend] Transactional annotation in all services
- [Backend] jakarta.transaction.Transactional vs org.springframework.transaction.annotation.Transactional

## Feature

- [Frontend] Tool Detail page - refactor tool instructor
- [Backend] Implement new interfaces for device
- [Feature] Assignment und Unassignment von fixen RFID tags
- [Feature] Problems API
- [Backend] Open Telemetry
- [PostgreSql] init.sql Add indexes
- [Backend/Frontend] Unit tests

## Question

- [Question] Jahresabo vom 1.1 oder vom Tag wo es gakauft wurde

## Production

- [PROD] docker compose - change postgres port from "5432:5432" to "127.0.0.1:5432:5432"

## Other

Der gesamte Ausdruck http://**IP-Adresse**/relay/0 ist eine URL (Uniform Resource Locator). Einzelnen Bestandteile haben eigene Namen:

- http:// = Das Schema / Protokoll
- IP-Adresse = Der Host (oder die Domain)
- /relay/0 = Der Path (Pfad)

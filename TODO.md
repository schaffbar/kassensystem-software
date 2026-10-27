# TODO

## Important

- [Frontend] Bei `WaitingForUnassignment` zeigt der User-Header „Kein RFID-Tag“ und keine Aktionen - gewollt?

- [Frontend] RFID reader after init shows type.null in the list and detail page
- [Frontend] RFID-Reader detail page trennen (mac + type / name + socket)

- [Backend] close all tool sessions when restarting server/backend
- [Frontend] Fix Geburtsdatum
- [PostgreSql] init.sql Add NOT NULL constraints to customer
- [Frontend] Validate Session before closing it
- [Backend] Scheduler to clean up waiting for assignment
- [Backend] Genauer definieren was erlaubt ist in WaitingForAssignment und was in Assigned status
- [Frontend] check required properties in all entities and forms (update)

- Fehlerbehandlung / Problems API:
  - [Backend] Problems API: `@ControllerAdvice` mit `ProblemDetail` (RFC 9457). Fachliche Exceptions kommen heute
    als `500` ohne lesbare Meldung an (z. B. `RfidReaderHasToolAssignedException`, `ToolNameAlreadyUsedException`) –
    sollten `409`/`400` mit verständlichem `detail` liefern
  - [Frontend] handle API errors – teilweise erledigt: Fehler-Interceptor zeigt eine Snackbar mit allgemeiner
    Meldung (laden / speichern / Server nicht erreichbar). Mit Problems API stattdessen `detail` aus dem
    `ProblemDetail` anzeigen
  - [Frontend] Action feedback - Snackbar? – Fehler erledigt (Interceptor); Erfolgsmeldungen noch offen
  - [Frontend] Mit Problems API werden reine Vorab-Prüfungen im Frontend optional, z. B. `getToolsByRfidReader`
    vor dem Typwechsel eines RFID-Readers (das Backend prüft das bereits in `RfidReaderChangeType`)

## Maintenance

- [Backend] put bruno collection to backend
- [Frontend] angular 22 - migrate to new forms
- [Backend] Migrate to Spring Boot 4
- [Backend] fix/implement/remove TODOs

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

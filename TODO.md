# TODO

- [Frontend]
- [Backend]

## Important

- [Backend] Is it problem when we delete RFID reader during operation (exmaple Switch Box)

- [Feature] WaitingForAssignment auch im Portal stoppen
- [Backend] Scheduler to clean up WaitingForAssignment

- [PostgreSql] init.sql Add NOT NULL constraints to customer
- [Backend] close all tool sessions when restarting server/backend

## Fields

Tool.createdAt and updatedAt - add
ToolCertification.certifiedBy - mandatory

## UX / UI

## Fehlerbehandlung / Problems API

- [Frontend] Erfolgsmeldungen nach dem Speichern (Snackbar) – Fehler zeigt der Interceptor schon
- [Frontend] Reine Vorab-Prüfungen sind optional, seit das Backend jede Regel mit Code ablehnt, z. B. `getToolsByRfidReader` vor dem Typwechsel eines RFID-Readers – behalten, wo sie den Button vorher erklären

## Maintenance

- [Backend] put bruno collection to backend
- [Backend] Migrate to Spring Boot 4
- [Backend] fix/implement/remove TODOs

- [Backend] Enter and leave time as LocalDateTime instead of Instant?
- [Backend] Beim Löschen immer überprüfen ob Entity nicht irgendwo referenziert wird
- [Backend] Transactional annotation in all services
- [Backend] jakarta.transaction.Transactional vs org.springframework.transaction.annotation.Transactional

## Architecture

- [Backend] Validate HTTP response before sending back
- [Backend] HTTP response - missing values as null or NON_NULL option -> adapt ApiModels in portal

## Feature

- [RFID-Tag] deactivate
- [RFID-Tag] Show time left to assign - after schedular is implemented

- [Feature] Test connection to shelly
- [Backend] Implement new interfaces for device
- [Feature] Assignment und Unassignment von fixen RFID tags
- [Feature] Jahresabo (vom 1.1 oder vom Tag wo es gakauft wurde)

- [Backend] Open Telemetry
- [PostgreSql] init.sql Add indexes

## Production

- [PROD] docker compose - change postgres port from "5432:5432" to "127.0.0.1:5432:5432"

## Other

Der gesamte Ausdruck http://**IP-Adresse**/relay/0 ist eine URL (Uniform Resource Locator). Einzelnen Bestandteile haben eigene Namen:

- http:// = Das Schema / Protokoll
- IP-Adresse = Der Host (oder die Domain)
- /relay/0 = Der Path (Pfad)

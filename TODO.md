# Important

- [Feature] WaitingForAssignment auch im Portal stoppen
- [Backend] Scheduler to clean up WaitingForAssignment
- [Frontend] Erfolgsmeldungen nach dem Speichern (Snackbar) – Fehler zeigt der Interceptor schon

## TODO

- [Api] Tool.createdAt and updatedAt - add
- [Api] ToolCertification.certifiedBy - mandatory
- [Backend] Validate HTTP response before sending back
- [Frontend] Reine Vorab-Prüfungen sind optional, seit das Backend jede Regel mit Code ablehnt, z. B. `getToolsByRfidReader` vor dem Typwechsel eines RFID-Readers – behalten, wo sie den Button vorher erklären
- [Backend] Migrate to Spring Boot 4
- [Backend] close all tool sessions when restarting server/backend
- [PostgreSql] init.sql Add NOT NULL constraints to customer

## Architecture

- [Docs] ADR instead of entscheidung.md
- [Backend] LocalDateTime instead of Instant? (WorkshopSession, WorkshopUsage, ToolUsage)
- [Backend] HTTP response - missing values as null or NON_NULL option -> adapt ApiModels in portal
- [Backend] Open Telemetry
- [PostgreSql] Add indexes

## Feature

- [RFID-Tag] deactivate
- [RFID-Tag] Show time left to assign - after schedular is implemented

- [Feature] Test connection to shelly
- [Backend] Implement new interfaces for device
- [Feature] Assignment und Unassignment von fixen RFID tags
- [Feature] Jahresabo (vom 1.1 oder vom Tag wo es gakauft wurde)

## Production

- [PROD] DSGVO Art. 17 - Outbox table and logs
- [PROD] Database backup
- [PROD] docker compose - change postgres port from "5432:5432" to "127.0.0.1:5432:5432"

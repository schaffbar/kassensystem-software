# Schaffbar - Dokumentation

Ein Überblick über das Backend core-pos für alle, die neu darin sind. Die Regeln selbst – wie ein Aggregat, ein Use
Case oder ein Test geschrieben wird – stehen in `schaffbar-backend/CLAUDE.md` und in den Skills `aggregate` und
`backend-testing`; die Regeln jedes Aggregats in seiner `README.md`. Weicht diese Übersicht davon ab, gilt dort.

## Inhaltsverzeichnis

1. [Domänenübersicht](#1-domänenübersicht)
2. [Aggregat-Strukturmuster](#2-aggregat-strukturmuster)
3. [Aggregat-Katalog](#3-aggregat-katalog)
4. [Use Cases](#4-use-cases)
5. [Read Facades](#5-read-facades)
6. [Wertobjekte (Value Objects)](#6-wertobjekte-value-objects)
7. [Domänenereignisse (Domain Events)](#7-domänenereignisse-domain-events)

---

## 1. Domänenübersicht

Das **Schaffbar Core-POS**-System verwaltet eine **Werkstatt**, in der Personen einchecken, Werkzeuge nutzen und nach
Zeit abgerechnet werden. Die Kerndomäne umfasst:

- **Personenverwaltung** (`Customer`) — Personendaten, Kontakt, Adresse, Altersgruppe
- **RFID-Infrastruktur** — Lesegeräte, Tags und ihre Zuweisung an Personen
- **Werkzeugverwaltung** — Werkzeuge mit Schaltbox (RFID-Lesegerät), WLAN-Relais (Shelly) und Einweisern
- **Einweisungen** — pro Person und Werkzeug, regeln den Zugang zu den Maschinen (Lebenszyklus
  ACTIVE/PAUSED/REVOKED)
- **Werkstattzugang** — Sitzungen (was abgerechnet wird, `OPEN` → `PAID`) und Werkstattbesuche (ein Aufenthalt vom
  Türwächter am Eingang bis zum Türwächter am Ausgang)
- **Werkzeugnutzung** — welche Person welches Werkzeug nutzt, für Abrechnung und Sicherheit
- **Geräte** — die RFID-Lesegeräte an der Theke, an den Türen und an den Werkzeugen sprechen mit `/device/*`
- **Übersicht (Dashboard)** — wer gerade in der Werkstatt ist und welche Werkzeuge laufen

### Bounded Context

Alle Aggregate liegen in einem Bounded Context: **Core-POS**. Ein Aggregat ändert nur sich selbst; sein Service kennt
nur sein eigenes Repository und die Outbox. **Jede Änderung ist ein Use Case** in `use_case/` – eine Klasse pro
Aktion, auch für ein einfaches Anlegen, Ändern oder Löschen, damit jeder Controller gleich aussieht. Ein Use Case
prüft, was ein zweites Aggregat braucht (es existiert, eine Regel über Aggregate hinweg), und ruft dann den oder die
Service-Befehle auf. **Jede Abfrage läuft über die Read Facade** ihres Controllers in `read_facade/`. Controller rufen
nie einen Service auf.

### Ereignisgesteuerte Architektur

Jede Datenbankänderung erzeugt **`SchaffbarEvent`**-Instanzen – ein Anlegen, ein Ändern, ein Löschen, das Schreiben
einer inneren Entity. Sie werden in der **transaktionalen Outbox** (Tabelle `outbox_event`) in der Transaktion der
Änderung gespeichert – oder gar nicht. Heute ist die Outbox das **Änderungsprotokoll** des Systems: noch kein Prozess
liest oder veröffentlicht sie. Die Spalten `processed`, `processed_at` und `owner` sind für ein späteres Relay
vorbereitet und bleiben leer.

---

## 2. Aggregat-Strukturmuster

Jedes Aggregat ist ein Ordner in `domain/` mit denselben Bausteinen (Skill `aggregate`, „Folder“):

```txt
domain/<aggregat>/
│
├── <Aggregate>.java              # Aggregate Root (@Entity)
├── <Aggregate>Commands.java      # Command-Records (optional)
├── <Aggregate>EventFactory.java  # erzeugt die Domänenereignisse
├── <Aggregate>PayloadMapper.java # Entity → Event-Payload (MapStruct)
├── <Aggregate>Repository.java    # Spring-Data-Repository, package-private
├── <Aggregate>Service.java       # Abfragen und Befehle des Aggregats
├── <Aggregate>Views.java         # View-Records + Entity → View (MapStruct)
├── <Inner>.java + <Inner>Repository.java  # innere Entity mit Repository (optional, RfidTagAssignmentHistory)
├── <Enum>.java                   # Enums des Aggregats (RfidReaderType, AgeGroup) – public
├── <Part>.java                   # @Embeddable-Teil der Entity (CustomerAddress) – package-private
├── README.md                     # Attribute, Regeln, Ereignisse
│
└── web/                          # nur, wenn das Portal es aufruft
    ├── <Aggregate>Controller.java   # REST-Controller
    └── <Aggregate>ApiModel.java     # Response- und Request-Records, MapStruct-Mapping
```

### Verantwortlichkeiten der Komponenten

| Komponente                | Verantwortlichkeit                                                                                                                                                                                                                                      |
|---------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Aggregate Root**        | Kapselt Zustand und Invarianten. Entsteht über ein statisches `of(…)`; jede Zustandsänderung ist eine Befehlsmethode, die `List<SchaffbarEvent>` zurückgibt. Außerhalb des Ordners sieht niemand die Entity.                                            |
| **Commands**              | Unveränderliche `record`-Typen mit Bean Validation, gruppiert in einem `*Commands`-Interface.                                                                                                                                                           |
| **Service**               | Lädt die Entity (sonst `404`), prüft die Regeln, die andere Zeilen des Aggregats brauchen (Eindeutigkeit), ruft den Befehl der Entity auf und speichert ihre Ereignisse über `OutboxEventStore`. Jede öffentliche Methode ist `@Query` oder `@Command`. |
| **Repository**            | Spring Data `JpaRepository`, package-private; jede Methode mit einem rohen Wert hat eine Überladung mit dem Value Object.                                                                                                                               |
| **Views**                 | Schreibgeschützte `record`-Projektionen, mit MapStruct abgebildet (`MAPPER`) – das, was die Außenwelt statt der Entity bekommt.                                                                                                                         |
| **EventFactory**          | Erzeugt das `SchaffbarEvent` jedes Ereignistyps: bildet die Entity auf ihre Payload ab (`PayloadMapper`), validiert die Payload, verpackt sie.                                                                                                          |
| **PayloadMapper**         | MapStruct-Interface von der Entity zu den Payload-Records der Ereignisse.                                                                                                                                                                               |
| **Controller / ApiModel** | REST-Schicht: ein Befehl ruft einen Use Case auf, eine Abfrage die Read Facade des Controllers.                                                                                                                                                         |

### Interne Struktur des Aggregate Root

```java
class Aggregate {

    // Felder: JPA @Id (rohe UUID), Attribute, Referenzen als rohe Ids, @Version Instant updatedAt

    // --- static constructor ---
    // static Aggregate of(<CreateCommand>) { ... }   – der Service speichert sie und baut das Created-Event

    // --- query ---
    // getId() und die Getter der Referenzen geben Value Objects zurück
    // abgeleitete Abfragen: is…, has…, get…

    // --- command ---
    // List<SchaffbarEvent> update(...) { ... }
    // List<SchaffbarEvent> <fachlicheAktion>(...) { ... }

    // --- helper ---
}
```

**Wichtige Konventionen:**

- **Keine öffentlichen Setter** — Zustandsänderungen nur über benannte Befehlsmethoden
- **Jede Befehlsmethode gibt `List<SchaffbarEvent>` zurück** — der Service speichert sie in der Outbox
- **`of(…)` gibt die neue Entity zurück** — der Service speichert sie und erzeugt das Created-Event mit der EventFactory
- **`@Version`-Feld** (`Instant updatedAt`) für optimistisches Locking
- **Ids intern als rohe Typen** (`UUID`, `String`) gespeichert, nach außen über die Getter als **Value Objects**

---

## 3. Aggregat-Katalog

Die ausführliche Dokumentation jedes Aggregats (Attribute, Befehle, Geschäftsregeln, Domänenereignisse) liegt in einer
`README.md` in seinem Paket – nah am Code, den sie beschreibt.

| #   | Aggregat                        | Paket                                              | Identität                    | README                                                                                                       |
|-----|---------------------------------|----------------------------------------------------|------------------------------|--------------------------------------------------------------------------------------------------------------|
| 3.1 | Customer (Person)               | `de.schaffbar.core_pos.domain.customer`            | `CustomerId` (UUID)          | [customer/README.md](../src/main/java/de/schaffbar/core_pos/domain/customer/README.md)                       |
| 3.2 | Tool (Werkzeug)                 | `de.schaffbar.core_pos.domain.tool`                | `ToolId` (UUID)              | [tool/README.md](../src/main/java/de/schaffbar/core_pos/domain/tool/README.md)                               |
| 3.3 | RfidReader (RFID-Lesegerät)     | `de.schaffbar.core_pos.domain.rfid_reader`         | `RfidReaderId` (UUID)        | [rfid_reader/README.md](../src/main/java/de/schaffbar/core_pos/domain/rfid_reader/README.md)                 |
| 3.4 | RfidTag (RFID-Tag)              | `de.schaffbar.core_pos.domain.rfid_tag`            | `RfidTagId` (String)         | [rfid_tag/README.md](../src/main/java/de/schaffbar/core_pos/domain/rfid_tag/README.md)                       |
| 3.5 | RfidTagAssignment (Zuweisung)   | `de.schaffbar.core_pos.domain.rfid_tag_assignment` | `RfidTagAssignmentId` (UUID) | [rfid_tag_assignment/README.md](../src/main/java/de/schaffbar/core_pos/domain/rfid_tag_assignment/README.md) |
| 3.6 | WorkshopSession (Sitzung)       | `de.schaffbar.core_pos.domain.workshop_session`    | `WorkshopSessionId` (UUID)   | [workshop_session/README.md](../src/main/java/de/schaffbar/core_pos/domain/workshop_session/README.md)       |
| 3.7 | WorkshopUsage (Werkstattbesuch) | `de.schaffbar.core_pos.domain.workshop_usage`      | `WorkshopUsageId` (UUID)     | [workshop_usage/README.md](../src/main/java/de/schaffbar/core_pos/domain/workshop_usage/README.md)           |
| 3.8 | ToolUsage (Werkzeugnutzung)     | `de.schaffbar.core_pos.domain.tool_usage`          | `ToolUsageId` (UUID)         | [tool_usage/README.md](../src/main/java/de/schaffbar/core_pos/domain/tool_usage/README.md)                   |
| 3.9 | ToolCertification (Einweisung)  | `de.schaffbar.core_pos.domain.tool_certification`  | `ToolCertificationId` (UUID) | [tool_certification/README.md](../src/main/java/de/schaffbar/core_pos/domain/tool_certification/README.md)   |

---

## 4. Use Cases

Eine Klasse pro Aktion in `use_case/`, benannt `<Aggregat><Befehl der Entity>`: das Präfix ist das Aggregat, dessen
Zustand sich ändert, die Aktion der Befehl seiner Entity (Skill `aggregate`, „Service“). Die Regel-Ids sind
die der READMEs der Aggregate.

**Mit einem zweiten Aggregat** – der Use Case liest oder ändert mehr als sein eigenes Aggregat:

| Use Case                           | Liest außerdem / ändert                                            | Was er prüft und tut                                                                                                                                                                                                                                                                             |
|------------------------------------|--------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `CustomerDelete`                   | WorkshopSession; ändert RfidTagAssignment, ToolCertification, Tool | keine offene Sitzung (C-5); gibt den Tag frei – ein zugewiesener wird gelöst, eine wartende Anfrage abgebrochen (C-4) –, löscht die Einweisungen der Person und nimmt sie als Einweiser heraus (C-9); bezahlte Sitzungen und Nutzungen bleiben (C-11), von ihr gegebene Einweisungen auch (C-13) |
| `RfidReaderChangeType`             | Tool                                                               | eine Schaltbox mit Werkzeug behält ihren Typ (R-3)                                                                                                                                                                                                                                               |
| `RfidReaderDelete`                 | Tool                                                               | ein Lesegerät an einem Werkzeug wird nicht gelöscht (R-5)                                                                                                                                                                                                                                        |
| `RfidTagAssignmentRequest`         | Customer                                                           | die Person existiert, dann wartet die Anfrage auf einen Tag                                                                                                                                                                                                                                      |
| `RfidTagAssignmentAssignRfidTag`   | RfidTag                                                            | der Tag ist registriert, dann wird er der wartenden Anfrage zugewiesen (das Lesegerät an der Theke)                                                                                                                                                                                              |
| `RfidTagAssignmentUnassignRfidTag` | Customer                                                           | die Person existiert, dann wird ihr Tag gelöst (Eintrag in der Historie)                                                                                                                                                                                                                         |
| `RfidTagDelete`                    | RfidTagAssignment                                                  | ein zugewiesener Tag wird nicht gelöscht (RT-3)                                                                                                                                                                                                                                                  |
| `ToolAddInstructors`               | Customer                                                           | die Einweiser existieren als Personen (T-22)                                                                                                                                                                                                                                                     |
| `ToolRemoveInstructors`            | Customer                                                           | die Einweiser existieren als Personen (T-22)                                                                                                                                                                                                                                                     |
| `ToolAssignRfidReader`             | RfidReader                                                         | das Lesegerät existiert und ist eine Schaltbox (T-9)                                                                                                                                                                                                                                             |
| `ToolClearRfidReader`              | ToolUsage                                                          | nicht, während das Werkzeug läuft – die Nutzung ließe sich an der Schaltbox nicht mehr beenden (T-14)                                                                                                                                                                                            |
| `ToolDelete`                       | ToolUsage; ändert ToolCertification                                | nicht, während das Werkzeug läuft (T-17); seine Einweisungen werden mit gelöscht                                                                                                                                                                                                                 |
| `ToolCertificationCreate`          | Tool, Customer                                                     | alle Personen existieren – alle oder keine (TC-4); wer einweist, ist Einweiser des Werkzeugs (TC-8); Einweiser des Werkzeugs werden nicht eingewiesen (TC-5)                                                                                                                                     |
| `ToolUsageStart`                   | Customer, Tool, ToolCertification, WorkshopUsage                   | ein `RED`-Werkzeug braucht eine aktive Einweisung, `YELLOW` empfiehlt sie nur (TU-10); die Person ist in der Werkstatt, die Nutzung gehört zur Sitzung des laufenden Besuchs (TU-2)                                                                                                              |
| `ToolUsageToggle`                  | – (ruft `ToolUsageStart` oder `ToolUsageStop`)                     | die eine Aktion der Schaltbox: schaltet das Werkzeug aus, wenn die Person es nutzt, sonst ein (TU-11)                                                                                                                                                                                            |
| `WorkshopSessionClose`             | Customer, WorkshopUsage, ToolUsage                                 | die offene Sitzung der Person (WS-4); kein Besuch und keine Werkzeugnutzung läuft (WS-5); schließt sie mit den Summen beider                                                                                                                                                                     |
| `WorkshopUsageStart`               | Customer; ändert WorkshopSession                                   | der erste Check-in startet die Sitzung, jeder weitere tritt ihr bei (WU-4)                                                                                                                                                                                                                       |
| `WorkshopUsageStop`                | Customer, WorkshopSession; ändert ToolUsage                        | eine offene Sitzung – „nie eingecheckt“ getrennt von „schon ausgecheckt“ (WU-9, WU-2); die laufenden Werkzeuge werden ausgeschaltet (WU-5, TU-4)                                                                                                                                                 |

**Nur das eigene Aggregat** – sie reichen an den Service weiter, der die Regeln des Aggregats prüft: `CustomerCreate`,
`CustomerUpdate`, `CustomerChangeContact`, `CustomerChangeAddress`, `RfidReaderCreate`, `RfidReaderUpdate`,
`RfidTagCreate`, `ToolCreate`, `ToolUpdate`, `ToolSetWlanRelais`, `ToolClearWlanRelais`, `ToolCertificationPause`,
`ToolCertificationReactivate`, `ToolCertificationRevoke`, `ToolCertificationDelete`, `ToolUsageStop`.

---

## 5. Read Facades

Jeder Controller liest über die eine Read Facade in `read_facade/`, die nach ihm benannt ist; eine Read Facade
liest nur. Die, die mehrere Aggregate verbinden, haben eigene Views (`DashboardViews`, `DeviceViews`,
`OpenWorkshopSessionViews`):

| Read Facade                   | Liest                                                                                             | Wofür                                                                                                                                                                                                 |
|-------------------------------|---------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `DashboardReadFacade`         | WorkshopUsage, ToolUsage, Tool, Customer                                                          | `GET /dashboard/active-users`: die laufenden Werkstattbesuche mit ihrer Person, die längste zuerst; `GET /dashboard/active-tools`: die laufenden Werkzeugnutzungen mit Werkzeug und Person            |
| `DeviceReadFacade`            | RfidReader, RfidTag, RfidTagAssignment, Customer, Tool, ToolUsage, WorkshopSession, WorkshopUsage | `/device/*`: das Lesegerät über seine MAC-Adresse, die Person eines Tags (Tag → Zuweisung → Person), das Werkzeug einer Schaltbox, ob ein Werkzeug läuft, der Aufenthalt nach Check-in oder Check-out |
| `WorkshopSessionReadFacade`   | WorkshopSession, WorkshopUsage, ToolUsage, Tool, Customer                                         | `GET /workshop-sessions/{customerId}/open`: die offene Sitzung mit ihren Besuchen und einer Zusammenfassung pro Werkzeug, mit den Summen                                                              |
| `RfidTagAssignmentReadFacade` | RfidTagAssignment, Customer                                                                       | die Zuweisungen (die Person muss existieren) und ihre Historie                                                                                                                                        |
| die übrigen                   | ihr Aggregat                                                                                      | `CustomerReadFacade`, `RfidReaderReadFacade`, `RfidTagReadFacade`, `ToolReadFacade`, `ToolCertificationReadFacade`                                                                                    |

---

## 6. Wertobjekte (Value Objects)

Alle Wertobjekte liegen in `de.schaffbar.core_pos.shared.id`. Sie prüfen sich beim Erzeugen; die Ids haben
`random()`, die Werte, die von einem Gerät kommen (`MacAddress`, `RfidTagId`, `IpAddress`), nicht – das System erfindet
nie einen.

| Wertobjekt            | Umschließt | Verwendet für                                       |
|-----------------------|------------|-----------------------------------------------------|
| `CustomerId`          | `UUID`     | Identität einer Person                              |
| `ToolId`              | `UUID`     | Identität eines Werkzeugs                           |
| `ToolUsageId`         | `UUID`     | Identität einer Werkzeugnutzung                     |
| `ToolCertificationId` | `UUID`     | Identität einer Einweisung                          |
| `RfidReaderId`        | `UUID`     | Identität eines RFID-Lesegeräts                     |
| `RfidTagId`           | `String`   | Identität eines RFID-Tags (Hardware-Id, `04A31F9C`) |
| `RfidTagAssignmentId` | `UUID`     | Identität einer Zuweisung                           |
| `WorkshopSessionId`   | `UUID`     | Identität einer Sitzung                             |
| `WorkshopUsageId`     | `UUID`     | Identität eines Werkstattbesuchs                    |
| `MacAddress`          | `String`   | MAC-Adresse eines Lesegeräts (`34:CD:B0:D3:A9:30`)  |
| `IpAddress`           | `String`   | IPv4-Adresse des WLAN-Relais eines Werkzeugs        |

---

## 7. Domänenereignisse (Domain Events)

### Ereignis-Infrastruktur

- **`SchaffbarEvent`** — unveränderlicher Umschlag mit `id`, `type`, `version`, `aggregateType`, `aggregateId`,
  `timestamp`, `payload` (JSON) und `owner` (noch nicht gesetzt); entsteht nur über seine statischen Factories, die
  die EventFactory jedes Aggregats aufruft
- **`EventPayload`** — das Interface aller Payload-Records, mit `validate()` (Bean Validation)
- **`OutboxEventStore`** — speichert die Ereignisse eines Befehls (`Propagation.MANDATORY`: außerhalb der Transaktion
  des Befehls schlägt es fehl, ein Ereignis wird also nie ohne seine Änderung gespeichert)
- **`OutboxEvent`** — JPA-Entity der Tabelle `schaffbar.outbox_event`, das ganze Ereignis als JSON in `payload`
- **`AggregateType`** — Enum, das das auslösende Aggregat bezeichnet

### Ereignisfluss

```txt
Befehlsmethode des Aggregate Root (oder of(…) + das Created-Event der EventFactory)
  └── gibt List<SchaffbarEvent> zurück
        └── der Service ruft OutboxEventStore.save(events) auf – in der Transaktion des Befehls
              └── Tabelle outbox_event – das Änderungsprotokoll; noch kein Relay liest sie
```

### Muster zur Ereigniserzeugung

```txt
EventFactory.someEvent(aggregate)
  ├── PayloadMapper.MAPPER.toSomePayload(aggregate)           // MapStruct-Abbildung
  ├── payload.validate()                                      // Bean Validation
  └── SchaffbarEvent.<aggregateType>Event(type, id, payload)  // Umschlag
```

### Vollständige Ereignisliste

| Aggregat          | Ereignistyp                                                                                                                                                                                                       |
|-------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Customer          | `CUSTOMER_CREATED`, `CUSTOMER_UPDATED`, `CUSTOMER_CONTACT_CHANGED`, `CUSTOMER_ADDRESS_CHANGED`, `CUSTOMER_DELETED`                                                                                                |
| Tool              | `TOOL_CREATED`, `TOOL_UPDATED`, `TOOL_WLAN_RELAIS_SET`, `TOOL_WLAN_RELAIS_CLEARED`, `TOOL_RFID_READER_ASSIGNED`, `TOOL_RFID_READER_CLEARED`, `TOOL_INSTRUCTORS_ADDED`, `TOOL_INSTRUCTORS_REMOVED`, `TOOL_DELETED` |
| ToolUsage         | `TOOL_USAGE_STARTED`, `TOOL_USAGE_STOPPED`                                                                                                                                                                        |
| ToolCertification | `TOOL_CERTIFICATION_CREATED`, `TOOL_CERTIFICATION_PAUSED`, `TOOL_CERTIFICATION_REACTIVATED`, `TOOL_CERTIFICATION_REVOKED`, `TOOL_CERTIFICATION_DELETED`                                                           |
| RfidReader        | `RFID_READER_CREATED`, `RFID_READER_UPDATED`, `RFID_READER_TYPE_CHANGED`, `RFID_READER_DELETED`                                                                                                                   |
| RfidTag           | `RFID_TAG_CREATED`, `RFID_TAG_DELETED`                                                                                                                                                                            |
| RfidTagAssignment | `RFID_TAG_ASSIGNMENT_REQUESTED`, `RFID_TAG_ASSIGNMENT_CANCELLED`, `RFID_TAG_ASSIGNED`, `RFID_TAG_UNASSIGNED`                                                                                                      |
| WorkshopSession   | `WORKSHOP_SESSION_STARTED`, `WORKSHOP_SESSION_CLOSED`                                                                                                                                                             |
| WorkshopUsage     | `WORKSHOP_USAGE_ENTERED`, `WORKSHOP_USAGE_LEFT`                                                                                                                                                                   |

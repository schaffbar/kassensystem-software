package de.schaffbar.core_pos.domain.workshop_session;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import de.schaffbar.core_pos.shared.event.EventType;
import de.schaffbar.core_pos.shared.event.SchaffbarEvent;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class WorkshopSessionEventFactory {

    static SchaffbarEvent workshopSessionStarted(WorkshopSession session) {
        var payload = WorkshopSessionPayloadMapper.MAPPER.toWorkshopSessionStartedPayload(session);
        payload.validate();

        return SchaffbarEvent.workshopSessionEvent(EventType.WORKSHOP_SESSION_STARTED, session.getId(), payload);
    }

    static SchaffbarEvent workshopSessionClosed(WorkshopSession session) {
        var payload = WorkshopSessionPayloadMapper.MAPPER.toWorkshopSessionClosedPayload(session);
        payload.validate();

        return SchaffbarEvent.workshopSessionEvent(EventType.WORKSHOP_SESSION_CLOSED, session.getId(), payload);
    }

}

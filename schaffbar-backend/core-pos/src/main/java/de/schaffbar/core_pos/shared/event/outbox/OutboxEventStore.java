package de.schaffbar.core_pos.shared.event.outbox;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import de.schaffbar.core_pos.shared.event.SchaffbarEvent;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OutboxEventStore {

    private final @NonNull OutboxEventRepository outboxEventRepository;

    // the events are saved in the transaction that changes the aggregate – or not at all
    @Transactional(propagation = Propagation.MANDATORY)
    public void save(List<SchaffbarEvent> events) {
        List<OutboxEvent> outboxEvents = events.stream() //
                .map(OutboxEvent::of) //
                .toList();

        this.outboxEventRepository.saveAll(outboxEvents);
    }

}

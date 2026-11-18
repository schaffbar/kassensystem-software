package de.schaffbar.core_pos.domain.rfid_tag;

import java.util.List;
import java.util.Optional;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import de.schaffbar.core_pos.domain.rfid_tag.RfidTagViews.RfidTagView;
import de.schaffbar.core_pos.shared.cqrs.Command;
import de.schaffbar.core_pos.shared.cqrs.Query;
import de.schaffbar.core_pos.shared.event.SchaffbarEvent;
import de.schaffbar.core_pos.shared.event.outbox.OutboxEventStore;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;
import de.schaffbar.core_pos.shared.exception.RfidTagAlreadyRegisteredException;
import de.schaffbar.core_pos.shared.id.RfidTagId;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class RfidTagService {

    private final @NonNull RfidTagRepository rfidTagRepository;

    private final @NonNull OutboxEventStore outboxEventStore;

    // ------------------------------------------------------------------------
    // query

    @Query
    public List<RfidTagView> getRfidTags() {
        return this.rfidTagRepository.findAll().stream() //
                .map(RfidTagViews.MAPPER::toRfidTagView) //
                .toList();
    }

    @Query
    public Optional<RfidTagView> getRfidTag(@NotNull @Valid RfidTagId id) {
        return this.rfidTagRepository.findById(id.getValue()) //
                .map(RfidTagViews.MAPPER::toRfidTagView);
    }

    // ------------------------------------------------------------------------
    // command

    @Command
    public RfidTagId createRfidTag(@NotNull @Valid RfidTagId id) {
        ensureRfidTagNotRegistered(id);

        RfidTag rfidTag = RfidTag.of(id);
        RfidTag savedRfidTag = this.rfidTagRepository.save(rfidTag);
        List<SchaffbarEvent> events = List.of(RfidTagEventFactory.rfidTagCreated(savedRfidTag));

        this.outboxEventStore.save(events);

        log.info("Created RFID tag with id {} and published events {}", savedRfidTag.getId(), events);

        return savedRfidTag.getId();
    }

    @Command
    public void deleteRfidTag(@NotNull @Valid RfidTagId id) {
        RfidTagView rfidTag = getRfidTag(id) //
                .orElseThrow(() -> ResourceNotFoundException.rfidTag(id));

        this.rfidTagRepository.deleteById(rfidTag.id().getValue());
        List<SchaffbarEvent> events = List.of(RfidTagEventFactory.rfidTagDeleted(id));

        this.outboxEventStore.save(events);

        log.info("Deleted RFID tag with id {} and published events {}", id, events);
    }

    // ------------------------------------------------------------------------
    // helper

    // RT-5
    private void ensureRfidTagNotRegistered(RfidTagId id) {
        if (this.rfidTagRepository.existsById(id.getValue())) {
            throw new RfidTagAlreadyRegisteredException(id);
        }
    }

}

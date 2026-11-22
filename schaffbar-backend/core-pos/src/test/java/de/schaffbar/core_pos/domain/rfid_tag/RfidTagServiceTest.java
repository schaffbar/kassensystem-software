package de.schaffbar.core_pos.domain.rfid_tag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.schaffbar.core_pos.builder.RandomValues;
import de.schaffbar.core_pos.shared.event.EventType;
import de.schaffbar.core_pos.shared.event.SchaffbarEvent;
import de.schaffbar.core_pos.shared.event.outbox.OutboxEventStore;
import de.schaffbar.core_pos.shared.event.payload.RfidTagEventPayload.RfidTagCreatedPayload;
import de.schaffbar.core_pos.shared.event.payload.RfidTagEventPayload.RfidTagDeletedPayload;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException.Resource;
import de.schaffbar.core_pos.shared.exception.RfidTagAlreadyRegisteredException;
import de.schaffbar.core_pos.shared.id.RfidTagId;

@ExtendWith(MockitoExtension.class)
class RfidTagServiceTest {

    @Mock
    private RfidTagRepository rfidTagRepository;

    @Mock
    private OutboxEventStore outboxEventStore;

    @Captor
    private ArgumentCaptor<List<SchaffbarEvent>> events;

    @Captor
    private ArgumentCaptor<RfidTag> savedTag;

    @InjectMocks
    private RfidTagService rfidTagService;

    // ------------------------------------------------------------------------
    // createRfidTag

    @Test
    void createRfidTag_savesAnActiveTagAndItsCreatedEvent() {
        // given
        RfidTagId rfidTagId = givenUnregisteredTag();
        givenSaveReturnsTheTag();

        // when
        RfidTagId createdId = this.rfidTagService.createRfidTag(rfidTagId);

        // then
        assertThat(createdId).isEqualTo(rfidTagId);
        assertThat(expectSavedTag().isActive()).isTrue();
        RfidTagCreatedPayload payload = expectSavedEvent(EventType.RFID_TAG_CREATED, rfidTagId) //
                .deserializeAndValidatePayload(RfidTagCreatedPayload.class);
        assertThat(payload.id()).isEqualTo(rfidTagId);
    }

    @Test
    void createRfidTag_rejectsARegisteredTag() {
        // given
        RfidTagId rfidTagId = givenRegisteredTag();

        // when
        Throwable thrown = catchThrowable(() -> this.rfidTagService.createRfidTag(rfidTagId));

        // then
        assertThat(thrown) //
                .isInstanceOf(RfidTagAlreadyRegisteredException.class) //
                .hasFieldOrPropertyWithValue("code", RfidTagAlreadyRegisteredException.CODE);
        expectNothingSaved();
    }

    // ------------------------------------------------------------------------
    // deleteRfidTag

    @Test
    void deleteRfidTag_deletesTheTagAndSavesTheEvent() {
        // given
        RfidTag rfidTag = givenTag();

        // when
        this.rfidTagService.deleteRfidTag(rfidTag.getId());

        // then
        expectDeleted(rfidTag);
        RfidTagDeletedPayload payload = expectSavedEvent(EventType.RFID_TAG_DELETED, rfidTag.getId()) //
                .deserializeAndValidatePayload(RfidTagDeletedPayload.class);
        assertThat(payload.id()).isEqualTo(rfidTag.getId());
    }

    @Test
    void deleteRfidTag_rejectsAnUnknownTag() {
        // given
        RfidTagId unknown = givenUnknownTag();

        // when
        Throwable thrown = catchThrowable(() -> this.rfidTagService.deleteRfidTag(unknown));

        // then
        assertThat(thrown) //
                .isInstanceOf(ResourceNotFoundException.class) //
                .hasFieldOrPropertyWithValue("resource", Resource.RFID_TAG);
        expectNothingDeleted();
        expectNoEvent();
    }

    // ------------------------------------------------------------------------
    // given

    private RfidTag givenTag() {
        RfidTag rfidTag = RfidTag.of(RandomValues.rfidTagId());
        when(this.rfidTagRepository.findById(rfidTag.getId().getValue())).thenReturn(Optional.of(rfidTag));

        return rfidTag;
    }

    private RfidTagId givenUnknownTag() {
        RfidTagId unknown = RandomValues.rfidTagId();
        when(this.rfidTagRepository.findById(unknown.getValue())).thenReturn(Optional.empty());

        return unknown;
    }

    private RfidTagId givenRegisteredTag() {
        RfidTagId rfidTagId = RandomValues.rfidTagId();
        when(this.rfidTagRepository.existsById(rfidTagId)).thenReturn(true);

        return rfidTagId;
    }

    private RfidTagId givenUnregisteredTag() {
        RfidTagId rfidTagId = RandomValues.rfidTagId();
        when(this.rfidTagRepository.existsById(rfidTagId)).thenReturn(false);

        return rfidTagId;
    }

    private void givenSaveReturnsTheTag() {
        when(this.rfidTagRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    // ------------------------------------------------------------------------
    // then

    private SchaffbarEvent expectSavedEvent(EventType type, RfidTagId rfidTagId) {
        verify(this.outboxEventStore).save(this.events.capture());
        assertThat(this.events.getValue()) //
                .singleElement() //
                .satisfies(event -> {
                    assertThat(event.getType()).isEqualTo(type);
                    assertThat(event.getAggregateId()).isEqualTo(rfidTagId.getValue());
                });

        return this.events.getValue().getFirst();
    }

    private RfidTag expectSavedTag() {
        verify(this.rfidTagRepository).save(this.savedTag.capture());

        return this.savedTag.getValue();
    }

    private void expectDeleted(RfidTag rfidTag) {
        verify(this.rfidTagRepository).delete(rfidTag);
    }

    private void expectNothingDeleted() {
        verify(this.rfidTagRepository, never()).delete(any());
    }

    private void expectNothingSaved() {
        verify(this.rfidTagRepository, never()).save(any());
        expectNoEvent();
    }

    private void expectNoEvent() {
        verifyNoInteractions(this.outboxEventStore);
    }

}

package de.schaffbar.core_pos.domain.rfid_reader;

import static de.schaffbar.core_pos.builder.CommandBuilder.changeRfidReaderTypeCommand;
import static de.schaffbar.core_pos.builder.CommandBuilder.updateRfidReaderCommand;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.schaffbar.core_pos.builder.RandomValues;
import de.schaffbar.core_pos.domain.rfid_reader.RfidReaderCommands.ChangeRfidReaderTypeCommand;
import de.schaffbar.core_pos.domain.rfid_reader.RfidReaderCommands.UpdateRfidReaderCommand;
import de.schaffbar.core_pos.shared.event.EventType;
import de.schaffbar.core_pos.shared.event.SchaffbarEvent;
import de.schaffbar.core_pos.shared.event.outbox.OutboxEventStore;
import de.schaffbar.core_pos.shared.event.payload.RfidReaderEventPayload.RfidReaderCreatedPayload;
import de.schaffbar.core_pos.shared.event.payload.RfidReaderEventPayload.RfidReaderDeletedPayload;
import de.schaffbar.core_pos.shared.exception.MacAddressAlreadyUsedException;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException.Resource;
import de.schaffbar.core_pos.shared.exception.RfidReaderNameAlreadyUsedException;
import de.schaffbar.core_pos.shared.id.MacAddress;
import de.schaffbar.core_pos.shared.id.RfidReaderId;

@ExtendWith(MockitoExtension.class)
class RfidReaderServiceTest {

    @Mock
    private RfidReaderRepository rfidReaderRepository;

    @Mock
    private OutboxEventStore outboxEventStore;

    @Captor
    private ArgumentCaptor<List<SchaffbarEvent>> events;

    @InjectMocks
    private RfidReaderService rfidReaderService;

    // ------------------------------------------------------------------------
    // createRfidReader

    @Test
    void createRfidReader_savesTheReaderAndItsCreatedEvent() {
        // given
        MacAddress macAddress = RandomValues.macAddress();
        givenNoReaderWith(macAddress);
        givenSaveReturnsTheReader();

        // when
        RfidReaderId rfidReaderId = this.rfidReaderService.createRfidReader(macAddress);

        // then
        RfidReaderCreatedPayload payload = expectSavedEvent(EventType.RFID_READER_CREATED, rfidReaderId) //
                .deserializeAndValidatePayload(RfidReaderCreatedPayload.class);
        assertThat(payload.id()).isEqualTo(rfidReaderId);
        assertThat(payload.macAddress()).isEqualTo(macAddress);
    }

    @Test
    void createRfidReader_rejectsAUsedMacAddress() {
        // given
        MacAddress macAddress = RandomValues.macAddress();
        givenReaderWith(macAddress);

        // when
        Throwable thrown = catchThrowable(() -> this.rfidReaderService.createRfidReader(macAddress));

        // then
        assertThat(thrown) //
                .isInstanceOf(MacAddressAlreadyUsedException.class) //
                .hasFieldOrPropertyWithValue("code", MacAddressAlreadyUsedException.CODE);
        expectNothingSaved();
    }

    // ------------------------------------------------------------------------
    // updateRfidReader

    @Test
    void updateRfidReader_ofANewReaderWithoutType_changesNameAndSocketNameAndSavesTheEvent() {
        // given
        RfidReader rfidReader = givenReader();
        UpdateRfidReaderCommand command = updateRfidReaderCommand(rfidReader.getId()).build();
        givenReadersNamed(command.name());

        // when
        this.rfidReaderService.updateRfidReader(command);

        // then
        assertThat(rfidReader.getName()).isEqualTo(command.name());
        assertThat(rfidReader.getSocketName()).isEqualTo(command.socketName());
        expectSavedEvent(EventType.RFID_READER_UPDATED, rfidReader.getId());
    }

    @Test
    void updateRfidReader_keepsItsOwnNameInAnotherCase() {
        // given
        RfidReader rfidReader = givenReaderNamed("Theke");
        UpdateRfidReaderCommand command = updateRfidReaderCommand(rfidReader.getId()).name("THEKE").build();
        givenReadersNamed(command.name(), rfidReader);

        // when
        this.rfidReaderService.updateRfidReader(command);

        // then
        assertThat(rfidReader.getName()).isEqualTo(command.name());
        expectSavedEvent(EventType.RFID_READER_UPDATED, rfidReader.getId());
    }

    @Test
    void updateRfidReader_withoutName_isNotComparedWithOtherReaders() {
        // given
        RfidReader rfidReader = givenReaderNamed("Theke");
        UpdateRfidReaderCommand command = updateRfidReaderCommand(rfidReader.getId()).name(null).build();

        // when
        this.rfidReaderService.updateRfidReader(command);

        // then
        assertThat(rfidReader.getName()).isNull();
        expectSavedEvent(EventType.RFID_READER_UPDATED, rfidReader.getId());
        expectNoNameLookup();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Theke", "THEKE"})
    void updateRfidReader_rejectsTheNameOfAnotherReader(String name) {
        // given
        RfidReader rfidReader = givenReader();
        RfidReader other = readerNamed("Theke");
        UpdateRfidReaderCommand command = updateRfidReaderCommand(rfidReader.getId()).name(name).build();
        givenReadersNamed(command.name(), other);

        // when
        Throwable thrown = catchThrowable(() -> this.rfidReaderService.updateRfidReader(command));

        // then
        expectNameAlreadyUsedBy(thrown, other);
        assertThat(rfidReader.getName()).isNull();
        expectNoEvent();
    }

    @Test
    void updateRfidReader_rejectsAnUnknownReader() {
        // given
        RfidReaderId unknown = givenUnknownReader();
        UpdateRfidReaderCommand command = updateRfidReaderCommand(unknown).build();

        // when
        Throwable thrown = catchThrowable(() -> this.rfidReaderService.updateRfidReader(command));

        // then
        expectRfidReaderNotFound(thrown);
        expectNoEvent();
    }

    // ------------------------------------------------------------------------
    // changeRfidReaderType

    @Test
    void changeRfidReaderType_setsTheTypeAndSavesTheEvent() {
        // given
        RfidReader rfidReader = givenReader();
        ChangeRfidReaderTypeCommand command = changeRfidReaderTypeCommand(rfidReader.getId()).build();

        // when
        this.rfidReaderService.changeRfidReaderType(command);

        // then
        assertThat(rfidReader.getType()).isEqualTo(command.type());
        expectSavedEvent(EventType.RFID_READER_TYPE_CHANGED, rfidReader.getId());
    }

    @Test
    void changeRfidReaderType_rejectsAnUnknownReader() {
        // given
        RfidReaderId unknown = givenUnknownReader();
        ChangeRfidReaderTypeCommand command = changeRfidReaderTypeCommand(unknown).build();

        // when
        Throwable thrown = catchThrowable(() -> this.rfidReaderService.changeRfidReaderType(command));

        // then
        expectRfidReaderNotFound(thrown);
        expectNoEvent();
    }

    // ------------------------------------------------------------------------
    // deleteRfidReader

    @Test
    void deleteRfidReader_deletesTheReaderAndSavesTheEvent() {
        // given
        RfidReader rfidReader = givenReader();

        // when
        this.rfidReaderService.deleteRfidReader(rfidReader.getId());

        // then
        expectDeleted(rfidReader);
        RfidReaderDeletedPayload payload = expectSavedEvent(EventType.RFID_READER_DELETED, rfidReader.getId()) //
                .deserializeAndValidatePayload(RfidReaderDeletedPayload.class);
        assertThat(payload.id()).isEqualTo(rfidReader.getId());
    }

    @Test
    void deleteRfidReader_rejectsAnUnknownReader() {
        // given
        RfidReaderId unknown = givenUnknownReader();

        // when
        Throwable thrown = catchThrowable(() -> this.rfidReaderService.deleteRfidReader(unknown));

        // then
        expectRfidReaderNotFound(thrown);
        expectNothingDeleted();
        expectNoEvent();
    }

    // ------------------------------------------------------------------------
    // given

    private RfidReader givenReader() {
        RfidReader rfidReader = RfidReader.of(RandomValues.macAddress());
        when(this.rfidReaderRepository.findById(rfidReader.getId().getValue())).thenReturn(Optional.of(rfidReader));

        return rfidReader;
    }

    private RfidReader givenReaderNamed(String name) {
        RfidReader rfidReader = readerNamed(name);
        when(this.rfidReaderRepository.findById(rfidReader.getId().getValue())).thenReturn(Optional.of(rfidReader));

        return rfidReader;
    }

    private void givenReadersNamed(String name, RfidReader... rfidReaders) {
        when(this.rfidReaderRepository.findByNameIgnoreCase(name)).thenReturn(List.of(rfidReaders));
    }

    private RfidReaderId givenUnknownReader() {
        RfidReaderId unknown = RfidReaderId.random();
        when(this.rfidReaderRepository.findById(unknown.getValue())).thenReturn(Optional.empty());

        return unknown;
    }

    private void givenReaderWith(MacAddress macAddress) {
        when(this.rfidReaderRepository.findByMacAddress(macAddress)).thenReturn(Optional.of(RfidReader.of(macAddress)));
    }

    private void givenNoReaderWith(MacAddress macAddress) {
        when(this.rfidReaderRepository.findByMacAddress(macAddress)).thenReturn(Optional.empty());
    }

    private void givenSaveReturnsTheReader() {
        when(this.rfidReaderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    // ------------------------------------------------------------------------
    // then

    private SchaffbarEvent expectSavedEvent(EventType type, RfidReaderId rfidReaderId) {
        verify(this.outboxEventStore).save(this.events.capture());
        assertThat(this.events.getValue()) //
                .singleElement() //
                .satisfies(event -> {
                    assertThat(event.getType()).isEqualTo(type);
                    assertThat(event.getAggregateId()).isEqualTo(rfidReaderId.getValue().toString());
                });

        return this.events.getValue().getFirst();
    }

    private void expectRfidReaderNotFound(Throwable thrown) {
        assertThat(thrown) //
                .isInstanceOf(ResourceNotFoundException.class) //
                .hasFieldOrPropertyWithValue("resource", Resource.RFID_READER);
    }

    private void expectNameAlreadyUsedBy(Throwable thrown, RfidReader other) {
        assertThat(thrown) //
                .isInstanceOf(RfidReaderNameAlreadyUsedException.class) //
                .hasFieldOrPropertyWithValue("code", RfidReaderNameAlreadyUsedException.CODE) //
                .hasMessageContaining(other.getId().getValue().toString());
    }

    private void expectNoNameLookup() {
        verify(this.rfidReaderRepository, never()).findByNameIgnoreCase(any());
    }

    private void expectDeleted(RfidReader rfidReader) {
        verify(this.rfidReaderRepository).delete(rfidReader);
    }

    private void expectNothingDeleted() {
        verify(this.rfidReaderRepository, never()).delete(any());
    }

    private void expectNothingSaved() {
        verify(this.rfidReaderRepository, never()).save(any());
        expectNoEvent();
    }

    private void expectNoEvent() {
        verifyNoInteractions(this.outboxEventStore);
    }

    // ------------------------------------------------------------------------
    // helper

    private static RfidReader readerNamed(String name) {
        RfidReader rfidReader = RfidReader.of(RandomValues.macAddress());
        rfidReader.update(updateRfidReaderCommand(rfidReader.getId()).name(name).build());

        return rfidReader;
    }

}

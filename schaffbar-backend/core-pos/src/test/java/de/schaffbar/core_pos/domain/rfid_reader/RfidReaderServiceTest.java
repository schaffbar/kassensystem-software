package de.schaffbar.core_pos.domain.rfid_reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

import de.schaffbar.core_pos.domain.rfid_reader.RfidReaderCommands.ChangeRfidReaderTypeCommand;
import de.schaffbar.core_pos.domain.rfid_reader.RfidReaderCommands.UpdateRfidReaderCommand;
import de.schaffbar.core_pos.shared.event.EventType;
import de.schaffbar.core_pos.shared.event.SchaffbarEvent;
import de.schaffbar.core_pos.shared.event.outbox.OutboxEventStore;
import de.schaffbar.core_pos.shared.exception.MacAddressAlreadyUsedException;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;
import de.schaffbar.core_pos.shared.id.MacAddress;
import de.schaffbar.core_pos.shared.id.RfidReaderId;

@ExtendWith(MockitoExtension.class)
class RfidReaderServiceTest {

    private static final MacAddress MAC_ADDRESS = MacAddress.of("34:CD:B0:D3:A9:30");

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
        when(this.rfidReaderRepository.findByMacAddress(MAC_ADDRESS.getValue())).thenReturn(Optional.empty());
        when(this.rfidReaderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RfidReaderId rfidReaderId = this.rfidReaderService.createRfidReader(MAC_ADDRESS);

        verify(this.outboxEventStore).save(this.events.capture());
        assertThat(this.events.getValue()) //
                .singleElement() //
                .satisfies(event -> {
                    assertThat(event.getType()).isEqualTo(EventType.RFID_READER_CREATED);
                    assertThat(event.getAggregateId()).isEqualTo(rfidReaderId.getValue().toString());
                });
    }

    @Test
    void createRfidReader_rejectsAUsedMacAddress() {
        RfidReader existing = RfidReader.of(MAC_ADDRESS);
        when(this.rfidReaderRepository.findByMacAddress(MAC_ADDRESS.getValue())).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> this.rfidReaderService.createRfidReader(MAC_ADDRESS)) //
                .isInstanceOf(MacAddressAlreadyUsedException.class) //
                .hasFieldOrPropertyWithValue("code", MacAddressAlreadyUsedException.CODE);

        verify(this.rfidReaderRepository, never()).save(any());
        verifyNoInteractions(this.outboxEventStore);
    }

    // ------------------------------------------------------------------------
    // updateRfidReader

    @Test
    void updateRfidReader_ofANewReaderWithoutType_changesNameAndSocketNameAndSavesTheEvent() {
        RfidReader rfidReader = givenReader();
        UpdateRfidReaderCommand command = UpdateRfidReaderCommand.builder() //
                .id(rfidReader.getId()) //
                .name("Theke") //
                .socketName("socket-43") //
                .build();

        this.rfidReaderService.updateRfidReader(command);

        assertThat(rfidReader.getName()).isEqualTo("Theke");
        assertThat(rfidReader.getSocketName()).isEqualTo("socket-43");
        expectSavedEvent(EventType.RFID_READER_UPDATED, rfidReader.getId());
    }

    @Test
    void updateRfidReader_rejectsAnUnknownReader() {
        RfidReaderId unknown = RfidReaderId.random();
        when(this.rfidReaderRepository.findById(unknown.getValue())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> this.rfidReaderService.updateRfidReader(UpdateRfidReaderCommand.builder().id(unknown).build())) //
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(this.outboxEventStore);
    }

    // ------------------------------------------------------------------------
    // changeRfidReaderType

    @Test
    void changeRfidReaderType_setsTheTypeAndSavesTheEvent() {
        RfidReader rfidReader = givenReader();
        ChangeRfidReaderTypeCommand command = ChangeRfidReaderTypeCommand.builder() //
                .id(rfidReader.getId()) //
                .type(RfidReaderType.SWITCH_BOX) //
                .build();

        this.rfidReaderService.changeRfidReaderType(command);

        assertThat(rfidReader.getType()).isEqualTo(RfidReaderType.SWITCH_BOX);
        expectSavedEvent(EventType.RFID_READER_TYPE_CHANGED, rfidReader.getId());
    }

    @Test
    void changeRfidReaderType_rejectsAnUnknownReader() {
        RfidReaderId unknown = RfidReaderId.random();
        when(this.rfidReaderRepository.findById(unknown.getValue())).thenReturn(Optional.empty());
        ChangeRfidReaderTypeCommand command = ChangeRfidReaderTypeCommand.builder().id(unknown).type(RfidReaderType.SWITCH_BOX).build();

        assertThatThrownBy(() -> this.rfidReaderService.changeRfidReaderType(command)) //
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(this.outboxEventStore);
    }

    // ------------------------------------------------------------------------
    // deleteRfidReader

    @Test
    void deleteRfidReader_deletesTheReaderAndSavesTheEvent() {
        RfidReader rfidReader = givenReader();

        this.rfidReaderService.deleteRfidReader(rfidReader.getId());

        verify(this.rfidReaderRepository).deleteById(rfidReader.getId().getValue());
        expectSavedEvent(EventType.RFID_READER_DELETED, rfidReader.getId());
    }

    @Test
    void deleteRfidReader_rejectsAnUnknownReader() {
        RfidReaderId unknown = RfidReaderId.random();
        when(this.rfidReaderRepository.findById(unknown.getValue())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> this.rfidReaderService.deleteRfidReader(unknown)) //
                .isInstanceOf(ResourceNotFoundException.class);

        verify(this.rfidReaderRepository, never()).deleteById(any());
        verifyNoInteractions(this.outboxEventStore);
    }

    // ------------------------------------------------------------------------
    // helper

    private RfidReader givenReader() {
        RfidReader rfidReader = RfidReader.of(MAC_ADDRESS);
        when(this.rfidReaderRepository.findById(rfidReader.getId().getValue())).thenReturn(Optional.of(rfidReader));
        return rfidReader;
    }

    private void expectSavedEvent(EventType type, RfidReaderId rfidReaderId) {
        verify(this.outboxEventStore).save(this.events.capture());
        assertThat(this.events.getValue()) //
                .singleElement() //
                .satisfies(event -> {
                    assertThat(event.getType()).isEqualTo(type);
                    assertThat(event.getAggregateId()).isEqualTo(rfidReaderId.getValue().toString());
                });
    }

}

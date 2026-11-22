package de.schaffbar.core_pos.domain.rfid_reader;

import static de.schaffbar.core_pos.builder.CommandBuilder.changeRfidReaderTypeCommand;
import static de.schaffbar.core_pos.builder.CommandBuilder.updateRfidReaderCommand;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import de.schaffbar.core_pos.builder.RandomValues;
import de.schaffbar.core_pos.domain.rfid_reader.RfidReaderCommands.ChangeRfidReaderTypeCommand;
import de.schaffbar.core_pos.domain.rfid_reader.RfidReaderCommands.UpdateRfidReaderCommand;
import de.schaffbar.core_pos.shared.event.EventType;
import de.schaffbar.core_pos.shared.event.SchaffbarEvent;
import de.schaffbar.core_pos.shared.event.payload.RfidReaderEventPayload.RfidReaderTypeChangedPayload;
import de.schaffbar.core_pos.shared.event.payload.RfidReaderEventPayload.RfidReaderUpdatedPayload;
import de.schaffbar.core_pos.shared.id.MacAddress;

class RfidReaderTest {

    // ------------------------------------------------------------------------
    // of

    @Test
    void of_createsAReaderWithoutType() {
        // given
        MacAddress macAddress = RandomValues.macAddress();

        // when
        RfidReader rfidReader = RfidReader.of(macAddress);

        // then
        assertThat(rfidReader.getId()).isNotNull();
        assertThat(rfidReader.getMacAddress()).isEqualTo(macAddress);
        assertThat(rfidReader.getType()).isNull();
        assertThat(rfidReader.getCreatedAt()).isNotNull();
    }

    // ------------------------------------------------------------------------
    // update

    @Test
    void update_ofANewReaderWithoutType_changesNameAndSocketNameAndReturnsTheEvent() {
        // given
        RfidReader rfidReader = RfidReader.of(RandomValues.macAddress());
        UpdateRfidReaderCommand command = updateRfidReaderCommand(rfidReader.getId()).build();

        // when
        List<SchaffbarEvent> events = rfidReader.update(command);

        // then
        assertThat(rfidReader.getName()).isEqualTo(command.name());
        assertThat(rfidReader.getSocketName()).isEqualTo(command.socketName());

        RfidReaderUpdatedPayload payload = expectSingleEvent(events, EventType.RFID_READER_UPDATED, rfidReader) //
                .deserializeAndValidatePayload(RfidReaderUpdatedPayload.class);
        assertThat(payload.id()).isEqualTo(rfidReader.getId());
        assertThat(payload.type()).isNull();
        assertThat(payload.name()).isEqualTo(command.name());
        assertThat(payload.socketName()).isEqualTo(command.socketName());
    }

    // ------------------------------------------------------------------------
    // changeType

    @Test
    void changeType_setsTheTypeAndReturnsTheEvent() {
        // given
        RfidReader rfidReader = RfidReader.of(RandomValues.macAddress());
        ChangeRfidReaderTypeCommand command = changeRfidReaderTypeCommand(rfidReader.getId()).build();

        // when
        List<SchaffbarEvent> events = rfidReader.changeType(command);

        // then
        assertThat(rfidReader.getType()).isEqualTo(command.type());

        RfidReaderTypeChangedPayload payload = expectSingleEvent(events, EventType.RFID_READER_TYPE_CHANGED, rfidReader) //
                .deserializeAndValidatePayload(RfidReaderTypeChangedPayload.class);
        assertThat(payload.id()).isEqualTo(rfidReader.getId());
        assertThat(payload.type()).isEqualTo(command.type());
    }

    // ------------------------------------------------------------------------
    // then

    private SchaffbarEvent expectSingleEvent(List<SchaffbarEvent> events, EventType type, RfidReader rfidReader) {
        assertThat(events).hasSize(1);
        SchaffbarEvent event = events.getFirst();
        assertThat(event.getType()).isEqualTo(type);
        assertThat(event.getAggregateId()).isEqualTo(rfidReader.getId().getValue().toString());

        return event;
    }

}

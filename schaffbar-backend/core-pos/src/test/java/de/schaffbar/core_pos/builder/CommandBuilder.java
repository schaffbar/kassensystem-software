package de.schaffbar.core_pos.builder;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import de.schaffbar.core_pos.domain.rfid_reader.RfidReaderCommands.ChangeRfidReaderTypeCommand;
import de.schaffbar.core_pos.domain.rfid_reader.RfidReaderCommands.ChangeRfidReaderTypeCommand.ChangeRfidReaderTypeCommandBuilder;
import de.schaffbar.core_pos.domain.rfid_reader.RfidReaderCommands.UpdateRfidReaderCommand;
import de.schaffbar.core_pos.domain.rfid_reader.RfidReaderCommands.UpdateRfidReaderCommand.UpdateRfidReaderCommandBuilder;
import de.schaffbar.core_pos.domain.rfid_reader.RfidReaderType;
import de.schaffbar.core_pos.shared.id.RfidReaderId;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CommandBuilder {

    // ------------------------------------------------------------------------
    // RFID reader

    public static UpdateRfidReaderCommandBuilder updateRfidReaderCommand() {
        return updateRfidReaderCommand(RfidReaderId.random());
    }

    public static UpdateRfidReaderCommandBuilder updateRfidReaderCommand(RfidReaderId id) {
        return UpdateRfidReaderCommand.builder() //
                .id(id) //
                .name("Theke") //
                .socketName("socket-43");
    }

    public static ChangeRfidReaderTypeCommandBuilder changeRfidReaderTypeCommand() {
        return changeRfidReaderTypeCommand(RfidReaderId.random());
    }

    public static ChangeRfidReaderTypeCommandBuilder changeRfidReaderTypeCommand(RfidReaderId id) {
        return ChangeRfidReaderTypeCommand.builder() //
                .id(id) //
                .type(RfidReaderType.SWITCH_BOX);
    }

}

package de.schaffbar.core_pos.use_case;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.schaffbar.core_pos.domain.rfid_reader.RfidReaderCommands.ChangeRfidReaderTypeCommand;
import de.schaffbar.core_pos.domain.rfid_reader.RfidReaderService;
import de.schaffbar.core_pos.domain.rfid_reader.RfidReaderType;
import de.schaffbar.core_pos.domain.rfid_reader.RfidReaderViews.RfidReaderView;
import de.schaffbar.core_pos.domain.tool.ToolService;
import de.schaffbar.core_pos.domain.tool.ToolViews.ToolView;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;
import de.schaffbar.core_pos.shared.exception.RfidReaderHasToolAssignedException;
import de.schaffbar.core_pos.shared.id.RfidReaderId;

@ExtendWith(MockitoExtension.class)
class RfidReaderChangeTypeTest {

    private final RfidReaderId rfidReaderId = RfidReaderId.random();

    private final ChangeRfidReaderTypeCommand command = ChangeRfidReaderTypeCommand.builder() //
            .id(this.rfidReaderId) //
            .type(RfidReaderType.GATE_KEEPER_IN) //
            .build();

    @Mock
    private RfidReaderService rfidReaderService;

    @Mock
    private ToolService toolService;

    @InjectMocks
    private RfidReaderChangeType rfidReaderChangeType;

    @Test
    void rejectsUnknownReader() {
        when(this.rfidReaderService.getRfidReader(this.rfidReaderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> this.rfidReaderChangeType.process(this.command)) //
                .isInstanceOf(ResourceNotFoundException.class);

        verify(this.rfidReaderService, never()).changeRfidReaderType(any());
    }

    @Test
    void changesReaderThatIsNoSwitchBox_withoutLookingForTools() {
        givenReaderOfType(RfidReaderType.RFID_TAG_ASSIGNER);

        this.rfidReaderChangeType.process(this.command);

        verify(this.rfidReaderService).changeRfidReaderType(this.command);
        verifyNoInteractions(this.toolService);
    }

    @Test
    void changesSwitchBoxWithoutTool() {
        givenReaderOfType(RfidReaderType.SWITCH_BOX);
        when(this.toolService.getTool(this.rfidReaderId)).thenReturn(Optional.empty());

        this.rfidReaderChangeType.process(this.command);

        verify(this.rfidReaderService).changeRfidReaderType(this.command);
    }

    @Test
    void rejectsSwitchBoxAssignedToTool() {
        givenReaderOfType(RfidReaderType.SWITCH_BOX);
        when(this.toolService.getTool(this.rfidReaderId)).thenReturn(Optional.of(mock(ToolView.class)));

        assertThatThrownBy(() -> this.rfidReaderChangeType.process(this.command)) //
                .isInstanceOf(RfidReaderHasToolAssignedException.class) //
                .hasFieldOrPropertyWithValue("code", RfidReaderHasToolAssignedException.CODE);

        verify(this.rfidReaderService, never()).changeRfidReaderType(any());
    }

    // ------------------------------------------------------------------------
    // helper

    private void givenReaderOfType(RfidReaderType type) {
        RfidReaderView rfidReader = RfidReaderView.builder() //
                .id(this.rfidReaderId) //
                .macAddress("34:CD:B0:D3:A9:30") //
                .type(type) //
                .createdAt(Instant.now()) //
                .build();
        when(this.rfidReaderService.getRfidReader(this.rfidReaderId)).thenReturn(Optional.of(rfidReader));
    }

}

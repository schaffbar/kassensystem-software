package de.schaffbar.core_pos.use_case;

import static de.schaffbar.core_pos.builder.CommandBuilder.changeRfidReaderTypeCommand;
import static de.schaffbar.core_pos.builder.ViewBuilder.rfidReaderView;
import static de.schaffbar.core_pos.builder.ViewBuilder.toolView;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException.Resource;
import de.schaffbar.core_pos.shared.exception.RfidReaderHasToolAssignedException;
import de.schaffbar.core_pos.shared.id.RfidReaderId;

@ExtendWith(MockitoExtension.class)
class RfidReaderChangeTypeTest {

    @Mock
    private RfidReaderService rfidReaderService;

    @Mock
    private ToolService toolService;

    @InjectMocks
    private RfidReaderChangeType rfidReaderChangeType;

    @Test
    void rejectsUnknownReader() {
        // given
        RfidReaderId unknown = givenUnknownReader();
        ChangeRfidReaderTypeCommand command = changeRfidReaderTypeCommand(unknown).build();

        // when
        Throwable thrown = catchThrowable(() -> this.rfidReaderChangeType.process(command));

        // then
        assertThat(thrown) //
                .isInstanceOf(ResourceNotFoundException.class) //
                .hasFieldOrPropertyWithValue("resource", Resource.RFID_READER);
        expectTypeNotChanged();
    }

    @Test
    void changesReaderThatIsNoSwitchBox_withoutLookingForTools() {
        // given
        RfidReaderId rfidReaderId = givenReaderOfType(RfidReaderType.RFID_TAG_ASSIGNER);
        ChangeRfidReaderTypeCommand command = changeRfidReaderTypeCommand(rfidReaderId).type(RfidReaderType.GATE_KEEPER_IN).build();

        // when
        this.rfidReaderChangeType.process(command);

        // then
        expectTypeChanged(command);
        expectNoToolLookedUp();
    }

    @Test
    void changesSwitchBoxWithoutTool() {
        // given
        RfidReaderId rfidReaderId = givenReaderOfType(RfidReaderType.SWITCH_BOX);
        givenNoToolWith(rfidReaderId);
        ChangeRfidReaderTypeCommand command = changeRfidReaderTypeCommand(rfidReaderId).type(RfidReaderType.GATE_KEEPER_IN).build();

        // when
        this.rfidReaderChangeType.process(command);

        // then
        expectTypeChanged(command);
    }

    @Test
    void rejectsSwitchBoxAssignedToTool() {
        // given
        RfidReaderId rfidReaderId = givenReaderOfType(RfidReaderType.SWITCH_BOX);
        givenToolWith(rfidReaderId);
        ChangeRfidReaderTypeCommand command = changeRfidReaderTypeCommand(rfidReaderId).type(RfidReaderType.GATE_KEEPER_IN).build();

        // when
        Throwable thrown = catchThrowable(() -> this.rfidReaderChangeType.process(command));

        // then
        assertThat(thrown) //
                .isInstanceOf(RfidReaderHasToolAssignedException.class) //
                .hasFieldOrPropertyWithValue("code", RfidReaderHasToolAssignedException.CODE);
        expectTypeNotChanged();
    }

    // ------------------------------------------------------------------------
    // given

    private RfidReaderId givenReaderOfType(RfidReaderType type) {
        RfidReaderView rfidReader = rfidReaderView().type(type).build();
        when(this.rfidReaderService.getRfidReader(rfidReader.id())).thenReturn(Optional.of(rfidReader));

        return rfidReader.id();
    }

    private RfidReaderId givenUnknownReader() {
        RfidReaderId unknown = RfidReaderId.random();
        when(this.rfidReaderService.getRfidReader(unknown)).thenReturn(Optional.empty());

        return unknown;
    }

    private void givenToolWith(RfidReaderId rfidReaderId) {
        when(this.toolService.getTool(rfidReaderId)).thenReturn(Optional.of(toolView().rfidReaderId(rfidReaderId).build()));
    }

    private void givenNoToolWith(RfidReaderId rfidReaderId) {
        when(this.toolService.getTool(rfidReaderId)).thenReturn(Optional.empty());
    }

    // ------------------------------------------------------------------------
    // then

    private void expectTypeChanged(ChangeRfidReaderTypeCommand command) {
        verify(this.rfidReaderService).changeRfidReaderType(command);
    }

    private void expectTypeNotChanged() {
        verify(this.rfidReaderService, never()).changeRfidReaderType(any());
    }

    private void expectNoToolLookedUp() {
        verifyNoInteractions(this.toolService);
    }

}

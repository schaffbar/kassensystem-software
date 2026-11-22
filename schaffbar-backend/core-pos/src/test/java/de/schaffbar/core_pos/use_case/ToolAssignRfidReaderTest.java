package de.schaffbar.core_pos.use_case;

import static de.schaffbar.core_pos.builder.ViewBuilder.rfidReaderView;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.schaffbar.core_pos.domain.rfid_reader.RfidReaderService;
import de.schaffbar.core_pos.domain.rfid_reader.RfidReaderType;
import de.schaffbar.core_pos.domain.tool.ToolService;
import de.schaffbar.core_pos.shared.exception.InvalidRfidReaderTypeForToolException;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException.Resource;
import de.schaffbar.core_pos.shared.id.RfidReaderId;
import de.schaffbar.core_pos.shared.id.ToolId;

@ExtendWith(MockitoExtension.class)
class ToolAssignRfidReaderTest {

    @Mock
    private RfidReaderService rfidReaderService;

    @Mock
    private ToolService toolService;

    @InjectMocks
    private ToolAssignRfidReader toolAssignRfidReader;

    @Test
    void assignsASwitchBox() {
        // given
        ToolId toolId = ToolId.random();
        RfidReaderId rfidReaderId = givenReaderOfType(RfidReaderType.SWITCH_BOX);

        // when
        this.toolAssignRfidReader.process(toolId, rfidReaderId);

        // then
        expectAssigned(toolId, rfidReaderId);
    }

    @Test
    void rejectsAReaderOfAnotherType() {
        // given
        RfidReaderId rfidReaderId = givenReaderOfType(RfidReaderType.GATE_KEEPER_IN);

        // when
        Throwable thrown = catchThrowable(() -> this.toolAssignRfidReader.process(ToolId.random(), rfidReaderId));

        // then
        assertThat(thrown) //
                .isInstanceOf(InvalidRfidReaderTypeForToolException.class) //
                .hasFieldOrPropertyWithValue("code", InvalidRfidReaderTypeForToolException.CODE);
        expectNotAssigned();
    }

    @Test
    void rejectsAReaderWithoutType() {
        // given
        RfidReaderId rfidReaderId = givenReaderOfType(null);

        // when
        Throwable thrown = catchThrowable(() -> this.toolAssignRfidReader.process(ToolId.random(), rfidReaderId));

        // then
        assertThat(thrown) //
                .isInstanceOf(InvalidRfidReaderTypeForToolException.class) //
                .hasFieldOrPropertyWithValue("code", InvalidRfidReaderTypeForToolException.CODE);
        expectNotAssigned();
    }

    @Test
    void rejectsAnUnknownReader() {
        // given
        RfidReaderId unknown = givenUnknownReader();

        // when
        Throwable thrown = catchThrowable(() -> this.toolAssignRfidReader.process(ToolId.random(), unknown));

        // then
assertThat(thrown) //
                .isInstanceOf(ResourceNotFoundException.class) //
                .hasFieldOrPropertyWithValue("resource", Resource.RFID_READER);
        expectNotAssigned();
    }

    // ------------------------------------------------------------------------
    // given

    private RfidReaderId givenReaderOfType(RfidReaderType type) {
        RfidReaderId rfidReaderId = RfidReaderId.random();
        when(this.rfidReaderService.getRfidReader(rfidReaderId)).thenReturn(Optional.of(rfidReaderView(rfidReaderId).type(type).build()));

        return rfidReaderId;
    }

    private RfidReaderId givenUnknownReader() {
        RfidReaderId unknown = RfidReaderId.random();
        when(this.rfidReaderService.getRfidReader(unknown)).thenReturn(Optional.empty());

        return unknown;
    }

    private void expectAssigned(ToolId toolId, RfidReaderId rfidReaderId) {
        verify(this.toolService).assignRfidReader(toolId, rfidReaderId);
    }

    private void expectNotAssigned() {
        verifyNoInteractions(this.toolService);
    }

}

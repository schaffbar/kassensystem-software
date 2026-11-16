package de.schaffbar.core_pos.use_case;

import static de.schaffbar.core_pos.builder.ViewBuilder.toolView;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.schaffbar.core_pos.domain.rfid_reader.RfidReaderService;
import de.schaffbar.core_pos.domain.tool.ToolService;
import de.schaffbar.core_pos.shared.exception.RfidReaderHasToolAssignedException;
import de.schaffbar.core_pos.shared.id.RfidReaderId;

@ExtendWith(MockitoExtension.class)
class RfidReaderDeleteTest {

    @Mock
    private RfidReaderService rfidReaderService;

    @Mock
    private ToolService toolService;

    @InjectMocks
    private RfidReaderDelete rfidReaderDelete;

    @Test
    void deletesReaderWithoutTool() {
        // given
        RfidReaderId rfidReaderId = RfidReaderId.random();
        givenNoToolWith(rfidReaderId);

        // when
        this.rfidReaderDelete.process(rfidReaderId);

        // then
        verify(this.rfidReaderService).deleteRfidReader(rfidReaderId);
    }

    @Test
    void rejectsReaderAssignedToTool() {
        // given
        RfidReaderId rfidReaderId = RfidReaderId.random();
        givenToolWith(rfidReaderId);

        // when
        Throwable thrown = catchThrowable(() -> this.rfidReaderDelete.process(rfidReaderId));

        // then
        assertThat(thrown) //
                .isInstanceOf(RfidReaderHasToolAssignedException.class) //
                .hasFieldOrPropertyWithValue("code", RfidReaderHasToolAssignedException.CODE);
        verify(this.rfidReaderService, never()).deleteRfidReader(any());
    }

    // ------------------------------------------------------------------------
    // given

    private void givenToolWith(RfidReaderId rfidReaderId) {
        when(this.toolService.getTool(rfidReaderId)).thenReturn(Optional.of(toolView().rfidReaderId(rfidReaderId).build()));
    }

    private void givenNoToolWith(RfidReaderId rfidReaderId) {
        when(this.toolService.getTool(rfidReaderId)).thenReturn(Optional.empty());
    }

}

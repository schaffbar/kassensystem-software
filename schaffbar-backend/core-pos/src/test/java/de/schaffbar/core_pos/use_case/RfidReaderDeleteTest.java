package de.schaffbar.core_pos.use_case;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
import de.schaffbar.core_pos.domain.tool.ToolViews.ToolView;
import de.schaffbar.core_pos.shared.exception.RfidReaderHasToolAssignedException;
import de.schaffbar.core_pos.shared.id.RfidReaderId;

@ExtendWith(MockitoExtension.class)
class RfidReaderDeleteTest {

    private final RfidReaderId rfidReaderId = RfidReaderId.random();

    @Mock
    private RfidReaderService rfidReaderService;

    @Mock
    private ToolService toolService;

    @InjectMocks
    private RfidReaderDelete rfidReaderDelete;

    @Test
    void deletesReaderWithoutTool() {
        when(this.toolService.getTool(this.rfidReaderId)).thenReturn(Optional.empty());

        this.rfidReaderDelete.process(this.rfidReaderId);

        verify(this.rfidReaderService).deleteRfidReader(this.rfidReaderId);
    }

    @Test
    void rejectsReaderAssignedToTool() {
        when(this.toolService.getTool(this.rfidReaderId)).thenReturn(Optional.of(mock(ToolView.class)));

        assertThatThrownBy(() -> this.rfidReaderDelete.process(this.rfidReaderId)) //
                .isInstanceOf(RfidReaderHasToolAssignedException.class);

        verify(this.rfidReaderService, never()).deleteRfidReader(any());
    }

}

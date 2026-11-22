package de.schaffbar.core_pos.use_case;

import static de.schaffbar.core_pos.builder.ViewBuilder.activeToolUsageView;
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

import de.schaffbar.core_pos.domain.tool.ToolService;
import de.schaffbar.core_pos.domain.tool_usage.ToolUsageService;
import de.schaffbar.core_pos.shared.exception.ToolInUseException;
import de.schaffbar.core_pos.shared.id.ToolId;

@ExtendWith(MockitoExtension.class)
class ToolClearRfidReaderTest {

    @Mock
    private ToolService toolService;

    @Mock
    private ToolUsageService toolUsageService;

    @InjectMocks
    private ToolClearRfidReader toolClearRfidReader;

    @Test
    void clearsTheSwitchBoxOfAToolNobodyUses() {
        // given
        ToolId toolId = givenToolNobodyUses();

        // when
        this.toolClearRfidReader.process(toolId);

        // then
        expectCleared(toolId);
    }

    @Test
    void rejectsAToolInUse() {
        // given
        ToolId toolId = givenToolInUse();

        // when
        Throwable thrown = catchThrowable(() -> this.toolClearRfidReader.process(toolId));

        // then
        assertThat(thrown) //
                .isInstanceOf(ToolInUseException.class) //
                .hasFieldOrPropertyWithValue("code", ToolInUseException.CODE);
        expectNotCleared();
    }

    // ------------------------------------------------------------------------
    // given

    private ToolId givenToolNobodyUses() {
        ToolId toolId = ToolId.random();
        when(this.toolUsageService.getActiveToolUsage(toolId)).thenReturn(Optional.empty());

        return toolId;
    }

    private ToolId givenToolInUse() {
        ToolId toolId = ToolId.random();
        when(this.toolUsageService.getActiveToolUsage(toolId)).thenReturn(Optional.of(activeToolUsageView(toolId).build()));

        return toolId;
    }

    private void expectCleared(ToolId toolId) {
        verify(this.toolService).clearRfidReader(toolId);
    }

    private void expectNotCleared() {
        verify(this.toolService, never()).clearRfidReader(any());
    }

}

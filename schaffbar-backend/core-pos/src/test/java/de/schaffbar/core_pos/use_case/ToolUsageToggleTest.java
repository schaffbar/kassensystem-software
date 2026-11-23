package de.schaffbar.core_pos.use_case;

import static de.schaffbar.core_pos.builder.ViewBuilder.runningToolUsageView;
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

import de.schaffbar.core_pos.domain.tool_usage.ToolUsageService;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.ToolId;

@ExtendWith(MockitoExtension.class)
class ToolUsageToggleTest {

    @Mock
    private ToolUsageService toolUsageService;

    @Mock
    private ToolUsageStart toolUsageStart;

    @Mock
    private ToolUsageStop toolUsageStop;

    @InjectMocks
    private ToolUsageToggle toolUsageToggle;

    @Test
    void switchesTheToolOnWhenTheCustomerDoesNotUseIt() {
        // given
        CustomerId customerId = CustomerId.random();
        ToolId toolId = ToolId.random();
        givenToolNotInUseBy(customerId, toolId);

        // when
        this.toolUsageToggle.process(customerId, toolId);

        // then
        expectSwitchedOn(customerId, toolId);
    }

    @Test
    void switchesTheToolOffWhenTheCustomerUsesIt() {
        // given
        CustomerId customerId = CustomerId.random();
        ToolId toolId = ToolId.random();
        givenToolInUseBy(customerId, toolId);

        // when
        this.toolUsageToggle.process(customerId, toolId);

        // then
        expectSwitchedOff(customerId, toolId);
    }

    // ------------------------------------------------------------------------
    // given

    private void givenToolNotInUseBy(CustomerId customerId, ToolId toolId) {
        when(this.toolUsageService.getRunningToolUsage(customerId, toolId)).thenReturn(Optional.empty());
    }

    private void givenToolInUseBy(CustomerId customerId, ToolId toolId) {
        when(this.toolUsageService.getRunningToolUsage(customerId, toolId)) //
                .thenReturn(Optional.of(runningToolUsageView(toolId).customerId(customerId).build()));
    }

    // ------------------------------------------------------------------------
    // then

    private void expectSwitchedOn(CustomerId customerId, ToolId toolId) {
        verify(this.toolUsageStart).process(customerId, toolId);
        verify(this.toolUsageStop, never()).process(any(), any());
    }

    private void expectSwitchedOff(CustomerId customerId, ToolId toolId) {
        verify(this.toolUsageStop).process(customerId, toolId);
        verify(this.toolUsageStart, never()).process(any(), any());
    }

}

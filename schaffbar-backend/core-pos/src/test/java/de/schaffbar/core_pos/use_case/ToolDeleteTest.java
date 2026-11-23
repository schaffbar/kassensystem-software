package de.schaffbar.core_pos.use_case;

import static de.schaffbar.core_pos.builder.ViewBuilder.runningToolUsageView;
import static de.schaffbar.core_pos.builder.ViewBuilder.toolView;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.schaffbar.core_pos.domain.tool.ToolService;
import de.schaffbar.core_pos.domain.tool_certification.ToolCertificationService;
import de.schaffbar.core_pos.domain.tool_usage.ToolUsageService;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException.Resource;
import de.schaffbar.core_pos.shared.exception.ToolInUseException;
import de.schaffbar.core_pos.shared.id.ToolId;

@ExtendWith(MockitoExtension.class)
class ToolDeleteTest {

    @Mock
    private ToolService toolService;

    @Mock
    private ToolUsageService toolUsageService;

    @Mock
    private ToolCertificationService toolCertificationService;

    @InjectMocks
    private ToolDelete toolDelete;

    @Test
    void deletesTheCertificationsAndThenTheTool() {
        // given
        ToolId toolId = givenToolNobodyUses();

        // when
        this.toolDelete.process(toolId);

        // then
        expectCertificationsDeletedBeforeTheTool(toolId);
    }

    @Test
    void rejectsAToolInUse() {
        // given
        ToolId toolId = givenToolInUse();

        // when
        Throwable thrown = catchThrowable(() -> this.toolDelete.process(toolId));

        // then
        assertThat(thrown) //
                .isInstanceOf(ToolInUseException.class) //
                .hasFieldOrPropertyWithValue("code", ToolInUseException.CODE);
        expectNothingDeleted();
    }

    @Test
    void rejectsAnUnknownTool() {
        // given
        ToolId unknown = givenUnknownTool();

        // when
        Throwable thrown = catchThrowable(() -> this.toolDelete.process(unknown));

        // then
assertThat(thrown) //
                .isInstanceOf(ResourceNotFoundException.class) //
                .hasFieldOrPropertyWithValue("resource", Resource.TOOL);
        expectNothingDeleted();
    }

    // ------------------------------------------------------------------------
    // given

    private ToolId givenTool() {
        ToolId toolId = ToolId.random();
        when(this.toolService.getTool(toolId)).thenReturn(Optional.of(toolView(toolId).build()));

        return toolId;
    }

    private ToolId givenToolNobodyUses() {
        ToolId toolId = givenTool();
        when(this.toolUsageService.getRunningToolUsage(toolId)).thenReturn(Optional.empty());

        return toolId;
    }

    private ToolId givenToolInUse() {
        ToolId toolId = givenTool();
        when(this.toolUsageService.getRunningToolUsage(toolId)).thenReturn(Optional.of(runningToolUsageView(toolId).build()));

        return toolId;
    }

    private ToolId givenUnknownTool() {
        ToolId unknown = ToolId.random();
        when(this.toolService.getTool(unknown)).thenReturn(Optional.empty());

        return unknown;
    }

    // ------------------------------------------------------------------------
    // then

    private void expectCertificationsDeletedBeforeTheTool(ToolId toolId) {
        InOrder order = inOrder(this.toolCertificationService, this.toolService);
        order.verify(this.toolCertificationService).deleteToolCertifications(toolId);
        order.verify(this.toolService).deleteTool(toolId);
    }

    private void expectNothingDeleted() {
        verifyNoInteractions(this.toolCertificationService);
        verify(this.toolService, never()).deleteTool(any());
    }

}

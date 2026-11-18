package de.schaffbar.core_pos.use_case;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import de.schaffbar.core_pos.domain.tool.CertificationRequirement;
import de.schaffbar.core_pos.domain.tool.ToolService;
import de.schaffbar.core_pos.domain.tool.ToolViews.ToolView;
import de.schaffbar.core_pos.domain.tool_certification.ToolCertificationService;
import de.schaffbar.core_pos.domain.tool_usage.ToolUsageCommands.StartToolUsageCommand;
import de.schaffbar.core_pos.domain.tool_usage.ToolUsageService;
import de.schaffbar.core_pos.domain.workshop_session.WorkshopSessionService;
import de.schaffbar.core_pos.domain.workshop_session.WorkshopSessionViews.WorkshopSessionView;
import de.schaffbar.core_pos.domain.workshop_usage.WorkshopUsageService;
import de.schaffbar.core_pos.domain.workshop_usage.WorkshopUsageViews.WorkshopUsageView;
import de.schaffbar.core_pos.shared.exception.CustomerNotCertifiedForToolException;
import de.schaffbar.core_pos.shared.exception.CustomerNotInWorkshopException;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.ToolId;
import de.schaffbar.core_pos.shared.id.WorkshopSessionId;

@Service
@Validated
@RequiredArgsConstructor
public class StartToolUsage {

    private final @NonNull ToolService toolService;

    private final @NonNull ToolUsageService toolUsageService;

    private final @NonNull ToolCertificationService toolCertificationService;

    private final @NonNull WorkshopSessionService workshopSessionService;

    private final @NonNull WorkshopUsageService workshopUsageService;

    @Transactional
    public void process(@NotNull @Valid CustomerId customerId, @NotNull @Valid ToolId toolId) {
        ToolView tool = this.toolService.getTool(toolId) //
                .orElseThrow(() -> ResourceNotFoundException.tool(toolId));

        ensureCustomerIsInWorkshop(customerId);

        if (tool.certificationRequirement() == CertificationRequirement.RED) {
            ensureCustomerHasActiveCertification(customerId, toolId);
        }

        WorkshopSessionId sessionId = this.workshopSessionService.getOpenWorkshopSession(customerId) //
                .map(WorkshopSessionView::id) //
                .orElseThrow(() -> new CustomerNotInWorkshopException(customerId));

        StartToolUsageCommand command = new StartToolUsageCommand(customerId, toolId, sessionId);
        this.toolUsageService.startToolUsage(command);
    }

    // ------------------------------------------------------------------------
    // helper

    private void ensureCustomerIsInWorkshop(CustomerId customerId) {
        boolean isInWorkshop = this.workshopUsageService.getActiveWorkshopUsages().stream() //
                .map(WorkshopUsageView::customerId) //
                .anyMatch(id -> id.sameValueAs(customerId));

        if (!isInWorkshop) {
            throw new CustomerNotInWorkshopException(customerId);
        }
    }

    private void ensureCustomerHasActiveCertification(CustomerId customerId, ToolId toolId) {
        if (!this.toolCertificationService.hasActiveToolCertification(customerId, toolId)) {
            throw new CustomerNotCertifiedForToolException(customerId, toolId);
        }
    }

}

package de.schaffbar.core_pos.use_case;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import de.schaffbar.core_pos.domain.customer.CustomerService;
import de.schaffbar.core_pos.domain.tool.CertificationRequirement;
import de.schaffbar.core_pos.domain.tool.ToolService;
import de.schaffbar.core_pos.domain.tool.ToolViews.ToolView;
import de.schaffbar.core_pos.domain.tool_certification.ToolCertificationService;
import de.schaffbar.core_pos.domain.tool_usage.ToolUsageCommands.StartToolUsageCommand;
import de.schaffbar.core_pos.domain.tool_usage.ToolUsageService;
import de.schaffbar.core_pos.domain.workshop_usage.WorkshopUsageService;
import de.schaffbar.core_pos.domain.workshop_usage.WorkshopUsageViews.WorkshopUsageView;
import de.schaffbar.core_pos.shared.cqrs.Command;
import de.schaffbar.core_pos.shared.exception.CustomerNotCertifiedForToolException;
import de.schaffbar.core_pos.shared.exception.CustomerNotInWorkshopException;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.ToolId;
import de.schaffbar.core_pos.shared.id.WorkshopSessionId;

@Service
@Validated
@RequiredArgsConstructor
public class ToolUsageStart {

    private final @NonNull CustomerService customerService;

    private final @NonNull ToolService toolService;

    private final @NonNull WorkshopUsageService workshopUsageService;

    private final @NonNull ToolCertificationService toolCertificationService;

    private final @NonNull ToolUsageService toolUsageService;

    @Command
    public void process(@NotNull @Valid CustomerId customerId, @NotNull @Valid ToolId toolId) {
        this.customerService.getCustomer(customerId) //
                .orElseThrow(() -> ResourceNotFoundException.customer(customerId));
        ToolView tool = this.toolService.getTool(toolId) //
                .orElseThrow(() -> ResourceNotFoundException.tool(toolId));

        ensureCertifiedIfRequired(customerId, tool);

        // TU-2 – the running workshop usage belongs to the open session: a session with one is not closed (WS-5)
        WorkshopSessionId workshopSessionId = this.workshopUsageService.getActiveWorkshopUsage(customerId) //
                .map(WorkshopUsageView::workshopSessionId) //
                .orElseThrow(() -> new CustomerNotInWorkshopException(customerId));

        this.toolUsageService.startToolUsage(new StartToolUsageCommand(customerId, toolId, workshopSessionId));
    }

    // ------------------------------------------------------------------------
    // helper

    // TU-10 – only RED requires a certification, YELLOW only recommends one
    private void ensureCertifiedIfRequired(CustomerId customerId, ToolView tool) {
        if (tool.certificationRequirement() != CertificationRequirement.RED) {
            return;
        }

        if (!this.toolCertificationService.hasActiveToolCertification(customerId, tool.id())) {
            throw new CustomerNotCertifiedForToolException(customerId, tool.id());
        }
    }

}

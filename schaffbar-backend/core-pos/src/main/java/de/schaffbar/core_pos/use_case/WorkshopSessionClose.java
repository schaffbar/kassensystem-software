package de.schaffbar.core_pos.use_case;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import de.schaffbar.core_pos.domain.customer.CustomerService;
import de.schaffbar.core_pos.domain.tool_usage.ToolUsageService;
import de.schaffbar.core_pos.domain.workshop_session.WorkshopSessionCommands.CloseWorkshopSessionCommand;
import de.schaffbar.core_pos.domain.workshop_session.WorkshopSessionService;
import de.schaffbar.core_pos.domain.workshop_session.WorkshopSessionViews.WorkshopSessionView;
import de.schaffbar.core_pos.domain.workshop_usage.WorkshopUsageService;
import de.schaffbar.core_pos.shared.billing.BillingTotal;
import de.schaffbar.core_pos.shared.cqrs.Command;
import de.schaffbar.core_pos.shared.exception.NoOpenWorkshopSessionException;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;
import de.schaffbar.core_pos.shared.exception.RunningToolUsageExistsForSessionException;
import de.schaffbar.core_pos.shared.exception.RunningWorkshopUsageExistsForSessionException;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.WorkshopSessionId;

@Service
@Validated
@RequiredArgsConstructor
public class WorkshopSessionClose {

    private final @NonNull CustomerService customerService;

    private final @NonNull WorkshopSessionService workshopSessionService;

    private final @NonNull WorkshopUsageService workshopUsageService;

    private final @NonNull ToolUsageService toolUsageService;

    @Command
    public void process(@NotNull @Valid CustomerId customerId) {
        this.customerService.getCustomer(customerId) //
                .orElseThrow(() -> ResourceNotFoundException.customer(customerId));

        // WS-4 – the session is found by its state, the service closes it by its id (E29)
        WorkshopSessionId workshopSessionId = this.workshopSessionService.getOpenWorkshopSession(customerId) //
                .map(WorkshopSessionView::id) //
                .orElseThrow(() -> new NoOpenWorkshopSessionException(customerId));

        ensureNoWorkshopUsageRunning(workshopSessionId);
        ensureNoToolUsageRunning(workshopSessionId);

        BillingTotal workshopTotal = this.workshopUsageService.getBillingTotal(workshopSessionId);
        BillingTotal toolTotal = this.toolUsageService.getBillingTotal(workshopSessionId);

        this.workshopSessionService.closeWorkshopSession(new CloseWorkshopSessionCommand(workshopSessionId, workshopTotal, toolTotal));
    }

    // ------------------------------------------------------------------------
    // helper

    // WS-5 – checked before the tools: checking out stops them too (WU-5), so it is the one thing left to do
    private void ensureNoWorkshopUsageRunning(WorkshopSessionId workshopSessionId) {
        if (this.workshopUsageService.hasRunningWorkshopUsage(workshopSessionId)) {
            throw new RunningWorkshopUsageExistsForSessionException(workshopSessionId);
        }
    }

    // WS-5
    private void ensureNoToolUsageRunning(WorkshopSessionId workshopSessionId) {
        if (this.toolUsageService.hasRunningToolUsage(workshopSessionId)) {
            throw new RunningToolUsageExistsForSessionException(workshopSessionId);
        }
    }

}

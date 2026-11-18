package de.schaffbar.core_pos.use_case;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import de.schaffbar.core_pos.domain.customer.CustomerService;
import de.schaffbar.core_pos.domain.tool_usage.ToolUsageService;
import de.schaffbar.core_pos.domain.workshop_session.WorkshopSessionService;
import de.schaffbar.core_pos.domain.workshop_usage.WorkshopUsageService;
import de.schaffbar.core_pos.shared.cqrs.Command;
import de.schaffbar.core_pos.shared.exception.NoOpenWorkshopSessionException;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;
import de.schaffbar.core_pos.shared.id.CustomerId;

@Service
@Validated
@RequiredArgsConstructor
public class WorkshopUsageStop {

    private final @NonNull CustomerService customerService;

    private final @NonNull WorkshopSessionService workshopSessionService;

    private final @NonNull WorkshopUsageService workshopUsageService;

    private final @NonNull ToolUsageService toolUsageService;

    @Command
    public void process(@NotNull @Valid CustomerId customerId) {
        this.customerService.getCustomer(customerId) //
                .orElseThrow(() -> ResourceNotFoundException.customer(customerId));

        ensureOpenWorkshopSession(customerId);

        // the tools switch off when the person leaves (WU-5, TU-4); a rejected check-out undoes it, it is one transaction
        this.toolUsageService.stopToolUsages(customerId);
        this.workshopUsageService.stopWorkshopUsage(customerId);
    }

    // ------------------------------------------------------------------------
    // helper

    // tells "never checked in" apart from "already checked out" (NoActiveWorkshopUsageFoundException)
    private void ensureOpenWorkshopSession(CustomerId customerId) {
        if (this.workshopSessionService.getOpenWorkshopSession(customerId).isEmpty()) {
            throw new NoOpenWorkshopSessionException(customerId);
        }
    }

}

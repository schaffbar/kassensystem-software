package de.schaffbar.core_pos.use_case;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import de.schaffbar.core_pos.domain.customer.CustomerService;
import de.schaffbar.core_pos.domain.tool.ToolService;
import de.schaffbar.core_pos.shared.cqrs.Command;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.ToolId;

@Service
@Validated
@RequiredArgsConstructor
public class ToolRemoveInstructors {

    private final @NonNull ToolService toolService;

    private final @NonNull CustomerService customerService;

    @Command
    public void process(@NotNull @Valid ToolId toolId, @NotEmpty List<@NotNull @Valid CustomerId> instructorIds) {
        instructorIds.forEach(this::ensureCustomerExists);

        this.toolService.removeInstructors(toolId, instructorIds);
    }

    // ------------------------------------------------------------------------
    // helper

    // T-22
    private void ensureCustomerExists(CustomerId customerId) {
        this.customerService.getCustomer(customerId) //
                .orElseThrow(() -> ResourceNotFoundException.customer(customerId));
    }

}

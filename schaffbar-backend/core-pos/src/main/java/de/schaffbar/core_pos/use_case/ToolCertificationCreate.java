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
import de.schaffbar.core_pos.domain.tool.ToolViews.ToolView;
import de.schaffbar.core_pos.domain.tool_certification.ToolCertificationCommands.CreateToolCertificationCommand;
import de.schaffbar.core_pos.domain.tool_certification.ToolCertificationService;
import de.schaffbar.core_pos.shared.cqrs.Command;
import de.schaffbar.core_pos.shared.exception.CustomerIsInstructorException;
import de.schaffbar.core_pos.shared.exception.CustomerNotInstructorException;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.ToolId;

// all or nothing: one rejected customer rolls back the certifications of the others (TC-4)
@Service
@Validated
@RequiredArgsConstructor
public class ToolCertificationCreate {

    private final @NonNull ToolCertificationService toolCertificationService;

    private final @NonNull CustomerService customerService;

    private final @NonNull ToolService toolService;

    @Command
    public void process( //
            @NotEmpty List<@NotNull @Valid CustomerId> customerIds, //
            @NotNull @Valid ToolId toolId, //
            @NotNull @Valid CustomerId certifiedBy //
    ) {
        ToolView tool = this.toolService.getTool(toolId) //
                .orElseThrow(() -> ResourceNotFoundException.tool(toolId));
        customerIds.forEach(this::ensureCustomerExists);
        ensureIsInstructor(tool, certifiedBy);
        customerIds.forEach(customerId -> ensureIsNoInstructor(tool, customerId));

        customerIds.forEach(customerId -> this.toolCertificationService
                .createToolCertification(new CreateToolCertificationCommand(customerId, toolId, certifiedBy)));
    }

    // ------------------------------------------------------------------------
    // helper

    // only an instructor of the tool certifies for it (TC-8)
    private void ensureIsInstructor(ToolView tool, CustomerId certifiedBy) {
        if (!tool.instructors().contains(certifiedBy)) {
            throw new CustomerNotInstructorException(certifiedBy, tool.id());
        }
    }

    // a referenced aggregate exists – all entries of the batch before any rule (TC-4)
    private void ensureCustomerExists(CustomerId customerId) {
        this.customerService.getCustomer(customerId) //
                .orElseThrow(() -> ResourceNotFoundException.customer(customerId));
    }

    // instructors need no certification for their own tool (TC-5)
    private void ensureIsNoInstructor(ToolView tool, CustomerId customerId) {
        if (tool.instructors().contains(customerId)) {
            throw new CustomerIsInstructorException(customerId, tool.id());
        }
    }

}

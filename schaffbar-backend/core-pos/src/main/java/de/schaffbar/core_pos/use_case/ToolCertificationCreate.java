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
        verifyIsInstructor(tool, certifiedBy);
        customerIds.forEach(customerId -> verifyCanBeCertified(tool, customerId));

        customerIds.forEach(customerId -> this.toolCertificationService
                .createToolCertification(new CreateToolCertificationCommand(customerId, toolId, certifiedBy)));
    }

    // ------------------------------------------------------------------------
    // helper

    private void verifyIsInstructor(ToolView tool, CustomerId certifiedBy) {
        if (!tool.instructors().contains(certifiedBy)) {
            throw new CustomerNotInstructorException(certifiedBy, tool.id());
        }
    }

    // instructors need no certification for their own tool (TC-5)
    private void verifyCanBeCertified(ToolView tool, CustomerId customerId) {
        this.customerService.getCustomer(customerId) //
                .orElseThrow(() -> ResourceNotFoundException.customer(customerId));

        if (tool.instructors().contains(customerId)) {
            throw new CustomerIsInstructorException(customerId, tool.id());
        }
    }

}

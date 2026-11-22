package de.schaffbar.core_pos.use_case;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import de.schaffbar.core_pos.domain.customer.CustomerService;
import de.schaffbar.core_pos.domain.rfid_tag_assignment.RfidTagAssignmentService;
import de.schaffbar.core_pos.shared.cqrs.Command;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;
import de.schaffbar.core_pos.shared.id.CustomerId;

@Service
@Validated
@RequiredArgsConstructor
public class RfidTagAssignmentUnassignRfidTag {

    private final @NonNull CustomerService customerService;

    private final @NonNull RfidTagAssignmentService rfidTagAssignmentService;

    @Command
    public void process(@NotNull @Valid CustomerId customerId) {
        this.customerService.getCustomer(customerId) //
                .orElseThrow(() -> ResourceNotFoundException.customer(customerId));

        this.rfidTagAssignmentService.unassignRfidTag(customerId);
    }

}

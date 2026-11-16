package de.schaffbar.core_pos.read_facade;

import java.util.List;
import java.util.Optional;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import de.schaffbar.core_pos.domain.customer.CustomerService;
import de.schaffbar.core_pos.domain.rfid_tag_assignment.RfidTagAssignmentService;
import de.schaffbar.core_pos.domain.rfid_tag_assignment.RfidTagAssignmentViews.RfidTagAssignmentView;
import de.schaffbar.core_pos.shared.cqrs.Query;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.RfidTagId;

@Service
@Validated
@RequiredArgsConstructor
public class RfidTagAssignmentReadFacade {

    private final @NonNull RfidTagAssignmentService rfidTagAssignmentService;

    private final @NonNull CustomerService customerService;

    @Query
    public List<RfidTagAssignmentView> getRfidTagAssignments() {
        return this.rfidTagAssignmentService.getRfidTagAssignments();
    }

    @Query
    public Optional<RfidTagAssignmentView> getRfidTagAssignment(@NotNull @Valid CustomerId customerId) {
        this.customerService.getCustomer(customerId) //
                .orElseThrow(() -> ResourceNotFoundException.customer(customerId));

        return this.rfidTagAssignmentService.getRfidTagAssignment(customerId);
    }

    @Query
    public Optional<RfidTagAssignmentView> getRfidTagAssignment(@NotNull @Valid RfidTagId rfidTagId) {
        return this.rfidTagAssignmentService.getRfidTagAssignment(rfidTagId);
    }

}

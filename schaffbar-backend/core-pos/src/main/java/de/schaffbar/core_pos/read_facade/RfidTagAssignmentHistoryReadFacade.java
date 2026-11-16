package de.schaffbar.core_pos.read_facade;

import java.util.List;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import de.schaffbar.core_pos.domain.rfid_tag_assignment.RfidTagAssignmentService;
import de.schaffbar.core_pos.domain.rfid_tag_assignment.RfidTagAssignmentViews.RfidTagAssignmentHistoryView;
import de.schaffbar.core_pos.shared.cqrs.Query;

@Service
@Validated
@RequiredArgsConstructor
public class RfidTagAssignmentHistoryReadFacade {

    private final @NonNull RfidTagAssignmentService rfidTagAssignmentService;

    @Query
    public List<RfidTagAssignmentHistoryView> getRfidTagAssignmentHistory() {
        return this.rfidTagAssignmentService.getRfidTagAssignmentHistory();
    }

}

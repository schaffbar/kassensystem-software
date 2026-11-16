package de.schaffbar.core_pos.domain.rfid_tag_assignment.web;

import static java.util.Objects.nonNull;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.schaffbar.core_pos.domain.rfid_tag_assignment.RfidTagAssignmentViews.RfidTagAssignmentView;
import de.schaffbar.core_pos.domain.rfid_tag_assignment.web.RfidTagAssignmentApiModel.AssignRfidTagRequestBody;
import de.schaffbar.core_pos.domain.rfid_tag_assignment.web.RfidTagAssignmentApiModel.RequestRfidTagAssignmentRequestBody;
import de.schaffbar.core_pos.domain.rfid_tag_assignment.web.RfidTagAssignmentApiModel.RfidTagAssignmentApiDto;
import de.schaffbar.core_pos.domain.rfid_tag_assignment.web.RfidTagAssignmentApiModel.UnassignRfidTagRequestBody;
import de.schaffbar.core_pos.read_facade.RfidTagAssignmentReadFacade;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.RfidTagId;
import de.schaffbar.core_pos.use_case.CustomerAssignRfidTag;
import de.schaffbar.core_pos.use_case.CustomerRequestRfidTagAssignment;
import de.schaffbar.core_pos.use_case.CustomerUnassignRfidTag;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/api/v1/rfid-tag-assignments")
public class RfidTagAssignmentController {

    private final @NonNull RfidTagAssignmentReadFacade rfidTagAssignmentReadFacade;

    private final @NonNull CustomerRequestRfidTagAssignment customerRequestRfidTagAssignment;

    private final @NonNull CustomerAssignRfidTag customerAssignRfidTag;

    private final @NonNull CustomerUnassignRfidTag customerUnassignRfidTag;

    // ------------------------------------------------------------------------
    // query

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<RfidTagAssignmentApiDto>> getRfidTagAssignments( //
            @RequestParam(value = "rfid-tag-id", required = false) @Valid RfidTagId rfidTagId, //
            @RequestParam(value = "customer-id", required = false) @Valid CustomerId customerId //
    ) {
        List<RfidTagAssignmentView> assignments;
        if (nonNull(rfidTagId)) {
            assignments = this.rfidTagAssignmentReadFacade.getRfidTagAssignment(rfidTagId).stream().toList();
        }
        else if (nonNull(customerId)) {
            assignments = this.rfidTagAssignmentReadFacade.getRfidTagAssignment(customerId).stream().toList();
        }
        else {
            assignments = this.rfidTagAssignmentReadFacade.getRfidTagAssignments();
        }

        return ResponseEntity.ok(assignments.stream() //
                .map(RfidTagAssignmentApiModel.MAPPER::toRfidTagAssignmentApiDto) //
                .toList());
    }

    // ------------------------------------------------------------------------
    // command

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> requestRfidTagAssignment(@RequestBody @NotNull @Valid RequestRfidTagAssignmentRequestBody requestBody) {
        this.customerRequestRfidTagAssignment.process(requestBody.customerId(), requestBody.assignmentType());

        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/assign", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> assignRfidTag(@RequestBody @NotNull @Valid AssignRfidTagRequestBody requestBody) {
        this.customerAssignRfidTag.process(requestBody.rfidTagId());

        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/unassign", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> unassignRfidTag(@RequestBody @NotNull @Valid UnassignRfidTagRequestBody requestBody) {
        this.customerUnassignRfidTag.process(requestBody.customerId());

        return ResponseEntity.noContent().build();
    }

}

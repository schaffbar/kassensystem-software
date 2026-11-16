package de.schaffbar.core_pos.domain.rfid_tag_assignment.web;

import java.util.List;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.schaffbar.core_pos.domain.rfid_tag_assignment.web.RfidTagAssignmentApiModel.RfidTagAssignmentHistoryApiDto;
import de.schaffbar.core_pos.read_facade.RfidTagAssignmentHistoryReadFacade;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/api/v1/rfid-tag-assignment-history")
public class RfidTagAssignmentHistoryController {

    private final @NonNull RfidTagAssignmentHistoryReadFacade rfidTagAssignmentHistoryReadFacade;

    // ------------------------------------------------------------------------
    // query

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<RfidTagAssignmentHistoryApiDto>> getRfidTagAssignmentHistory() {
        List<RfidTagAssignmentHistoryApiDto> result = this.rfidTagAssignmentHistoryReadFacade.getRfidTagAssignmentHistory().stream() //
                .map(RfidTagAssignmentApiModel.MAPPER::toRfidTagAssignmentHistoryApiDto) //
                .toList();

        return ResponseEntity.ok(result);
    }

}

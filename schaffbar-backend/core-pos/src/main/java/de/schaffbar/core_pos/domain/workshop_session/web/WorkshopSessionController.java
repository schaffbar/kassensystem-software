package de.schaffbar.core_pos.domain.workshop_session.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.schaffbar.core_pos.domain.workshop_session.web.WorkshopSessionApiModel.WorkshopSessionApiDto;
import de.schaffbar.core_pos.read_facade.WorkshopSessionReadFacade;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.use_case.WorkshopSessionClose;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/api/v1/workshop-sessions")
public class WorkshopSessionController {

    private final @NonNull WorkshopSessionReadFacade workshopSessionReadFacade;

    private final @NonNull WorkshopSessionClose workshopSessionClose;

    // ------------------------------------------------------------------------
    // query

    // the counter knows the person, not the session: the open session of the person
    @GetMapping(value = "/{customerId}/open", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WorkshopSessionApiDto> getOpenWorkshopSession(@PathVariable @NotNull @Valid CustomerId customerId) {
        // without an open session the body is empty
        WorkshopSessionApiDto result = this.workshopSessionReadFacade.getOpenWorkshopSession(customerId) //
                .map(WorkshopSessionApiModel.MAPPER::toWorkshopSessionApiDto) //
                .orElse(null);

        return ResponseEntity.ok(result);
    }

    // ------------------------------------------------------------------------
    // command

    // closes the open session of the person – like the query, by the person the counter knows
    @PutMapping(value = "/{customerId}/close")
    public ResponseEntity<Void> closeWorkshopSession(@PathVariable @NotNull @Valid CustomerId customerId) {
        this.workshopSessionClose.process(customerId);

        return ResponseEntity.noContent().build();
    }

}

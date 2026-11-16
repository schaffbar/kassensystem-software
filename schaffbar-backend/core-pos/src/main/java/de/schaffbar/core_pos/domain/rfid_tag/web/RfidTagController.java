package de.schaffbar.core_pos.domain.rfid_tag.web;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.schaffbar.core_pos.domain.rfid_tag.web.RfidTagApiModel.CreateRfidTagRequestBody;
import de.schaffbar.core_pos.domain.rfid_tag.web.RfidTagApiModel.RfidTagApiDto;
import de.schaffbar.core_pos.read_facade.RfidTagReadFacade;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;
import de.schaffbar.core_pos.shared.id.RfidTagId;
import de.schaffbar.core_pos.use_case.RfidTagCreate;
import de.schaffbar.core_pos.use_case.RfidTagDelete;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/api/v1/rfid-tags")
public class RfidTagController {

    private final @NonNull RfidTagReadFacade rfidTagReadFacade;

    private final @NonNull RfidTagCreate rfidTagCreate;

    private final @NonNull RfidTagDelete rfidTagDelete;

    // ------------------------------------------------------------------------
    // query

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<RfidTagApiDto>> getRfidTags() {
        List<RfidTagApiDto> rfidTags = this.rfidTagReadFacade.getRfidTags().stream() //
                .map(RfidTagApiModel.MAPPER::toRfidTagApiDto) //
                .toList();

        return ResponseEntity.ok(rfidTags);
    }

    @GetMapping(value = "/{rfidTagId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RfidTagApiDto> getRfidTag(@PathVariable @NotNull @Valid RfidTagId rfidTagId) {
        RfidTagApiDto rfidTag = this.rfidTagReadFacade.getRfidTag(rfidTagId) //
                .map(RfidTagApiModel.MAPPER::toRfidTagApiDto) //
                .orElseThrow(() -> ResourceNotFoundException.rfidTag(rfidTagId));

        return ResponseEntity.ok(rfidTag);
    }

    // ------------------------------------------------------------------------
    // command

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> createRfidTag(@RequestBody @NotNull @Valid CreateRfidTagRequestBody requestBody) {
        RfidTagId rfidTagId = this.rfidTagCreate.process(requestBody.rfidTagId());
        URI location = URI.create("/api/v1/rfid-tags/" + rfidTagId.getValue());

        return ResponseEntity.created(location).build();
    }

    @DeleteMapping(value = "/{rfidTagId}")
    public ResponseEntity<Void> deleteRfidTag(@PathVariable @NotNull @Valid RfidTagId rfidTagId) {
        this.rfidTagDelete.process(rfidTagId);

        return ResponseEntity.noContent().build();
    }

}

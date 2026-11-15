package de.schaffbar.core_pos.domain.rfid_reader.web;

import static java.util.Objects.nonNull;

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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.schaffbar.core_pos.domain.rfid_reader.RfidReaderCommands.ChangeRfidReaderTypeCommand;
import de.schaffbar.core_pos.domain.rfid_reader.RfidReaderCommands.UpdateRfidReaderCommand;
import de.schaffbar.core_pos.domain.rfid_reader.RfidReaderViews.RfidReaderView;
import de.schaffbar.core_pos.domain.rfid_reader.web.RfidReaderApiModel.ChangeRfidReaderTypeRequestBody;
import de.schaffbar.core_pos.domain.rfid_reader.web.RfidReaderApiModel.CreateRfidReaderRequestBody;
import de.schaffbar.core_pos.domain.rfid_reader.web.RfidReaderApiModel.RfidReaderApiDto;
import de.schaffbar.core_pos.domain.rfid_reader.web.RfidReaderApiModel.UpdateRfidReaderRequestBody;
import de.schaffbar.core_pos.read_facade.RfidReaderReadFacade;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;
import de.schaffbar.core_pos.shared.id.MacAddress;
import de.schaffbar.core_pos.shared.id.RfidReaderId;
import de.schaffbar.core_pos.use_case.RfidReaderChangeType;
import de.schaffbar.core_pos.use_case.RfidReaderCreate;
import de.schaffbar.core_pos.use_case.RfidReaderDelete;
import de.schaffbar.core_pos.use_case.RfidReaderUpdate;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/api/v1/rfid-readers")
public class RfidReaderController {

    private final @NonNull RfidReaderReadFacade rfidReaderReadFacade;

    private final @NonNull RfidReaderCreate rfidReaderCreate;

    private final @NonNull RfidReaderUpdate rfidReaderUpdate;

    private final @NonNull RfidReaderDelete rfidReaderDelete;

    private final @NonNull RfidReaderChangeType rfidReaderChangeType;

    // ------------------------------------------------------------------------
    // query

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<RfidReaderApiDto>> getRfidReaders(@RequestParam(required = false) @Valid MacAddress macAddress) {
        List<RfidReaderView> rfidReaders = nonNull(macAddress) //
                ? this.rfidReaderReadFacade.getRfidReader(macAddress).stream().toList() //
                : this.rfidReaderReadFacade.getRfidReaders();

        return ResponseEntity.ok(rfidReaders.stream() //
                .map(RfidReaderApiModel.MAPPER::toRfidReaderApiDto) //
                .toList());
    }

    @GetMapping(value = "/{rfidReaderId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RfidReaderApiDto> getRfidReader(@PathVariable @NotNull @Valid RfidReaderId rfidReaderId) {
        RfidReaderApiDto rfidReader = this.rfidReaderReadFacade.getRfidReader(rfidReaderId) //
                .map(RfidReaderApiModel.MAPPER::toRfidReaderApiDto) //
                .orElseThrow(() -> ResourceNotFoundException.rfidReader(rfidReaderId));

        return ResponseEntity.ok(rfidReader);
    }

    // ------------------------------------------------------------------------
    // command

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> createRfidReader(@RequestBody @NotNull @Valid CreateRfidReaderRequestBody requestBody) {
        RfidReaderId rfidReaderId = this.rfidReaderCreate.process(requestBody.macAddress());
        URI location = URI.create("/api/v1/rfid-readers/" + rfidReaderId.getValue());

        return ResponseEntity.created(location).build();
    }

    @PutMapping(value = "/{rfidReaderId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> updateRfidReader( //
            @PathVariable @NotNull @Valid RfidReaderId rfidReaderId, //
            @RequestBody @NotNull @Valid UpdateRfidReaderRequestBody requestBody //
    ) {
        UpdateRfidReaderCommand command = RfidReaderApiModel.MAPPER.toRfidReaderCommand(rfidReaderId, requestBody);
        this.rfidReaderUpdate.process(command);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping(value = "/{rfidReaderId}/type", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> changeRfidReaderType( //
            @PathVariable @NotNull @Valid RfidReaderId rfidReaderId, //
            @RequestBody @NotNull @Valid ChangeRfidReaderTypeRequestBody requestBody //
    ) {
        ChangeRfidReaderTypeCommand command = RfidReaderApiModel.MAPPER.toChangeRfidReaderTypeCommand(rfidReaderId, requestBody);
        this.rfidReaderChangeType.process(command);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping(value = "/{rfidReaderId}")
    public ResponseEntity<Void> deleteRfidReader(@PathVariable @NotNull @Valid RfidReaderId rfidReaderId) {
        this.rfidReaderDelete.process(rfidReaderId);

        return ResponseEntity.noContent().build();
    }

}

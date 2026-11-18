package de.schaffbar.core_pos.domain.tool.web;

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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.schaffbar.core_pos.domain.tool.ToolCommands.CreateToolCommand;
import de.schaffbar.core_pos.domain.tool.ToolCommands.SetWlanRelaisCommand;
import de.schaffbar.core_pos.domain.tool.ToolCommands.UpdateToolCommand;
import de.schaffbar.core_pos.domain.tool.ToolViews.ToolView;
import de.schaffbar.core_pos.domain.tool.web.ToolApiModel.CreateToolRequestBody;
import de.schaffbar.core_pos.domain.tool.web.ToolApiModel.InstructorsRequestBody;
import de.schaffbar.core_pos.domain.tool.web.ToolApiModel.SetWlanRelaisRequestBody;
import de.schaffbar.core_pos.domain.tool.web.ToolApiModel.ToolApiDto;
import de.schaffbar.core_pos.domain.tool.web.ToolApiModel.UpdateToolRequestBody;
import de.schaffbar.core_pos.read_facade.ToolReadFacade;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;
import de.schaffbar.core_pos.shared.id.RfidReaderId;
import de.schaffbar.core_pos.shared.id.ToolId;
import de.schaffbar.core_pos.use_case.ToolAddInstructors;
import de.schaffbar.core_pos.use_case.ToolAssignRfidReader;
import de.schaffbar.core_pos.use_case.ToolClearRfidReader;
import de.schaffbar.core_pos.use_case.ToolClearWlanRelais;
import de.schaffbar.core_pos.use_case.ToolCreate;
import de.schaffbar.core_pos.use_case.ToolDelete;
import de.schaffbar.core_pos.use_case.ToolRemoveInstructors;
import de.schaffbar.core_pos.use_case.ToolSetWlanRelais;
import de.schaffbar.core_pos.use_case.ToolUpdate;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/api/v1/tools")
public class ToolController {

    private final @NonNull ToolReadFacade toolReadFacade;

    private final @NonNull ToolCreate toolCreate;

    private final @NonNull ToolUpdate toolUpdate;

    private final @NonNull ToolAssignRfidReader toolAssignRfidReader;

    private final @NonNull ToolClearRfidReader toolClearRfidReader;

    private final @NonNull ToolSetWlanRelais toolSetWlanRelais;

    private final @NonNull ToolClearWlanRelais toolClearWlanRelais;

    private final @NonNull ToolAddInstructors toolAddInstructors;

    private final @NonNull ToolRemoveInstructors toolRemoveInstructors;

    private final @NonNull ToolDelete toolDelete;

    // ------------------------------------------------------------------------
    // query

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ToolApiDto>> getTools(@RequestParam(required = false) @Valid RfidReaderId rfidReaderId) {
        List<ToolView> tools = nonNull(rfidReaderId) //
                ? this.toolReadFacade.getTool(rfidReaderId).stream().toList() //
                : this.toolReadFacade.getTools();

        return ResponseEntity.ok(tools.stream() //
                .map(ToolApiModel.MAPPER::toToolApiDto) //
                .toList());
    }

    @GetMapping(value = "/{toolId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ToolApiDto> getTool(@PathVariable @NotNull @Valid ToolId toolId) {
        ToolApiDto tool = this.toolReadFacade.getTool(toolId) //
                .map(ToolApiModel.MAPPER::toToolApiDto) //
                .orElseThrow(() -> ResourceNotFoundException.tool(toolId));

        return ResponseEntity.ok(tool);
    }

    // ------------------------------------------------------------------------
    // command

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> createTool(@RequestBody @NotNull @Valid CreateToolRequestBody requestBody) {
        CreateToolCommand command = ToolApiModel.MAPPER.toCreateToolCommand(requestBody);
        ToolId toolId = this.toolCreate.process(command);
        URI location = URI.create("/api/v1/tools/" + toolId.getValue());

        return ResponseEntity.created(location).build();
    }

    @PutMapping(value = "/{toolId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> updateTool( //
            @PathVariable @NotNull @Valid ToolId toolId, //
            @RequestBody @NotNull @Valid UpdateToolRequestBody requestBody //
    ) {
        UpdateToolCommand command = ToolApiModel.MAPPER.toUpdateToolCommand(toolId, requestBody);
        this.toolUpdate.process(command);

        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/{toolId}/rfid-reader/{rfidReaderId}")
    public ResponseEntity<Void> assignRfidReader( //
            @PathVariable @NotNull @Valid ToolId toolId, //
            @PathVariable @NotNull @Valid RfidReaderId rfidReaderId //
    ) {
        this.toolAssignRfidReader.process(toolId, rfidReaderId);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping(value = "/{toolId}/rfid-reader")
    public ResponseEntity<Void> clearRfidReader(@PathVariable @NotNull @Valid ToolId toolId) {
        this.toolClearRfidReader.process(toolId);

        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/{toolId}/wlan-relais", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> setWlanRelais( //
            @PathVariable @NotNull @Valid ToolId toolId, //
            @RequestBody @NotNull @Valid SetWlanRelaisRequestBody requestBody //
    ) {
        SetWlanRelaisCommand command = ToolApiModel.MAPPER.toSetWlanRelaisCommand(toolId, requestBody);
        this.toolSetWlanRelais.process(command);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping(value = "/{toolId}/wlan-relais")
    public ResponseEntity<Void> clearWlanRelais(@PathVariable @NotNull @Valid ToolId toolId) {
        this.toolClearWlanRelais.process(toolId);

        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{toolId}/instructors", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> addInstructors( //
            @PathVariable @NotNull @Valid ToolId toolId, //
            @RequestBody @NotNull @Valid InstructorsRequestBody requestBody //
    ) {
        this.toolAddInstructors.process(toolId, requestBody.instructorIds());

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping(value = "/{toolId}/instructors", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> removeInstructors( //
            @PathVariable @NotNull @Valid ToolId toolId, //
            @RequestBody @NotNull @Valid InstructorsRequestBody requestBody //
    ) {
        this.toolRemoveInstructors.process(toolId, requestBody.instructorIds());

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping(value = "/{toolId}")
    public ResponseEntity<Void> deleteTool(@PathVariable @NotNull @Valid ToolId toolId) {
        this.toolDelete.process(toolId);

        return ResponseEntity.noContent().build();
    }

}

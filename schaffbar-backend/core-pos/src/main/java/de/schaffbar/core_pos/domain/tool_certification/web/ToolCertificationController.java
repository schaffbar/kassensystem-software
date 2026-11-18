package de.schaffbar.core_pos.domain.tool_certification.web;

import static java.util.Objects.nonNull;

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

import de.schaffbar.core_pos.domain.tool_certification.BatchCertificationResult;
import de.schaffbar.core_pos.domain.tool_certification.ToolCertificationViews.ToolCertificationView;
import de.schaffbar.core_pos.domain.tool_certification.web.ToolCertificationApiModel.BatchCertifyRequestBody;
import de.schaffbar.core_pos.domain.tool_certification.web.ToolCertificationApiModel.ToolCertificationApiDto;
import de.schaffbar.core_pos.read_facade.ToolCertificationReadFacade;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.ToolCertificationId;
import de.schaffbar.core_pos.shared.id.ToolId;
import de.schaffbar.core_pos.use_case.CertifyCustomersForTool;
import de.schaffbar.core_pos.use_case.DeleteToolCertification;
import de.schaffbar.core_pos.use_case.PauseToolCertification;
import de.schaffbar.core_pos.use_case.ReactivateToolCertification;
import de.schaffbar.core_pos.use_case.RevokeToolCertification;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/api/v1/tool-certifications")
public class ToolCertificationController {

    private final @NonNull ToolCertificationReadFacade toolCertificationReadFacade;

    private final @NonNull CertifyCustomersForTool certifyCustomersForTool;

    private final @NonNull PauseToolCertification pauseToolCertification;

    private final @NonNull ReactivateToolCertification reactivateToolCertification;

    private final @NonNull RevokeToolCertification revokeToolCertification;

    private final @NonNull DeleteToolCertification deleteToolCertification;

    // ------------------------------------------------------------------------
    // query

    @GetMapping(value = "/{toolCertificationId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ToolCertificationApiDto> getToolCertification(@PathVariable @NotNull @Valid ToolCertificationId toolCertificationId) {
        ToolCertificationApiDto toolCertification = this.toolCertificationReadFacade.getToolCertification(toolCertificationId) //
                .map(ToolCertificationApiModel.MAPPER::toToolCertificationApiDto) //
                .orElseThrow(() -> ResourceNotFoundException.toolCertification(toolCertificationId));

        return ResponseEntity.ok(toolCertification);
    }

    // one of the filters is required; with both, customerId wins
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ToolCertificationApiDto>> getToolCertifications(@RequestParam(required = false) @Valid CustomerId customerId,
            @RequestParam(required = false) @Valid ToolId toolId) {

        List<ToolCertificationView> toolCertifications;
        if (nonNull(customerId)) {
            toolCertifications = this.toolCertificationReadFacade.getToolCertifications(customerId);
        }
        else if (nonNull(toolId)) {
            toolCertifications = this.toolCertificationReadFacade.getToolCertifications(toolId);
        }
        else {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(toolCertifications.stream() //
                .map(ToolCertificationApiModel.MAPPER::toToolCertificationApiDto) //
                .toList());
    }

    // ------------------------------------------------------------------------
    // command

    @PostMapping(value = "/batch", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BatchCertificationResult> createToolCertifications(@RequestBody @Valid BatchCertifyRequestBody requestBody) {
        BatchCertificationResult result = this.certifyCustomersForTool.process(requestBody.customerIds(), requestBody.toolId(), requestBody.certifiedBy());

        return ResponseEntity.ok(result);
    }

    @PutMapping(value = "/customers/{customerId}/tools/{toolId}/pause")
    public ResponseEntity<Void> pauseToolCertification(@PathVariable @NotNull @Valid CustomerId customerId, @PathVariable @NotNull @Valid ToolId toolId) {
        this.pauseToolCertification.process(customerId, toolId);

        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/customers/{customerId}/tools/{toolId}/reactivate")
    public ResponseEntity<Void> reactivateToolCertification(@PathVariable @NotNull @Valid CustomerId customerId,
            @PathVariable @NotNull @Valid ToolId toolId) {
        this.reactivateToolCertification.process(customerId, toolId);

        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/customers/{customerId}/tools/{toolId}/revoke")
    public ResponseEntity<Void> revokeToolCertification(@PathVariable @NotNull @Valid CustomerId customerId, @PathVariable @NotNull @Valid ToolId toolId) {
        this.revokeToolCertification.process(customerId, toolId);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping(value = "/customers/{customerId}/tools/{toolId}")
    public ResponseEntity<Void> deleteToolCertification(@PathVariable @NotNull @Valid CustomerId customerId, @PathVariable @NotNull @Valid ToolId toolId) {
        this.deleteToolCertification.process(customerId, toolId);

        return ResponseEntity.noContent().build();
    }

}

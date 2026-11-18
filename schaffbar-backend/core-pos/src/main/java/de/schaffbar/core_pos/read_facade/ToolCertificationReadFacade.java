package de.schaffbar.core_pos.read_facade;

import java.util.List;
import java.util.Optional;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import de.schaffbar.core_pos.domain.tool_certification.ToolCertificationService;
import de.schaffbar.core_pos.domain.tool_certification.ToolCertificationViews.ToolCertificationView;
import de.schaffbar.core_pos.shared.cqrs.Query;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.ToolCertificationId;
import de.schaffbar.core_pos.shared.id.ToolId;

@Service
@Validated
@RequiredArgsConstructor
public class ToolCertificationReadFacade {

    private final @NonNull ToolCertificationService toolCertificationService;

    @Query
    public Optional<ToolCertificationView> getToolCertification(@NotNull @Valid ToolCertificationId toolCertificationId) {
        return this.toolCertificationService.getToolCertification(toolCertificationId);
    }

    @Query
    public List<ToolCertificationView> getToolCertifications(@NotNull @Valid CustomerId customerId) {
        return this.toolCertificationService.getToolCertifications(customerId);
    }

    @Query
    public List<ToolCertificationView> getToolCertifications(@NotNull @Valid ToolId toolId) {
        return this.toolCertificationService.getToolCertifications(toolId);
    }

}

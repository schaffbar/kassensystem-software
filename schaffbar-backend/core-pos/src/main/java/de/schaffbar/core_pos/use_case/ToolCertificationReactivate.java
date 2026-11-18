package de.schaffbar.core_pos.use_case;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import de.schaffbar.core_pos.domain.tool_certification.ToolCertificationService;
import de.schaffbar.core_pos.shared.cqrs.Command;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.ToolId;

@Service
@Validated
@RequiredArgsConstructor
public class ToolCertificationReactivate {

    private final @NonNull ToolCertificationService toolCertificationService;

    @Command
    public void process(@NotNull @Valid CustomerId customerId, @NotNull @Valid ToolId toolId) {
        this.toolCertificationService.reactivateToolCertification(customerId, toolId);
    }

}

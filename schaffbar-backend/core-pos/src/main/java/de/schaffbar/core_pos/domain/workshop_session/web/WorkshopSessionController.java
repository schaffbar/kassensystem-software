package de.schaffbar.core_pos.domain.workshop_session.web;

import static java.util.Objects.isNull;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

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

import de.schaffbar.core_pos.domain.tool.ToolService;
import de.schaffbar.core_pos.domain.tool.ToolViews.ToolView;
import de.schaffbar.core_pos.domain.tool_usage.ToolUsageService;
import de.schaffbar.core_pos.domain.tool_usage.ToolUsageViews.ToolUsageView;
import de.schaffbar.core_pos.domain.workshop_session.WorkshopSessionService;
import de.schaffbar.core_pos.domain.workshop_session.WorkshopSessionViews.WorkshopSessionView;
import de.schaffbar.core_pos.domain.workshop_session.web.WorkshopSessionApiModel.ToolUsageSummaryApiDto;
import de.schaffbar.core_pos.domain.workshop_session.web.WorkshopSessionApiModel.WorkshopSessionApiDto;
import de.schaffbar.core_pos.domain.workshop_usage.WorkshopUsageService;
import de.schaffbar.core_pos.domain.workshop_usage.WorkshopUsageViews.WorkshopUsageView;
import de.schaffbar.core_pos.shared.billing.BillingTotal;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.ToolId;
import de.schaffbar.core_pos.use_case.CloseSession;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/api/v1/workshop-sessions")
public class WorkshopSessionController {

    private final @NonNull WorkshopSessionService workshopSessionService;

    private final @NonNull WorkshopUsageService workshopUsageService;

    private final @NonNull ToolUsageService toolUsageService;

    private final @NonNull ToolService toolService;

    private final @NonNull CloseSession closeSessionUseCase;

    // ------------------------------------------------------------------------
    // query

    // TODO: REST - remove customerId and active from path, use query parameter instead
    @GetMapping(value = "{customerId}/active", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WorkshopSessionApiDto> getActiveWorkshopSession(@PathVariable @NotNull CustomerId customerId) {
        // TODO: check if customer with given id exists
        WorkshopSessionView openSession = this.workshopSessionService.getOpenWorkshopSession(customerId) //
                .orElse(null);

        if (isNull(openSession)) {
            return ResponseEntity.ok(null);
        }

        // one point in time for all running usages, so the totals fit together
        Instant now = Instant.now();

        List<WorkshopUsageView> usages = this.workshopUsageService.getWorkshopUsages(openSession.id());
        BillingTotal workshopTotal = this.workshopUsageService.getBillingTotal(openSession.id(), now);

        List<ToolUsageView> toolUsages = this.toolUsageService.getToolUsages(openSession.id());
        Map<ToolId, BillingTotal> toolTotals = this.toolUsageService.getBillingTotalsByTool(openSession.id(), now);

        List<ToolUsageSummaryApiDto> toolUsageSummaries = buildToolUsageSummaries(toolUsages, toolTotals);
        WorkshopSessionApiDto result = WorkshopSessionApiModel.MAPPER.toWorkshopSessionApiDto(openSession, usages, workshopTotal, toolUsageSummaries);

        return ResponseEntity.ok(result);
    }

    // ------------------------------------------------------------------------
    // command

    // TODO: REST - customerId does not fit in RESTful design here, change to sessionId?
    @PutMapping(value = "/{customerId}/close")
    public ResponseEntity<Void> closeSession(@PathVariable @NotNull CustomerId customerId) {
        this.closeSessionUseCase.process(customerId);

        return ResponseEntity.ok().build();
    }

    // ------------------------------------------------------------------------
    // helper

    private List<ToolUsageSummaryApiDto> buildToolUsageSummaries(List<ToolUsageView> toolUsages, Map<ToolId, BillingTotal> toolTotals) {
        if (toolUsages.isEmpty()) {
            return List.of();
        }

        // TODO: make it better
        Map<ToolId, ToolView> toolMap = toolUsages.stream() //
                .map(ToolUsageView::toolId) //
                .distinct() //
                .map(toolId -> this.toolService.getTool(toolId).orElse(null)) //
                .filter(Objects::nonNull) //
                .collect(Collectors.toMap(ToolView::id, Function.identity()));

        return WorkshopSessionApiModel.MAPPER.toToolUsageSummaries(toolUsages, toolMap, toolTotals);
    }

}

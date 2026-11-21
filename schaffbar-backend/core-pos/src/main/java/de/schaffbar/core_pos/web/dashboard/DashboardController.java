package de.schaffbar.core_pos.web.dashboard;

import java.util.List;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.schaffbar.core_pos.read_facade.DashboardReadFacade;
import de.schaffbar.core_pos.web.dashboard.DashboardApiModel.ActiveToolUsageApiDto;
import de.schaffbar.core_pos.web.dashboard.DashboardApiModel.ActiveWorkshopUserApiDto;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/api/v1/dashboard")
public class DashboardController {

    private final @NonNull DashboardReadFacade dashboardReadFacade;

    // ------------------------------------------------------------------------
    // query

    @GetMapping(value = "/active-users", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ActiveWorkshopUserApiDto>> getActiveWorkshopUsers() {
        List<ActiveWorkshopUserApiDto> activeUsers = this.dashboardReadFacade.getActiveWorkshopUsers().stream() //
                .map(DashboardApiModel.MAPPER::toActiveWorkshopUserApiDto) //
                .toList();

        return ResponseEntity.ok(activeUsers);
    }

    @GetMapping(value = "/active-tools", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ActiveToolUsageApiDto>> getActiveToolUsages() {
        List<ActiveToolUsageApiDto> activeToolUsages = this.dashboardReadFacade.getActiveToolUsages().stream() //
                .map(DashboardApiModel.MAPPER::toActiveToolUsageApiDto) //
                .toList();

        return ResponseEntity.ok(activeToolUsages);
    }

}

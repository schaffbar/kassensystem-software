package de.schaffbar.core_pos.web.dashboard;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import de.schaffbar.core_pos.read_facade.DashboardReadFacade;
import de.schaffbar.core_pos.read_facade.DashboardViews.ActiveToolUsageView;
import de.schaffbar.core_pos.read_facade.DashboardViews.ActiveWorkshopUserView;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.ToolId;
import de.schaffbar.core_pos.shared.web.ApiExceptionHandler;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    @Mock
    private DashboardReadFacade dashboardReadFacade;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        DashboardController controller = new DashboardController(this.dashboardReadFacade);

        this.mockMvc = MockMvcBuilders.standaloneSetup(controller) //
                .setControllerAdvice(new ApiExceptionHandler()) //
                .build();
    }

    // ------------------------------------------------------------------------
    // getActiveWorkshopUsers

    @Test
    void getActiveWorkshopUsers_returnsThePeopleInTheWorkshop() throws Exception {
        // given
        ActiveWorkshopUserView user = new ActiveWorkshopUserView(CustomerId.random(), "Anna", "Becker", Instant.parse("2026-11-21T09:15:00Z"));
        when(this.dashboardReadFacade.getActiveWorkshopUsers()).thenReturn(List.of(user));

        // when
        ResultActions result = getActiveWorkshopUsers();

        // then
        result.andExpect(status().isOk()) //
                .andExpect(jsonPath("$.length()").value(1)) //
                .andExpect(jsonPath("$[0].customerId").value(user.customerId().getValue().toString())) //
                .andExpect(jsonPath("$[0].firstName").value("Anna")) //
                .andExpect(jsonPath("$[0].lastName").value("Becker")) //
                .andExpect(jsonPath("$[0].entryTime").exists());
    }

    @Test
    void getActiveWorkshopUsers_withNobodyInTheWorkshop_isAnEmptyList() throws Exception {
        // given
        when(this.dashboardReadFacade.getActiveWorkshopUsers()).thenReturn(List.of());

        // when
        ResultActions result = getActiveWorkshopUsers();

        // then
        result.andExpect(status().isOk()) //
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ------------------------------------------------------------------------
    // getActiveToolUsages

    @Test
    void getActiveToolUsages_returnsTheRunningToolsWithThePersonUsingThem() throws Exception {
        // given
        ActiveToolUsageView toolUsage = new ActiveToolUsageView(ToolId.random(), "Kreissäge", CustomerId.random(), "Anna", "Becker");
        when(this.dashboardReadFacade.getActiveToolUsages()).thenReturn(List.of(toolUsage));

        // when
        ResultActions result = getActiveToolUsages();

        // then
        result.andExpect(status().isOk()) //
                .andExpect(jsonPath("$.length()").value(1)) //
                .andExpect(jsonPath("$[0].toolId").value(toolUsage.toolId().getValue().toString())) //
                .andExpect(jsonPath("$[0].toolName").value("Kreissäge")) //
                .andExpect(jsonPath("$[0].customerId").value(toolUsage.customerId().getValue().toString())) //
                .andExpect(jsonPath("$[0].firstName").value("Anna")) //
                .andExpect(jsonPath("$[0].lastName").value("Becker"));
    }

    @Test
    void getActiveToolUsages_withNoToolRunning_isAnEmptyList() throws Exception {
        // given
        when(this.dashboardReadFacade.getActiveToolUsages()).thenReturn(List.of());

        // when
        ResultActions result = getActiveToolUsages();

        // then
        result.andExpect(status().isOk()) //
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ------------------------------------------------------------------------
    // when

    private ResultActions getActiveWorkshopUsers() throws Exception {
        return this.mockMvc.perform(get("/api/v1/dashboard/active-users"));
    }

    private ResultActions getActiveToolUsages() throws Exception {
        return this.mockMvc.perform(get("/api/v1/dashboard/active-tools"));
    }

}

package de.schaffbar.core_pos.web.dashboard;

import static de.schaffbar.core_pos.builder.ViewBuilder.activeToolUsageView;
import static de.schaffbar.core_pos.builder.ViewBuilder.activeWorkshopUserView;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import de.schaffbar.core_pos.shared.web.ApiExceptionHandler;
import de.schaffbar.core_pos.support.Json;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    @Mock
    private DashboardReadFacade dashboardReadFacade;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        DashboardController controller = new DashboardController(this.dashboardReadFacade);

        this.mockMvc = MockMvcBuilders.standaloneSetup(controller) //
                .setMessageConverters(Json.messageConverter()) //
                .setControllerAdvice(new ApiExceptionHandler()) //
                .build();
    }

    // ------------------------------------------------------------------------
    // getActiveWorkshopUsers

    @Test
    void getActiveWorkshopUsers_returnsThePeopleInTheWorkshop() throws Exception {
        // given
        ActiveWorkshopUserView user = activeWorkshopUserView().build();
        givenActiveWorkshopUsers(user);

        // when
        ResultActions result = getActiveWorkshopUsers();

        // then
        result.andExpect(status().isOk()) //
                .andExpect(jsonPath("$.length()").value(1)) //
                .andExpect(jsonPath("$[0].customerId").value(user.customerId().getValue().toString())) //
                .andExpect(jsonPath("$[0].firstName").value(user.firstName())) //
                .andExpect(jsonPath("$[0].lastName").value(user.lastName())) //
                .andExpect(jsonPath("$[0].entryTime").value(user.entryTime().toString()));
    }

    @Test
    void getActiveWorkshopUsers_withNobodyInTheWorkshop_isAnEmptyList() throws Exception {
        // given
        givenNobodyInTheWorkshop();

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
        ActiveToolUsageView toolUsage = activeToolUsageView().build();
        givenActiveToolUsages(toolUsage);

        // when
        ResultActions result = getActiveToolUsages();

        // then
        result.andExpect(status().isOk()) //
                .andExpect(jsonPath("$.length()").value(1)) //
                .andExpect(jsonPath("$[0].toolId").value(toolUsage.toolId().getValue().toString())) //
                .andExpect(jsonPath("$[0].toolName").value(toolUsage.toolName())) //
                .andExpect(jsonPath("$[0].customerId").value(toolUsage.customerId().getValue().toString())) //
                .andExpect(jsonPath("$[0].firstName").value(toolUsage.firstName())) //
                .andExpect(jsonPath("$[0].lastName").value(toolUsage.lastName()));
    }

    @Test
    void getActiveToolUsages_withNoToolRunning_isAnEmptyList() throws Exception {
        // given
        givenNoToolRunning();

        // when
        ResultActions result = getActiveToolUsages();

        // then
        result.andExpect(status().isOk()) //
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ------------------------------------------------------------------------
    // given

    private void givenActiveWorkshopUsers(ActiveWorkshopUserView... users) {
        when(this.dashboardReadFacade.getActiveWorkshopUsers()).thenReturn(List.of(users));
    }

    private void givenNobodyInTheWorkshop() {
        givenActiveWorkshopUsers();
    }

    private void givenActiveToolUsages(ActiveToolUsageView... toolUsages) {
        when(this.dashboardReadFacade.getActiveToolUsages()).thenReturn(List.of(toolUsages));
    }

    private void givenNoToolRunning() {
        givenActiveToolUsages();
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

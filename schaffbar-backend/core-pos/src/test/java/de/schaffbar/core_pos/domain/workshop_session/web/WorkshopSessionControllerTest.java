package de.schaffbar.core_pos.domain.workshop_session.web;

import static de.schaffbar.core_pos.builder.ViewBuilder.activeToolUsageView;
import static de.schaffbar.core_pos.builder.ViewBuilder.activeWorkshopUsageView;
import static de.schaffbar.core_pos.builder.ViewBuilder.openWorkshopSessionView;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import de.schaffbar.core_pos.domain.tool_usage.ToolUsageViews.ToolUsageView;
import de.schaffbar.core_pos.domain.workshop_session.WorkshopSessionViews.WorkshopSessionView;
import de.schaffbar.core_pos.domain.workshop_usage.WorkshopUsageViews.WorkshopUsageView;
import de.schaffbar.core_pos.read_facade.OpenWorkshopSessionViews.OpenWorkshopSessionView;
import de.schaffbar.core_pos.read_facade.OpenWorkshopSessionViews.ToolUsageSummaryView;
import de.schaffbar.core_pos.read_facade.WorkshopSessionReadFacade;
import de.schaffbar.core_pos.shared.billing.BillingTotal;
import de.schaffbar.core_pos.shared.exception.ActiveToolUsageExistsForSessionException;
import de.schaffbar.core_pos.shared.exception.NoOpenWorkshopSessionException;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.ToolId;
import de.schaffbar.core_pos.shared.id.WorkshopSessionId;
import de.schaffbar.core_pos.shared.util.StringToCustomerIdConverter;
import de.schaffbar.core_pos.shared.web.ApiExceptionHandler;
import de.schaffbar.core_pos.use_case.CloseSession;

@ExtendWith(MockitoExtension.class)
class WorkshopSessionControllerTest {

    @Mock
    private WorkshopSessionReadFacade workshopSessionReadFacade;

    @Mock
    private CloseSession closeSession;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        DefaultFormattingConversionService conversionService = new DefaultFormattingConversionService();
        conversionService.addConverter(new StringToCustomerIdConverter());

        WorkshopSessionController controller = new WorkshopSessionController(this.workshopSessionReadFacade, this.closeSession);

        this.mockMvc = MockMvcBuilders.standaloneSetup(controller) //
                .setConversionService(conversionService) //
                .setControllerAdvice(new ApiExceptionHandler()) //
                .build();
    }

    // ------------------------------------------------------------------------
    // getOpenWorkshopSession

    @Test
    void getOpenWorkshopSession_returnsTheSessionWithItsUsagesAndTotals() throws Exception {
        // given
        OpenWorkshopSessionView open = givenOpenSession();
        WorkshopSessionView session = open.session();
        WorkshopUsageView workshopUsage = open.workshopUsages().getFirst();
        ToolUsageSummaryView summary = open.toolUsageSummaries().getFirst();
        ToolUsageView toolUsage = summary.usages().getFirst();

        // when
        ResultActions result = getOpenWorkshopSession(session.customerId());

        // then
        result.andExpect(status().isOk()) //
                .andExpect(jsonPath("$.id").value(session.id().getValue().toString())) //
                .andExpect(jsonPath("$.customerId").value(session.customerId().getValue().toString())) //
                .andExpect(jsonPath("$.startTime").exists()) //
                .andExpect(jsonPath("$.closeTime").doesNotExist()) //
                .andExpect(jsonPath("$.status").value("OPEN")) //
                .andExpect(jsonPath("$.totalWorkshopDurationInMinutes").value(61)) //
                .andExpect(jsonPath("$.totalWorkshopUnits").value(11)) //
                .andExpect(jsonPath("$.workshopUsages.length()").value(1)) //
                .andExpect(jsonPath("$.workshopUsages[0].id").value(workshopUsage.id().getValue().toString())) //
                .andExpect(jsonPath("$.workshopUsages[0].entryTime").exists()) //
                .andExpect(jsonPath("$.workshopUsages[0].exitTime").doesNotExist()) //
                .andExpect(jsonPath("$.toolUsageSummaries.length()").value(1)) //
                .andExpect(jsonPath("$.toolUsageSummaries[0].toolId").value(summary.toolId().getValue().toString())) //
                .andExpect(jsonPath("$.toolUsageSummaries[0].toolName").value(summary.toolName())) //
                .andExpect(jsonPath("$.toolUsageSummaries[0].active").value(true)) //
                .andExpect(jsonPath("$.toolUsageSummaries[0].totalDurationInMinutes").value(7)) //
                .andExpect(jsonPath("$.toolUsageSummaries[0].totalUnits").value(2)) //
                .andExpect(jsonPath("$.toolUsageSummaries[0].usages[0].id").value(toolUsage.id().getValue().toString())) //
                .andExpect(jsonPath("$.toolUsageSummaries[0].usages[0].startTime").exists());
    }

    @Test
    void getOpenWorkshopSession_withoutOpenSession_is200WithAnEmptyBody() throws Exception {
        // given
        CustomerId customerId = CustomerId.random();
        when(this.workshopSessionReadFacade.getOpenWorkshopSession(customerId)).thenReturn(Optional.empty());

        // when
        ResultActions result = getOpenWorkshopSession(customerId);

        // then
        result.andExpect(status().isOk()) //
                .andExpect(content().string(""));
    }

    @Test
    void getOpenWorkshopSession_unknownCustomer_is404() throws Exception {
        // given
        CustomerId unknown = CustomerId.random();
        when(this.workshopSessionReadFacade.getOpenWorkshopSession(unknown)).thenThrow(ResourceNotFoundException.customer(unknown));

        // when
        ResultActions result = getOpenWorkshopSession(unknown);

        // then
        result.andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------------
    // closeSession

    @Test
    void closeSession_passesTheCustomerAndIs200() throws Exception {
        // given
        CustomerId customerId = CustomerId.random();

        // when
        ResultActions result = closeSession(customerId);

        // then
        result.andExpect(status().isOk());
        verify(this.closeSession).process(customerId);
    }

    @Test
    void closeSession_withARunningToolUsage_is409WithTheCode() throws Exception {
        // given
        CustomerId customerId = CustomerId.random();
        doThrow(new ActiveToolUsageExistsForSessionException(WorkshopSessionId.random())).when(this.closeSession).process(customerId);

        // when
        ResultActions result = closeSession(customerId);

        // then
        result.andExpect(status().isConflict()) //
                .andExpect(jsonPath("$.code").value(ActiveToolUsageExistsForSessionException.CODE));
    }

    @Test
    void closeSession_withoutOpenSession_is409WithTheCode() throws Exception {
        // given
        CustomerId customerId = CustomerId.random();
        doThrow(new NoOpenWorkshopSessionException(customerId)).when(this.closeSession).process(customerId);

        // when
        ResultActions result = closeSession(customerId);

        // then
        result.andExpect(status().isConflict()) //
                .andExpect(jsonPath("$.code").value(NoOpenWorkshopSessionException.CODE));
    }

    // ------------------------------------------------------------------------
    // given

    private OpenWorkshopSessionView givenOpenSession() {
        WorkshopSessionView session = openWorkshopSessionView().build();
        ToolId toolId = ToolId.random();
        OpenWorkshopSessionView open = OpenWorkshopSessionView.builder() //
                .session(session) //
                .workshopUsages(List.of(activeWorkshopUsageView().workshopSessionId(session.id()).build())) //
                .workshopTotal(new BillingTotal(61, 11)) //
                .toolUsageSummaries(List.of(ToolUsageSummaryView.builder() //
                        .toolId(toolId) //
                        .toolName("Kreissäge") //
                        .active(true) //
                        .total(new BillingTotal(7, 2)) //
                        .usages(List.of(activeToolUsageView(toolId).build())) //
                        .build())) //
                .build();
        when(this.workshopSessionReadFacade.getOpenWorkshopSession(session.customerId())).thenReturn(Optional.of(open));

        return open;
    }

    // ------------------------------------------------------------------------
    // when

    private ResultActions getOpenWorkshopSession(CustomerId customerId) throws Exception {
        return this.mockMvc.perform(get("/api/v1/workshop-sessions/{customerId}/active", customerId.getValue()));
    }

    private ResultActions closeSession(CustomerId customerId) throws Exception {
        return this.mockMvc.perform(put("/api/v1/workshop-sessions/{customerId}/close", customerId.getValue()));
    }

}

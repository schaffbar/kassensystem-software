package de.schaffbar.core_pos.domain.workshop_session.web;

import static de.schaffbar.core_pos.builder.ViewBuilder.openWorkshopSessionView;
import static de.schaffbar.core_pos.builder.ViewBuilder.openWorkshopSessionWithUsagesView;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import de.schaffbar.core_pos.shared.exception.NoOpenWorkshopSessionException;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException.Resource;
import de.schaffbar.core_pos.shared.exception.RunningToolUsageExistsForSessionException;
import de.schaffbar.core_pos.shared.exception.RunningWorkshopUsageExistsForSessionException;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.WorkshopSessionId;
import de.schaffbar.core_pos.shared.util.StringToCustomerIdConverter;
import de.schaffbar.core_pos.shared.web.ApiExceptionHandler;
import de.schaffbar.core_pos.support.Json;
import de.schaffbar.core_pos.use_case.WorkshopSessionClose;

@ExtendWith(MockitoExtension.class)
class WorkshopSessionControllerTest {

    @Mock
    private WorkshopSessionReadFacade workshopSessionReadFacade;

    @Mock
    private WorkshopSessionClose workshopSessionClose;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        DefaultFormattingConversionService conversionService = new DefaultFormattingConversionService();
        conversionService.addConverter(new StringToCustomerIdConverter());

        WorkshopSessionController controller = new WorkshopSessionController(this.workshopSessionReadFacade, this.workshopSessionClose);

        this.mockMvc = MockMvcBuilders.standaloneSetup(controller) //
                .setConversionService(conversionService) //
                .setMessageConverters(Json.messageConverter()) //
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
                .andExpect(jsonPath("$.startTime").value(session.startTime().toString())) //
                .andExpect(jsonPath("$.closeTime").doesNotExist()) //
                .andExpect(jsonPath("$.status").value(session.status().name())) //
                .andExpect(jsonPath("$.totalWorkshopDurationInMinutes").value(open.workshopTotal().minutes())) //
                .andExpect(jsonPath("$.totalWorkshopUnits").value(open.workshopTotal().units())) //
                .andExpect(jsonPath("$.workshopUsages.length()").value(1)) //
                .andExpect(jsonPath("$.workshopUsages[0].id").value(workshopUsage.id().getValue().toString())) //
                .andExpect(jsonPath("$.workshopUsages[0].entryTime").value(workshopUsage.entryTime().toString())) //
                .andExpect(jsonPath("$.workshopUsages[0].exitTime").doesNotExist()) //
                .andExpect(jsonPath("$.toolUsageSummaries.length()").value(1)) //
                .andExpect(jsonPath("$.toolUsageSummaries[0].toolId").value(summary.toolId().getValue().toString())) //
                .andExpect(jsonPath("$.toolUsageSummaries[0].toolName").value(summary.toolName())) //
                .andExpect(jsonPath("$.toolUsageSummaries[0].running").value(summary.running())) //
                .andExpect(jsonPath("$.toolUsageSummaries[0].totalDurationInMinutes").value(summary.total().minutes())) //
                .andExpect(jsonPath("$.toolUsageSummaries[0].totalUnits").value(summary.total().units())) //
                .andExpect(jsonPath("$.toolUsageSummaries[0].usages[0].id").value(toolUsage.id().getValue().toString())) //
                .andExpect(jsonPath("$.toolUsageSummaries[0].usages[0].startTime").value(toolUsage.startTime().toString())) //
                .andExpect(jsonPath("$.toolUsageSummaries[0].usages[0].endTime").value(toolUsage.endTime().toString()));
    }

    @Test
    void getOpenWorkshopSession_withoutOpenSession_is200WithAnEmptyBody() throws Exception {
        // given
        CustomerId customerId = givenNoOpenSession();

        // when
        ResultActions result = getOpenWorkshopSession(customerId);

        // then
        result.andExpect(status().isOk()) //
                .andExpect(content().string(""));
    }

    @Test
    void getOpenWorkshopSession_unknownCustomer_is404() throws Exception {
        // given
        CustomerId unknown = givenUnknownCustomer();

        // when
        ResultActions result = getOpenWorkshopSession(unknown);

        // then
        expectNotFound(result);
    }

    // ------------------------------------------------------------------------
    // closeWorkshopSession

    @Test
    void closeWorkshopSession_passesTheCustomerAndIs204() throws Exception {
        // given
        CustomerId customerId = CustomerId.random();

        // when
        ResultActions result = closeWorkshopSession(customerId);

        // then
        result.andExpect(status().isNoContent());
        expectClosed(customerId);
    }

    // WS-5 – the one the API can get: checking out first stops the tools, so a running tool comes only with a running visit
    @Test
    void closeWorkshopSession_whileThePersonIsInTheWorkshop_is409WithTheCode() throws Exception {
        // given
        CustomerId customerId = CustomerId.random();
        givenCloseRejectedWith(customerId, new RunningWorkshopUsageExistsForSessionException(WorkshopSessionId.random()));

        // when
        ResultActions result = closeWorkshopSession(customerId);

        // then
        result.andExpect(status().isConflict()) //
                .andExpect(jsonPath("$.code").value(RunningWorkshopUsageExistsForSessionException.CODE));
    }

    @Test
    void closeWorkshopSession_withARunningToolUsage_is409WithTheCode() throws Exception {
        // given
        CustomerId customerId = CustomerId.random();
        givenCloseRejectedWith(customerId, new RunningToolUsageExistsForSessionException(WorkshopSessionId.random()));

        // when
        ResultActions result = closeWorkshopSession(customerId);

        // then
        result.andExpect(status().isConflict()) //
                .andExpect(jsonPath("$.code").value(RunningToolUsageExistsForSessionException.CODE));
    }

    @Test
    void closeWorkshopSession_withoutOpenSession_is409WithTheCode() throws Exception {
        // given
        CustomerId customerId = CustomerId.random();
        givenCloseRejectedWith(customerId, new NoOpenWorkshopSessionException(customerId));

        // when
        ResultActions result = closeWorkshopSession(customerId);

        // then
        result.andExpect(status().isConflict()) //
                .andExpect(jsonPath("$.code").value(NoOpenWorkshopSessionException.CODE));
    }

    @Test
    void closeWorkshopSession_unknownCustomer_is404() throws Exception {
        // given
        CustomerId unknown = CustomerId.random();
        givenCloseRejectedWith(unknown, ResourceNotFoundException.customer(unknown));

        // when
        ResultActions result = closeWorkshopSession(unknown);

        // then
        expectNotFound(result);
    }

    // ------------------------------------------------------------------------
    // given

    private OpenWorkshopSessionView givenOpenSession() {
        WorkshopSessionView session = openWorkshopSessionView().build();
        OpenWorkshopSessionView open = openWorkshopSessionWithUsagesView(session).build();
        when(this.workshopSessionReadFacade.getOpenWorkshopSession(session.customerId())).thenReturn(Optional.of(open));

        return open;
    }

    private CustomerId givenNoOpenSession() {
        CustomerId customerId = CustomerId.random();
        when(this.workshopSessionReadFacade.getOpenWorkshopSession(customerId)).thenReturn(Optional.empty());

        return customerId;
    }

    private CustomerId givenUnknownCustomer() {
        CustomerId unknown = CustomerId.random();
        when(this.workshopSessionReadFacade.getOpenWorkshopSession(unknown)).thenThrow(ResourceNotFoundException.customer(unknown));

        return unknown;
    }

    private void givenCloseRejectedWith(CustomerId customerId, RuntimeException rejection) {
        doThrow(rejection).when(this.workshopSessionClose).process(customerId);
    }

    // ------------------------------------------------------------------------
    // when

    private ResultActions getOpenWorkshopSession(CustomerId customerId) throws Exception {
        return this.mockMvc.perform(get("/api/v1/workshop-sessions/{customerId}/open", customerId.getValue()));
    }

    private ResultActions closeWorkshopSession(CustomerId customerId) throws Exception {
        return this.mockMvc.perform(put("/api/v1/workshop-sessions/{customerId}/close", customerId.getValue()));
    }

    // ------------------------------------------------------------------------
    // then

    private void expectNotFound(ResultActions result) throws Exception {
        result.andExpect(status().isNotFound()) //
                .andExpect(jsonPath("$.code").value(ResourceNotFoundException.CODE)) //
                .andExpect(jsonPath("$.resource").value(Resource.CUSTOMER.name()));
    }

    private void expectClosed(CustomerId customerId) {
        verify(this.workshopSessionClose).process(customerId);
    }

}

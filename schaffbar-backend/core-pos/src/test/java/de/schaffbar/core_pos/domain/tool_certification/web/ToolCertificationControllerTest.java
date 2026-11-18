package de.schaffbar.core_pos.domain.tool_certification.web;

import static de.schaffbar.core_pos.builder.ViewBuilder.toolCertificationView;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import de.schaffbar.core_pos.domain.tool_certification.ToolCertificationStatus;
import de.schaffbar.core_pos.domain.tool_certification.ToolCertificationViews.ToolCertificationView;
import de.schaffbar.core_pos.domain.tool_certification.web.ToolCertificationApiModel.BatchCertifyRequestBody;
import de.schaffbar.core_pos.read_facade.ToolCertificationReadFacade;
import de.schaffbar.core_pos.shared.exception.CustomerIsInstructorException;
import de.schaffbar.core_pos.shared.exception.InvalidCertificationStateTransitionException;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.ToolCertificationId;
import de.schaffbar.core_pos.shared.id.ToolId;
import de.schaffbar.core_pos.shared.util.StringToCustomerIdConverter;
import de.schaffbar.core_pos.shared.util.StringToToolCertificationIdConverter;
import de.schaffbar.core_pos.shared.util.StringToToolIdConverter;
import de.schaffbar.core_pos.shared.web.ApiExceptionHandler;
import de.schaffbar.core_pos.support.Json;
import de.schaffbar.core_pos.use_case.ToolCertificationCreate;
import de.schaffbar.core_pos.use_case.ToolCertificationDelete;
import de.schaffbar.core_pos.use_case.ToolCertificationPause;
import de.schaffbar.core_pos.use_case.ToolCertificationReactivate;
import de.schaffbar.core_pos.use_case.ToolCertificationRevoke;

@ExtendWith(MockitoExtension.class)
class ToolCertificationControllerTest {

    @Mock
    private ToolCertificationReadFacade toolCertificationReadFacade;

    @Mock
    private ToolCertificationCreate toolCertificationCreate;

    @Mock
    private ToolCertificationPause toolCertificationPause;

    @Mock
    private ToolCertificationReactivate toolCertificationReactivate;

    @Mock
    private ToolCertificationRevoke toolCertificationRevoke;

    @Mock
    private ToolCertificationDelete toolCertificationDelete;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        DefaultFormattingConversionService conversionService = new DefaultFormattingConversionService();
        conversionService.addConverter(new StringToToolCertificationIdConverter());
        conversionService.addConverter(new StringToCustomerIdConverter());
        conversionService.addConverter(new StringToToolIdConverter());

        ToolCertificationController controller = new ToolCertificationController(this.toolCertificationReadFacade, this.toolCertificationCreate,
                this.toolCertificationPause, this.toolCertificationReactivate, this.toolCertificationRevoke, this.toolCertificationDelete);

        this.mockMvc = MockMvcBuilders.standaloneSetup(controller) //
                .setConversionService(conversionService) //
                .setControllerAdvice(new ApiExceptionHandler()) //
                .build();
    }

    // ------------------------------------------------------------------------
    // getToolCertifications

    @Test
    void getToolCertifications_ofACustomer_returnsTheirCertifications() throws Exception {
        // given
        ToolCertificationView certification = givenCertificationsOfTheCustomer();

        // when
        ResultActions result = getToolCertifications("customerId", certification.customerId().getValue().toString());

        // then
        result.andExpect(status().isOk()) //
                .andExpect(jsonPath("$.length()").value(1)) //
                .andExpect(jsonPath("$[0].id").value(certification.id().getValue().toString())) //
                .andExpect(jsonPath("$[0].customerId").value(certification.customerId().getValue().toString()));
    }

    @Test
    void getToolCertifications_ofATool_returnsItsCertifications() throws Exception {
        // given
        ToolCertificationView certification = givenCertificationsOfTheTool();

        // when
        ResultActions result = getToolCertifications("toolId", certification.toolId().getValue().toString());

        // then
        result.andExpect(status().isOk()) //
                .andExpect(jsonPath("$.length()").value(1)) //
                .andExpect(jsonPath("$[0].id").value(certification.id().getValue().toString())) //
                .andExpect(jsonPath("$[0].toolId").value(certification.toolId().getValue().toString()));
    }

    @Test
    void getToolCertifications_withoutFilter_is400() throws Exception {
        // when
        ResultActions result = getToolCertificationsWithoutFilter();

        // then
        result.andExpect(status().isBadRequest());
        verifyNoInteractions(this.toolCertificationReadFacade);
    }

    // ------------------------------------------------------------------------
    // getToolCertification

    @Test
    void getToolCertification_returnsTheCertification() throws Exception {
        // given
        ToolCertificationView certification = givenCertification();

        // when
        ResultActions result = getToolCertification(certification.id());

        // then
        result.andExpect(status().isOk()) //
                .andExpect(jsonPath("$.id").value(certification.id().getValue().toString())) //
                .andExpect(jsonPath("$.customerId").value(certification.customerId().getValue().toString())) //
                .andExpect(jsonPath("$.toolId").value(certification.toolId().getValue().toString())) //
                .andExpect(jsonPath("$.status").value(certification.status().name())) //
                .andExpect(jsonPath("$.certifiedAt").exists()) //
                .andExpect(jsonPath("$.certifiedBy").value(certification.certifiedBy().getValue().toString()));
    }

    @Test
    void getToolCertification_unknownId_is404() throws Exception {
        // given
        ToolCertificationId unknown = givenUnknownCertification();

        // when
        ResultActions result = getToolCertification(unknown);

        // then
        result.andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------------
    // createToolCertifications

    @Test
    void createToolCertifications_passesTheIdsAndIs204() throws Exception {
        // given
        BatchCertifyRequestBody body = new BatchCertifyRequestBody(List.of(CustomerId.random(), CustomerId.random()), ToolId.random(), CustomerId.random());

        // when
        ResultActions result = createToolCertifications(body);

        // then
        result.andExpect(status().isNoContent());
        verify(this.toolCertificationCreate).process(body.customerIds(), body.toolId(), body.certifiedBy());
    }

    @Test
    void createToolCertifications_withAnInstructorOfTheTool_is409WithTheCode() throws Exception {
        // given
        CustomerId instructorId = CustomerId.random();
        BatchCertifyRequestBody body = new BatchCertifyRequestBody(List.of(instructorId), ToolId.random(), CustomerId.random());
        doThrow(new CustomerIsInstructorException(instructorId, body.toolId())).when(this.toolCertificationCreate)
                .process(body.customerIds(), body.toolId(), body.certifiedBy());

        // when
        ResultActions result = createToolCertifications(body);

        // then
        result.andExpect(status().isConflict()) //
                .andExpect(jsonPath("$.code").value(CustomerIsInstructorException.CODE));
    }

    @Test
    void createToolCertifications_withoutCustomers_is400() throws Exception {
        // given
        String body = "{\"customerIds\":[],\"toolId\":\"" + ToolId.random().getValue() + "\",\"certifiedBy\":\"" + CustomerId.random().getValue() + "\"}";

        // when
        ResultActions result = createToolCertifications(body);

        // then
        result.andExpect(status().isBadRequest());
        verify(this.toolCertificationCreate, never()).process(any(), any(), any());
    }

    // ------------------------------------------------------------------------
    // pauseToolCertification

    @Test
    void pauseToolCertification_passesTheIdsAndIs204() throws Exception {
        // given
        CustomerId customerId = CustomerId.random();
        ToolId toolId = ToolId.random();

        // when
        ResultActions result = pauseToolCertification(customerId, toolId);

        // then
        result.andExpect(status().isNoContent());
        verify(this.toolCertificationPause).process(customerId, toolId);
    }

    @Test
    void pauseToolCertification_unknownCertification_is404() throws Exception {
        // given
        CustomerId customerId = CustomerId.random();
        ToolId toolId = ToolId.random();
        doThrow(ResourceNotFoundException.toolCertification(customerId, toolId)).when(this.toolCertificationPause).process(customerId, toolId);

        // when
        ResultActions result = pauseToolCertification(customerId, toolId);

        // then
        result.andExpect(status().isNotFound());
    }

    @Test
    void pauseToolCertification_withAnInvalidTransition_is409WithTheCode() throws Exception {
        // given
        CustomerId customerId = CustomerId.random();
        ToolId toolId = ToolId.random();
        doThrow(invalidTransition(ToolCertificationStatus.PAUSED)).when(this.toolCertificationPause).process(customerId, toolId);

        // when
        ResultActions result = pauseToolCertification(customerId, toolId);

        // then
        expectInvalidTransition(result);
    }

    // ------------------------------------------------------------------------
    // reactivateToolCertification

    @Test
    void reactivateToolCertification_passesTheIdsAndIs204() throws Exception {
        // given
        CustomerId customerId = CustomerId.random();
        ToolId toolId = ToolId.random();

        // when
        ResultActions result = reactivateToolCertification(customerId, toolId);

        // then
        result.andExpect(status().isNoContent());
        verify(this.toolCertificationReactivate).process(customerId, toolId);
    }

    @Test
    void reactivateToolCertification_withAnInvalidTransition_is409WithTheCode() throws Exception {
        // given
        CustomerId customerId = CustomerId.random();
        ToolId toolId = ToolId.random();
        doThrow(invalidTransition(ToolCertificationStatus.ACTIVE)).when(this.toolCertificationReactivate).process(customerId, toolId);

        // when
        ResultActions result = reactivateToolCertification(customerId, toolId);

        // then
        expectInvalidTransition(result);
    }

    // ------------------------------------------------------------------------
    // revokeToolCertification

    @Test
    void revokeToolCertification_passesTheIdsAndIs204() throws Exception {
        // given
        CustomerId customerId = CustomerId.random();
        ToolId toolId = ToolId.random();

        // when
        ResultActions result = revokeToolCertification(customerId, toolId);

        // then
        result.andExpect(status().isNoContent());
        verify(this.toolCertificationRevoke).process(customerId, toolId);
    }

    @Test
    void revokeToolCertification_withAnInvalidTransition_is409WithTheCode() throws Exception {
        // given
        CustomerId customerId = CustomerId.random();
        ToolId toolId = ToolId.random();
        doThrow(invalidTransition(ToolCertificationStatus.REVOKED)).when(this.toolCertificationRevoke).process(customerId, toolId);

        // when
        ResultActions result = revokeToolCertification(customerId, toolId);

        // then
        expectInvalidTransition(result);
    }

    // ------------------------------------------------------------------------
    // deleteToolCertification

    @Test
    void deleteToolCertification_passesTheIdsAndIs204() throws Exception {
        // given
        CustomerId customerId = CustomerId.random();
        ToolId toolId = ToolId.random();

        // when
        ResultActions result = deleteToolCertification(customerId, toolId);

        // then
        result.andExpect(status().isNoContent());
        verify(this.toolCertificationDelete).process(customerId, toolId);
    }

    @Test
    void deleteToolCertification_unknownCertification_is404() throws Exception {
        // given
        CustomerId customerId = CustomerId.random();
        ToolId toolId = ToolId.random();
        doThrow(ResourceNotFoundException.toolCertification(customerId, toolId)).when(this.toolCertificationDelete).process(customerId, toolId);

        // when
        ResultActions result = deleteToolCertification(customerId, toolId);

        // then
        result.andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------------
    // given

    private ToolCertificationView givenCertification() {
        ToolCertificationView certification = toolCertificationView().build();
        when(this.toolCertificationReadFacade.getToolCertification(certification.id())).thenReturn(Optional.of(certification));

        return certification;
    }

    private ToolCertificationId givenUnknownCertification() {
        ToolCertificationId unknown = ToolCertificationId.random();
        when(this.toolCertificationReadFacade.getToolCertification(unknown)).thenReturn(Optional.empty());

        return unknown;
    }

    private ToolCertificationView givenCertificationsOfTheCustomer() {
        ToolCertificationView certification = toolCertificationView().build();
        when(this.toolCertificationReadFacade.getToolCertifications(certification.customerId())).thenReturn(List.of(certification));

        return certification;
    }

    private ToolCertificationView givenCertificationsOfTheTool() {
        ToolCertificationView certification = toolCertificationView().build();
        when(this.toolCertificationReadFacade.getToolCertifications(certification.toolId())).thenReturn(List.of(certification));

        return certification;
    }

    // ------------------------------------------------------------------------
    // when

    private ResultActions getToolCertification(ToolCertificationId toolCertificationId) throws Exception {
        return this.mockMvc.perform(get("/api/v1/tool-certifications/" + toolCertificationId.getValue()));
    }

    private ResultActions getToolCertifications(String filter, String id) throws Exception {
        return this.mockMvc.perform(get("/api/v1/tool-certifications").param(filter, id));
    }

    private ResultActions getToolCertificationsWithoutFilter() throws Exception {
        return this.mockMvc.perform(get("/api/v1/tool-certifications"));
    }

    private ResultActions createToolCertifications(BatchCertifyRequestBody body) throws Exception {
        return createToolCertifications(Json.write(body));
    }

    private ResultActions createToolCertifications(String body) throws Exception {
        return this.mockMvc.perform(post("/api/v1/tool-certifications/batch") //
                .contentType(MediaType.APPLICATION_JSON) //
                .content(body));
    }

    private ResultActions pauseToolCertification(CustomerId customerId, ToolId toolId) throws Exception {
        return this.mockMvc.perform(put(certificationUrl(customerId, toolId) + "/pause"));
    }

    private ResultActions reactivateToolCertification(CustomerId customerId, ToolId toolId) throws Exception {
        return this.mockMvc.perform(put(certificationUrl(customerId, toolId) + "/reactivate"));
    }

    private ResultActions revokeToolCertification(CustomerId customerId, ToolId toolId) throws Exception {
        return this.mockMvc.perform(put(certificationUrl(customerId, toolId) + "/revoke"));
    }

    private ResultActions deleteToolCertification(CustomerId customerId, ToolId toolId) throws Exception {
        return this.mockMvc.perform(delete(certificationUrl(customerId, toolId)));
    }

    // ------------------------------------------------------------------------
    // then

    private void expectInvalidTransition(ResultActions result) throws Exception {
        result.andExpect(status().isConflict()) //
                .andExpect(jsonPath("$.code").value(InvalidCertificationStateTransitionException.CODE));
    }

    // ------------------------------------------------------------------------
    // helper

    private String certificationUrl(CustomerId customerId, ToolId toolId) {
        return "/api/v1/tool-certifications/customers/" + customerId.getValue() + "/tools/" + toolId.getValue();
    }

    private InvalidCertificationStateTransitionException invalidTransition(ToolCertificationStatus targetStatus) {
        return new InvalidCertificationStateTransitionException(ToolCertificationId.random(), ToolCertificationStatus.REVOKED.name(), targetStatus.name());
    }

}

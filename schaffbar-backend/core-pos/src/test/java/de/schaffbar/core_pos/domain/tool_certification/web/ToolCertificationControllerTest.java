package de.schaffbar.core_pos.domain.tool_certification.web;

import static de.schaffbar.core_pos.builder.ViewBuilder.toolCertificationView;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import de.schaffbar.core_pos.domain.tool_certification.ToolCertificationStatus;
import de.schaffbar.core_pos.domain.tool_certification.ToolCertificationViews.ToolCertificationView;
import de.schaffbar.core_pos.read_facade.ToolCertificationReadFacade;
import de.schaffbar.core_pos.shared.exception.CertificationAlreadyExistsException;
import de.schaffbar.core_pos.shared.exception.CertificationPermanentlyRevokedException;
import de.schaffbar.core_pos.shared.exception.CustomerIsInstructorException;
import de.schaffbar.core_pos.shared.exception.CustomerNotInstructorException;
import de.schaffbar.core_pos.shared.exception.InvalidCertificationStateTransitionException;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException.Resource;
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
                .setMessageConverters(Json.messageConverter()) //
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
        ResultActions result = getToolCertifications(certification.customerId());

        // then
        result.andExpect(status().isOk()) //
                .andExpect(jsonPath("$.length()").value(1)) //
                .andExpect(jsonPath("$[0].id").value(certification.id().getValue().toString())) //
                .andExpect(jsonPath("$[0].customerId").value(certification.customerId().getValue().toString())) //
                .andExpect(jsonPath("$[0].toolId").value(certification.toolId().getValue().toString())) //
                .andExpect(jsonPath("$[0].status").value(certification.status().name())) //
                .andExpect(jsonPath("$[0].certifiedAt").value(certification.certifiedAt().toString())) //
                .andExpect(jsonPath("$[0].certifiedBy").value(certification.certifiedBy().getValue().toString()));
    }

    @Test
    void getToolCertifications_ofATool_returnsItsCertifications() throws Exception {
        // given
        ToolCertificationView certification = givenCertificationsOfTheTool();

        // when
        ResultActions result = getToolCertifications(certification.toolId());

        // then
        result.andExpect(status().isOk()) //
                .andExpect(jsonPath("$.length()").value(1)) //
                .andExpect(jsonPath("$[0].id").value(certification.id().getValue().toString()));
    }

    @Test
    void getToolCertifications_ofACustomerWithoutCertifications_returnsAnEmptyList() throws Exception {
        // given
        CustomerId customerId = givenNoCertificationsOf(CustomerId.random());

        // when
        ResultActions result = getToolCertifications(customerId);

        // then
        result.andExpect(status().isOk()) //
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getToolCertifications_withBothFilters_filtersByTheCustomer() throws Exception {
        // given
        ToolCertificationView certification = givenCertificationsOfTheCustomer();

        // when
        ResultActions result = getToolCertifications(certification.customerId(), ToolId.random());

        // then
        result.andExpect(status().isOk()) //
                .andExpect(jsonPath("$.length()").value(1)) //
                .andExpect(jsonPath("$[0].id").value(certification.id().getValue().toString()));
    }

    @Test
    void getToolCertifications_withoutFilter_is400() throws Exception {
        // when
        ResultActions result = getToolCertifications();

        // then
        result.andExpect(status().isBadRequest()) //
                .andExpect(jsonPath("$.status").value(400));
        expectNothingRead();
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
                .andExpect(jsonPath("$.certifiedAt").value(certification.certifiedAt().toString())) //
                .andExpect(jsonPath("$.certifiedBy").value(certification.certifiedBy().getValue().toString()));
    }

    @Test
    void getToolCertification_unknownId_is404() throws Exception {
        // given
        ToolCertificationId unknown = givenUnknownCertification();

        // when
        ResultActions result = getToolCertification(unknown);

        // then
        expectNotFound(result, Resource.TOOL_CERTIFICATION);
    }

    // ------------------------------------------------------------------------
    // createToolCertifications

    @Test
    void createToolCertifications_passesTheIdsAndIs204() throws Exception {
        // given
        List<CustomerId> customerIds = List.of(CustomerId.random(), CustomerId.random());
        ToolId toolId = ToolId.random();
        CustomerId certifiedBy = CustomerId.random();

        // when
        ResultActions result = createToolCertifications(customerIds, toolId, certifiedBy);

        // then
        result.andExpect(status().isNoContent());
        expectCreated(customerIds, toolId, certifiedBy);
    }

    @Test
    void createToolCertifications_withoutCustomers_is400() throws Exception {
        // when
        ResultActions result = createToolCertifications(List.of(), ToolId.random(), CustomerId.random());

        // then
        result.andExpect(status().isBadRequest());
        expectNothingCreated();
    }

    @ParameterizedTest
    @MethodSource("rejectionsOfTheBatch")
    void createToolCertifications_rejectedByARule_is409WithTheCode(RuntimeException rejection, String code) throws Exception {
        // given
        List<CustomerId> customerIds = List.of(CustomerId.random());
        ToolId toolId = ToolId.random();
        CustomerId certifiedBy = CustomerId.random();
        givenCreateRejectedWith(customerIds, toolId, certifiedBy, rejection);

        // when
        ResultActions result = createToolCertifications(customerIds, toolId, certifiedBy);

        // then
        result.andExpect(status().isConflict()) //
                .andExpect(jsonPath("$.code").value(code));
    }

    @Test
    void createToolCertifications_ofAnUnknownCustomer_is404() throws Exception {
        // given
        CustomerId unknown = CustomerId.random();
        ToolId toolId = ToolId.random();
        CustomerId certifiedBy = CustomerId.random();
        givenCreateRejectedWith(List.of(unknown), toolId, certifiedBy, ResourceNotFoundException.customer(unknown));

        // when
        ResultActions result = createToolCertifications(List.of(unknown), toolId, certifiedBy);

        // then
        expectNotFound(result, Resource.CUSTOMER);
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
        expectPaused(customerId, toolId);
    }

    @Test
    void pauseToolCertification_unknownCertification_is404() throws Exception {
        // given
        CustomerId customerId = CustomerId.random();
        ToolId toolId = ToolId.random();
        givenPauseRejectedWith(customerId, toolId, ResourceNotFoundException.toolCertification(customerId, toolId));

        // when
        ResultActions result = pauseToolCertification(customerId, toolId);

        // then
        expectNotFound(result, Resource.TOOL_CERTIFICATION);
    }

    @Test
    void pauseToolCertification_withAnInvalidTransition_is409WithTheCode() throws Exception {
        // given
        CustomerId customerId = CustomerId.random();
        ToolId toolId = ToolId.random();
        givenPauseRejectedWith(customerId, toolId, invalidTransition(ToolCertificationStatus.PAUSED));

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
        expectReactivated(customerId, toolId);
    }

    @Test
    void reactivateToolCertification_unknownCertification_is404() throws Exception {
        // given
        CustomerId customerId = CustomerId.random();
        ToolId toolId = ToolId.random();
        givenReactivateRejectedWith(customerId, toolId, ResourceNotFoundException.toolCertification(customerId, toolId));

        // when
        ResultActions result = reactivateToolCertification(customerId, toolId);

        // then
        expectNotFound(result, Resource.TOOL_CERTIFICATION);
    }

    @Test
    void reactivateToolCertification_withAnInvalidTransition_is409WithTheCode() throws Exception {
        // given
        CustomerId customerId = CustomerId.random();
        ToolId toolId = ToolId.random();
        givenReactivateRejectedWith(customerId, toolId, invalidTransition(ToolCertificationStatus.ACTIVE));

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
        expectRevoked(customerId, toolId);
    }

    @Test
    void revokeToolCertification_unknownCertification_is404() throws Exception {
        // given
        CustomerId customerId = CustomerId.random();
        ToolId toolId = ToolId.random();
        givenRevokeRejectedWith(customerId, toolId, ResourceNotFoundException.toolCertification(customerId, toolId));

        // when
        ResultActions result = revokeToolCertification(customerId, toolId);

        // then
        expectNotFound(result, Resource.TOOL_CERTIFICATION);
    }

    @Test
    void revokeToolCertification_withAnInvalidTransition_is409WithTheCode() throws Exception {
        // given
        CustomerId customerId = CustomerId.random();
        ToolId toolId = ToolId.random();
        givenRevokeRejectedWith(customerId, toolId, invalidTransition(ToolCertificationStatus.REVOKED));

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
        expectDeleted(customerId, toolId);
    }

    @Test
    void deleteToolCertification_unknownCertification_is404() throws Exception {
        // given
        CustomerId customerId = CustomerId.random();
        ToolId toolId = ToolId.random();
        givenDeleteRejectedWith(customerId, toolId, ResourceNotFoundException.toolCertification(customerId, toolId));

        // when
        ResultActions result = deleteToolCertification(customerId, toolId);

        // then
        expectNotFound(result, Resource.TOOL_CERTIFICATION);
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

    private CustomerId givenNoCertificationsOf(CustomerId customerId) {
        when(this.toolCertificationReadFacade.getToolCertifications(customerId)).thenReturn(List.of());

        return customerId;
    }

    private void givenCreateRejectedWith(List<CustomerId> customerIds, ToolId toolId, CustomerId certifiedBy, RuntimeException rejection) {
        doThrow(rejection).when(this.toolCertificationCreate).process(customerIds, toolId, certifiedBy);
    }

    private void givenPauseRejectedWith(CustomerId customerId, ToolId toolId, RuntimeException rejection) {
        doThrow(rejection).when(this.toolCertificationPause).process(customerId, toolId);
    }

    private void givenReactivateRejectedWith(CustomerId customerId, ToolId toolId, RuntimeException rejection) {
        doThrow(rejection).when(this.toolCertificationReactivate).process(customerId, toolId);
    }

    private void givenRevokeRejectedWith(CustomerId customerId, ToolId toolId, RuntimeException rejection) {
        doThrow(rejection).when(this.toolCertificationRevoke).process(customerId, toolId);
    }

    private void givenDeleteRejectedWith(CustomerId customerId, ToolId toolId, RuntimeException rejection) {
        doThrow(rejection).when(this.toolCertificationDelete).process(customerId, toolId);
    }

    // TC-8, TC-5, TC-1, TC-2 – each rule of the batch with its code
    static Stream<Arguments> rejectionsOfTheBatch() {
        CustomerId customerId = CustomerId.random();
        ToolId toolId = ToolId.random();
        return Stream.of( //
                arguments(named("certifier is no instructor (TC-8)", new CustomerNotInstructorException(customerId, toolId)),
                        CustomerNotInstructorException.CODE), //
                arguments(named("customer is an instructor (TC-5)", new CustomerIsInstructorException(customerId, toolId)),
                        CustomerIsInstructorException.CODE), //
                arguments(named("certification exists (TC-1)", new CertificationAlreadyExistsException(customerId, toolId)),
                        CertificationAlreadyExistsException.CODE), //
                arguments(named("certification revoked (TC-2)", new CertificationPermanentlyRevokedException(customerId, toolId)),
                        CertificationPermanentlyRevokedException.CODE));
    }

    // ------------------------------------------------------------------------
    // when

    private ResultActions getToolCertification(ToolCertificationId toolCertificationId) throws Exception {
        return this.mockMvc.perform(get("/api/v1/tool-certifications/" + toolCertificationId.getValue()));
    }

    private ResultActions getToolCertifications() throws Exception {
        return this.mockMvc.perform(get("/api/v1/tool-certifications"));
    }

    private ResultActions getToolCertifications(CustomerId customerId) throws Exception {
        return this.mockMvc.perform(get("/api/v1/tool-certifications").param("customerId", customerId.getValue().toString()));
    }

    private ResultActions getToolCertifications(ToolId toolId) throws Exception {
        return this.mockMvc.perform(get("/api/v1/tool-certifications").param("toolId", toolId.getValue().toString()));
    }

    private ResultActions getToolCertifications(CustomerId customerId, ToolId toolId) throws Exception {
        return this.mockMvc.perform(get("/api/v1/tool-certifications") //
                .param("customerId", customerId.getValue().toString()) //
                .param("toolId", toolId.getValue().toString()));
    }

    private ResultActions createToolCertifications(List<CustomerId> customerIds, ToolId toolId, CustomerId certifiedBy) throws Exception {
        String customers = customerIds.stream() //
                .map(customerId -> "\"" + customerId.getValue() + "\"") //
                .collect(Collectors.joining(","));
        return this.mockMvc.perform(post("/api/v1/tool-certifications/batch") //
                .contentType(MediaType.APPLICATION_JSON) //
                .content("""
                        { "customerIds": [%s], "toolId": "%s", "certifiedBy": "%s" }
                        """.formatted(customers, toolId.getValue(), certifiedBy.getValue())));
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

    private void expectNotFound(ResultActions result, Resource resource) throws Exception {
        result.andExpect(status().isNotFound()) //
                .andExpect(jsonPath("$.code").value(ResourceNotFoundException.CODE)) //
                .andExpect(jsonPath("$.resource").value(resource.name()));
    }

    private void expectInvalidTransition(ResultActions result) throws Exception {
        result.andExpect(status().isConflict()) //
                .andExpect(jsonPath("$.code").value(InvalidCertificationStateTransitionException.CODE));
    }

    private void expectNothingRead() {
        verifyNoInteractions(this.toolCertificationReadFacade);
    }

    private void expectCreated(List<CustomerId> customerIds, ToolId toolId, CustomerId certifiedBy) {
        verify(this.toolCertificationCreate).process(customerIds, toolId, certifiedBy);
    }

    private void expectNothingCreated() {
        verify(this.toolCertificationCreate, never()).process(any(), any(), any());
    }

    private void expectPaused(CustomerId customerId, ToolId toolId) {
        verify(this.toolCertificationPause).process(customerId, toolId);
    }

    private void expectReactivated(CustomerId customerId, ToolId toolId) {
        verify(this.toolCertificationReactivate).process(customerId, toolId);
    }

    private void expectRevoked(CustomerId customerId, ToolId toolId) {
        verify(this.toolCertificationRevoke).process(customerId, toolId);
    }

    private void expectDeleted(CustomerId customerId, ToolId toolId) {
        verify(this.toolCertificationDelete).process(customerId, toolId);
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

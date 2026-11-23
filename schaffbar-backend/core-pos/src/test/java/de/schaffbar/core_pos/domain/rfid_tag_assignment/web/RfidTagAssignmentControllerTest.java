package de.schaffbar.core_pos.domain.rfid_tag_assignment.web;

import static de.schaffbar.core_pos.builder.ViewBuilder.rfidTagAssignmentHistoryView;
import static de.schaffbar.core_pos.builder.ViewBuilder.rfidTagAssignmentView;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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

import de.schaffbar.core_pos.builder.RandomValues;
import de.schaffbar.core_pos.domain.rfid_tag_assignment.RfidTagAssignmentType;
import de.schaffbar.core_pos.domain.rfid_tag_assignment.RfidTagAssignmentViews.RfidTagAssignmentHistoryView;
import de.schaffbar.core_pos.domain.rfid_tag_assignment.RfidTagAssignmentViews.RfidTagAssignmentView;
import de.schaffbar.core_pos.read_facade.RfidTagAssignmentReadFacade;
import de.schaffbar.core_pos.shared.exception.CustomerHasRfidTagAssignmentException;
import de.schaffbar.core_pos.shared.exception.NoRfidTagAssignedException;
import de.schaffbar.core_pos.shared.exception.NoRfidTagAssignmentWaitingException;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException.Resource;
import de.schaffbar.core_pos.shared.exception.RfidTagAlreadyAssignedException;
import de.schaffbar.core_pos.shared.exception.RfidTagAssignmentAlreadyWaitingException;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.RfidTagId;
import de.schaffbar.core_pos.shared.util.StringToCustomerIdConverter;
import de.schaffbar.core_pos.shared.util.StringToRfidTagIdConverter;
import de.schaffbar.core_pos.shared.web.ApiExceptionHandler;
import de.schaffbar.core_pos.support.Json;
import de.schaffbar.core_pos.use_case.RfidTagAssignmentAssignRfidTag;
import de.schaffbar.core_pos.use_case.RfidTagAssignmentRequest;
import de.schaffbar.core_pos.use_case.RfidTagAssignmentUnassignRfidTag;

@ExtendWith(MockitoExtension.class)
class RfidTagAssignmentControllerTest {

    @Mock
    private RfidTagAssignmentReadFacade rfidTagAssignmentReadFacade;

    @Mock
    private RfidTagAssignmentRequest rfidTagAssignmentRequest;

    @Mock
    private RfidTagAssignmentAssignRfidTag rfidTagAssignmentAssignRfidTag;

    @Mock
    private RfidTagAssignmentUnassignRfidTag rfidTagAssignmentUnassignRfidTag;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        DefaultFormattingConversionService conversionService = new DefaultFormattingConversionService();
        conversionService.addConverter(new StringToCustomerIdConverter());
        conversionService.addConverter(new StringToRfidTagIdConverter());

        RfidTagAssignmentController controller = new RfidTagAssignmentController(this.rfidTagAssignmentReadFacade, this.rfidTagAssignmentRequest,
                this.rfidTagAssignmentAssignRfidTag, this.rfidTagAssignmentUnassignRfidTag);

        this.mockMvc = MockMvcBuilders.standaloneSetup(controller) //
                .setConversionService(conversionService) //
                .setMessageConverters(Json.messageConverter()) //
                .setControllerAdvice(new ApiExceptionHandler()) //
                .build();
    }

    // ------------------------------------------------------------------------
    // getRfidTagAssignments

    @Test
    void getRfidTagAssignments_withoutFilter_returnsAll() throws Exception {
        // given
        RfidTagAssignmentView assignment = rfidTagAssignmentView().build();
        givenAssignments(assignment);

        // when
        ResultActions result = getRfidTagAssignments();

        // then
        result.andExpect(status().isOk()) //
                .andExpect(jsonPath("$.length()").value(1)) //
                .andExpect(jsonPath("$[0].id").value(assignment.id().getValue().toString())) //
                .andExpect(jsonPath("$[0].customerId").value(assignment.customerId().getValue().toString())) //
                .andExpect(jsonPath("$[0].assignmentType").value(assignment.assignmentType().name())) //
                .andExpect(jsonPath("$[0].status").value(assignment.status().name())) //
                .andExpect(jsonPath("$[0].rfidTagId").value(assignment.rfidTagId().getValue())) //
                .andExpect(jsonPath("$[0].assignmentDate").value(assignment.assignmentDate().toString()));
    }

    @Test
    void getRfidTagAssignments_withCustomerId_returnsTheAssignmentOfTheCustomer() throws Exception {
        // given
        RfidTagAssignmentView assignment = givenAssignmentOfCustomer();

        // when
        ResultActions result = getRfidTagAssignments(assignment.customerId());

        // then
        result.andExpect(status().isOk()) //
                .andExpect(jsonPath("$.length()").value(1)) //
                .andExpect(jsonPath("$[0].id").value(assignment.id().getValue().toString()));
    }

    @Test
    void getRfidTagAssignments_withCustomerIdWithoutAssignment_returnsAnEmptyList() throws Exception {
        // given
        CustomerId customerId = givenCustomerWithoutAssignment();

        // when
        ResultActions result = getRfidTagAssignments(customerId);

        // then
        result.andExpect(status().isOk()) //
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getRfidTagAssignments_withUnknownCustomerId_is404() throws Exception {
        // given
        CustomerId unknown = givenUnknownCustomer();

        // when
        ResultActions result = getRfidTagAssignments(unknown);

        // then
        expectNotFound(result, Resource.CUSTOMER);
    }

    @Test
    void getRfidTagAssignments_withRfidTagId_returnsTheAssignmentOfTheTag() throws Exception {
        // given
        RfidTagAssignmentView assignment = givenAssignmentOfTag();

        // when
        ResultActions result = getRfidTagAssignments(assignment.rfidTagId());

        // then
        result.andExpect(status().isOk()) //
                .andExpect(jsonPath("$.length()").value(1)) //
                .andExpect(jsonPath("$[0].id").value(assignment.id().getValue().toString()));
    }

    @Test
    void getRfidTagAssignments_withUnassignedRfidTagId_returnsAnEmptyList() throws Exception {
        // given
        RfidTagId rfidTagId = givenUnassignedTag();

        // when
        ResultActions result = getRfidTagAssignments(rfidTagId);

        // then
        result.andExpect(status().isOk()) //
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getRfidTagAssignments_withBothFilters_filtersByTheTag() throws Exception {
        // given
        RfidTagAssignmentView assignment = givenAssignmentOfTag();

        // when
        ResultActions result = getRfidTagAssignments(CustomerId.random(), assignment.rfidTagId());

        // then
        result.andExpect(status().isOk()) //
                .andExpect(jsonPath("$.length()").value(1)) //
                .andExpect(jsonPath("$[0].id").value(assignment.id().getValue().toString()));
    }

    // ------------------------------------------------------------------------
    // getRfidTagAssignmentHistory

    @Test
    void getRfidTagAssignmentHistory_returnsAllEntries() throws Exception {
        // given
        RfidTagAssignmentHistoryView historyEntry = rfidTagAssignmentHistoryView().build();
        givenHistory(historyEntry);

        // when
        ResultActions result = getRfidTagAssignmentHistory();

        // then
        result.andExpect(status().isOk()) //
                .andExpect(jsonPath("$.length()").value(1)) //
                .andExpect(jsonPath("$[0].customerId").value(historyEntry.customerId().getValue().toString())) //
                .andExpect(jsonPath("$[0].rfidTagId").value(historyEntry.rfidTagId().getValue())) //
                .andExpect(jsonPath("$[0].assignmentType").value(historyEntry.assignmentType().name())) //
                .andExpect(jsonPath("$[0].assignmentDate").value(historyEntry.assignmentDate().toString())) //
                .andExpect(jsonPath("$[0].unassignmentDate").value(historyEntry.unassignmentDate().toString()));
    }

    // ------------------------------------------------------------------------
    // requestRfidTagAssignment

    @Test
    void requestRfidTagAssignment_is204() throws Exception {
        // given
        CustomerId customerId = CustomerId.random();

        // when
        ResultActions result = requestRfidTagAssignment(customerId, RfidTagAssignmentType.TEMPORARY);

        // then
        result.andExpect(status().isNoContent());
        expectRequested(customerId, RfidTagAssignmentType.TEMPORARY);
    }

    @Test
    void requestRfidTagAssignment_withoutCustomerId_is400() throws Exception {
        // when
        ResultActions result = requestRfidTagAssignment("{\"assignmentType\":\"FIXED\"}");

        // then
        result.andExpect(status().isBadRequest());
        expectNothingRequested();
    }

    @Test
    void requestRfidTagAssignment_ofACustomerWithAssignment_is409WithTheCode() throws Exception {
        // given
        CustomerId customerId = CustomerId.random();
        givenRequestRejectedWith(customerId, new CustomerHasRfidTagAssignmentException(customerId));

        // when
        ResultActions result = requestRfidTagAssignment(customerId, RfidTagAssignmentType.FIXED);

        // then
        result.andExpect(status().isConflict()) //
                .andExpect(jsonPath("$.code").value(CustomerHasRfidTagAssignmentException.CODE));
    }

    @Test
    void requestRfidTagAssignment_whileAnotherRequestWaits_is409WithTheCode() throws Exception {
        // given
        CustomerId customerId = CustomerId.random();
        givenRequestRejectedWith(customerId, new RfidTagAssignmentAlreadyWaitingException(customerId, CustomerId.random()));

        // when
        ResultActions result = requestRfidTagAssignment(customerId, RfidTagAssignmentType.FIXED);

        // then
        result.andExpect(status().isConflict()) //
                .andExpect(jsonPath("$.code").value(RfidTagAssignmentAlreadyWaitingException.CODE));
    }

    @Test
    void requestRfidTagAssignment_ofAnUnknownCustomer_is404() throws Exception {
        // given
        CustomerId unknown = CustomerId.random();
        givenRequestRejectedWith(unknown, ResourceNotFoundException.customer(unknown));

        // when
        ResultActions result = requestRfidTagAssignment(unknown, RfidTagAssignmentType.FIXED);

        // then
        expectNotFound(result, Resource.CUSTOMER);
    }

    // ------------------------------------------------------------------------
    // assignRfidTag

    @Test
    void assignRfidTag_is204() throws Exception {
        // given
        RfidTagId rfidTagId = RandomValues.rfidTagId();

        // when
        ResultActions result = assignRfidTag(rfidTagId);

        // then
        result.andExpect(status().isNoContent());
        expectAssigned(rfidTagId);
    }

    @Test
    void assignRfidTag_ofAnAssignedTag_is409WithTheCode() throws Exception {
        // given
        RfidTagId rfidTagId = RandomValues.rfidTagId();
        givenAssignRejectedWith(rfidTagId, new RfidTagAlreadyAssignedException(rfidTagId, CustomerId.random()));

        // when
        ResultActions result = assignRfidTag(rfidTagId);

        // then
        result.andExpect(status().isConflict()) //
                .andExpect(jsonPath("$.code").value(RfidTagAlreadyAssignedException.CODE));
    }

    @Test
    void assignRfidTag_withoutWaitingRequest_is409WithTheCode() throws Exception {
        // given
        RfidTagId rfidTagId = RandomValues.rfidTagId();
        givenAssignRejectedWith(rfidTagId, NoRfidTagAssignmentWaitingException.forRfidTag(rfidTagId));

        // when
        ResultActions result = assignRfidTag(rfidTagId);

        // then
        result.andExpect(status().isConflict()) //
                .andExpect(jsonPath("$.code").value(NoRfidTagAssignmentWaitingException.CODE));
    }

    @Test
    void assignRfidTag_ofAnUnknownTag_is404() throws Exception {
        // given
        RfidTagId unknown = RandomValues.rfidTagId();
        givenAssignRejectedWith(unknown, ResourceNotFoundException.rfidTag(unknown));

        // when
        ResultActions result = assignRfidTag(unknown);

        // then
        expectNotFound(result, Resource.RFID_TAG);
    }

    // ------------------------------------------------------------------------
    // unassignRfidTag

    @Test
    void unassignRfidTag_is204() throws Exception {
        // given
        CustomerId customerId = CustomerId.random();

        // when
        ResultActions result = unassignRfidTag(customerId);

        // then
        result.andExpect(status().isNoContent());
        expectUnassigned(customerId);
    }

    @Test
    void unassignRfidTag_withoutAssignedTag_is409WithTheCode() throws Exception {
        // given
        CustomerId customerId = CustomerId.random();
        givenUnassignRejectedWith(customerId, new NoRfidTagAssignedException(customerId));

        // when
        ResultActions result = unassignRfidTag(customerId);

        // then
        result.andExpect(status().isConflict()) //
                .andExpect(jsonPath("$.code").value(NoRfidTagAssignedException.CODE));
    }

    @Test
    void unassignRfidTag_ofAnUnknownCustomer_is404() throws Exception {
        // given
        CustomerId unknown = CustomerId.random();
        givenUnassignRejectedWith(unknown, ResourceNotFoundException.customer(unknown));

        // when
        ResultActions result = unassignRfidTag(unknown);

        // then
        expectNotFound(result, Resource.CUSTOMER);
    }

    // ------------------------------------------------------------------------
    // given

    private void givenAssignments(RfidTagAssignmentView... assignments) {
        when(this.rfidTagAssignmentReadFacade.getRfidTagAssignments()).thenReturn(List.of(assignments));
    }

    private RfidTagAssignmentView givenAssignmentOfCustomer() {
        RfidTagAssignmentView assignment = rfidTagAssignmentView().build();
        when(this.rfidTagAssignmentReadFacade.getRfidTagAssignment(assignment.customerId())).thenReturn(Optional.of(assignment));

        return assignment;
    }

    private CustomerId givenCustomerWithoutAssignment() {
        CustomerId customerId = CustomerId.random();
        when(this.rfidTagAssignmentReadFacade.getRfidTagAssignment(customerId)).thenReturn(Optional.empty());

        return customerId;
    }

    private CustomerId givenUnknownCustomer() {
        CustomerId unknown = CustomerId.random();
        when(this.rfidTagAssignmentReadFacade.getRfidTagAssignment(unknown)).thenThrow(ResourceNotFoundException.customer(unknown));

        return unknown;
    }

    private RfidTagAssignmentView givenAssignmentOfTag() {
        RfidTagAssignmentView assignment = rfidTagAssignmentView().build();
        when(this.rfidTagAssignmentReadFacade.getRfidTagAssignment(assignment.rfidTagId())).thenReturn(Optional.of(assignment));

        return assignment;
    }

    private RfidTagId givenUnassignedTag() {
        RfidTagId rfidTagId = RandomValues.rfidTagId();
        when(this.rfidTagAssignmentReadFacade.getRfidTagAssignment(rfidTagId)).thenReturn(Optional.empty());

        return rfidTagId;
    }

    private void givenHistory(RfidTagAssignmentHistoryView... historyEntries) {
        when(this.rfidTagAssignmentReadFacade.getRfidTagAssignmentHistory()).thenReturn(List.of(historyEntries));
    }

    private void givenRequestRejectedWith(CustomerId customerId, RuntimeException rejection) {
        doThrow(rejection).when(this.rfidTagAssignmentRequest).process(customerId, RfidTagAssignmentType.FIXED);
    }

    private void givenAssignRejectedWith(RfidTagId rfidTagId, RuntimeException rejection) {
        doThrow(rejection).when(this.rfidTagAssignmentAssignRfidTag).process(rfidTagId);
    }

    private void givenUnassignRejectedWith(CustomerId customerId, RuntimeException rejection) {
        doThrow(rejection).when(this.rfidTagAssignmentUnassignRfidTag).process(customerId);
    }

    // ------------------------------------------------------------------------
    // when

    private ResultActions getRfidTagAssignments() throws Exception {
        return this.mockMvc.perform(get("/api/v1/rfid-tag-assignments"));
    }

    private ResultActions getRfidTagAssignments(CustomerId customerId) throws Exception {
        return this.mockMvc.perform(get("/api/v1/rfid-tag-assignments").param("customerId", customerId.getValue().toString()));
    }

    private ResultActions getRfidTagAssignments(RfidTagId rfidTagId) throws Exception {
        return this.mockMvc.perform(get("/api/v1/rfid-tag-assignments").param("rfidTagId", rfidTagId.getValue()));
    }

    private ResultActions getRfidTagAssignments(CustomerId customerId, RfidTagId rfidTagId) throws Exception {
        return this.mockMvc.perform(get("/api/v1/rfid-tag-assignments") //
                .param("customerId", customerId.getValue().toString()) //
                .param("rfidTagId", rfidTagId.getValue()));
    }

    private ResultActions getRfidTagAssignmentHistory() throws Exception {
        return this.mockMvc.perform(get("/api/v1/rfid-tag-assignments/history"));
    }

    private ResultActions requestRfidTagAssignment(CustomerId customerId, RfidTagAssignmentType assignmentType) throws Exception {
        return requestRfidTagAssignment("{\"customerId\":\"" + customerId.getValue() + "\",\"assignmentType\":\"" + assignmentType + "\"}");
    }

    private ResultActions requestRfidTagAssignment(String body) throws Exception {
        return this.mockMvc.perform(post("/api/v1/rfid-tag-assignments") //
                .contentType(MediaType.APPLICATION_JSON) //
                .content(body));
    }

    private ResultActions assignRfidTag(RfidTagId rfidTagId) throws Exception {
        return this.mockMvc.perform(put("/api/v1/rfid-tag-assignments/assign") //
                .contentType(MediaType.APPLICATION_JSON) //
                .content("{\"rfidTagId\":\"" + rfidTagId.getValue() + "\"}"));
    }

    private ResultActions unassignRfidTag(CustomerId customerId) throws Exception {
        return this.mockMvc.perform(put("/api/v1/rfid-tag-assignments/unassign") //
                .contentType(MediaType.APPLICATION_JSON) //
                .content("{\"customerId\":\"" + customerId.getValue() + "\"}"));
    }

    // ------------------------------------------------------------------------
    // then

    private void expectNotFound(ResultActions result, Resource resource) throws Exception {
        result.andExpect(status().isNotFound()) //
                .andExpect(jsonPath("$.code").value(ResourceNotFoundException.CODE)) //
                .andExpect(jsonPath("$.resource").value(resource.name()));
    }

    private void expectRequested(CustomerId customerId, RfidTagAssignmentType assignmentType) {
        verify(this.rfidTagAssignmentRequest).process(customerId, assignmentType);
    }

    private void expectNothingRequested() {
        verify(this.rfidTagAssignmentRequest, never()).process(any(), any());
    }

    private void expectAssigned(RfidTagId rfidTagId) {
        verify(this.rfidTagAssignmentAssignRfidTag).process(rfidTagId);
    }

    private void expectUnassigned(CustomerId customerId) {
        verify(this.rfidTagAssignmentUnassignRfidTag).process(customerId);
    }

}

package de.schaffbar.core_pos.domain.rfid_tag_assignment.web;

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
import de.schaffbar.core_pos.domain.rfid_tag_assignment.RfidTagAssignmentViews.RfidTagAssignmentView;
import de.schaffbar.core_pos.read_facade.RfidTagAssignmentReadFacade;
import de.schaffbar.core_pos.shared.exception.RfidTagAssignmentAlreadyWaitingException;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.RfidTagId;
import de.schaffbar.core_pos.shared.util.StringToCustomerIdConverter;
import de.schaffbar.core_pos.shared.util.StringToRfidTagIdConverter;
import de.schaffbar.core_pos.shared.web.ApiExceptionHandler;
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
                .andExpect(jsonPath("$[0].customerId").value(assignment.customerId().getValue().toString())) //
                .andExpect(jsonPath("$[0].rfidTagId").value(assignment.rfidTagId().getValue())) //
                .andExpect(jsonPath("$[0].status").value("ASSIGNED"));
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
    void getRfidTagAssignments_withRfidTagId_returnsTheAssignmentOfTheTag() throws Exception {
        // given
        RfidTagAssignmentView assignment = givenAssignmentOfTag();

        // when
        ResultActions result = getRfidTagAssignments(assignment.rfidTagId());

        // then
        result.andExpect(status().isOk()) //
                .andExpect(jsonPath("$.length()").value(1)) //
                .andExpect(jsonPath("$[0].id").value(assignment.id().getValue().toString())) //
                .andExpect(jsonPath("$[0].rfidTagId").value(assignment.rfidTagId().getValue()));
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

    // ------------------------------------------------------------------------
    // requestRfidTagAssignment

    @Test
    void requestRfidTagAssignment_returns204() throws Exception {
        // given
        CustomerId customerId = CustomerId.random();

        // when
        ResultActions result = requestRfidTagAssignment(customerId, RfidTagAssignmentType.TEMPORARY);

        // then
        result.andExpect(status().isNoContent());
        verify(this.rfidTagAssignmentRequest).process(customerId, RfidTagAssignmentType.TEMPORARY);
    }

    @Test
    void requestRfidTagAssignment_withoutCustomerId_is400() throws Exception {
        // when
        ResultActions result = requestRfidTagAssignment("{\"assignmentType\":\"FIXED\"}");

        // then
        result.andExpect(status().isBadRequest());
        verify(this.rfidTagAssignmentRequest, never()).process(any(), any());
    }

    @Test
    void requestRfidTagAssignment_whileAnotherRequestWaits_is409WithCode() throws Exception {
        // given
        CustomerId customerId = givenAnotherRequestWaiting();

        // when
        ResultActions result = requestRfidTagAssignment(customerId, RfidTagAssignmentType.FIXED);

        // then
        result.andExpect(status().isConflict()) //
                .andExpect(jsonPath("$.code").value(RfidTagAssignmentAlreadyWaitingException.CODE));
    }

    // ------------------------------------------------------------------------
    // assignRfidTag

    @Test
    void assignRfidTag_returns204() throws Exception {
        // given
        RfidTagId rfidTagId = RandomValues.rfidTagId();

        // when
        ResultActions result = assignRfidTag(rfidTagId);

        // then
        result.andExpect(status().isNoContent());
        verify(this.rfidTagAssignmentAssignRfidTag).process(rfidTagId);
    }

    // ------------------------------------------------------------------------
    // unassignRfidTag

    @Test
    void unassignRfidTag_returns204() throws Exception {
        // given
        CustomerId customerId = CustomerId.random();

        // when
        ResultActions result = unassignRfidTag(customerId);

        // then
        result.andExpect(status().isNoContent());
        verify(this.rfidTagAssignmentUnassignRfidTag).process(customerId);
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

    private CustomerId givenAnotherRequestWaiting() {
        CustomerId customerId = CustomerId.random();
        doThrow(new RfidTagAssignmentAlreadyWaitingException(customerId, CustomerId.random())) //
                .when(this.rfidTagAssignmentRequest).process(customerId, RfidTagAssignmentType.FIXED);

        return customerId;
    }

    // ------------------------------------------------------------------------
    // when

    private ResultActions getRfidTagAssignments() throws Exception {
        return this.mockMvc.perform(get("/api/v1/rfid-tag-assignments"));
    }

    private ResultActions getRfidTagAssignments(CustomerId customerId) throws Exception {
        return this.mockMvc.perform(get("/api/v1/rfid-tag-assignments").param("customer-id", customerId.getValue().toString()));
    }

    private ResultActions getRfidTagAssignments(RfidTagId rfidTagId) throws Exception {
        return this.mockMvc.perform(get("/api/v1/rfid-tag-assignments").param("rfid-tag-id", rfidTagId.getValue()));
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

}

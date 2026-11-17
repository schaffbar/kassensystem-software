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
import de.schaffbar.core_pos.use_case.CustomerAssignRfidTag;
import de.schaffbar.core_pos.use_case.CustomerRequestRfidTagAssignment;
import de.schaffbar.core_pos.use_case.CustomerUnassignRfidTag;

@ExtendWith(MockitoExtension.class)
class RfidTagAssignmentControllerTest {

    @Mock
    private RfidTagAssignmentReadFacade rfidTagAssignmentReadFacade;

    @Mock
    private CustomerRequestRfidTagAssignment customerRequestRfidTagAssignment;

    @Mock
    private CustomerAssignRfidTag customerAssignRfidTag;

    @Mock
    private CustomerUnassignRfidTag customerUnassignRfidTag;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        DefaultFormattingConversionService conversionService = new DefaultFormattingConversionService();
        conversionService.addConverter(new StringToCustomerIdConverter());
        conversionService.addConverter(new StringToRfidTagIdConverter());

        RfidTagAssignmentController controller = new RfidTagAssignmentController(this.rfidTagAssignmentReadFacade, this.customerRequestRfidTagAssignment,
                this.customerAssignRfidTag, this.customerUnassignRfidTag);

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
        ResultActions result = this.mockMvc.perform(get("/api/v1/rfid-tag-assignments"));

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
        ResultActions result = this.mockMvc.perform(get("/api/v1/rfid-tag-assignments").param("customer-id", assignment.customerId().getValue().toString()));

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
        ResultActions result = this.mockMvc.perform(get("/api/v1/rfid-tag-assignments").param("rfid-tag-id", assignment.rfidTagId().getValue()));

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
        ResultActions result = this.mockMvc.perform(get("/api/v1/rfid-tag-assignments").param("rfid-tag-id", rfidTagId.getValue()));

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
        ResultActions result = this.mockMvc.perform(post("/api/v1/rfid-tag-assignments") //
                .contentType(MediaType.APPLICATION_JSON) //
                .content("{\"customerId\":\"" + customerId.getValue() + "\",\"assignmentType\":\"TEMPORARY\"}"));

        // then
        result.andExpect(status().isNoContent());
        verify(this.customerRequestRfidTagAssignment).process(customerId, RfidTagAssignmentType.TEMPORARY);
    }

    @Test
    void requestRfidTagAssignment_withoutCustomerId_is400() throws Exception {
        // when
        ResultActions result = this.mockMvc.perform(post("/api/v1/rfid-tag-assignments") //
                .contentType(MediaType.APPLICATION_JSON) //
                .content("{\"assignmentType\":\"FIXED\"}"));

        // then
        result.andExpect(status().isBadRequest());
        verify(this.customerRequestRfidTagAssignment, never()).process(any(), any());
    }

    @Test
    void requestRfidTagAssignment_whileAnotherRequestWaits_is409WithCode() throws Exception {
        // given
        CustomerId customerId = givenAnotherRequestWaiting();

        // when
        ResultActions result = this.mockMvc.perform(post("/api/v1/rfid-tag-assignments") //
                .contentType(MediaType.APPLICATION_JSON) //
                .content("{\"customerId\":\"" + customerId.getValue() + "\",\"assignmentType\":\"FIXED\"}"));

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
        ResultActions result = this.mockMvc.perform(put("/api/v1/rfid-tag-assignments/assign") //
                .contentType(MediaType.APPLICATION_JSON) //
                .content("{\"rfidTagId\":\"" + rfidTagId.getValue() + "\"}"));

        // then
        result.andExpect(status().isNoContent());
        verify(this.customerAssignRfidTag).process(rfidTagId);
    }

    // ------------------------------------------------------------------------
    // unassignRfidTag

    @Test
    void unassignRfidTag_returns204() throws Exception {
        // given
        CustomerId customerId = CustomerId.random();

        // when
        ResultActions result = this.mockMvc.perform(put("/api/v1/rfid-tag-assignments/unassign") //
                .contentType(MediaType.APPLICATION_JSON) //
                .content("{\"customerId\":\"" + customerId.getValue() + "\"}"));

        // then
        result.andExpect(status().isNoContent());
        verify(this.customerUnassignRfidTag).process(customerId);
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
                .when(this.customerRequestRfidTagAssignment).process(customerId, RfidTagAssignmentType.FIXED);

        return customerId;
    }

}

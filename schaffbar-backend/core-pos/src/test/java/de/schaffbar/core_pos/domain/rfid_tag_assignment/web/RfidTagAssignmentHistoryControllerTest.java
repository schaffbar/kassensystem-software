package de.schaffbar.core_pos.domain.rfid_tag_assignment.web;

import static de.schaffbar.core_pos.builder.ViewBuilder.rfidTagAssignmentHistoryView;
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

import de.schaffbar.core_pos.domain.rfid_tag_assignment.RfidTagAssignmentViews.RfidTagAssignmentHistoryView;
import de.schaffbar.core_pos.read_facade.RfidTagAssignmentHistoryReadFacade;
import de.schaffbar.core_pos.shared.web.ApiExceptionHandler;

@ExtendWith(MockitoExtension.class)
class RfidTagAssignmentHistoryControllerTest {

    @Mock
    private RfidTagAssignmentHistoryReadFacade rfidTagAssignmentHistoryReadFacade;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(new RfidTagAssignmentHistoryController(this.rfidTagAssignmentHistoryReadFacade)) //
                .setControllerAdvice(new ApiExceptionHandler()) //
                .build();
    }

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
                .andExpect(jsonPath("$[0].assignmentType").value("TEMPORARY"));
    }

    // ------------------------------------------------------------------------
    // given

    private void givenHistory(RfidTagAssignmentHistoryView... historyEntries) {
        when(this.rfidTagAssignmentHistoryReadFacade.getRfidTagAssignmentHistory()).thenReturn(List.of(historyEntries));
    }

    // ------------------------------------------------------------------------
    // when

    private ResultActions getRfidTagAssignmentHistory() throws Exception {
        return this.mockMvc.perform(get("/api/v1/rfid-tag-assignment-history"));
    }

}

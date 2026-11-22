package de.schaffbar.core_pos.use_case;

import static de.schaffbar.core_pos.builder.ViewBuilder.rfidTagAssignmentView;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.schaffbar.core_pos.builder.RandomValues;
import de.schaffbar.core_pos.domain.rfid_tag.RfidTagService;
import de.schaffbar.core_pos.domain.rfid_tag_assignment.RfidTagAssignmentService;
import de.schaffbar.core_pos.shared.exception.RfidTagAssignedToCustomerException;
import de.schaffbar.core_pos.shared.id.RfidTagId;

@ExtendWith(MockitoExtension.class)
class RfidTagDeleteTest {

    @Mock
    private RfidTagService rfidTagService;

    @Mock
    private RfidTagAssignmentService rfidTagAssignmentService;

    @InjectMocks
    private RfidTagDelete rfidTagDelete;

    @Test
    void deletesTagWithoutAssignment() {
        // given
        RfidTagId rfidTagId = RandomValues.rfidTagId();
        givenNoAssignmentOf(rfidTagId);

        // when
        this.rfidTagDelete.process(rfidTagId);

        // then
        expectDeleted(rfidTagId);
    }

    @Test
    void rejectsTagAssignedToCustomer() {
        // given
        RfidTagId rfidTagId = RandomValues.rfidTagId();
        givenAssignmentOf(rfidTagId);

        // when
        Throwable thrown = catchThrowable(() -> this.rfidTagDelete.process(rfidTagId));

        // then
        assertThat(thrown) //
                .isInstanceOf(RfidTagAssignedToCustomerException.class) //
                .hasFieldOrPropertyWithValue("code", RfidTagAssignedToCustomerException.CODE);
        expectNotDeleted();
    }

    // ------------------------------------------------------------------------
    // given

    private void givenAssignmentOf(RfidTagId rfidTagId) {
        when(this.rfidTagAssignmentService.getRfidTagAssignment(rfidTagId)) //
                .thenReturn(Optional.of(rfidTagAssignmentView().rfidTagId(rfidTagId).build()));
    }

    private void givenNoAssignmentOf(RfidTagId rfidTagId) {
        when(this.rfidTagAssignmentService.getRfidTagAssignment(rfidTagId)) //
                .thenReturn(Optional.empty());
    }

    // ------------------------------------------------------------------------
    // then

    private void expectDeleted(RfidTagId rfidTagId) {
        verify(this.rfidTagService).deleteRfidTag(rfidTagId);
    }

    private void expectNotDeleted() {
        verify(this.rfidTagService, never()).deleteRfidTag(any());
    }

}

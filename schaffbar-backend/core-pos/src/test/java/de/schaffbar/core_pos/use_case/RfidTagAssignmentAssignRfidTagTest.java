package de.schaffbar.core_pos.use_case;

import static de.schaffbar.core_pos.builder.ViewBuilder.rfidTagView;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException.Resource;
import de.schaffbar.core_pos.shared.id.RfidTagId;

@ExtendWith(MockitoExtension.class)
class RfidTagAssignmentAssignRfidTagTest {

    @Mock
    private RfidTagService rfidTagService;

    @Mock
    private RfidTagAssignmentService rfidTagAssignmentService;

    @InjectMocks
    private RfidTagAssignmentAssignRfidTag rfidTagAssignmentAssignRfidTag;

    @Test
    void assignsARegisteredTag() {
        // given
        RfidTagId rfidTagId = givenRegisteredTag();

        // when
        this.rfidTagAssignmentAssignRfidTag.process(rfidTagId);

        // then
        expectAssigned(rfidTagId);
    }

    @Test
    void rejectsAnUnknownTag() {
        // given
        RfidTagId unknown = givenUnknownTag();

        // when
        Throwable thrown = catchThrowable(() -> this.rfidTagAssignmentAssignRfidTag.process(unknown));

        // then
        assertThat(thrown) //
                .isInstanceOf(ResourceNotFoundException.class) //
                .hasFieldOrPropertyWithValue("resource", Resource.RFID_TAG);
        expectNothingAssigned();
    }

    // ------------------------------------------------------------------------
    // given

    private RfidTagId givenRegisteredTag() {
        RfidTagId rfidTagId = RandomValues.rfidTagId();
        when(this.rfidTagService.getRfidTag(rfidTagId)).thenReturn(Optional.of(rfidTagView(rfidTagId).build()));

        return rfidTagId;
    }

    private RfidTagId givenUnknownTag() {
        RfidTagId unknown = RandomValues.rfidTagId();
        when(this.rfidTagService.getRfidTag(unknown)).thenReturn(Optional.empty());

        return unknown;
    }

    // ------------------------------------------------------------------------
    // then

    private void expectAssigned(RfidTagId rfidTagId) {
        verify(this.rfidTagAssignmentService).assignRfidTag(rfidTagId);
    }

    private void expectNothingAssigned() {
        verifyNoInteractions(this.rfidTagAssignmentService);
    }

}

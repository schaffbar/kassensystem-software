package de.schaffbar.core_pos.read_facade;

import static de.schaffbar.core_pos.builder.ViewBuilder.activeWorkshopUsageView;
import static de.schaffbar.core_pos.builder.ViewBuilder.customerView;
import static de.schaffbar.core_pos.builder.ViewBuilder.openWorkshopSessionView;
import static de.schaffbar.core_pos.builder.ViewBuilder.rfidTagAssignmentView;
import static de.schaffbar.core_pos.builder.ViewBuilder.rfidTagView;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.schaffbar.core_pos.builder.RandomValues;
import de.schaffbar.core_pos.domain.customer.CustomerService;
import de.schaffbar.core_pos.domain.customer.CustomerViews.CustomerView;
import de.schaffbar.core_pos.domain.rfid_reader.RfidReaderService;
import de.schaffbar.core_pos.domain.rfid_tag.RfidTagService;
import de.schaffbar.core_pos.domain.rfid_tag_assignment.RfidTagAssignmentService;
import de.schaffbar.core_pos.domain.tool.ToolService;
import de.schaffbar.core_pos.domain.tool_usage.ToolUsageService;
import de.schaffbar.core_pos.domain.workshop_session.WorkshopSessionService;
import de.schaffbar.core_pos.domain.workshop_session.WorkshopSessionViews.WorkshopSessionView;
import de.schaffbar.core_pos.domain.workshop_usage.WorkshopUsageService;
import de.schaffbar.core_pos.domain.workshop_usage.WorkshopUsageViews.WorkshopUsageView;
import de.schaffbar.core_pos.read_facade.DeviceViews.WorkshopStayView;
import de.schaffbar.core_pos.shared.billing.BillableDuration;
import de.schaffbar.core_pos.shared.exception.NoCustomerAssignedException;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException.Resource;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.MacAddress;
import de.schaffbar.core_pos.shared.id.RfidTagAssignmentId;
import de.schaffbar.core_pos.shared.id.RfidTagId;

@ExtendWith(MockitoExtension.class)
class DeviceReadFacadeTest {

    @Mock
    private RfidReaderService rfidReaderService;

    @Mock
    private RfidTagService rfidTagService;

    @Mock
    private RfidTagAssignmentService rfidTagAssignmentService;

    @Mock
    private CustomerService customerService;

    @Mock
    private ToolService toolService;

    @Mock
    private ToolUsageService toolUsageService;

    @Mock
    private WorkshopSessionService workshopSessionService;

    @Mock
    private WorkshopUsageService workshopUsageService;

    @InjectMocks
    private DeviceReadFacade deviceReadFacade;

    // ------------------------------------------------------------------------
    // getRfidReader

    @Test
    void getRfidReader_unknownMacAddress_isNotFound() {
        // given
        MacAddress unknown = RandomValues.macAddress();
        when(this.rfidReaderService.getRfidReader(unknown)).thenReturn(Optional.empty());

        // when
        Throwable thrown = catchThrowable(() -> this.deviceReadFacade.getRfidReader(unknown));

        // then
        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class).hasFieldOrPropertyWithValue("resource", Resource.RFID_READER);
    }

    // ------------------------------------------------------------------------
    // getCustomer

    @Test
    void getCustomer_returnsTheOwnerOfTheTag() {
        // given
        RfidTagId rfidTagId = givenRegisteredTag();
        CustomerView owner = customerView().build();
        givenAssignment(rfidTagId, owner.id());
        when(this.customerService.getCustomer(owner.id())).thenReturn(Optional.of(owner));

        // when
        CustomerView customer = this.deviceReadFacade.getCustomer(rfidTagId);

        // then
        assertThat(customer).isEqualTo(owner);
    }

    @Test
    void getCustomer_unregisteredTag_isNotFound() {
        // given
        RfidTagId unregistered = RandomValues.rfidTagId();
        when(this.rfidTagService.getRfidTag(unregistered)).thenReturn(Optional.empty());

        // when
        Throwable thrown = catchThrowable(() -> this.deviceReadFacade.getCustomer(unregistered));

        // then
        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class).hasFieldOrPropertyWithValue("resource", Resource.RFID_TAG);
    }

    @Test
    void getCustomer_tagWithoutCustomer_isRejected() {
        // given
        RfidTagId rfidTagId = givenRegisteredTag();
        when(this.rfidTagAssignmentService.getRfidTagAssignment(rfidTagId)).thenReturn(Optional.empty());

        // when
        Throwable thrown = catchThrowable(() -> this.deviceReadFacade.getCustomer(rfidTagId));

        // then
        assertThat(thrown) //
                .isInstanceOf(NoCustomerAssignedException.class) //
                .hasFieldOrPropertyWithValue("code", NoCustomerAssignedException.CODE);
    }

    // ------------------------------------------------------------------------
    // isRfidTagAssigned

    @Test
    void isRfidTagAssigned_tagWithOwner_isTrue() {
        // given
        RfidTagId rfidTagId = RandomValues.rfidTagId();
        givenAssignment(rfidTagId, CustomerId.random());

        // when
        boolean assigned = this.deviceReadFacade.isRfidTagAssigned(rfidTagId);

        // then
        assertThat(assigned).isTrue();
    }

    @Test
    void isRfidTagAssigned_tagWithoutOwner_isFalse() {
        // given
        RfidTagId rfidTagId = RandomValues.rfidTagId();
        when(this.rfidTagAssignmentService.getRfidTagAssignment(rfidTagId)).thenReturn(Optional.empty());

        // when
        boolean assigned = this.deviceReadFacade.isRfidTagAssigned(rfidTagId);

        // then
        assertThat(assigned).isFalse();
    }

    // ------------------------------------------------------------------------
    // getWorkshopStay

    @Test
    void getWorkshopStay_countsTheFinishedStaysAndShowsTheLastOne() {
        // given – checked in, out and in again
        WorkshopSessionView session = givenOpenSession();
        Instant firstEntry = Instant.now().minusSeconds(3600);
        Instant firstExit = firstEntry.plusSeconds(1500);
        WorkshopUsageView finished = activeWorkshopUsageView() //
                .entryTime(firstEntry) //
                .exitTime(firstExit) //
                .billableDuration(BillableDuration.between(firstEntry, firstExit)) //
                .build();
        WorkshopUsageView running = activeWorkshopUsageView().entryTime(Instant.now().minusSeconds(60)).build();
        when(this.workshopUsageService.getWorkshopUsages(session.id())).thenReturn(List.of(finished, running));

        // when
        WorkshopStayView stay = this.deviceReadFacade.getWorkshopStay(session.customerId());

        // then
        assertThat(stay.finishedUnits()).isEqualTo(finished.billableDuration().getUnits());
        assertThat(stay.entryTime()).isEqualTo(running.entryTime());
        assertThat(stay.exitTime()).isNull();
    }

    @Test
    void getWorkshopStay_withoutOpenSession_isABug() {
        // given
        CustomerId customerId = CustomerId.random();
        when(this.workshopSessionService.getOpenWorkshopSession(customerId)).thenReturn(Optional.empty());

        // when
        Throwable thrown = catchThrowable(() -> this.deviceReadFacade.getWorkshopStay(customerId));

        // then
        assertThat(thrown).isInstanceOf(IllegalStateException.class);
    }

    // ------------------------------------------------------------------------
    // given

    private RfidTagId givenRegisteredTag() {
        RfidTagId rfidTagId = RandomValues.rfidTagId();
        when(this.rfidTagService.getRfidTag(rfidTagId)).thenReturn(Optional.of(rfidTagView(rfidTagId).build()));

        return rfidTagId;
    }

    private void givenAssignment(RfidTagId rfidTagId, CustomerId customerId) {
        when(this.rfidTagAssignmentService.getRfidTagAssignment(rfidTagId)) //
                .thenReturn(Optional.of(rfidTagAssignmentView(RfidTagAssignmentId.random()).rfidTagId(rfidTagId).customerId(customerId).build()));
    }

    private WorkshopSessionView givenOpenSession() {
        WorkshopSessionView session = openWorkshopSessionView().build();
        when(this.workshopSessionService.getOpenWorkshopSession(session.customerId())).thenReturn(Optional.of(session));

        return session;
    }

}

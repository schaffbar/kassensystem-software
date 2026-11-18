package de.schaffbar.core_pos.builder;

import static de.schaffbar.core_pos.domain.workshop_session.WorkshopSessionViews.WorkshopSessionView.WorkshopSessionViewBuilder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import de.schaffbar.core_pos.domain.customer.CustomerViews.CustomerAddressView;
import de.schaffbar.core_pos.domain.customer.CustomerViews.CustomerView;
import de.schaffbar.core_pos.domain.customer.CustomerViews.CustomerView.CustomerViewBuilder;
import de.schaffbar.core_pos.domain.rfid_reader.RfidReaderType;
import de.schaffbar.core_pos.domain.rfid_reader.RfidReaderViews.RfidReaderView;
import de.schaffbar.core_pos.domain.rfid_reader.RfidReaderViews.RfidReaderView.RfidReaderViewBuilder;
import de.schaffbar.core_pos.domain.rfid_tag.RfidTagViews.RfidTagView;
import de.schaffbar.core_pos.domain.rfid_tag.RfidTagViews.RfidTagView.RfidTagViewBuilder;
import de.schaffbar.core_pos.domain.rfid_tag_assignment.RfidTagAssignmentStatus;
import de.schaffbar.core_pos.domain.rfid_tag_assignment.RfidTagAssignmentType;
import de.schaffbar.core_pos.domain.rfid_tag_assignment.RfidTagAssignmentViews.RfidTagAssignmentHistoryView;
import de.schaffbar.core_pos.domain.rfid_tag_assignment.RfidTagAssignmentViews.RfidTagAssignmentHistoryView.RfidTagAssignmentHistoryViewBuilder;
import de.schaffbar.core_pos.domain.rfid_tag_assignment.RfidTagAssignmentViews.RfidTagAssignmentView;
import de.schaffbar.core_pos.domain.rfid_tag_assignment.RfidTagAssignmentViews.RfidTagAssignmentView.RfidTagAssignmentViewBuilder;
import de.schaffbar.core_pos.domain.tool.CertificationRequirement;
import de.schaffbar.core_pos.domain.tool.ToolArea;
import de.schaffbar.core_pos.domain.tool.ToolViews.ToolView;
import de.schaffbar.core_pos.domain.tool.ToolViews.ToolView.ToolViewBuilder;
import de.schaffbar.core_pos.domain.tool_usage.ToolUsageViews.ToolUsageView;
import de.schaffbar.core_pos.domain.tool_usage.ToolUsageViews.ToolUsageView.ToolUsageViewBuilder;
import de.schaffbar.core_pos.domain.workshop_session.WorkshopSessionStatus;
import de.schaffbar.core_pos.domain.workshop_session.WorkshopSessionViews.WorkshopSessionView;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.RfidReaderId;
import de.schaffbar.core_pos.shared.id.RfidTagAssignmentId;
import de.schaffbar.core_pos.shared.id.RfidTagId;
import de.schaffbar.core_pos.shared.id.ToolId;
import de.schaffbar.core_pos.shared.id.ToolUsageId;
import de.schaffbar.core_pos.shared.id.WorkshopSessionId;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ViewBuilder {

    // ------------------------------------------------------------------------
    // customer

    public static CustomerViewBuilder customerView() {
        return customerView(CustomerId.random());
    }

    public static CustomerViewBuilder customerView(CustomerId id) {
        return CustomerView.builder() //
                .id(id) //
                .firstName("Erika") //
                .lastName("Mustermann") //
                .dateOfBirth(LocalDate.of(1990, 1, 31)) //
                .clubMember(false) //
                .email("erika@example.org") //
                .address(CustomerAddressView.builder() //
                        .addressLine1("Hauptstraße 1") //
                        .postalCode("12345") //
                        .city("Berlin") //
                        .country("DE") //
                        .build()) //
                .createdAt(Instant.now()) //
                .updatedAt(Instant.now());
    }

    // ------------------------------------------------------------------------
    // RFID reader

    public static RfidReaderViewBuilder rfidReaderView() {
        return rfidReaderView(RfidReaderId.random());
    }

    public static RfidReaderViewBuilder rfidReaderView(RfidReaderId id) {
        return RfidReaderView.builder() //
                .id(id) //
                .macAddress(RandomValues.macAddress().getValue()) //
                .type(RfidReaderType.RFID_TAG_ASSIGNER) //
                .name("Theke") //
                .socketName("socket-43") //
                .createdAt(Instant.now());
    }

    // ------------------------------------------------------------------------
    // RFID tag

    public static RfidTagViewBuilder rfidTagView() {
        return rfidTagView(RandomValues.rfidTagId());
    }

    public static RfidTagViewBuilder rfidTagView(RfidTagId id) {
        return RfidTagView.builder() //
                .id(id) //
                .active(true) //
                .createdAt(Instant.now());
    }

    // ------------------------------------------------------------------------
    // RFID tag assignment

    public static RfidTagAssignmentViewBuilder rfidTagAssignmentView() {
        return rfidTagAssignmentView(RfidTagAssignmentId.random());
    }

    public static RfidTagAssignmentViewBuilder rfidTagAssignmentView(RfidTagAssignmentId id) {
        return RfidTagAssignmentView.builder() //
                .id(id) //
                .customerId(CustomerId.random()) //
                .assignmentType(RfidTagAssignmentType.FIXED) //
                .status(RfidTagAssignmentStatus.ASSIGNED) //
                .rfidTagId(RandomValues.rfidTagId()) //
                .assignmentDate(Instant.now());
    }

    public static RfidTagAssignmentHistoryViewBuilder rfidTagAssignmentHistoryView() {
        return RfidTagAssignmentHistoryView.builder() //
                .customerId(CustomerId.random()) //
                .rfidTagId(RandomValues.rfidTagId()) //
                .assignmentType(RfidTagAssignmentType.TEMPORARY) //
                .assignmentDate(Instant.now().minusSeconds(3600)) //
                .unassignmentDate(Instant.now());
    }

    // ------------------------------------------------------------------------
    // tool

    public static ToolViewBuilder toolView() {
        return toolView(ToolId.random());
    }

    public static ToolViewBuilder toolView(ToolId id) {
        return ToolView.builder() //
                .id(id) //
                .name(RandomValues.toolName()) //
                .area(ToolArea.HOLZ) //
                .certificationRequirement(CertificationRequirement.YELLOW) //
                .instructors(Set.of());
    }

    // ------------------------------------------------------------------------
    // workshop session

    public static WorkshopSessionViewBuilder openWorkshopSessionView() {
        return WorkshopSessionView.builder() //
                .id(WorkshopSessionId.random()) //
                .status(WorkshopSessionStatus.OPEN);
    }

    // ------------------------------------------------------------------------
    // tool usage

    public static ToolUsageViewBuilder activeToolUsageView(ToolId toolId) {
        return ToolUsageView.builder() //
                .id(ToolUsageId.random()) //
                .customerId(CustomerId.random()) //
                .toolId(toolId) //
                .workshopSessionId(WorkshopSessionId.random()) //
                .startTime(Instant.now().minusSeconds(600));
    }

}

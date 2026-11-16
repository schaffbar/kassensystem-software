package de.schaffbar.core_pos.builder;

import static de.schaffbar.core_pos.domain.workshop_session.WorkshopSessionViews.WorkshopSessionView.WorkshopSessionViewBuilder;

import java.time.Instant;
import java.util.Set;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import de.schaffbar.core_pos.domain.rfid_reader.RfidReaderType;
import de.schaffbar.core_pos.domain.rfid_reader.RfidReaderViews.RfidReaderView;
import de.schaffbar.core_pos.domain.rfid_reader.RfidReaderViews.RfidReaderView.RfidReaderViewBuilder;
import de.schaffbar.core_pos.domain.rfid_tag.RfidTagViews.RfidTagView;
import de.schaffbar.core_pos.domain.rfid_tag.RfidTagViews.RfidTagView.RfidTagViewBuilder;
import de.schaffbar.core_pos.domain.rfid_tag_assignment.RfidTagAssignmentStatus;
import de.schaffbar.core_pos.domain.rfid_tag_assignment.RfidTagAssignmentType;
import de.schaffbar.core_pos.domain.rfid_tag_assignment.RfidTagAssignmentViews.RfidTagAssignmentView;
import de.schaffbar.core_pos.domain.rfid_tag_assignment.RfidTagAssignmentViews.RfidTagAssignmentView.RfidTagAssignmentViewBuilder;
import de.schaffbar.core_pos.domain.tool.CertificationRequirement;
import de.schaffbar.core_pos.domain.tool.ToolArea;
import de.schaffbar.core_pos.domain.tool.ToolViews.ToolView;
import de.schaffbar.core_pos.domain.tool.ToolViews.ToolView.ToolViewBuilder;
import de.schaffbar.core_pos.domain.workshop_session.WorkshopSessionStatus;
import de.schaffbar.core_pos.domain.workshop_session.WorkshopSessionViews.WorkshopSessionView;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.RfidReaderId;
import de.schaffbar.core_pos.shared.id.RfidTagAssignmentId;
import de.schaffbar.core_pos.shared.id.RfidTagId;
import de.schaffbar.core_pos.shared.id.ToolId;
import de.schaffbar.core_pos.shared.id.WorkshopSessionId;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ViewBuilder {

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

}

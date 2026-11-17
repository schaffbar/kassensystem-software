package de.schaffbar.core_pos.domain.tool;

import static de.schaffbar.core_pos.builder.CommandBuilder.createToolCommand;
import static de.schaffbar.core_pos.builder.CommandBuilder.setWlanRelaisCommand;
import static de.schaffbar.core_pos.builder.CommandBuilder.updateToolCommand;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.List;

import org.junit.jupiter.api.Test;

import de.schaffbar.core_pos.domain.tool.ToolCommands.CreateToolCommand;
import de.schaffbar.core_pos.domain.tool.ToolCommands.SetWlanRelaisCommand;
import de.schaffbar.core_pos.domain.tool.ToolCommands.UpdateToolCommand;
import de.schaffbar.core_pos.shared.event.EventType;
import de.schaffbar.core_pos.shared.event.SchaffbarEvent;
import de.schaffbar.core_pos.shared.event.payload.ToolEventPayload.ToolInstructorsAddedPayload;
import de.schaffbar.core_pos.shared.event.payload.ToolEventPayload.ToolInstructorsRemovedPayload;
import de.schaffbar.core_pos.shared.event.payload.ToolEventPayload.ToolRfidReaderAssignedPayload;
import de.schaffbar.core_pos.shared.event.payload.ToolEventPayload.ToolRfidReaderClearedPayload;
import de.schaffbar.core_pos.shared.event.payload.ToolEventPayload.ToolUpdatedPayload;
import de.schaffbar.core_pos.shared.event.payload.ToolEventPayload.ToolWlanRelaisClearedPayload;
import de.schaffbar.core_pos.shared.event.payload.ToolEventPayload.ToolWlanRelaisSetPayload;
import de.schaffbar.core_pos.shared.exception.CustomerAlreadyInstructorException;
import de.schaffbar.core_pos.shared.exception.CustomerNotInstructorException;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.RfidReaderId;

class ToolTest {

    // ------------------------------------------------------------------------
    // of

    @Test
    void of_createsTheToolWithoutHardwareAndInstructors() {
        // given
        CreateToolCommand command = createToolCommand().build();

        // when
        Tool tool = Tool.of(command);

        // then
        assertThat(tool.getId()).isNotNull();
        assertThat(tool.getName()).isEqualTo(command.name());
        assertThat(tool.getDescription()).isEqualTo(command.description());
        assertThat(tool.getArea()).isEqualTo(command.area());
        assertThat(tool.getCertificationRequirement()).isEqualTo(command.certificationRequirement());
        assertThat(tool.getRfidReaderId()).isNull();
        assertThat(tool.getWlanRelaisType()).isNull();
        assertThat(tool.getInstructors()).isEmpty();
        assertThat(tool.getCreatedAt()).isNotNull();
    }

    // ------------------------------------------------------------------------
    // update

    @Test
    void update_changesNameDescriptionAreaAndRequirementAndReturnsTheEvent() {
        // given
        Tool tool = givenTool();
        UpdateToolCommand command = updateToolCommand(tool.getId()).build();

        // when
        List<SchaffbarEvent> events = tool.update(command);

        // then
        assertThat(tool.getName()).isEqualTo(command.name());
        assertThat(tool.getDescription()).isEqualTo(command.description());
        assertThat(tool.getArea()).isEqualTo(command.area());
        assertThat(tool.getCertificationRequirement()).isEqualTo(command.certificationRequirement());

        ToolUpdatedPayload payload = expectSingleEvent(events, EventType.TOOL_UPDATED, tool) //
                .deserializeAndValidatePayload(ToolUpdatedPayload.class);
        assertThat(payload.id()).isEqualTo(tool.getId());
        assertThat(payload.name()).isEqualTo(command.name());
        assertThat(payload.description()).isEqualTo(command.description());
        assertThat(payload.area()).isEqualTo(command.area());
        assertThat(payload.certificationRequirement()).isEqualTo(command.certificationRequirement());
    }

    // ------------------------------------------------------------------------
    // assignRfidReader

    @Test
    void assignRfidReader_setsTheSwitchBoxAndReturnsTheEvent() {
        // given
        Tool tool = givenTool();
        RfidReaderId rfidReaderId = RfidReaderId.random();

        // when
        List<SchaffbarEvent> events = tool.assignRfidReader(rfidReaderId);

        // then
        assertThat(tool.getRfidReaderId()).isEqualTo(rfidReaderId);

        ToolRfidReaderAssignedPayload payload = expectSingleEvent(events, EventType.TOOL_RFID_READER_ASSIGNED, tool) //
                .deserializeAndValidatePayload(ToolRfidReaderAssignedPayload.class);
        assertThat(payload.id()).isEqualTo(tool.getId());
        assertThat(payload.rfidReaderId()).isEqualTo(rfidReaderId);
    }

    @Test
    void assignRfidReader_replacesAnotherSwitchBox() {
        // given
        Tool tool = givenToolWithSwitchBox();
        RfidReaderId rfidReaderId = RfidReaderId.random();

        // when
        List<SchaffbarEvent> events = tool.assignRfidReader(rfidReaderId);

        // then
        assertThat(tool.getRfidReaderId()).isEqualTo(rfidReaderId);
        expectSingleEvent(events, EventType.TOOL_RFID_READER_ASSIGNED, tool);
    }

    @Test
    void assignRfidReader_theSameSwitchBoxAgain_changesNothing() {
        // given
        Tool tool = givenToolWithSwitchBox();
        RfidReaderId rfidReaderId = tool.getRfidReaderId();

        // when
        List<SchaffbarEvent> events = tool.assignRfidReader(rfidReaderId);

        // then
        assertThat(tool.getRfidReaderId()).isEqualTo(rfidReaderId);
        assertThat(events).isEmpty();
    }

    // ------------------------------------------------------------------------
    // clearRfidReader

    @Test
    void clearRfidReader_removesTheSwitchBoxAndReturnsTheEvent() {
        // given
        Tool tool = givenToolWithSwitchBox();

        // when
        List<SchaffbarEvent> events = tool.clearRfidReader();

        // then
        assertThat(tool.getRfidReaderId()).isNull();

        ToolRfidReaderClearedPayload payload = expectSingleEvent(events, EventType.TOOL_RFID_READER_CLEARED, tool) //
                .deserializeAndValidatePayload(ToolRfidReaderClearedPayload.class);
        assertThat(payload.id()).isEqualTo(tool.getId());
    }

    // ------------------------------------------------------------------------
    // setWlanRelais

    @Test
    void setWlanRelais_setsTypeAndIpAddressWithTheCommandsOfTheTypeAndReturnsTheEvent() {
        // given
        Tool tool = givenTool();
        SetWlanRelaisCommand command = setWlanRelaisCommand(tool.getId()).build();

        // when
        List<SchaffbarEvent> events = tool.setWlanRelais(command);

        // then
        assertThat(tool.getWlanRelaisType()).isEqualTo(command.wlanRelaisType());
        assertThat(tool.getIpAddress()).isEqualTo(command.ipAddress());
        assertThat(tool.getHttpStartCommand()).isEqualTo(command.wlanRelaisType().getHttpStartCommand());
        assertThat(tool.getOnCommand()).isEqualTo(command.wlanRelaisType().getOnCommand());
        assertThat(tool.getOffCommand()).isEqualTo(command.wlanRelaisType().getOffCommand());

        ToolWlanRelaisSetPayload payload = expectSingleEvent(events, EventType.TOOL_WLAN_RELAIS_SET, tool) //
                .deserializeAndValidatePayload(ToolWlanRelaisSetPayload.class);
        assertThat(payload.id()).isEqualTo(tool.getId());
        assertThat(payload.wlanRelaisType()).isEqualTo(command.wlanRelaisType());
        assertThat(payload.ipAddress()).isEqualTo(command.ipAddress());
    }

    // ------------------------------------------------------------------------
    // clearWlanRelais

    @Test
    void clearWlanRelais_resetsAllRelaisFieldsAndReturnsTheEvent() {
        // given
        Tool tool = givenToolWithWlanRelais();

        // when
        List<SchaffbarEvent> events = tool.clearWlanRelais();

        // then
        assertThat(tool.getWlanRelaisType()).isNull();
        assertThat(tool.getIpAddress()).isNull();
        assertThat(tool.getHttpStartCommand()).isNull();
        assertThat(tool.getOnCommand()).isNull();
        assertThat(tool.getOffCommand()).isNull();

        ToolWlanRelaisClearedPayload payload = expectSingleEvent(events, EventType.TOOL_WLAN_RELAIS_CLEARED, tool) //
                .deserializeAndValidatePayload(ToolWlanRelaisClearedPayload.class);
        assertThat(payload.id()).isEqualTo(tool.getId());
    }

    // ------------------------------------------------------------------------
    // addInstructors

    @Test
    void addInstructors_addsThePeopleAndReturnsTheEvent() {
        // given
        Tool tool = givenTool();
        List<CustomerId> instructorIds = List.of(CustomerId.random(), CustomerId.random());

        // when
        List<SchaffbarEvent> events = tool.addInstructors(instructorIds);

        // then
        assertThat(tool.getInstructors()).containsExactlyInAnyOrderElementsOf(instructorIds);

        ToolInstructorsAddedPayload payload = expectSingleEvent(events, EventType.TOOL_INSTRUCTORS_ADDED, tool) //
                .deserializeAndValidatePayload(ToolInstructorsAddedPayload.class);
        assertThat(payload.id()).isEqualTo(tool.getId());
        assertThat(payload.instructorIds()).containsExactlyElementsOf(instructorIds);
    }

    @Test
    void addInstructors_rejectsAnInstructorOfTheToolAndAddsNobody() {
        // given
        CustomerId instructorId = CustomerId.random();
        Tool tool = givenToolWithInstructor(instructorId);
        CustomerId newInstructorId = CustomerId.random();

        // when
        Throwable thrown = catchThrowable(() -> tool.addInstructors(List.of(newInstructorId, instructorId)));

        // then
        assertThat(thrown) //
                .isInstanceOf(CustomerAlreadyInstructorException.class) //
                .hasFieldOrPropertyWithValue("code", CustomerAlreadyInstructorException.CODE);
        assertThat(tool.getInstructors()).containsExactly(instructorId);
    }

    // ------------------------------------------------------------------------
    // removeInstructors

    @Test
    void removeInstructors_removesThePeopleAndReturnsTheEvent() {
        // given
        CustomerId instructorId = CustomerId.random();
        CustomerId otherInstructorId = CustomerId.random();
        Tool tool = givenToolWithInstructors(instructorId, otherInstructorId);

        // when
        List<SchaffbarEvent> events = tool.removeInstructors(List.of(instructorId));

        // then
        assertThat(tool.getInstructors()).containsExactly(otherInstructorId);

        ToolInstructorsRemovedPayload payload = expectSingleEvent(events, EventType.TOOL_INSTRUCTORS_REMOVED, tool) //
                .deserializeAndValidatePayload(ToolInstructorsRemovedPayload.class);
        assertThat(payload.id()).isEqualTo(tool.getId());
        assertThat(payload.instructorIds()).containsExactly(instructorId);
    }

    @Test
    void removeInstructors_rejectsSomebodyWhoIsNoInstructorAndRemovesNobody() {
        // given
        CustomerId instructorId = CustomerId.random();
        Tool tool = givenToolWithInstructor(instructorId);

        // when
        Throwable thrown = catchThrowable(() -> tool.removeInstructors(List.of(instructorId, CustomerId.random())));

        // then
        assertThat(thrown) //
                .isInstanceOf(CustomerNotInstructorException.class) //
                .hasFieldOrPropertyWithValue("code", CustomerNotInstructorException.CODE);
        assertThat(tool.getInstructors()).containsExactly(instructorId);
    }

    // ------------------------------------------------------------------------
    // given

    private Tool givenTool() {
        return Tool.of(createToolCommand().build());
    }

    private Tool givenToolWithSwitchBox() {
        Tool tool = givenTool();
        tool.assignRfidReader(RfidReaderId.random());

        return tool;
    }

    private Tool givenToolWithWlanRelais() {
        Tool tool = givenTool();
        tool.setWlanRelais(setWlanRelaisCommand(tool.getId()).build());

        return tool;
    }

    private Tool givenToolWithInstructor(CustomerId instructorId) {
        return givenToolWithInstructors(instructorId);
    }

    private Tool givenToolWithInstructors(CustomerId... instructorIds) {
        Tool tool = givenTool();
        tool.addInstructors(List.of(instructorIds));

        return tool;
    }

    // ------------------------------------------------------------------------
    // then

    private SchaffbarEvent expectSingleEvent(List<SchaffbarEvent> events, EventType type, Tool tool) {
        assertThat(events).hasSize(1);

        SchaffbarEvent event = events.getFirst();
        assertThat(event.getType()).isEqualTo(type);
        assertThat(event.getAggregateId()).isEqualTo(tool.getId().getValue().toString());

        return event;
    }

}

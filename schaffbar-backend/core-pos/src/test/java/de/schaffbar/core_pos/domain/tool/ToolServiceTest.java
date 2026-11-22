package de.schaffbar.core_pos.domain.tool;

import static de.schaffbar.core_pos.builder.CommandBuilder.createToolCommand;
import static de.schaffbar.core_pos.builder.CommandBuilder.setWlanRelaisCommand;
import static de.schaffbar.core_pos.builder.CommandBuilder.updateToolCommand;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.schaffbar.core_pos.domain.tool.ToolCommands.CreateToolCommand;
import de.schaffbar.core_pos.domain.tool.ToolCommands.SetWlanRelaisCommand;
import de.schaffbar.core_pos.domain.tool.ToolCommands.UpdateToolCommand;
import de.schaffbar.core_pos.shared.event.EventType;
import de.schaffbar.core_pos.shared.event.SchaffbarEvent;
import de.schaffbar.core_pos.shared.event.outbox.OutboxEventStore;
import de.schaffbar.core_pos.shared.event.payload.ToolEventPayload.ToolCreatedPayload;
import de.schaffbar.core_pos.shared.event.payload.ToolEventPayload.ToolDeletedPayload;
import de.schaffbar.core_pos.shared.exception.CustomerAlreadyInstructorException;
import de.schaffbar.core_pos.shared.exception.CustomerNotInstructorException;
import de.schaffbar.core_pos.shared.exception.IpAddressAlreadyUsedException;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException.Resource;
import de.schaffbar.core_pos.shared.exception.RfidReaderAlreadyAssignedToToolException;
import de.schaffbar.core_pos.shared.exception.ToolNameAlreadyUsedException;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.IpAddress;
import de.schaffbar.core_pos.shared.id.RfidReaderId;
import de.schaffbar.core_pos.shared.id.ToolId;

@ExtendWith(MockitoExtension.class)
class ToolServiceTest {

    @Mock
    private ToolRepository toolRepository;

    @Mock
    private OutboxEventStore outboxEventStore;

    @Captor
    private ArgumentCaptor<List<SchaffbarEvent>> events;

    @InjectMocks
    private ToolService toolService;

    // ------------------------------------------------------------------------
    // createTool

    @Test
    void createTool_savesTheToolAndItsCreatedEvent() {
        // given
        CreateToolCommand command = createToolCommand().build();
        givenSaveReturnsTheTool();

        // when
        ToolId toolId = this.toolService.createTool(command);

        // then
        expectSaved();
        ToolCreatedPayload payload = expectSavedEvent(EventType.TOOL_CREATED, toolId) //
                .deserializeAndValidatePayload(ToolCreatedPayload.class);
        assertThat(payload.id()).isEqualTo(toolId);
        assertThat(payload.name()).isEqualTo(command.name());
        assertThat(payload.description()).isEqualTo(command.description());
        assertThat(payload.area()).isEqualTo(command.area());
        assertThat(payload.certificationRequirement()).isEqualTo(command.certificationRequirement());
    }

    @Test
    void createTool_rejectsTheNameOfAnotherTool() {
        // given
        CreateToolCommand command = createToolCommand().build();
        Tool other = givenOtherToolNamed(command.name());

        // when
        Throwable thrown = catchThrowable(() -> this.toolService.createTool(command));

        // then
        assertThat(thrown) //
                .isInstanceOf(ToolNameAlreadyUsedException.class) //
                .hasFieldOrPropertyWithValue("code", ToolNameAlreadyUsedException.CODE) //
                .hasMessageContaining(other.getId().getValue().toString());
        expectNothingSaved();
    }

    // ------------------------------------------------------------------------
    // updateTool

    @Test
    void updateTool_changesTheToolAndSavesTheEvent() {
        // given
        Tool tool = givenTool();
        UpdateToolCommand command = updateToolCommand(tool.getId()).build();

        // when
        this.toolService.updateTool(command);

        // then
        assertThat(tool.getName()).isEqualTo(command.name());
        expectSavedEvent(EventType.TOOL_UPDATED, tool.getId());
    }

    @Test
    void updateTool_keepsItsOwnName() {
        // given
        Tool tool = givenTool();
        givenToolsNamed(tool.getName(), tool);
        UpdateToolCommand command = updateToolCommand(tool.getId()).name(tool.getName()).build();

        // when
        this.toolService.updateTool(command);

        // then
        assertThat(tool.getName()).isEqualTo(command.name());
        expectSavedEvent(EventType.TOOL_UPDATED, tool.getId());
    }

    @Test
    void updateTool_rejectsTheNameOfAnotherTool() {
        // given
        Tool tool = givenTool();
        UpdateToolCommand command = updateToolCommand(tool.getId()).build();
        Tool other = givenOtherToolNamed(command.name());

        // when
        Throwable thrown = catchThrowable(() -> this.toolService.updateTool(command));

        // then
        assertThat(thrown) //
                .isInstanceOf(ToolNameAlreadyUsedException.class) //
                .hasFieldOrPropertyWithValue("code", ToolNameAlreadyUsedException.CODE) //
                .hasMessageContaining(other.getId().getValue().toString());
        assertThat(tool.getName()).isNotEqualTo(command.name());
        expectNoEvent();
    }

    // T-15 – two existing names that differ only in case: the next edit of one of them renames it
    @Test
    void updateTool_rejectsItsOwnNameWhileAnotherToolHasItInOtherCase() {
        // given
        Tool tool = givenTool();
        Tool other = Tool.of(createToolCommand().name(tool.getName().toUpperCase()).build());
        givenToolsNamed(tool.getName(), tool, other);
        UpdateToolCommand command = updateToolCommand(tool.getId()).name(tool.getName()).build();

        // when
        Throwable thrown = catchThrowable(() -> this.toolService.updateTool(command));

        // then
        assertThat(thrown) //
                .isInstanceOf(ToolNameAlreadyUsedException.class) //
                .hasFieldOrPropertyWithValue("code", ToolNameAlreadyUsedException.CODE) //
                .hasMessageContaining(other.getId().getValue().toString());
        expectNoEvent();
    }

    @Test
    void updateTool_rejectsAnUnknownTool() {
        // given
        ToolId unknown = givenUnknownTool();

        // when
        Throwable thrown = catchThrowable(() -> this.toolService.updateTool(updateToolCommand(unknown).build()));

        // then
        expectToolNotFound(thrown);
        expectNoEvent();
    }

    // ------------------------------------------------------------------------
    // setWlanRelais

    @Test
    void setWlanRelais_setsTheRelaisAndSavesTheEvent() {
        // given
        Tool tool = givenTool();
        SetWlanRelaisCommand command = setWlanRelaisCommand(tool.getId()).build();

        // when
        this.toolService.setWlanRelais(command);

        // then
        assertThat(tool.getWlanRelaisType()).isEqualTo(command.wlanRelaisType());
        assertThat(tool.getIpAddress()).isEqualTo(command.ipAddress());
        expectSavedEvent(EventType.TOOL_WLAN_RELAIS_SET, tool.getId());
    }

    @Test
    void setWlanRelais_keepsItsOwnIpAddress() {
        // given
        Tool tool = givenTool();
        SetWlanRelaisCommand command = setWlanRelaisCommand(tool.getId()).build();
        givenIpAddressUsedBy(tool, command.ipAddress());

        // when
        this.toolService.setWlanRelais(command);

        // then
        expectSavedEvent(EventType.TOOL_WLAN_RELAIS_SET, tool.getId());
    }

    @Test
    void setWlanRelais_rejectsTheIpAddressOfAnotherTool() {
        // given
        Tool tool = givenTool();
        SetWlanRelaisCommand command = setWlanRelaisCommand(tool.getId()).build();
        givenOtherToolWithIpAddress(command.ipAddress());

        // when
        Throwable thrown = catchThrowable(() -> this.toolService.setWlanRelais(command));

        // then
        assertThat(thrown) //
                .isInstanceOf(IpAddressAlreadyUsedException.class) //
                .hasFieldOrPropertyWithValue("code", IpAddressAlreadyUsedException.CODE);
        assertThat(tool.getIpAddress()).isNull();
        expectNoEvent();
    }

    @Test
    void setWlanRelais_rejectsAnUnknownTool() {
        // given
        ToolId unknown = givenUnknownTool();

        // when
        Throwable thrown = catchThrowable(() -> this.toolService.setWlanRelais(setWlanRelaisCommand(unknown).build()));

        // then
        expectToolNotFound(thrown);
        expectNoEvent();
    }

    // ------------------------------------------------------------------------
    // clearWlanRelais

    @Test
    void clearWlanRelais_clearsTheRelaisAndSavesTheEvent() {
        // given
        Tool tool = givenToolWithWlanRelais();

        // when
        this.toolService.clearWlanRelais(tool.getId());

        // then
        assertThat(tool.getWlanRelaisType()).isNull();
        assertThat(tool.getIpAddress()).isNull();
        expectSavedEvent(EventType.TOOL_WLAN_RELAIS_CLEARED, tool.getId());
    }

    @Test
    void clearWlanRelais_rejectsAnUnknownTool() {
        // given
        ToolId unknown = givenUnknownTool();

        // when
        Throwable thrown = catchThrowable(() -> this.toolService.clearWlanRelais(unknown));

        // then
        expectToolNotFound(thrown);
        expectNoEvent();
    }

    // ------------------------------------------------------------------------
    // assignRfidReader

    @Test
    void assignRfidReader_assignsTheSwitchBoxAndSavesTheEvent() {
        // given
        Tool tool = givenTool();
        RfidReaderId rfidReaderId = RfidReaderId.random();

        // when
        this.toolService.assignRfidReader(tool.getId(), rfidReaderId);

        // then
        assertThat(tool.getRfidReaderId()).isEqualTo(rfidReaderId);
        expectSavedEvent(EventType.TOOL_RFID_READER_ASSIGNED, tool.getId());
    }

    // T-8
    @Test
    void assignRfidReader_theSameSwitchBoxAgain_savesNoEvent() {
        // given
        RfidReaderId rfidReaderId = RfidReaderId.random();
        Tool tool = givenToolWithSwitchBox(rfidReaderId);
        givenSwitchBoxOf(tool, rfidReaderId);

        // when
        this.toolService.assignRfidReader(tool.getId(), rfidReaderId);

        // then
        assertThat(tool.getRfidReaderId()).isEqualTo(rfidReaderId);
        expectSavedWithoutEvent();
    }

    @Test
    void assignRfidReader_rejectsTheSwitchBoxOfAnotherTool() {
        // given
        Tool tool = givenTool();
        RfidReaderId rfidReaderId = RfidReaderId.random();
        givenOtherToolWithSwitchBox(rfidReaderId);

        // when
        Throwable thrown = catchThrowable(() -> this.toolService.assignRfidReader(tool.getId(), rfidReaderId));

        // then
        assertThat(thrown) //
                .isInstanceOf(RfidReaderAlreadyAssignedToToolException.class) //
                .hasFieldOrPropertyWithValue("code", RfidReaderAlreadyAssignedToToolException.CODE);
        assertThat(tool.getRfidReaderId()).isNull();
        expectNoEvent();
    }

    @Test
    void assignRfidReader_rejectsAnUnknownTool() {
        // given
        ToolId unknown = givenUnknownTool();

        // when
        Throwable thrown = catchThrowable(() -> this.toolService.assignRfidReader(unknown, RfidReaderId.random()));

        // then
        expectToolNotFound(thrown);
        expectNoEvent();
    }

    // ------------------------------------------------------------------------
    // clearRfidReader

    @Test
    void clearRfidReader_clearsTheSwitchBoxAndSavesTheEvent() {
        // given
        Tool tool = givenToolWithSwitchBox(RfidReaderId.random());

        // when
        this.toolService.clearRfidReader(tool.getId());

        // then
        assertThat(tool.getRfidReaderId()).isNull();
        expectSavedEvent(EventType.TOOL_RFID_READER_CLEARED, tool.getId());
    }

    @Test
    void clearRfidReader_rejectsAnUnknownTool() {
        // given
        ToolId unknown = givenUnknownTool();

        // when
        Throwable thrown = catchThrowable(() -> this.toolService.clearRfidReader(unknown));

        // then
        expectToolNotFound(thrown);
        expectNoEvent();
    }

    // ------------------------------------------------------------------------
    // addInstructors

    @Test
    void addInstructors_addsThePeopleAndSavesTheEvent() {
        // given
        Tool tool = givenTool();
        CustomerId instructorId = CustomerId.random();

        // when
        this.toolService.addInstructors(tool.getId(), List.of(instructorId));

        // then
        assertThat(tool.getInstructors()).containsExactly(instructorId);
        expectSavedEvent(EventType.TOOL_INSTRUCTORS_ADDED, tool.getId());
    }

    @Test
    void addInstructors_rejectsAnUnknownTool() {
        // given
        ToolId unknown = givenUnknownTool();

        // when
        Throwable thrown = catchThrowable(() -> this.toolService.addInstructors(unknown, List.of(CustomerId.random())));

        // then
        expectToolNotFound(thrown);
        expectNoEvent();
    }

    @Test
    void addInstructors_rejectsAnInstructorOfTheTool() {
        // given
        CustomerId instructorId = CustomerId.random();
        Tool tool = givenToolWithInstructor(instructorId);

        // when
        Throwable thrown = catchThrowable(() -> this.toolService.addInstructors(tool.getId(), List.of(instructorId)));

        // then
        assertThat(thrown) //
                .isInstanceOf(CustomerAlreadyInstructorException.class) //
                .hasFieldOrPropertyWithValue("code", CustomerAlreadyInstructorException.CODE);
        assertThat(tool.getInstructors()).containsExactly(instructorId);
        expectNoEvent();
    }

    // ------------------------------------------------------------------------
    // removeInstructors

    @Test
    void removeInstructors_removesThePeopleAndSavesTheEvent() {
        // given
        CustomerId instructorId = CustomerId.random();
        Tool tool = givenToolWithInstructor(instructorId);

        // when
        this.toolService.removeInstructors(tool.getId(), List.of(instructorId));

        // then
        assertThat(tool.getInstructors()).isEmpty();
        expectSavedEvent(EventType.TOOL_INSTRUCTORS_REMOVED, tool.getId());
    }

    @Test
    void removeInstructors_rejectsAnUnknownTool() {
        // given
        ToolId unknown = givenUnknownTool();

        // when
        Throwable thrown = catchThrowable(() -> this.toolService.removeInstructors(unknown, List.of(CustomerId.random())));

        // then
        expectToolNotFound(thrown);
        expectNoEvent();
    }

    @Test
    void removeInstructors_rejectsAPersonWhoIsNoInstructorOfTheTool() {
        // given
        CustomerId instructorId = CustomerId.random();
        Tool tool = givenToolWithInstructor(instructorId);

        // when
        Throwable thrown = catchThrowable(() -> this.toolService.removeInstructors(tool.getId(), List.of(CustomerId.random())));

        // then
        assertThat(thrown) //
                .isInstanceOf(CustomerNotInstructorException.class) //
                .hasFieldOrPropertyWithValue("code", CustomerNotInstructorException.CODE);
        assertThat(tool.getInstructors()).containsExactly(instructorId);
        expectNoEvent();
    }

    // ------------------------------------------------------------------------
    // deleteTool

    @Test
    void deleteTool_deletesTheToolAndSavesTheEvent() {
        // given
        Tool tool = givenTool();

        // when
        this.toolService.deleteTool(tool.getId());

        // then
        expectDeleted(tool);
        ToolDeletedPayload payload = expectSavedEvent(EventType.TOOL_DELETED, tool.getId()) //
                .deserializeAndValidatePayload(ToolDeletedPayload.class);
        assertThat(payload.id()).isEqualTo(tool.getId());
    }

    @Test
    void deleteTool_rejectsAnUnknownTool() {
        // given
        ToolId unknown = givenUnknownTool();

        // when
        Throwable thrown = catchThrowable(() -> this.toolService.deleteTool(unknown));

        // then
        expectToolNotFound(thrown);
        expectNothingDeleted();
        expectNoEvent();
    }

    // ------------------------------------------------------------------------
    // given

    private Tool givenTool() {
        Tool tool = Tool.of(createToolCommand().build());
        when(this.toolRepository.findById(tool.getId().getValue())).thenReturn(Optional.of(tool));

        return tool;
    }

    private Tool givenToolWithWlanRelais() {
        Tool tool = givenTool();
        tool.setWlanRelais(setWlanRelaisCommand(tool.getId()).build());

        return tool;
    }

    private Tool givenToolWithSwitchBox(RfidReaderId rfidReaderId) {
        Tool tool = givenTool();
        tool.assignRfidReader(rfidReaderId);

        return tool;
    }

    private Tool givenToolWithInstructor(CustomerId instructorId) {
        Tool tool = givenTool();
        tool.addInstructors(List.of(instructorId));

        return tool;
    }

    private ToolId givenUnknownTool() {
        ToolId unknown = ToolId.random();
        when(this.toolRepository.findById(unknown.getValue())).thenReturn(Optional.empty());

        return unknown;
    }

    private Tool givenOtherToolNamed(String name) {
        Tool other = Tool.of(createToolCommand().name(name).build());
        givenToolsNamed(name, other);

        return other;
    }

    private void givenToolsNamed(String name, Tool... tools) {
        when(this.toolRepository.findByNameIgnoreCase(name)).thenReturn(List.of(tools));
    }

    private void givenIpAddressUsedBy(Tool tool, IpAddress ipAddress) {
        when(this.toolRepository.findByIpAddress(ipAddress)).thenReturn(Optional.of(tool));
    }

    private void givenOtherToolWithIpAddress(IpAddress ipAddress) {
        givenIpAddressUsedBy(Tool.of(createToolCommand().build()), ipAddress);
    }

    private void givenSwitchBoxOf(Tool tool, RfidReaderId rfidReaderId) {
        when(this.toolRepository.findByRfidReaderId(rfidReaderId)).thenReturn(Optional.of(tool));
    }

    private void givenOtherToolWithSwitchBox(RfidReaderId rfidReaderId) {
        givenSwitchBoxOf(Tool.of(createToolCommand().build()), rfidReaderId);
    }

    private void givenSaveReturnsTheTool() {
        when(this.toolRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    // ------------------------------------------------------------------------
    // then

    private SchaffbarEvent expectSavedEvent(EventType type, ToolId toolId) {
        verify(this.outboxEventStore).save(this.events.capture());
        assertThat(this.events.getValue()) //
                .singleElement() //
                .satisfies(event -> {
                    assertThat(event.getType()).isEqualTo(type);
                    assertThat(event.getAggregateId()).isEqualTo(toolId.getValue().toString());
                });

        return this.events.getValue().getFirst();
    }

    // a command without a change still goes through the outbox, with no event in it
    private void expectSavedWithoutEvent() {
        verify(this.outboxEventStore).save(List.of());
    }

    private void expectToolNotFound(Throwable thrown) {
        assertThat(thrown) //
                .isInstanceOf(ResourceNotFoundException.class) //
                .hasFieldOrPropertyWithValue("resource", Resource.TOOL);
    }

    private void expectSaved() {
        verify(this.toolRepository).save(any());
    }

    private void expectNothingSaved() {
        verify(this.toolRepository, never()).save(any());
        expectNoEvent();
    }

    private void expectDeleted(Tool tool) {
        verify(this.toolRepository).delete(tool);
    }

    private void expectNothingDeleted() {
        verify(this.toolRepository, never()).delete(any());
    }

    private void expectNoEvent() {
        verifyNoInteractions(this.outboxEventStore);
    }

}

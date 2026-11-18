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
import de.schaffbar.core_pos.shared.exception.IpAddressAlreadyUsedException;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;
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
    // isInstructor

    @Test
    void isInstructor_rejectsAnUnknownTool() {
        // given
        ToolId unknown = givenUnknownTool();

        // when
        Throwable thrown = catchThrowable(() -> this.toolService.isInstructor(unknown, CustomerId.random()));

        // then
        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class);
    }

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
        verify(this.toolRepository).save(any());
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
        Tool other = givenToolNamed("Kreissäge");
        CreateToolCommand command = createToolCommand().name("Kreissäge").build();

        // when
        Throwable thrown = catchThrowable(() -> this.toolService.createTool(command));

        // then
        assertThat(thrown) //
                .isInstanceOf(ToolNameAlreadyUsedException.class) //
                .hasFieldOrPropertyWithValue("code", ToolNameAlreadyUsedException.CODE) //
                .hasMessageContaining(other.getId().getValue().toString());
        verify(this.toolRepository, never()).save(any());
        expectNoEvent();
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
        givenNameUsedBy(tool, tool.getName());
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
        Tool other = givenToolNamed("Bandsäge");
        UpdateToolCommand command = updateToolCommand(tool.getId()).name("Bandsäge").build();

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

    @Test
    void updateTool_rejectsAnUnknownTool() {
        // given
        ToolId unknown = givenUnknownTool();

        // when
        Throwable thrown = catchThrowable(() -> this.toolService.updateTool(updateToolCommand(unknown).build()));

        // then
        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class);
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
        givenIpAddressUsedBy(Tool.of(createToolCommand().build()), command.ipAddress());

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
        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class);
        expectNoEvent();
    }

    // ------------------------------------------------------------------------
    // clearWlanRelais

    @Test
    void clearWlanRelais_clearsTheRelaisAndSavesTheEvent() {
        // given
        Tool tool = givenTool();
        tool.setWlanRelais(setWlanRelaisCommand(tool.getId()).build());

        // when
        this.toolService.clearWlanRelais(tool.getId());

        // then
        assertThat(tool.getWlanRelaisType()).isNull();
        expectSavedEvent(EventType.TOOL_WLAN_RELAIS_CLEARED, tool.getId());
    }

    @Test
    void clearWlanRelais_rejectsAnUnknownTool() {
        // given
        ToolId unknown = givenUnknownTool();

        // when
        Throwable thrown = catchThrowable(() -> this.toolService.clearWlanRelais(unknown));

        // then
        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class);
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

    @Test
    void assignRfidReader_theSameSwitchBoxAgain_savesNoEvent() {
        // given
        Tool tool = givenTool();
        RfidReaderId rfidReaderId = RfidReaderId.random();
        tool.assignRfidReader(rfidReaderId);
        givenSwitchBoxOf(tool, rfidReaderId);

        // when
        this.toolService.assignRfidReader(tool.getId(), rfidReaderId);

        // then
        assertThat(tool.getRfidReaderId()).isEqualTo(rfidReaderId);
        verify(this.outboxEventStore).save(List.of());
    }

    @Test
    void assignRfidReader_rejectsTheSwitchBoxOfAnotherTool() {
        // given
        Tool tool = givenTool();
        RfidReaderId rfidReaderId = RfidReaderId.random();
        givenSwitchBoxOf(Tool.of(createToolCommand().build()), rfidReaderId);

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
        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class);
        expectNoEvent();
    }

    // ------------------------------------------------------------------------
    // clearRfidReader

    @Test
    void clearRfidReader_clearsTheSwitchBoxAndSavesTheEvent() {
        // given
        Tool tool = givenTool();
        tool.assignRfidReader(RfidReaderId.random());

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
        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class);
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
        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class);
        expectNoEvent();
    }

    // ------------------------------------------------------------------------
    // removeInstructors

    @Test
    void removeInstructors_removesThePeopleAndSavesTheEvent() {
        // given
        Tool tool = givenTool();
        CustomerId instructorId = CustomerId.random();
        tool.addInstructors(List.of(instructorId));

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
        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class);
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
        verify(this.toolRepository).delete(tool);
        expectSavedEvent(EventType.TOOL_DELETED, tool.getId());
    }

    @Test
    void deleteTool_rejectsAnUnknownTool() {
        // given
        ToolId unknown = givenUnknownTool();

        // when
        Throwable thrown = catchThrowable(() -> this.toolService.deleteTool(unknown));

        // then
        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class);
        verify(this.toolRepository, never()).delete(any());
        expectNoEvent();
    }

    // ------------------------------------------------------------------------
    // given

    private Tool givenTool() {
        Tool tool = Tool.of(createToolCommand().build());
        when(this.toolRepository.findById(tool.getId().getValue())).thenReturn(Optional.of(tool));

        return tool;
    }

    private ToolId givenUnknownTool() {
        ToolId unknown = ToolId.random();
        when(this.toolRepository.findById(unknown.getValue())).thenReturn(Optional.empty());

        return unknown;
    }

    private Tool givenToolNamed(String name) {
        Tool tool = Tool.of(createToolCommand().name(name).build());
        when(this.toolRepository.findByNameIgnoreCase(name)).thenReturn(List.of(tool));

        return tool;
    }

    private void givenNameUsedBy(Tool tool, String name) {
        when(this.toolRepository.findByNameIgnoreCase(name)).thenReturn(List.of(tool));
    }

    private void givenIpAddressUsedBy(Tool tool, IpAddress ipAddress) {
        when(this.toolRepository.findByIpAddress(ipAddress)).thenReturn(Optional.of(tool));
    }

    private void givenSwitchBoxOf(Tool tool, RfidReaderId rfidReaderId) {
        when(this.toolRepository.findByRfidReaderId(rfidReaderId)).thenReturn(Optional.of(tool));
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

    private void expectNoEvent() {
        verifyNoInteractions(this.outboxEventStore);
    }

}

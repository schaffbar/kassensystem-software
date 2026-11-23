package de.schaffbar.core_pos.domain.tool.web;

import static de.schaffbar.core_pos.builder.CommandBuilder.createToolCommand;
import static de.schaffbar.core_pos.builder.CommandBuilder.setWlanRelaisCommand;
import static de.schaffbar.core_pos.builder.CommandBuilder.updateToolCommand;
import static de.schaffbar.core_pos.builder.ViewBuilder.toolView;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import de.schaffbar.core_pos.builder.RandomValues;
import de.schaffbar.core_pos.domain.tool.ToolCommands.CreateToolCommand;
import de.schaffbar.core_pos.domain.tool.ToolCommands.SetWlanRelaisCommand;
import de.schaffbar.core_pos.domain.tool.ToolCommands.UpdateToolCommand;
import de.schaffbar.core_pos.domain.tool.ToolViews.ToolView;
import de.schaffbar.core_pos.domain.tool.WlanRelaisType;
import de.schaffbar.core_pos.read_facade.ToolReadFacade;
import de.schaffbar.core_pos.shared.exception.CustomerAlreadyInstructorException;
import de.schaffbar.core_pos.shared.exception.CustomerNotInstructorException;
import de.schaffbar.core_pos.shared.exception.InvalidRfidReaderTypeForToolException;
import de.schaffbar.core_pos.shared.exception.IpAddressAlreadyUsedException;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException.Resource;
import de.schaffbar.core_pos.shared.exception.RfidReaderAlreadyAssignedToToolException;
import de.schaffbar.core_pos.shared.exception.ToolInUseException;
import de.schaffbar.core_pos.shared.exception.ToolNameAlreadyUsedException;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.RfidReaderId;
import de.schaffbar.core_pos.shared.id.ToolId;
import de.schaffbar.core_pos.shared.util.StringToRfidReaderIdConverter;
import de.schaffbar.core_pos.shared.util.StringToToolIdConverter;
import de.schaffbar.core_pos.shared.web.ApiExceptionHandler;
import de.schaffbar.core_pos.support.Json;
import de.schaffbar.core_pos.use_case.ToolAddInstructors;
import de.schaffbar.core_pos.use_case.ToolAssignRfidReader;
import de.schaffbar.core_pos.use_case.ToolClearRfidReader;
import de.schaffbar.core_pos.use_case.ToolClearWlanRelais;
import de.schaffbar.core_pos.use_case.ToolCreate;
import de.schaffbar.core_pos.use_case.ToolDelete;
import de.schaffbar.core_pos.use_case.ToolRemoveInstructors;
import de.schaffbar.core_pos.use_case.ToolSetWlanRelais;
import de.schaffbar.core_pos.use_case.ToolUpdate;

@ExtendWith(MockitoExtension.class)
class ToolControllerTest {

    @Mock
    private ToolReadFacade toolReadFacade;

    @Mock
    private ToolCreate toolCreate;

    @Mock
    private ToolUpdate toolUpdate;

    @Mock
    private ToolAssignRfidReader toolAssignRfidReader;

    @Mock
    private ToolClearRfidReader toolClearRfidReader;

    @Mock
    private ToolSetWlanRelais toolSetWlanRelais;

    @Mock
    private ToolClearWlanRelais toolClearWlanRelais;

    @Mock
    private ToolAddInstructors toolAddInstructors;

    @Mock
    private ToolRemoveInstructors toolRemoveInstructors;

    @Mock
    private ToolDelete toolDelete;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        DefaultFormattingConversionService conversionService = new DefaultFormattingConversionService();
        conversionService.addConverter(new StringToToolIdConverter());
        conversionService.addConverter(new StringToRfidReaderIdConverter());

        ToolController controller = new ToolController(this.toolReadFacade, this.toolCreate, this.toolUpdate, this.toolAssignRfidReader,
                this.toolClearRfidReader, this.toolSetWlanRelais, this.toolClearWlanRelais, this.toolAddInstructors, this.toolRemoveInstructors,
                this.toolDelete);

        this.mockMvc = MockMvcBuilders.standaloneSetup(controller) //
                .setConversionService(conversionService) //
                .setMessageConverters(Json.messageConverter()) //
                .setControllerAdvice(new ApiExceptionHandler()) //
                .build();
    }

    // ------------------------------------------------------------------------
    // getTools

    @Test
    void getTools_withoutFilter_returnsAll() throws Exception {
        // given
        CustomerId instructorId = CustomerId.random();
        ToolView tool = toolView() //
                .rfidReaderId(RfidReaderId.random()) //
                .wlanRelaisType(WlanRelaisType.SHELLY_1) //
                .ipAddress(RandomValues.ipAddress()) //
                .instructors(Set.of(instructorId)) //
                .build();
        givenTools(tool);

        // when
        ResultActions result = getTools();

        // then
        result.andExpect(status().isOk()) //
                .andExpect(jsonPath("$.length()").value(1)) //
                .andExpect(jsonPath("$[0].id").value(tool.id().getValue().toString())) //
                .andExpect(jsonPath("$[0].name").value(tool.name())) //
                .andExpect(jsonPath("$[0].description").value(tool.description())) //
                .andExpect(jsonPath("$[0].area").value(tool.area().name())) //
                .andExpect(jsonPath("$[0].certificationRequirement").value(tool.certificationRequirement().name())) //
                .andExpect(jsonPath("$[0].rfidReaderId").value(tool.rfidReaderId().getValue().toString())) //
                .andExpect(jsonPath("$[0].wlanRelaisType").value(tool.wlanRelaisType().name())) //
                .andExpect(jsonPath("$[0].ipAddress").value(tool.ipAddress().getValue())) //
                .andExpect(jsonPath("$[0].instructors[0]").value(instructorId.getValue().toString()));
    }

    @Test
    void getTools_withRfidReaderId_returnsAListWithTheToolOfTheSwitchBox() throws Exception {
        // given
        ToolView tool = givenToolWithSwitchBox();

        // when
        ResultActions result = getTools(tool.rfidReaderId());

        // then
        result.andExpect(status().isOk()) //
                .andExpect(jsonPath("$.length()").value(1)) //
                .andExpect(jsonPath("$[0].id").value(tool.id().getValue().toString())) //
                .andExpect(jsonPath("$[0].rfidReaderId").value(tool.rfidReaderId().getValue().toString()));
    }

    @Test
    void getTools_withRfidReaderIdOfNoTool_returnsAnEmptyList() throws Exception {
        // given
        RfidReaderId rfidReaderId = givenRfidReaderWithoutTool();

        // when
        ResultActions result = getTools(rfidReaderId);

        // then
        result.andExpect(status().isOk()) //
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ------------------------------------------------------------------------
    // getTool

    @Test
    void getTool_returnsTheTool() throws Exception {
        // given
        ToolView tool = givenTool();

        // when
        ResultActions result = getTool(tool.id());

        // then
        result.andExpect(status().isOk()) //
                .andExpect(jsonPath("$.id").value(tool.id().getValue().toString())) //
                .andExpect(jsonPath("$.name").value(tool.name())) //
                .andExpect(jsonPath("$.description").value(tool.description())) //
                .andExpect(jsonPath("$.area").value(tool.area().name())) //
                .andExpect(jsonPath("$.certificationRequirement").value(tool.certificationRequirement().name())) //
                .andExpect(jsonPath("$.instructors.length()").value(tool.instructors().size()));
    }

    @Test
    void getTool_unknownId_is404() throws Exception {
        // given
        ToolId unknown = givenUnknownTool();

        // when
        ResultActions result = getTool(unknown);

        // then
        expectToolNotFound(result);
    }

    // ------------------------------------------------------------------------
    // createTool

    @Test
    void createTool_returns201WithTheLocation() throws Exception {
        // given
        CreateToolCommand command = createToolCommand().build();
        ToolId toolId = givenCreatedTool(command);

        // when
        ResultActions result = createTool(command);

        // then
        result.andExpect(status().isCreated()) //
                .andExpect(header().string("Location", "/api/v1/tools/" + toolId.getValue()));
    }

    @Test
    void createTool_withTheNameOfAnotherTool_is409WithTheCode() throws Exception {
        // given
        CreateToolCommand command = createToolCommand().build();
        givenNameUsedForCreate(command);

        // when
        ResultActions result = createTool(command);

        // then
        result.andExpect(status().isConflict()) //
                .andExpect(jsonPath("$.code").value(ToolNameAlreadyUsedException.CODE));
    }

    @ParameterizedTest
    @MethodSource("invalidCreateToolCommands")
    void createTool_withAnInvalidValue_is400(CreateToolCommand command) throws Exception {
        // when
        ResultActions result = createTool(command);

        // then
        result.andExpect(status().isBadRequest());
        expectNothingCreated();
    }

    // ------------------------------------------------------------------------
    // updateTool

    @Test
    void updateTool_passesTheCommandAndIs204() throws Exception {
        // given
        UpdateToolCommand command = updateToolCommand().description("x".repeat(255)).build();

        // when
        ResultActions result = updateTool(command);

        // then
        result.andExpect(status().isNoContent());
        expectUpdated(command);
    }

    @Test
    void updateTool_withTheNameOfAnotherTool_is409WithTheCode() throws Exception {
        // given
        UpdateToolCommand command = updateToolCommand().build();
        givenNameUsedForUpdate(command);

        // when
        ResultActions result = updateTool(command);

        // then
        result.andExpect(status().isConflict()) //
                .andExpect(jsonPath("$.code").value(ToolNameAlreadyUsedException.CODE));
    }

    @ParameterizedTest
    @MethodSource("invalidUpdateToolCommands")
    void updateTool_withAnInvalidValue_is400(UpdateToolCommand command) throws Exception {
        // when
        ResultActions result = updateTool(command);

        // then
        result.andExpect(status().isBadRequest());
        expectNothingUpdated();
    }

    @Test
    void updateTool_unknownId_is404() throws Exception {
        // given
        UpdateToolCommand command = updateToolCommand().build();
        givenUpdateRejectedWith(command, ResourceNotFoundException.tool(command.id()));

        // when
        ResultActions result = updateTool(command);

        // then
        expectToolNotFound(result);
    }

    // ------------------------------------------------------------------------
    // assignRfidReader

    @Test
    void assignRfidReader_is204() throws Exception {
        // given
        ToolId toolId = ToolId.random();
        RfidReaderId rfidReaderId = RfidReaderId.random();

        // when
        ResultActions result = assignRfidReader(toolId, rfidReaderId);

        // then
        result.andExpect(status().isNoContent());
        expectRfidReaderAssigned(toolId, rfidReaderId);
    }

    @Test
    void assignRfidReader_ofAReaderThatIsNoSwitchBox_is409WithTheCode() throws Exception {
        // given
        ToolId toolId = ToolId.random();
        RfidReaderId rfidReaderId = RfidReaderId.random();
        givenRejectedAssignment(toolId, rfidReaderId, new InvalidRfidReaderTypeForToolException(rfidReaderId));

        // when
        ResultActions result = assignRfidReader(toolId, rfidReaderId);

        // then
        result.andExpect(status().isConflict()) //
                .andExpect(jsonPath("$.code").value(InvalidRfidReaderTypeForToolException.CODE));
    }

    @Test
    void assignRfidReader_ofTheSwitchBoxOfAnotherTool_is409WithTheCode() throws Exception {
        // given
        ToolId toolId = ToolId.random();
        RfidReaderId rfidReaderId = RfidReaderId.random();
        givenRejectedAssignment(toolId, rfidReaderId, new RfidReaderAlreadyAssignedToToolException(rfidReaderId, ToolId.random()));

        // when
        ResultActions result = assignRfidReader(toolId, rfidReaderId);

        // then
        result.andExpect(status().isConflict()) //
                .andExpect(jsonPath("$.code").value(RfidReaderAlreadyAssignedToToolException.CODE));
    }

    @Test
    void assignRfidReader_unknownTool_is404() throws Exception {
        // given
        ToolId unknown = ToolId.random();
        RfidReaderId rfidReaderId = RfidReaderId.random();
        givenRejectedAssignment(unknown, rfidReaderId, ResourceNotFoundException.tool(unknown));

        // when
        ResultActions result = assignRfidReader(unknown, rfidReaderId);

        // then
        expectToolNotFound(result);
    }

    // ------------------------------------------------------------------------
    // clearRfidReader

    @Test
    void clearRfidReader_is204() throws Exception {
        // given
        ToolId toolId = ToolId.random();

        // when
        ResultActions result = clearRfidReader(toolId);

        // then
        result.andExpect(status().isNoContent());
        expectRfidReaderCleared(toolId);
    }

    @Test
    void clearRfidReader_ofAToolInUse_is409WithTheCode() throws Exception {
        // given
        ToolId toolId = ToolId.random();
        givenClearRfidReaderRejectedWith(toolId, ToolInUseException.clearRfidReader(toolId));

        // when
        ResultActions result = clearRfidReader(toolId);

        // then
        result.andExpect(status().isConflict()) //
                .andExpect(jsonPath("$.code").value(ToolInUseException.CODE));
    }

    @Test
    void clearRfidReader_unknownTool_is404() throws Exception {
        // given
        ToolId unknown = ToolId.random();
        givenClearRfidReaderRejectedWith(unknown, ResourceNotFoundException.tool(unknown));

        // when
        ResultActions result = clearRfidReader(unknown);

        // then
        expectToolNotFound(result);
    }

    // ------------------------------------------------------------------------
    // setWlanRelais

    @Test
    void setWlanRelais_passesTheCommandAndIs204() throws Exception {
        // given
        SetWlanRelaisCommand command = setWlanRelaisCommand().build();

        // when
        ResultActions result = setWlanRelais(command);

        // then
        result.andExpect(status().isNoContent());
        expectWlanRelaisSet(command);
    }

    @Test
    void setWlanRelais_withTheIpAddressOfAnotherTool_is409WithTheCode() throws Exception {
        // given
        SetWlanRelaisCommand command = setWlanRelaisCommand().build();
        givenIpAddressUsed(command);

        // when
        ResultActions result = setWlanRelais(command);

        // then
        result.andExpect(status().isConflict()) //
                .andExpect(jsonPath("$.code").value(IpAddressAlreadyUsedException.CODE));
    }

    @Test
    void setWlanRelais_withAnInvalidIpAddress_is400() throws Exception {
        // given
        ToolId toolId = ToolId.random();

        // when
        ResultActions result = setWlanRelais(toolId, "{\"wlanRelaisType\":\"SHELLY_1\",\"ipAddress\":\"192.168.1.256\"}");

        // then
        result.andExpect(status().isBadRequest());
        expectNoWlanRelaisSet();
    }

    // T-5
    @Test
    void setWlanRelais_withoutIpAddress_is400() throws Exception {
        // given
        ToolId toolId = ToolId.random();

        // when
        ResultActions result = setWlanRelais(toolId, "{\"wlanRelaisType\":\"SHELLY_1\"}");

        // then
        result.andExpect(status().isBadRequest());
        expectNoWlanRelaisSet();
    }

    @Test
    void setWlanRelais_unknownTool_is404() throws Exception {
        // given
        SetWlanRelaisCommand command = setWlanRelaisCommand().build();
        givenSetWlanRelaisRejectedWith(command, ResourceNotFoundException.tool(command.id()));

        // when
        ResultActions result = setWlanRelais(command);

        // then
        expectToolNotFound(result);
    }

    // ------------------------------------------------------------------------
    // clearWlanRelais

    @Test
    void clearWlanRelais_is204() throws Exception {
        // given
        ToolId toolId = ToolId.random();

        // when
        ResultActions result = clearWlanRelais(toolId);

        // then
        result.andExpect(status().isNoContent());
        expectWlanRelaisCleared(toolId);
    }

    @Test
    void clearWlanRelais_unknownTool_is404() throws Exception {
        // given
        ToolId unknown = ToolId.random();
        givenClearWlanRelaisRejectedWith(unknown, ResourceNotFoundException.tool(unknown));

        // when
        ResultActions result = clearWlanRelais(unknown);

        // then
        expectToolNotFound(result);
    }

    // ------------------------------------------------------------------------
    // addInstructors

    @Test
    void addInstructors_passesTheIdsAndIs204() throws Exception {
        // given
        ToolId toolId = ToolId.random();
        CustomerId instructorId = CustomerId.random();

        // when
        ResultActions result = addInstructors(toolId, instructorId);

        // then
        result.andExpect(status().isNoContent());
        expectInstructorsAdded(toolId, List.of(instructorId));
    }

    @Test
    void addInstructors_withAnInstructorOfTheTool_is409WithTheCode() throws Exception {
        // given
        ToolId toolId = ToolId.random();
        CustomerId instructorId = givenInstructorOf(toolId);

        // when
        ResultActions result = addInstructors(toolId, instructorId);

        // then
        result.andExpect(status().isConflict()) //
                .andExpect(jsonPath("$.code").value(CustomerAlreadyInstructorException.CODE));
    }

    @Test
    void addInstructors_withoutIds_is400() throws Exception {
        // given
        ToolId toolId = ToolId.random();

        // when
        ResultActions result = addInstructors(toolId, "{\"instructorIds\":[]}");

        // then
        result.andExpect(status().isBadRequest());
        expectNoInstructorsAdded();
    }

    @Test
    void addInstructors_unknownTool_is404() throws Exception {
        // given
        ToolId unknown = ToolId.random();
        CustomerId instructorId = CustomerId.random();
        givenAddInstructorsRejectedWith(unknown, instructorId, ResourceNotFoundException.tool(unknown));

        // when
        ResultActions result = addInstructors(unknown, instructorId);

        // then
        expectToolNotFound(result);
    }

    // ------------------------------------------------------------------------
    // removeInstructors

    @Test
    void removeInstructors_passesTheIdsAndIs204() throws Exception {
        // given
        ToolId toolId = ToolId.random();
        CustomerId instructorId = CustomerId.random();

        // when
        ResultActions result = removeInstructors(toolId, instructorId);

        // then
        result.andExpect(status().isNoContent());
        expectInstructorsRemoved(toolId, List.of(instructorId));
    }

    @Test
    void removeInstructors_withSomebodyWhoIsNoInstructor_is409WithTheCode() throws Exception {
        // given
        ToolId toolId = ToolId.random();
        CustomerId customerId = givenNoInstructorOf(toolId);

        // when
        ResultActions result = removeInstructors(toolId, customerId);

        // then
        result.andExpect(status().isConflict()) //
                .andExpect(jsonPath("$.code").value(CustomerNotInstructorException.CODE));
    }

    @Test
    void removeInstructors_unknownTool_is404() throws Exception {
        // given
        ToolId unknown = ToolId.random();
        CustomerId instructorId = CustomerId.random();
        givenRemoveInstructorsRejectedWith(unknown, instructorId, ResourceNotFoundException.tool(unknown));

        // when
        ResultActions result = removeInstructors(unknown, instructorId);

        // then
        expectToolNotFound(result);
    }

    // ------------------------------------------------------------------------
    // deleteTool

    @Test
    void deleteTool_is204() throws Exception {
        // given
        ToolId toolId = ToolId.random();

        // when
        ResultActions result = deleteTool(toolId);

        // then
        result.andExpect(status().isNoContent());
        expectDeleted(toolId);
    }

    @Test
    void deleteTool_ofAToolInUse_is409WithTheCode() throws Exception {
        // given
        ToolId toolId = ToolId.random();
        givenDeleteRejectedWith(toolId, ToolInUseException.delete(toolId));

        // when
        ResultActions result = deleteTool(toolId);

        // then
        result.andExpect(status().isConflict()) //
                .andExpect(jsonPath("$.code").value(ToolInUseException.CODE));
    }

    @Test
    void deleteTool_unknownId_is404() throws Exception {
        // given
        ToolId unknown = ToolId.random();
        givenDeleteRejectedWith(unknown, ResourceNotFoundException.tool(unknown));

        // when
        ResultActions result = deleteTool(unknown);

        // then
        expectToolNotFound(result);
    }

    // ------------------------------------------------------------------------
    // given

    private void givenTools(ToolView... tools) {
        when(this.toolReadFacade.getTools()).thenReturn(List.of(tools));
    }

    private ToolView givenTool() {
        ToolView tool = toolView().build();
        when(this.toolReadFacade.getTool(tool.id())).thenReturn(Optional.of(tool));

        return tool;
    }

    private ToolId givenUnknownTool() {
        ToolId unknown = ToolId.random();
        when(this.toolReadFacade.getTool(unknown)).thenReturn(Optional.empty());

        return unknown;
    }

    private ToolView givenToolWithSwitchBox() {
        ToolView tool = toolView().rfidReaderId(RfidReaderId.random()).build();
        when(this.toolReadFacade.getTool(tool.rfidReaderId())).thenReturn(Optional.of(tool));

        return tool;
    }

    private RfidReaderId givenRfidReaderWithoutTool() {
        RfidReaderId rfidReaderId = RfidReaderId.random();
        when(this.toolReadFacade.getTool(rfidReaderId)).thenReturn(Optional.empty());

        return rfidReaderId;
    }

    private ToolId givenCreatedTool(CreateToolCommand command) {
        ToolId toolId = ToolId.random();
        when(this.toolCreate.process(command)).thenReturn(toolId);

        return toolId;
    }

    private void givenNameUsedForCreate(CreateToolCommand command) {
        when(this.toolCreate.process(command)).thenThrow(new ToolNameAlreadyUsedException(command.name(), ToolId.random()));
    }

    private void givenNameUsedForUpdate(UpdateToolCommand command) {
        doThrow(new ToolNameAlreadyUsedException(command.name(), ToolId.random())).when(this.toolUpdate).process(command);
    }

    private void givenRejectedAssignment(ToolId toolId, RfidReaderId rfidReaderId, RuntimeException rejection) {
        doThrow(rejection).when(this.toolAssignRfidReader).process(toolId, rfidReaderId);
    }

    private void givenIpAddressUsed(SetWlanRelaisCommand command) {
        doThrow(new IpAddressAlreadyUsedException(command.ipAddress(), ToolId.random())).when(this.toolSetWlanRelais).process(command);
    }

    private void givenUpdateRejectedWith(UpdateToolCommand command, RuntimeException rejection) {
        doThrow(rejection).when(this.toolUpdate).process(command);
    }

    private void givenClearRfidReaderRejectedWith(ToolId toolId, RuntimeException rejection) {
        doThrow(rejection).when(this.toolClearRfidReader).process(toolId);
    }

    private void givenSetWlanRelaisRejectedWith(SetWlanRelaisCommand command, RuntimeException rejection) {
        doThrow(rejection).when(this.toolSetWlanRelais).process(command);
    }

    private void givenClearWlanRelaisRejectedWith(ToolId toolId, RuntimeException rejection) {
        doThrow(rejection).when(this.toolClearWlanRelais).process(toolId);
    }

    private void givenAddInstructorsRejectedWith(ToolId toolId, CustomerId instructorId, RuntimeException rejection) {
        doThrow(rejection).when(this.toolAddInstructors).process(toolId, List.of(instructorId));
    }

    private void givenRemoveInstructorsRejectedWith(ToolId toolId, CustomerId instructorId, RuntimeException rejection) {
        doThrow(rejection).when(this.toolRemoveInstructors).process(toolId, List.of(instructorId));
    }

    private void givenDeleteRejectedWith(ToolId toolId, RuntimeException rejection) {
        doThrow(rejection).when(this.toolDelete).process(toolId);
    }

    // T-1, T-10 and the length of the description – each format rule of the request body once (E33)
    static Stream<Arguments> invalidCreateToolCommands() {
        return Stream.of( //
                arguments(named("blank name (T-1)", createToolCommand().name(" ").build())), //
                arguments(named("description of 256 characters", createToolCommand().description("x".repeat(256)).build())), //
                arguments(named("no area (T-10)", createToolCommand().area(null).build())), //
                arguments(named("no certification requirement (T-10)", createToolCommand().certificationRequirement(null).build())));
    }

    static Stream<Arguments> invalidUpdateToolCommands() {
        return Stream.of( //
                arguments(named("blank name (T-1)", updateToolCommand().name(" ").build())), //
                arguments(named("description of 256 characters", updateToolCommand().description("x".repeat(256)).build())), //
                arguments(named("no area (T-10)", updateToolCommand().area(null).build())), //
                arguments(named("no certification requirement (T-10)", updateToolCommand().certificationRequirement(null).build())));
    }

    private CustomerId givenInstructorOf(ToolId toolId) {
        CustomerId instructorId = CustomerId.random();
        doThrow(new CustomerAlreadyInstructorException(instructorId, toolId)).when(this.toolAddInstructors).process(toolId, List.of(instructorId));

        return instructorId;
    }

    private CustomerId givenNoInstructorOf(ToolId toolId) {
        CustomerId customerId = CustomerId.random();
        doThrow(new CustomerNotInstructorException(customerId, toolId)).when(this.toolRemoveInstructors).process(toolId, List.of(customerId));

        return customerId;
    }

    // ------------------------------------------------------------------------
    // when

    private ResultActions getTools() throws Exception {
        return this.mockMvc.perform(get("/api/v1/tools"));
    }

    private ResultActions getTools(RfidReaderId rfidReaderId) throws Exception {
        return this.mockMvc.perform(get("/api/v1/tools").param("rfidReaderId", rfidReaderId.getValue().toString()));
    }

    private ResultActions getTool(ToolId toolId) throws Exception {
        return this.mockMvc.perform(get("/api/v1/tools/" + toolId.getValue()));
    }

    // the commands have the fields of the request bodies; an id in the body is ignored, the path decides
    private ResultActions createTool(CreateToolCommand command) throws Exception {
        return this.mockMvc.perform(post("/api/v1/tools") //
                .contentType(MediaType.APPLICATION_JSON) //
                .content(Json.write(command)));
    }

    private ResultActions updateTool(UpdateToolCommand command) throws Exception {
        return this.mockMvc.perform(put("/api/v1/tools/" + command.id().getValue()) //
                .contentType(MediaType.APPLICATION_JSON) //
                .content(Json.write(command)));
    }

    private ResultActions assignRfidReader(ToolId toolId, RfidReaderId rfidReaderId) throws Exception {
        return this.mockMvc.perform(put("/api/v1/tools/" + toolId.getValue() + "/rfid-reader/" + rfidReaderId.getValue()));
    }

    private ResultActions clearRfidReader(ToolId toolId) throws Exception {
        return this.mockMvc.perform(delete("/api/v1/tools/" + toolId.getValue() + "/rfid-reader"));
    }

    private ResultActions setWlanRelais(SetWlanRelaisCommand command) throws Exception {
        return setWlanRelais(command.id(), Json.write(command));
    }

    private ResultActions setWlanRelais(ToolId toolId, String body) throws Exception {
        return this.mockMvc.perform(put("/api/v1/tools/" + toolId.getValue() + "/wlan-relais") //
                .contentType(MediaType.APPLICATION_JSON) //
                .content(body));
    }

    private ResultActions clearWlanRelais(ToolId toolId) throws Exception {
        return this.mockMvc.perform(delete("/api/v1/tools/" + toolId.getValue() + "/wlan-relais"));
    }

    private ResultActions addInstructors(ToolId toolId, CustomerId instructorId) throws Exception {
        return addInstructors(toolId, instructorsBody(instructorId));
    }

    private ResultActions addInstructors(ToolId toolId, String body) throws Exception {
        return this.mockMvc.perform(post("/api/v1/tools/" + toolId.getValue() + "/instructors") //
                .contentType(MediaType.APPLICATION_JSON) //
                .content(body));
    }

    private ResultActions removeInstructors(ToolId toolId, CustomerId instructorId) throws Exception {
        return this.mockMvc.perform(delete("/api/v1/tools/" + toolId.getValue() + "/instructors") //
                .contentType(MediaType.APPLICATION_JSON) //
                .content(instructorsBody(instructorId)));
    }

    private ResultActions deleteTool(ToolId toolId) throws Exception {
        return this.mockMvc.perform(delete("/api/v1/tools/" + toolId.getValue()));
    }

    // ------------------------------------------------------------------------
    // then

    private void expectToolNotFound(ResultActions result) throws Exception {
        result.andExpect(status().isNotFound()) //
                .andExpect(jsonPath("$.code").value(ResourceNotFoundException.CODE)) //
                .andExpect(jsonPath("$.resource").value(Resource.TOOL.name()));
    }

    private void expectNothingCreated() {
        verify(this.toolCreate, never()).process(any());
    }

    private void expectUpdated(UpdateToolCommand command) {
        verify(this.toolUpdate).process(command);
    }

    private void expectNothingUpdated() {
        verify(this.toolUpdate, never()).process(any());
    }

    private void expectRfidReaderAssigned(ToolId toolId, RfidReaderId rfidReaderId) {
        verify(this.toolAssignRfidReader).process(toolId, rfidReaderId);
    }

    private void expectRfidReaderCleared(ToolId toolId) {
        verify(this.toolClearRfidReader).process(toolId);
    }

    private void expectWlanRelaisSet(SetWlanRelaisCommand command) {
        verify(this.toolSetWlanRelais).process(command);
    }

    private void expectNoWlanRelaisSet() {
        verify(this.toolSetWlanRelais, never()).process(any());
    }

    private void expectWlanRelaisCleared(ToolId toolId) {
        verify(this.toolClearWlanRelais).process(toolId);
    }

    private void expectInstructorsAdded(ToolId toolId, List<CustomerId> instructorIds) {
        verify(this.toolAddInstructors).process(toolId, instructorIds);
    }

    private void expectNoInstructorsAdded() {
        verify(this.toolAddInstructors, never()).process(any(), any());
    }

    private void expectInstructorsRemoved(ToolId toolId, List<CustomerId> instructorIds) {
        verify(this.toolRemoveInstructors).process(toolId, instructorIds);
    }

    private void expectDeleted(ToolId toolId) {
        verify(this.toolDelete).process(toolId);
    }

    // ------------------------------------------------------------------------
    // helper

    private String instructorsBody(CustomerId instructorId) {
        return "{\"instructorIds\":[\"" + instructorId.getValue() + "\"]}";
    }

}

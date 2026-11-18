package de.schaffbar.core_pos.domain.tool.web;

import static de.schaffbar.core_pos.builder.CommandBuilder.createToolCommand;
import static de.schaffbar.core_pos.builder.CommandBuilder.setWlanRelaisCommand;
import static de.schaffbar.core_pos.builder.CommandBuilder.updateToolCommand;
import static de.schaffbar.core_pos.builder.ViewBuilder.toolView;
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import de.schaffbar.core_pos.domain.tool.ToolCommands.CreateToolCommand;
import de.schaffbar.core_pos.domain.tool.ToolCommands.SetWlanRelaisCommand;
import de.schaffbar.core_pos.domain.tool.ToolCommands.UpdateToolCommand;
import de.schaffbar.core_pos.domain.tool.ToolViews.ToolView;
import de.schaffbar.core_pos.read_facade.ToolReadFacade;
import de.schaffbar.core_pos.shared.exception.CustomerAlreadyInstructorException;
import de.schaffbar.core_pos.shared.exception.CustomerNotInstructorException;
import de.schaffbar.core_pos.shared.exception.InvalidRfidReaderTypeForToolException;
import de.schaffbar.core_pos.shared.exception.IpAddressAlreadyUsedException;
import de.schaffbar.core_pos.shared.exception.RfidReaderAlreadyAssignedToToolException;
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
                .setControllerAdvice(new ApiExceptionHandler()) //
                .build();
    }

    // ------------------------------------------------------------------------
    // getTools

    @Test
    void getTools_withoutFilter_returnsAll() throws Exception {
        // given
        ToolView tool = toolView().instructors(Set.of(CustomerId.random())).build();
        givenTools(tool);

        // when
        ResultActions result = getTools();

        // then
        result.andExpect(status().isOk()) //
                .andExpect(jsonPath("$.length()").value(1)) //
                .andExpect(jsonPath("$[0].id").value(tool.id().getValue().toString())) //
                .andExpect(jsonPath("$[0].name").value(tool.name())) //
                .andExpect(jsonPath("$[0].area").value(tool.area().name())) //
                .andExpect(jsonPath("$[0].instructors.length()").value(1));
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
                .andExpect(jsonPath("$.certificationRequirement").value(tool.certificationRequirement().name()));
    }

    @Test
    void getTool_unknownId_is404() throws Exception {
        // given
        ToolId unknown = givenUnknownTool();

        // when
        ResultActions result = getTool(unknown);

        // then
        result.andExpect(status().isNotFound()) //
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
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

    @Test
    void createTool_withoutName_is400() throws Exception {
        // given
        CreateToolCommand command = createToolCommand().name(" ").build();

        // when
        ResultActions result = createTool(command);

        // then
        result.andExpect(status().isBadRequest());
        verify(this.toolCreate, never()).process(any());
    }

    @Test
    void createTool_withADescriptionOf256Characters_is400() throws Exception {
        // given
        CreateToolCommand command = createToolCommand().description("x".repeat(256)).build();

        // when
        ResultActions result = createTool(command);

        // then
        result.andExpect(status().isBadRequest());
        verify(this.toolCreate, never()).process(any());
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
        verify(this.toolUpdate).process(command);
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

    @Test
    void updateTool_withADescriptionOf256Characters_is400() throws Exception {
        // given
        UpdateToolCommand command = updateToolCommand().description("x".repeat(256)).build();

        // when
        ResultActions result = updateTool(command);

        // then
        result.andExpect(status().isBadRequest());
        verify(this.toolUpdate, never()).process(any());
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
        verify(this.toolAssignRfidReader).process(toolId, rfidReaderId);
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
        verify(this.toolClearRfidReader).process(toolId);
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
        verify(this.toolSetWlanRelais).process(command);
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
        verify(this.toolSetWlanRelais, never()).process(any());
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
        verify(this.toolClearWlanRelais).process(toolId);
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
        verify(this.toolAddInstructors).process(toolId, List.of(instructorId));
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
        verify(this.toolAddInstructors, never()).process(any(), any());
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
        verify(this.toolRemoveInstructors).process(toolId, List.of(instructorId));
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
        verify(this.toolDelete).process(toolId);
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
    // helper

    private String instructorsBody(CustomerId instructorId) {
        return "{\"instructorIds\":[\"" + instructorId.getValue() + "\"]}";
    }

}

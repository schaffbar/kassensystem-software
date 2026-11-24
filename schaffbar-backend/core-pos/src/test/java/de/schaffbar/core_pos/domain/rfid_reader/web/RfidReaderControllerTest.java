package de.schaffbar.core_pos.domain.rfid_reader.web;

import static de.schaffbar.core_pos.builder.CommandBuilder.changeRfidReaderTypeCommand;
import static de.schaffbar.core_pos.builder.CommandBuilder.updateRfidReaderCommand;
import static de.schaffbar.core_pos.builder.ViewBuilder.rfidReaderView;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

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

import de.schaffbar.core_pos.builder.RandomValues;
import de.schaffbar.core_pos.domain.rfid_reader.RfidReaderCommands.ChangeRfidReaderTypeCommand;
import de.schaffbar.core_pos.domain.rfid_reader.RfidReaderCommands.UpdateRfidReaderCommand;
import de.schaffbar.core_pos.domain.rfid_reader.RfidReaderViews.RfidReaderView;
import de.schaffbar.core_pos.read_facade.RfidReaderReadFacade;
import de.schaffbar.core_pos.shared.exception.MacAddressAlreadyUsedException;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException.Resource;
import de.schaffbar.core_pos.shared.exception.RfidReaderHasToolAssignedException;
import de.schaffbar.core_pos.shared.exception.RfidReaderNameAlreadyUsedException;
import de.schaffbar.core_pos.shared.id.MacAddress;
import de.schaffbar.core_pos.shared.id.RfidReaderId;
import de.schaffbar.core_pos.shared.util.StringToMacAddressConverter;
import de.schaffbar.core_pos.shared.util.StringToRfidReaderIdConverter;
import de.schaffbar.core_pos.shared.web.ApiExceptionHandler;
import de.schaffbar.core_pos.support.Json;
import de.schaffbar.core_pos.use_case.RfidReaderChangeType;
import de.schaffbar.core_pos.use_case.RfidReaderCreate;
import de.schaffbar.core_pos.use_case.RfidReaderDelete;
import de.schaffbar.core_pos.use_case.RfidReaderUpdate;

@ExtendWith(MockitoExtension.class)
class RfidReaderControllerTest {

    @Mock
    private RfidReaderReadFacade rfidReaderReadFacade;

    @Mock
    private RfidReaderCreate rfidReaderCreate;

    @Mock
    private RfidReaderUpdate rfidReaderUpdate;

    @Mock
    private RfidReaderDelete rfidReaderDelete;

    @Mock
    private RfidReaderChangeType rfidReaderChangeType;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        DefaultFormattingConversionService conversionService = new DefaultFormattingConversionService();
        conversionService.addConverter(new StringToRfidReaderIdConverter());
        conversionService.addConverter(new StringToMacAddressConverter());

        RfidReaderController controller = new RfidReaderController(this.rfidReaderReadFacade, this.rfidReaderCreate, this.rfidReaderUpdate,
                this.rfidReaderDelete, this.rfidReaderChangeType);

        this.mockMvc = MockMvcBuilders.standaloneSetup(controller) //
                .setConversionService(conversionService) //
                .setMessageConverters(Json.messageConverter()) //
                .setControllerAdvice(new ApiExceptionHandler()) //
                .build();
    }

    // ------------------------------------------------------------------------
    // getRfidReaders

    @Test
    void getRfidReaders_withoutFilter_returnsAll() throws Exception {
        // given
        RfidReaderView rfidReader = rfidReaderView().build();
        givenReaders(rfidReader);

        // when
        ResultActions result = getRfidReaders();

        // then
        result.andExpect(status().isOk()) //
                .andExpect(jsonPath("$.length()").value(1)) //
                .andExpect(jsonPath("$[0].id").value(rfidReader.id().getValue().toString())) //
                .andExpect(jsonPath("$[0].macAddress").value(rfidReader.macAddress().getValue())) //
                .andExpect(jsonPath("$[0].type").value(rfidReader.type().name())) //
                .andExpect(jsonPath("$[0].name").value(rfidReader.name())) //
                .andExpect(jsonPath("$[0].socketName").value(rfidReader.socketName())) //
                .andExpect(jsonPath("$[0].createdAt").value(rfidReader.createdAt().toString()));
    }

    @Test
    void getRfidReaders_withMacAddress_returnsAListWithTheReader() throws Exception {
        // given
        MacAddress macAddress = RandomValues.macAddress();
        givenReaderWith(macAddress);

        // when
        ResultActions result = getRfidReaders(macAddress.getValue());

        // then
        result.andExpect(status().isOk()) //
                .andExpect(jsonPath("$.length()").value(1)) //
                .andExpect(jsonPath("$[0].macAddress").value(macAddress.getValue()));
    }

    @Test
    void getRfidReaders_withUnknownMacAddress_returnsAnEmptyList() throws Exception {
        // given
        MacAddress macAddress = RandomValues.macAddress();
        givenNoReaderWith(macAddress);

        // when
        ResultActions result = getRfidReaders(macAddress.getValue());

        // then
        result.andExpect(status().isOk()) //
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getRfidReaders_withInvalidMacAddress_is400() throws Exception {
        // when
        ResultActions result = getRfidReaders("34CDB0D3A930");

        // then
        result.andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------------
    // getRfidReader

    @Test
    void getRfidReader_returnsTheReader() throws Exception {
        // given
        RfidReaderView rfidReader = givenReader();

        // when
        ResultActions result = getRfidReader(rfidReader.id());

        // then
        result.andExpect(status().isOk()) //
                .andExpect(jsonPath("$.id").value(rfidReader.id().getValue().toString())) //
                .andExpect(jsonPath("$.macAddress").value(rfidReader.macAddress().getValue())) //
                .andExpect(jsonPath("$.type").value(rfidReader.type().name())) //
                .andExpect(jsonPath("$.name").value(rfidReader.name())) //
                .andExpect(jsonPath("$.socketName").value(rfidReader.socketName())) //
                .andExpect(jsonPath("$.createdAt").value(rfidReader.createdAt().toString()));
    }

    @Test
    void getRfidReader_unknownId_is404() throws Exception {
        // given
        RfidReaderId unknown = givenUnknownReader();

        // when
        ResultActions result = getRfidReader(unknown);

        // then
        expectRfidReaderNotFound(result);
    }

    // ------------------------------------------------------------------------
    // createRfidReader

    @Test
    void createRfidReader_returns201WithTheLocation() throws Exception {
        // given
        MacAddress macAddress = RandomValues.macAddress();
        RfidReaderId rfidReaderId = givenCreatedReader(macAddress);

        // when
        ResultActions result = createRfidReader(macAddress);

        // then
        result.andExpect(status().isCreated()) //
                .andExpect(header().string("Location", "/api/v1/rfid-readers/" + rfidReaderId.getValue()));
    }

    @Test
    void createRfidReader_withAUsedMacAddress_is409WithTheCode() throws Exception {
        // given
        MacAddress macAddress = RandomValues.macAddress();
        givenCreateRejectedWith(macAddress, new MacAddressAlreadyUsedException(macAddress, RfidReaderId.random()));

        // when
        ResultActions result = createRfidReader(macAddress);

        // then
        result.andExpect(status().isConflict()) //
                .andExpect(jsonPath("$.code").value(MacAddressAlreadyUsedException.CODE));
    }

    @Test
    void createRfidReader_withoutMacAddress_is400() throws Exception {
        // when
        ResultActions result = createRfidReader("{}");

        // then
        result.andExpect(status().isBadRequest());
        expectNothingCreated();
    }

    @Test
    void createRfidReader_withInvalidMacAddress_is400() throws Exception {
        // when
        ResultActions result = createRfidReader("{\"macAddress\":\"34-CD-B0-D3-A9-30\"}");

        // then
        result.andExpect(status().isBadRequest());
        expectNothingCreated();
    }

    // ------------------------------------------------------------------------
    // updateRfidReader

    @Test
    void updateRfidReader_passesTheCommandAndIs204() throws Exception {
        // given
        UpdateRfidReaderCommand command = updateRfidReaderCommand().build();

        // when
        ResultActions result = updateRfidReader(command);

        // then
        result.andExpect(status().isNoContent());
        expectUpdated(command);
    }

    @Test
    void updateRfidReader_unknownId_is404() throws Exception {
        // given
        UpdateRfidReaderCommand command = updateRfidReaderCommand().build();
        givenUpdateRejectedWith(command, ResourceNotFoundException.rfidReader(command.id()));

        // when
        ResultActions result = updateRfidReader(command);

        // then
        expectRfidReaderNotFound(result);
    }

    @Test
    void updateRfidReader_withTheNameOfAnotherReader_is409WithTheCode() throws Exception {
        // given
        UpdateRfidReaderCommand command = updateRfidReaderCommand().build();
        givenUpdateRejectedWith(command, new RfidReaderNameAlreadyUsedException(command.name(), RfidReaderId.random()));

        // when
        ResultActions result = updateRfidReader(command);

        // then
        result.andExpect(status().isConflict()) //
                .andExpect(jsonPath("$.code").value(RfidReaderNameAlreadyUsedException.CODE));
    }

    // ------------------------------------------------------------------------
    // changeRfidReaderType

    @Test
    void changeRfidReaderType_passesTheCommandAndIs204() throws Exception {
        // given
        ChangeRfidReaderTypeCommand command = changeRfidReaderTypeCommand().build();

        // when
        ResultActions result = changeRfidReaderType(command);

        // then
        result.andExpect(status().isNoContent());
        expectTypeChanged(command);
    }

    @Test
    void changeRfidReaderType_withUnknownType_is400() throws Exception {
        // given
        RfidReaderId rfidReaderId = RfidReaderId.random();

        // when
        ResultActions result = changeRfidReaderType(rfidReaderId, "{\"type\":\"COFFEE_MACHINE\"}");

        // then
        result.andExpect(status().isBadRequest());
        expectTypeNotChanged();
    }

    @Test
    void changeRfidReaderType_ofASwitchBoxWithTool_is409WithTheCode() throws Exception {
        // given
        ChangeRfidReaderTypeCommand command = changeRfidReaderTypeCommand().build();
        givenChangeTypeRejectedWith(command, RfidReaderHasToolAssignedException.changeType(command.id()));

        // when
        ResultActions result = changeRfidReaderType(command);

        // then
        result.andExpect(status().isConflict()) //
                .andExpect(jsonPath("$.code").value(RfidReaderHasToolAssignedException.CODE));
    }

    @Test
    void changeRfidReaderType_unknownId_is404() throws Exception {
        // given
        ChangeRfidReaderTypeCommand command = changeRfidReaderTypeCommand().build();
        givenChangeTypeRejectedWith(command, ResourceNotFoundException.rfidReader(command.id()));

        // when
        ResultActions result = changeRfidReaderType(command);

        // then
        expectRfidReaderNotFound(result);
    }

    // ------------------------------------------------------------------------
    // deleteRfidReader

    @Test
    void deleteRfidReader_is204() throws Exception {
        // given
        RfidReaderId rfidReaderId = RfidReaderId.random();

        // when
        ResultActions result = deleteRfidReader(rfidReaderId);

        // then
        result.andExpect(status().isNoContent());
        expectDeleted(rfidReaderId);
    }

    @Test
    void deleteRfidReader_withTool_is409WithTheCode() throws Exception {
        // given
        RfidReaderId rfidReaderId = RfidReaderId.random();
        givenDeleteRejectedWith(rfidReaderId, RfidReaderHasToolAssignedException.delete(rfidReaderId));

        // when
        ResultActions result = deleteRfidReader(rfidReaderId);

        // then
        result.andExpect(status().isConflict()) //
                .andExpect(jsonPath("$.code").value(RfidReaderHasToolAssignedException.CODE));
    }

    @Test
    void deleteRfidReader_unknownId_is404() throws Exception {
        // given
        RfidReaderId unknown = RfidReaderId.random();
        givenDeleteRejectedWith(unknown, ResourceNotFoundException.rfidReader(unknown));

        // when
        ResultActions result = deleteRfidReader(unknown);

        // then
        expectRfidReaderNotFound(result);
    }

    // ------------------------------------------------------------------------
    // given

    private void givenReaders(RfidReaderView... rfidReaders) {
        when(this.rfidReaderReadFacade.getRfidReaders()).thenReturn(List.of(rfidReaders));
    }

    private void givenReaderWith(MacAddress macAddress) {
        RfidReaderView rfidReader = rfidReaderView().macAddress(macAddress).build();
        when(this.rfidReaderReadFacade.getRfidReader(macAddress)).thenReturn(Optional.of(rfidReader));
    }

    private void givenNoReaderWith(MacAddress macAddress) {
        when(this.rfidReaderReadFacade.getRfidReader(macAddress)).thenReturn(Optional.empty());
    }

    private RfidReaderView givenReader() {
        RfidReaderView rfidReader = rfidReaderView().build();
        when(this.rfidReaderReadFacade.getRfidReader(rfidReader.id())).thenReturn(Optional.of(rfidReader));

        return rfidReader;
    }

    private RfidReaderId givenUnknownReader() {
        RfidReaderId unknown = RfidReaderId.random();
        when(this.rfidReaderReadFacade.getRfidReader(unknown)).thenReturn(Optional.empty());

        return unknown;
    }

    private RfidReaderId givenCreatedReader(MacAddress macAddress) {
        RfidReaderId rfidReaderId = RfidReaderId.random();
        when(this.rfidReaderCreate.process(macAddress)).thenReturn(rfidReaderId);

        return rfidReaderId;
    }

    private void givenCreateRejectedWith(MacAddress macAddress, RuntimeException rejection) {
        when(this.rfidReaderCreate.process(macAddress)).thenThrow(rejection);
    }

    private void givenUpdateRejectedWith(UpdateRfidReaderCommand command, RuntimeException rejection) {
        doThrow(rejection).when(this.rfidReaderUpdate).process(command);
    }

    private void givenChangeTypeRejectedWith(ChangeRfidReaderTypeCommand command, RuntimeException rejection) {
        doThrow(rejection).when(this.rfidReaderChangeType).process(command);
    }

    private void givenDeleteRejectedWith(RfidReaderId rfidReaderId, RuntimeException rejection) {
        doThrow(rejection).when(this.rfidReaderDelete).process(rfidReaderId);
    }

    // ------------------------------------------------------------------------
    // when

    private ResultActions getRfidReaders() throws Exception {
        return this.mockMvc.perform(get("/api/v1/rfid-readers"));
    }

    private ResultActions getRfidReaders(String macAddress) throws Exception {
        return this.mockMvc.perform(get("/api/v1/rfid-readers").param("macAddress", macAddress));
    }

    private ResultActions getRfidReader(RfidReaderId rfidReaderId) throws Exception {
        return this.mockMvc.perform(get("/api/v1/rfid-readers/" + rfidReaderId.getValue()));
    }

    private ResultActions createRfidReader(MacAddress macAddress) throws Exception {
        return createRfidReader("{\"macAddress\":\"" + macAddress.getValue() + "\"}");
    }

    private ResultActions createRfidReader(String body) throws Exception {
        return this.mockMvc.perform(post("/api/v1/rfid-readers") //
                .contentType(MediaType.APPLICATION_JSON) //
                .content(body));
    }

    private ResultActions updateRfidReader(UpdateRfidReaderCommand command) throws Exception {
        return this.mockMvc.perform(put("/api/v1/rfid-readers/" + command.id().getValue()) //
                .contentType(MediaType.APPLICATION_JSON) //
                .content(Json.write(command)));
    }

    private ResultActions changeRfidReaderType(ChangeRfidReaderTypeCommand command) throws Exception {
        return changeRfidReaderType(command.id(), Json.write(command));
    }

    private ResultActions changeRfidReaderType(RfidReaderId rfidReaderId, String body) throws Exception {
        return this.mockMvc.perform(patch("/api/v1/rfid-readers/" + rfidReaderId.getValue() + "/type") //
                .contentType(MediaType.APPLICATION_JSON) //
                .content(body));
    }

    private ResultActions deleteRfidReader(RfidReaderId rfidReaderId) throws Exception {
        return this.mockMvc.perform(delete("/api/v1/rfid-readers/" + rfidReaderId.getValue()));
    }

    // ------------------------------------------------------------------------
    // then

    private void expectRfidReaderNotFound(ResultActions result) throws Exception {
        result.andExpect(status().isNotFound()) //
                .andExpect(jsonPath("$.code").value(ResourceNotFoundException.CODE)) //
                .andExpect(jsonPath("$.resource").value(Resource.RFID_READER.name()));
    }

    private void expectNothingCreated() {
        verify(this.rfidReaderCreate, never()).process(any());
    }

    private void expectUpdated(UpdateRfidReaderCommand command) {
        verify(this.rfidReaderUpdate).process(command);
    }

    private void expectTypeChanged(ChangeRfidReaderTypeCommand command) {
        verify(this.rfidReaderChangeType).process(command);
    }

    private void expectTypeNotChanged() {
        verify(this.rfidReaderChangeType, never()).process(any());
    }

    private void expectDeleted(RfidReaderId rfidReaderId) {
        verify(this.rfidReaderDelete).process(rfidReaderId);
    }

}

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
import de.schaffbar.core_pos.domain.rfid_reader.RfidReaderType;
import de.schaffbar.core_pos.domain.rfid_reader.RfidReaderViews.RfidReaderView;
import de.schaffbar.core_pos.read_facade.RfidReaderReadFacade;
import de.schaffbar.core_pos.shared.exception.RfidReaderHasToolAssignedException;
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
                .setControllerAdvice(new ApiExceptionHandler()) //
                .build();
    }

    // ------------------------------------------------------------------------
    // getRfidReaders

    @Test
    void getRfidReaders_withoutFilter_returnsAll() throws Exception {
        // given
        RfidReaderView rfidReader = rfidReaderView().type(RfidReaderType.SWITCH_BOX).build();
        givenReaders(rfidReader);

        // when
        ResultActions result = this.mockMvc.perform(get("/api/v1/rfid-readers"));

        // then
        result.andExpect(status().isOk()) //
                .andExpect(jsonPath("$.length()").value(1)) //
                .andExpect(jsonPath("$[0].id").value(rfidReader.id().getValue().toString())) //
                .andExpect(jsonPath("$[0].type").value("SWITCH_BOX"));
    }

    @Test
    void getRfidReaders_withMacAddress_returnsAListWithTheReader() throws Exception {
        // given
        MacAddress macAddress = RandomValues.macAddress();
        givenReaderWith(macAddress);

        // when
        ResultActions result = this.mockMvc.perform(get("/api/v1/rfid-readers").param("macAddress", macAddress.getValue()));

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
        ResultActions result = this.mockMvc.perform(get("/api/v1/rfid-readers").param("macAddress", macAddress.getValue()));

        // then
        result.andExpect(status().isOk()) //
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getRfidReaders_withInvalidMacAddress_is400() throws Exception {
        // when
        ResultActions result = this.mockMvc.perform(get("/api/v1/rfid-readers").param("macAddress", "34CDB0D3A930"));

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
        ResultActions result = this.mockMvc.perform(get("/api/v1/rfid-readers/" + rfidReader.id().getValue()));

        // then
        result.andExpect(status().isOk()) //
                .andExpect(jsonPath("$.id").value(rfidReader.id().getValue().toString())) //
                .andExpect(jsonPath("$.macAddress").value(rfidReader.macAddress())) //
                .andExpect(jsonPath("$.type").value("SWITCH_BOX"));
    }

    @Test
    void getRfidReader_unknownId_is404() throws Exception {
        // given
        RfidReaderId unknown = givenUnknownReader();

        // when
        ResultActions result = this.mockMvc.perform(get("/api/v1/rfid-readers/" + unknown.getValue()));

        // then
        result.andExpect(status().isNotFound()) //
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    // ------------------------------------------------------------------------
    // createRfidReader

    @Test
    void createRfidReader_returns201WithTheLocation() throws Exception {
        // given
        MacAddress macAddress = RandomValues.macAddress();
        RfidReaderId rfidReaderId = givenCreatedReader(macAddress);

        // when
        ResultActions result = this.mockMvc.perform(post("/api/v1/rfid-readers") //
                .contentType(MediaType.APPLICATION_JSON) //
                .content("{\"macAddress\":\"" + macAddress.getValue() + "\"}"));

        // then
        result.andExpect(status().isCreated()) //
                .andExpect(header().string("Location", "/api/v1/rfid-readers/" + rfidReaderId.getValue()));
    }

    @Test
    void createRfidReader_withoutMacAddress_is400() throws Exception {
        // when
        ResultActions result = this.mockMvc.perform(post("/api/v1/rfid-readers").contentType(MediaType.APPLICATION_JSON).content("{}"));

        // then
        result.andExpect(status().isBadRequest());
        expectNothingCreated();
    }

    @Test
    void createRfidReader_withInvalidMacAddress_is400() throws Exception {
        // when
        ResultActions result = this.mockMvc.perform(post("/api/v1/rfid-readers") //
                .contentType(MediaType.APPLICATION_JSON) //
                .content("{\"macAddress\":\"34-CD-B0-D3-A9-30\"}"));

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
        ResultActions result = this.mockMvc.perform(put("/api/v1/rfid-readers/" + command.id().getValue()) //
                .contentType(MediaType.APPLICATION_JSON) //
                .content(Json.write(command)));

        // then
        result.andExpect(status().isNoContent());
        verify(this.rfidReaderUpdate).process(command);
    }

    // ------------------------------------------------------------------------
    // changeRfidReaderType

    @Test
    void changeRfidReaderType_passesTheCommandAndIs204() throws Exception {
        // given
        ChangeRfidReaderTypeCommand command = changeRfidReaderTypeCommand().build();

        // when
        ResultActions result = this.mockMvc.perform(patch("/api/v1/rfid-readers/" + command.id().getValue() + "/type") //
                .contentType(MediaType.APPLICATION_JSON) //
                .content(Json.write(command)));

        // then
        result.andExpect(status().isNoContent());
        verify(this.rfidReaderChangeType).process(command);
    }

    @Test
    void changeRfidReaderType_ofASwitchBoxWithTool_is409WithTheCode() throws Exception {
        // given
        ChangeRfidReaderTypeCommand command = changeRfidReaderTypeCommand().type(RfidReaderType.GATE_KEEPER_IN).build();
        givenSwitchBoxWithToolForTypeChange(command);

        // when
        ResultActions result = this.mockMvc.perform(patch("/api/v1/rfid-readers/" + command.id().getValue() + "/type") //
                .contentType(MediaType.APPLICATION_JSON) //
                .content(Json.write(command)));

        // then
        result.andExpect(status().isConflict()) //
                .andExpect(jsonPath("$.code").value(RfidReaderHasToolAssignedException.CODE));
    }

    // ------------------------------------------------------------------------
    // deleteRfidReader

    @Test
    void deleteRfidReader_is204() throws Exception {
        // given
        RfidReaderId rfidReaderId = RfidReaderId.random();

        // when
        ResultActions result = this.mockMvc.perform(delete("/api/v1/rfid-readers/" + rfidReaderId.getValue()));

        // then
        result.andExpect(status().isNoContent());
        verify(this.rfidReaderDelete).process(rfidReaderId);
    }

    @Test
    void deleteRfidReader_withTool_is409WithTheCode() throws Exception {
        // given
        RfidReaderId rfidReaderId = givenReaderWithToolForDelete();

        // when
        ResultActions result = this.mockMvc.perform(delete("/api/v1/rfid-readers/" + rfidReaderId.getValue()));

        // then
        result.andExpect(status().isConflict()) //
                .andExpect(jsonPath("$.code").value(RfidReaderHasToolAssignedException.CODE));
    }

    // ------------------------------------------------------------------------
    // given

    private void givenReaders(RfidReaderView... rfidReaders) {
        when(this.rfidReaderReadFacade.getRfidReaders()).thenReturn(List.of(rfidReaders));
    }

    private void givenReaderWith(MacAddress macAddress) {
        RfidReaderView rfidReader = rfidReaderView().macAddress(macAddress.getValue()).build();
        when(this.rfidReaderReadFacade.getRfidReader(macAddress)).thenReturn(Optional.of(rfidReader));
    }

    private void givenNoReaderWith(MacAddress macAddress) {
        when(this.rfidReaderReadFacade.getRfidReader(macAddress)).thenReturn(Optional.empty());
    }

    private RfidReaderView givenReader() {
        RfidReaderView rfidReader = rfidReaderView().type(RfidReaderType.SWITCH_BOX).build();
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

    private void givenSwitchBoxWithToolForTypeChange(ChangeRfidReaderTypeCommand command) {
        doThrow(RfidReaderHasToolAssignedException.changeType(command.id())).when(this.rfidReaderChangeType).process(command);
    }

    private RfidReaderId givenReaderWithToolForDelete() {
        RfidReaderId rfidReaderId = RfidReaderId.random();
        doThrow(RfidReaderHasToolAssignedException.delete(rfidReaderId)).when(this.rfidReaderDelete).process(rfidReaderId);

        return rfidReaderId;
    }

    // ------------------------------------------------------------------------
    // then

    private void expectNothingCreated() {
        verify(this.rfidReaderCreate, never()).process(any());
    }

}

package de.schaffbar.core_pos.domain.rfid_reader.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import de.schaffbar.core_pos.domain.rfid_reader.RfidReaderType;
import de.schaffbar.core_pos.domain.rfid_reader.RfidReaderViews.RfidReaderView;
import de.schaffbar.core_pos.read_facade.RfidReaderReadFacade;
import de.schaffbar.core_pos.shared.id.MacAddress;
import de.schaffbar.core_pos.shared.id.RfidReaderId;
import de.schaffbar.core_pos.shared.util.StringToMacAddressConverter;
import de.schaffbar.core_pos.shared.util.StringToRfidReaderIdConverter;
import de.schaffbar.core_pos.shared.web.ApiExceptionHandler;
import de.schaffbar.core_pos.use_case.RfidReaderChangeType;
import de.schaffbar.core_pos.use_case.RfidReaderCreate;
import de.schaffbar.core_pos.use_case.RfidReaderDelete;
import de.schaffbar.core_pos.use_case.RfidReaderUpdate;

@ExtendWith(MockitoExtension.class)
class RfidReaderControllerTest {

    private static final MacAddress MAC_ADDRESS = MacAddress.of("34:CD:B0:D3:A9:30");

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

    private final RfidReaderView rfidReader = RfidReaderView.builder() //
            .id(RfidReaderId.random()) //
            .macAddress(MAC_ADDRESS.getValue()) //
            .type(RfidReaderType.SWITCH_BOX) //
            .createdAt(Instant.now()) //
            .build();

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

    @Test
    void getRfidReaders_withoutFilter_returnsAll() throws Exception {
        when(this.rfidReaderReadFacade.getRfidReaders()).thenReturn(List.of(this.rfidReader));

        this.mockMvc.perform(get("/api/v1/rfid-readers")) //
                .andExpect(status().isOk()) //
                .andExpect(jsonPath("$.length()").value(1)) //
                .andExpect(jsonPath("$[0].id").value(this.rfidReader.id().getValue().toString())) //
                .andExpect(jsonPath("$[0].type").value("SWITCH_BOX"));
    }

    @Test
    void getRfidReaders_withMacAddress_returnsAListWithTheReader() throws Exception {
        when(this.rfidReaderReadFacade.getRfidReader(MAC_ADDRESS)).thenReturn(Optional.of(this.rfidReader));

        this.mockMvc.perform(get("/api/v1/rfid-readers").param("macAddress", MAC_ADDRESS.getValue())) //
                .andExpect(status().isOk()) //
                .andExpect(jsonPath("$.length()").value(1)) //
                .andExpect(jsonPath("$[0].macAddress").value(MAC_ADDRESS.getValue()));
    }

    @Test
    void getRfidReaders_withUnknownMacAddress_returnsAnEmptyList() throws Exception {
        when(this.rfidReaderReadFacade.getRfidReader(MAC_ADDRESS)).thenReturn(Optional.empty());

        this.mockMvc.perform(get("/api/v1/rfid-readers").param("macAddress", MAC_ADDRESS.getValue())) //
                .andExpect(status().isOk()) //
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getRfidReader_unknownId_is404() throws Exception {
        RfidReaderId rfidReaderId = RfidReaderId.random();
        when(this.rfidReaderReadFacade.getRfidReader(rfidReaderId)).thenReturn(Optional.empty());

        this.mockMvc.perform(get("/api/v1/rfid-readers/" + rfidReaderId.getValue())) //
                .andExpect(status().isNotFound()) //
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void createRfidReader_returns201WithTheLocation() throws Exception {
        RfidReaderId rfidReaderId = RfidReaderId.random();
        when(this.rfidReaderCreate.process(MAC_ADDRESS)).thenReturn(rfidReaderId);

        this.mockMvc.perform(post("/api/v1/rfid-readers") //
                        .contentType(MediaType.APPLICATION_JSON) //
                        .content("{\"macAddress\":\"" + MAC_ADDRESS.getValue() + "\"}")) //
                .andExpect(status().isCreated()) //
                .andExpect(header().string("Location", "/api/v1/rfid-readers/" + rfidReaderId.getValue()));
    }

    @Test
    void createRfidReader_withoutMacAddress_is400() throws Exception {
        this.mockMvc.perform(post("/api/v1/rfid-readers").contentType(MediaType.APPLICATION_JSON).content("{}")) //
                .andExpect(status().isBadRequest());

        verify(this.rfidReaderCreate, never()).process(any());
    }

    @Test
    void createRfidReader_withInvalidMacAddress_is400() throws Exception {
        this.mockMvc.perform(post("/api/v1/rfid-readers").contentType(MediaType.APPLICATION_JSON).content("{\"macAddress\":\"34-CD-B0-D3-A9-30\"}")) //
                .andExpect(status().isBadRequest());

        verify(this.rfidReaderCreate, never()).process(any());
    }

    @Test
    void getRfidReaders_withInvalidMacAddress_is400() throws Exception {
        this.mockMvc.perform(get("/api/v1/rfid-readers").param("macAddress", "34CDB0D3A930")) //
                .andExpect(status().isBadRequest());
    }

}

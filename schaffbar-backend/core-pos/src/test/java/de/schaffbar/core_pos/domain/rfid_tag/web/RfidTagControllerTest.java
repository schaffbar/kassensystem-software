package de.schaffbar.core_pos.domain.rfid_tag.web;

import static de.schaffbar.core_pos.builder.ViewBuilder.rfidTagView;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import de.schaffbar.core_pos.domain.rfid_tag.RfidTagViews.RfidTagView;
import de.schaffbar.core_pos.read_facade.RfidTagReadFacade;
import de.schaffbar.core_pos.shared.exception.RfidTagAlreadyRegisteredException;
import de.schaffbar.core_pos.shared.exception.RfidTagAssignedToCustomerException;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.RfidTagId;
import de.schaffbar.core_pos.shared.util.StringToRfidTagIdConverter;
import de.schaffbar.core_pos.shared.web.ApiExceptionHandler;
import de.schaffbar.core_pos.use_case.RfidTagCreate;
import de.schaffbar.core_pos.use_case.RfidTagDelete;

@ExtendWith(MockitoExtension.class)
class RfidTagControllerTest {

    @Mock
    private RfidTagReadFacade rfidTagReadFacade;

    @Mock
    private RfidTagCreate rfidTagCreate;

    @Mock
    private RfidTagDelete rfidTagDelete;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        DefaultFormattingConversionService conversionService = new DefaultFormattingConversionService();
        conversionService.addConverter(new StringToRfidTagIdConverter());

        RfidTagController controller = new RfidTagController(this.rfidTagReadFacade, this.rfidTagCreate, this.rfidTagDelete);

        this.mockMvc = MockMvcBuilders.standaloneSetup(controller) //
                .setConversionService(conversionService) //
                .setControllerAdvice(new ApiExceptionHandler()) //
                .build();
    }

    // ------------------------------------------------------------------------
    // getRfidTags

    @Test
    void getRfidTags_returnsAll() throws Exception {
        // given
        RfidTagView rfidTag = rfidTagView().build();
        givenTags(rfidTag);

        // when
        ResultActions result = this.mockMvc.perform(get("/api/v1/rfid-tags"));

        // then
        result.andExpect(status().isOk()) //
                .andExpect(jsonPath("$.length()").value(1)) //
                .andExpect(jsonPath("$[0].id").value(rfidTag.id().getValue())) //
                .andExpect(jsonPath("$[0].active").value(true));
    }

    // ------------------------------------------------------------------------
    // getRfidTag

    @Test
    void getRfidTag_returnsTheTag() throws Exception {
        // given
        RfidTagView rfidTag = givenTag();

        // when
        ResultActions result = this.mockMvc.perform(get("/api/v1/rfid-tags/" + rfidTag.id().getValue()));

        // then
        result.andExpect(status().isOk()) //
                .andExpect(jsonPath("$.id").value(rfidTag.id().getValue()));
    }

    @Test
    void getRfidTag_unknownId_is404() throws Exception {
        // given
        RfidTagId unknown = givenUnknownTag();

        // when
        ResultActions result = this.mockMvc.perform(get("/api/v1/rfid-tags/" + unknown.getValue()));

        // then
        result.andExpect(status().isNotFound()) //
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    // ------------------------------------------------------------------------
    // createRfidTag

    @Test
    void createRfidTag_returns201WithTheLocation() throws Exception {
        // given
        RfidTagId rfidTagId = givenCreatedTag();

        // when
        ResultActions result = this.mockMvc.perform(post("/api/v1/rfid-tags") //
                .contentType(MediaType.APPLICATION_JSON) //
                .content("{\"rfidTagId\":\"" + rfidTagId.getValue() + "\"}"));

        // then
        result.andExpect(status().isCreated()) //
                .andExpect(header().string("Location", "/api/v1/rfid-tags/" + rfidTagId.getValue()));
    }

    @Test
    void createRfidTag_withoutId_is400() throws Exception {
        // when
        ResultActions result = this.mockMvc.perform(post("/api/v1/rfid-tags").contentType(MediaType.APPLICATION_JSON).content("{}"));

        // then
        result.andExpect(status().isBadRequest());
        verify(this.rfidTagCreate, never()).process(any());
    }

    @Test
    void createRfidTag_registeredTag_is409WithCode() throws Exception {
        // given
        RfidTagId rfidTagId = givenRegisteredTag();

        // when
        ResultActions result = this.mockMvc.perform(post("/api/v1/rfid-tags") //
                .contentType(MediaType.APPLICATION_JSON) //
                .content("{\"rfidTagId\":\"" + rfidTagId.getValue() + "\"}"));

        // then
        result.andExpect(status().isConflict()) //
                .andExpect(jsonPath("$.code").value(RfidTagAlreadyRegisteredException.CODE));
    }

    // ------------------------------------------------------------------------
    // deleteRfidTag

    @Test
    void deleteRfidTag_returns204() throws Exception {
        // given
        RfidTagId rfidTagId = RandomValues.rfidTagId();

        // when
        ResultActions result = this.mockMvc.perform(delete("/api/v1/rfid-tags/" + rfidTagId.getValue()));

        // then
        result.andExpect(status().isNoContent());
        verify(this.rfidTagDelete).process(rfidTagId);
    }

    @Test
    void deleteRfidTag_assignedTag_is409WithCode() throws Exception {
        // given
        RfidTagId rfidTagId = givenAssignedTag();

        // when
        ResultActions result = this.mockMvc.perform(delete("/api/v1/rfid-tags/" + rfidTagId.getValue()));

        // then
        result.andExpect(status().isConflict()) //
                .andExpect(jsonPath("$.code").value(RfidTagAssignedToCustomerException.CODE));
    }

    // ------------------------------------------------------------------------
    // given

    private void givenTags(RfidTagView... rfidTags) {
        when(this.rfidTagReadFacade.getRfidTags()).thenReturn(List.of(rfidTags));
    }

    private RfidTagView givenTag() {
        RfidTagView rfidTag = rfidTagView().build();
        when(this.rfidTagReadFacade.getRfidTag(rfidTag.id())).thenReturn(Optional.of(rfidTag));

        return rfidTag;
    }

    private RfidTagId givenUnknownTag() {
        RfidTagId unknown = RandomValues.rfidTagId();
        when(this.rfidTagReadFacade.getRfidTag(unknown)).thenReturn(Optional.empty());

        return unknown;
    }

    private RfidTagId givenCreatedTag() {
        RfidTagId rfidTagId = RandomValues.rfidTagId();
        when(this.rfidTagCreate.process(rfidTagId)).thenReturn(rfidTagId);

        return rfidTagId;
    }

    private RfidTagId givenRegisteredTag() {
        RfidTagId rfidTagId = RandomValues.rfidTagId();
        when(this.rfidTagCreate.process(rfidTagId)).thenThrow(new RfidTagAlreadyRegisteredException(rfidTagId));

        return rfidTagId;
    }

    private RfidTagId givenAssignedTag() {
        RfidTagId rfidTagId = RandomValues.rfidTagId();
        doThrow(new RfidTagAssignedToCustomerException(rfidTagId, CustomerId.random())).when(this.rfidTagDelete).process(rfidTagId);

        return rfidTagId;
    }

}

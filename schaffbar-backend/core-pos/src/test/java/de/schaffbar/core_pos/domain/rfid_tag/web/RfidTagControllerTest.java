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
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException.Resource;
import de.schaffbar.core_pos.shared.exception.RfidTagAlreadyRegisteredException;
import de.schaffbar.core_pos.shared.exception.RfidTagAssignedToCustomerException;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.RfidTagId;
import de.schaffbar.core_pos.shared.util.StringToRfidTagIdConverter;
import de.schaffbar.core_pos.shared.web.ApiExceptionHandler;
import de.schaffbar.core_pos.support.Json;
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
                .setMessageConverters(Json.messageConverter()) //
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
        ResultActions result = getRfidTags();

        // then
        result.andExpect(status().isOk()) //
                .andExpect(jsonPath("$.length()").value(1)) //
                .andExpect(jsonPath("$[0].id").value(rfidTag.id().getValue())) //
                .andExpect(jsonPath("$[0].active").value(rfidTag.active()));
    }

    // ------------------------------------------------------------------------
    // getRfidTag

    @Test
    void getRfidTag_returnsTheTag() throws Exception {
        // given
        RfidTagView rfidTag = givenTag();

        // when
        ResultActions result = getRfidTag(rfidTag.id());

        // then
        result.andExpect(status().isOk()) //
                .andExpect(jsonPath("$.id").value(rfidTag.id().getValue())) //
                .andExpect(jsonPath("$.active").value(rfidTag.active()));
    }

    @Test
    void getRfidTag_unknownId_is404() throws Exception {
        // given
        RfidTagId unknown = givenUnknownTag();

        // when
        ResultActions result = getRfidTag(unknown);

        // then
        expectRfidTagNotFound(result);
    }

    // ------------------------------------------------------------------------
    // createRfidTag

    @Test
    void createRfidTag_returns201WithTheLocation() throws Exception {
        // given
        RfidTagId rfidTagId = givenCreatedTag();

        // when
        ResultActions result = createRfidTag(rfidTagId);

        // then
        result.andExpect(status().isCreated()) //
                .andExpect(header().string("Location", "/api/v1/rfid-tags/" + rfidTagId.getValue()));
    }

    @Test
    void createRfidTag_withoutId_is400() throws Exception {
        // when
        ResultActions result = createRfidTag("{}");

        // then
        result.andExpect(status().isBadRequest());
        expectNothingCreated();
    }

    @Test
    void createRfidTag_registeredTag_is409WithTheCode() throws Exception {
        // given
        RfidTagId rfidTagId = RandomValues.rfidTagId();
        givenCreateRejectedWith(rfidTagId, new RfidTagAlreadyRegisteredException(rfidTagId));

        // when
        ResultActions result = createRfidTag(rfidTagId);

        // then
        result.andExpect(status().isConflict()) //
                .andExpect(jsonPath("$.code").value(RfidTagAlreadyRegisteredException.CODE));
    }

    // ------------------------------------------------------------------------
    // deleteRfidTag

    @Test
    void deleteRfidTag_is204() throws Exception {
        // given
        RfidTagId rfidTagId = RandomValues.rfidTagId();

        // when
        ResultActions result = deleteRfidTag(rfidTagId);

        // then
        result.andExpect(status().isNoContent());
        expectDeleted(rfidTagId);
    }

    @Test
    void deleteRfidTag_assignedTag_is409WithTheCode() throws Exception {
        // given
        RfidTagId rfidTagId = RandomValues.rfidTagId();
        givenDeleteRejectedWith(rfidTagId, new RfidTagAssignedToCustomerException(rfidTagId, CustomerId.random()));

        // when
        ResultActions result = deleteRfidTag(rfidTagId);

        // then
        result.andExpect(status().isConflict()) //
                .andExpect(jsonPath("$.code").value(RfidTagAssignedToCustomerException.CODE));
    }

    @Test
    void deleteRfidTag_unknownId_is404() throws Exception {
        // given
        RfidTagId unknown = RandomValues.rfidTagId();
        givenDeleteRejectedWith(unknown, ResourceNotFoundException.rfidTag(unknown));

        // when
        ResultActions result = deleteRfidTag(unknown);

        // then
        expectRfidTagNotFound(result);
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

    private void givenCreateRejectedWith(RfidTagId rfidTagId, RuntimeException rejection) {
        when(this.rfidTagCreate.process(rfidTagId)).thenThrow(rejection);
    }

    private void givenDeleteRejectedWith(RfidTagId rfidTagId, RuntimeException rejection) {
        doThrow(rejection).when(this.rfidTagDelete).process(rfidTagId);
    }

    // ------------------------------------------------------------------------
    // when

    private ResultActions getRfidTags() throws Exception {
        return this.mockMvc.perform(get("/api/v1/rfid-tags"));
    }

    private ResultActions getRfidTag(RfidTagId rfidTagId) throws Exception {
        return this.mockMvc.perform(get("/api/v1/rfid-tags/" + rfidTagId.getValue()));
    }

    private ResultActions createRfidTag(RfidTagId rfidTagId) throws Exception {
        return createRfidTag("{\"rfidTagId\":\"" + rfidTagId.getValue() + "\"}");
    }

    private ResultActions createRfidTag(String body) throws Exception {
        return this.mockMvc.perform(post("/api/v1/rfid-tags") //
                .contentType(MediaType.APPLICATION_JSON) //
                .content(body));
    }

    private ResultActions deleteRfidTag(RfidTagId rfidTagId) throws Exception {
        return this.mockMvc.perform(delete("/api/v1/rfid-tags/" + rfidTagId.getValue()));
    }

    // ------------------------------------------------------------------------
    // then

    private void expectRfidTagNotFound(ResultActions result) throws Exception {
        result.andExpect(status().isNotFound()) //
                .andExpect(jsonPath("$.code").value(ResourceNotFoundException.CODE)) //
                .andExpect(jsonPath("$.resource").value(Resource.RFID_TAG.name()));
    }

    private void expectNothingCreated() {
        verify(this.rfidTagCreate, never()).process(any());
    }

    private void expectDeleted(RfidTagId rfidTagId) {
        verify(this.rfidTagDelete).process(rfidTagId);
    }

}

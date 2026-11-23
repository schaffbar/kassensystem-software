package de.schaffbar.core_pos.shared.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import de.schaffbar.core_pos.domain.ExceptionThrowingTestController;
import de.schaffbar.core_pos.domain.ExceptionThrowingTestController.TestRuleViolationException;
import de.schaffbar.core_pos.shared.id.RfidReaderId;
import de.schaffbar.core_pos.support.Json;

class ApiExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders //
                .standaloneSetup(new ExceptionThrowingTestController(), new OutsideDomainController()) //
                .setMessageConverters(Json.messageConverter()) //
                .setControllerAdvice(new ApiExceptionHandler()) //
                .build();
    }

    @Test
    void resourceNotFound_is404WithCodeAndResource() throws Exception {
        // given
        RfidReaderId rfidReaderId = RfidReaderId.random();

        // when
        ResultActions result = notFound(rfidReaderId);

        // then
        result.andExpect(status().isNotFound()) //
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON)) //
                // the literal on purpose: the portal translates exactly this code (shared.errors.RESOURCE_NOT_FOUND)
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND")) //
                .andExpect(jsonPath("$.resource").value("RFID_READER")) //
                .andExpect(jsonPath("$.detail").value(containsString(rfidReaderId.getValue().toString())));
    }

    @Test
    void businessRuleViolation_is409WithCodeOfTheException() throws Exception {
        // when
        ResultActions result = ruleViolation();

        // then
        result.andExpect(status().isConflict()) //
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON)) //
                .andExpect(jsonPath("$.code").value(TestRuleViolationException.CODE)) //
                .andExpect(jsonPath("$.detail").value("Test rule violated"));
    }

    @Test
    void constraintViolation_is400WithoutCode() throws Exception {
        // when
        ResultActions result = constraintViolation();

        // then
        result.andExpect(status().isBadRequest()) //
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON)) //
                .andExpect(jsonPath("$.code").doesNotExist());
    }

    @Test
    void illegalArgument_is400() throws Exception {
        // when
        ResultActions result = illegalArgument();

        // then
        result.andExpect(status().isBadRequest()) //
                .andExpect(jsonPath("$.detail").value("Invalid MAC address"));
    }

    @Test
    void invalidRequestBody_is400ProblemDetail() throws Exception {
        // when
        ResultActions result = body("{\"name\":\"\"}");

        // then
        result.andExpect(status().isBadRequest()) //
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void unexpectedException_is500WithoutDetailsOfTheException() throws Exception {
        // when
        ResultActions result = unexpected();

        // then
        result.andExpect(status().isInternalServerError()) //
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON)) //
                .andExpect(jsonPath("$.detail").value("Unexpected error"));
    }

    @Test
    void controllerOutsideDomain_isNotAdvised() {
        // when
        Throwable thrown = catchThrowable(this::outside);

        // then
        assertThat(thrown) //
                .isInstanceOf(ServletException.class) //
                .hasCauseInstanceOf(TestRuleViolationException.class);
    }

    // ------------------------------------------------------------------------
    // when

    private ResultActions notFound(RfidReaderId rfidReaderId) throws Exception {
        return this.mockMvc.perform(get("/test/not-found/" + rfidReaderId.getValue()));
    }

    private ResultActions ruleViolation() throws Exception {
        return this.mockMvc.perform(get("/test/rule-violation"));
    }

    private ResultActions constraintViolation() throws Exception {
        return this.mockMvc.perform(get("/test/constraint-violation"));
    }

    private ResultActions illegalArgument() throws Exception {
        return this.mockMvc.perform(get("/test/illegal-argument"));
    }

    private ResultActions body(String body) throws Exception {
        return this.mockMvc.perform(post("/test/body") //
                .contentType(MediaType.APPLICATION_JSON) //
                .content(body));
    }

    private ResultActions unexpected() throws Exception {
        return this.mockMvc.perform(get("/test/unexpected"));
    }

    private ResultActions outside() throws Exception {
        return this.mockMvc.perform(get("/outside"));
    }

    // ------------------------------------------------------------------------
    // helper

    // like device/: answers in its own format, the advice must not touch it
    @RestController
    static class OutsideDomainController {

        @GetMapping("/outside")
        void outside() {
            throw new TestRuleViolationException();
        }

    }

}

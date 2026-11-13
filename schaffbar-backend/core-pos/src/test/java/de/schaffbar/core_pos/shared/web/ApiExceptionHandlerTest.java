package de.schaffbar.core_pos.shared.web;

import static de.schaffbar.core_pos.domain.ExceptionThrowingTestController.RFID_READER_ID;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import de.schaffbar.core_pos.domain.ExceptionThrowingTestController;
import de.schaffbar.core_pos.domain.ExceptionThrowingTestController.TestRuleViolationException;
import jakarta.servlet.ServletException;

class ApiExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders //
                .standaloneSetup(new ExceptionThrowingTestController(), new OutsideDomainController()) //
                .setControllerAdvice(new ApiExceptionHandler()) //
                .build();
    }

    @Test
    void resourceNotFound_is404WithCodeAndResource() throws Exception {
        this.mockMvc.perform(get("/test/not-found")) //
                .andExpect(status().isNotFound()) //
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON)) //
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND")) //
                .andExpect(jsonPath("$.resource").value("RFID_READER")) //
                .andExpect(jsonPath("$.detail").value(containsString(RFID_READER_ID.getValue().toString())));
    }

    @Test
    void businessRuleViolation_is409WithCodeOfTheException() throws Exception {
        this.mockMvc.perform(get("/test/rule-violation")) //
                .andExpect(status().isConflict()) //
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON)) //
                .andExpect(jsonPath("$.code").value(TestRuleViolationException.CODE)) //
                .andExpect(jsonPath("$.detail").value("Test rule violated"));
    }

    @Test
    void constraintViolation_is400WithoutCode() throws Exception {
        this.mockMvc.perform(get("/test/constraint-violation")) //
                .andExpect(status().isBadRequest()) //
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON)) //
                .andExpect(jsonPath("$.code").doesNotExist());
    }

    @Test
    void illegalArgument_is400() throws Exception {
        this.mockMvc.perform(get("/test/illegal-argument")) //
                .andExpect(status().isBadRequest()) //
                .andExpect(jsonPath("$.detail").value("Invalid MAC address"));
    }

    @Test
    void invalidRequestBody_is400ProblemDetail() throws Exception {
        this.mockMvc.perform(post("/test/body").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}")) //
                .andExpect(status().isBadRequest()) //
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void unexpectedException_is500WithoutDetailsOfTheException() throws Exception {
        this.mockMvc.perform(get("/test/unexpected")) //
                .andExpect(status().isInternalServerError()) //
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON)) //
                .andExpect(jsonPath("$.detail").value("Unexpected error"));
    }

    @Test
    void controllerOutsideDomain_isNotAdvised() {
        assertThatThrownBy(() -> this.mockMvc.perform(get("/outside"))) //
                .isInstanceOf(ServletException.class) //
                .hasCauseInstanceOf(TestRuleViolationException.class);
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

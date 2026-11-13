package de.schaffbar.core_pos.domain;

import java.util.Set;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.schaffbar.core_pos.shared.exception.BusinessRuleViolationException;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;
import de.schaffbar.core_pos.shared.id.RfidReaderId;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

// lives in domain/ because ApiExceptionHandler only advises the controllers there (see ApiExceptionHandlerTest)
@RestController
@RequestMapping("/test")
public class ExceptionThrowingTestController {

    public static final RfidReaderId RFID_READER_ID = RfidReaderId.random();

    @GetMapping("/not-found")
    void notFound() {
        throw ResourceNotFoundException.rfidReader(RFID_READER_ID);
    }

    @GetMapping("/rule-violation")
    void ruleViolation() {
        throw new TestRuleViolationException();
    }

    @GetMapping("/constraint-violation")
    void constraintViolation() {
        throw new ConstraintViolationException("getRfidReader.rfidReaderId: must not be null", Set.of());
    }

    @GetMapping("/illegal-argument")
    void illegalArgument() {
        throw new IllegalArgumentException("Invalid MAC address");
    }

    @PostMapping("/body")
    void body(@RequestBody @Valid TestRequestBody body) {
    }

    @GetMapping("/unexpected")
    void unexpected() {
        throw new IllegalStateException("More than one open session found");
    }

    // ------------------------------------------------------------------------
    // helper

    public record TestRequestBody(@NotBlank String name) {
    }

    public static class TestRuleViolationException extends BusinessRuleViolationException {

        public static final String CODE = "TEST_RULE_VIOLATED";

        public TestRuleViolationException() {
            super(CODE, "Test rule violated");
        }

    }

}

package de.schaffbar.core_pos.shared.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import de.schaffbar.core_pos.shared.exception.BusinessRuleViolationException;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

/**
 * Turns exceptions into RFC 9457 problem details. Only {@code 404} and {@code 409} carry a {@code code} – those are
 * the answers the portal explains to the user; the standard Spring MVC errors ({@code 400}, {@code 405}, …) come from
 * {@link ResponseEntityExceptionHandler}.
 */
@Slf4j
// only the portal API: the device endpoints (device/) answer the firmware in their own format
@RestControllerAdvice(basePackages = "de.schaffbar.core_pos.domain")
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    static final String CODE = "code";

    @ExceptionHandler(ResourceNotFoundException.class)
    ProblemDetail handleResourceNotFound(ResourceNotFoundException exception) {
        log.info(exception.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setProperty(CODE, "RESOURCE_NOT_FOUND");
        problem.setProperty("resource", exception.getResource());
        return problem;
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    ProblemDetail handleBusinessRuleViolation(BusinessRuleViolationException exception) {
        log.info(exception.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setProperty(CODE, exception.getCode());
        return problem;
    }

    @ExceptionHandler({ ConstraintViolationException.class, IllegalArgumentException.class })
    ProblemDetail handleInvalidInput(RuntimeException exception) {
        log.info(exception.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    // a bug or broken data: logged with the stack trace, the details stay in the log
    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception exception) {
        log.error("Unexpected error", exception);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error");
    }

}

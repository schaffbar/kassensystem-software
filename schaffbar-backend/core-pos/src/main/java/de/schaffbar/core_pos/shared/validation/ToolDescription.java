package de.schaffbar.core_pos.shared.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraints.Size;

// a tool description has at most 255 characters, the size of its column (T-16) – null is valid
@Documented
@Size(max = 255)
@ReportAsSingleViolation
@Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface ToolDescription {

    String message() default "must have at most 255 characters";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}

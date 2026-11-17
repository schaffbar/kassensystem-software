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

// a postal code has exactly 5 characters (C-7) – null is valid, combine with @NotBlank
@Documented
@Size(min = 5, max = 5)
@ReportAsSingleViolation
@Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface PostalCode {

    String message() default "must have exactly 5 characters";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}

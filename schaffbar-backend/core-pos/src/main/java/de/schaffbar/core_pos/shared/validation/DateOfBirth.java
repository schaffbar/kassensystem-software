package de.schaffbar.core_pos.shared.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

// a person is at most 100 years old and was born before the current year (C-8) – null is valid, combine with @NotNull
@Documented
@Constraint(validatedBy = DateOfBirthValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface DateOfBirth {

    String message() default "must lie within the last 100 years and before the current year";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}

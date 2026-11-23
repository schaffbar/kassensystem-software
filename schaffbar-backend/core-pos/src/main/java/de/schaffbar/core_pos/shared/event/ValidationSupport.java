package de.schaffbar.core_pos.shared.event;

import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ValidationException;

public interface ValidationSupport {

    default void validate() {
        Set<ConstraintViolation<ValidationSupport>> violations = DefaultValidator.INSTANCE.validate(this);
        if (!violations.isEmpty()) {
            throw new ValidationException(violations.toString());
        }
    }

}

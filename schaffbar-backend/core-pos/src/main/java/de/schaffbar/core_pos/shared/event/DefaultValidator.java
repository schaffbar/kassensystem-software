package de.schaffbar.core_pos.shared.event;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

final class DefaultValidator {

    // built once: a ValidatorFactory is expensive and keeps its resources until it is closed
    static final Validator INSTANCE = Validation.buildDefaultValidatorFactory().getValidator();

    private DefaultValidator() {}

}

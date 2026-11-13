package de.schaffbar.core_pos.shared.exception;

import java.io.Serial;

import lombok.Getter;

/**
 * A request that is valid in itself but not allowed in the current state – answered with {@code 409}.
 */
public abstract class BusinessRuleViolationException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    @Getter
    private final String code;

    protected BusinessRuleViolationException(String code, String message) {
        super(message);
        this.code = code;
    }

}

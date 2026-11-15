package de.schaffbar.core_pos.shared.id;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.isBlank;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

public class ValueObjectAssert {

    static final String NULL_ERROR_MESSAGE = "The value of %s must not be null";

    static final String BLANK_ERROR_MESSAGE = "The value of %s must not be blank";

    static final String POSITIVE_ERROR_MESSAGE = "The value of %s must be positive";

    public static void notNull(Object value, ValueObjectName valueObjectName) {
        if (isNull(value)) {
            throw new IllegalArgumentException(notNullMessage(valueObjectName));
        }
    }

    public static void notBlank(String value, ValueObjectName valueObjectName) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(notBlankMessage(valueObjectName));
        }
    }

    public static void positive(long value, ValueObjectName valueObjectName) {
        if (value <= 0) {
            throw new IllegalArgumentException(positiveMessage(valueObjectName));
        }
    }

    public static void positive(int value, ValueObjectName valueObjectName) {
        if (value <= 0) {
            throw new IllegalArgumentException(positiveMessage(valueObjectName));
        }
    }

    // ------------------------------------------------------------------------
    // helper

    static String notNullMessage(ValueObjectName valueObjectName) {
        return String.format(NULL_ERROR_MESSAGE, valueObjectName.getValue());
    }

    static String notBlankMessage(ValueObjectName valueObjectName) {
        return String.format(BLANK_ERROR_MESSAGE, valueObjectName.getValue());
    }

    static String positiveMessage(ValueObjectName valueObjectName) {
        return String.format(POSITIVE_ERROR_MESSAGE, valueObjectName.getValue());
    }

    // ------------------------------------------------------------------------
    // enum

    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public enum ValueObjectName {

        CUSTOMER_ID("customer id"), //
        TOOL_ID("tool id"), //
        TOOL_USAGE_ID("tool usage id"), //
        RFID_READER_ID("rfid reader id"), //
        MAC_ADDRESS("mac address"), //
        RFID_TAG_ID("rfid tag id"), //
        RFID_TAG_ASSIGNMENT_ID("rfid tag assignment id"), //
        TOOL_CERTIFICATION_ID("tool certification id"), //
        WORKSHOP_SESSION_ID("workshop session id"), //
        WORKSHOP_USAGE_ID("workshop usage id"), //
        IP_ADDRESS("ip address");

        private final String value;

    }

}

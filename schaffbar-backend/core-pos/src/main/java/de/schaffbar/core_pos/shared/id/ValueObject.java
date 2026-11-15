package de.schaffbar.core_pos.shared.id;

import static java.util.Objects.nonNull;

// T is the value object itself, so toolId.sameValueAs(customerId) does not compile
public interface ValueObject<T extends ValueObject<T>> {

    Object getValue();

    default boolean sameValueAs(T other) {
        return nonNull(other) && getValue().equals(other.getValue());
    }

}

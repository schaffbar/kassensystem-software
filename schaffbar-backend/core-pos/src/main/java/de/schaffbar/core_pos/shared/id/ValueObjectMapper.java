package de.schaffbar.core_pos.shared.id;

import static java.util.Objects.isNull;

public interface ValueObjectMapper {

    // responses carry the value as String – a single field maps "id.value", a collection (Set<CustomerId>) needs this
    default String toValue(ValueObject<?> valueObject) {
        return isNull(valueObject) ? null : valueObject.getValue().toString();
    }

}

package de.schaffbar.core_pos.shared.util;

import java.util.UUID;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import de.schaffbar.core_pos.shared.id.CustomerId;

@Component
public class StringToCustomerIdConverter implements Converter<String, CustomerId> {

    @Override
    public CustomerId convert(String source) {
        return CustomerId.of(UUID.fromString(source));
    }

}

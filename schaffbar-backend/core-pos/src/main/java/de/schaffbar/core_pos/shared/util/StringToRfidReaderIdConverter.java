package de.schaffbar.core_pos.shared.util;

import java.util.UUID;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import de.schaffbar.core_pos.shared.id.RfidReaderId;

@Component
public class StringToRfidReaderIdConverter implements Converter<String, RfidReaderId> {

    @Override
    public RfidReaderId convert(String source) {
        return RfidReaderId.of(UUID.fromString(source));
    }

}

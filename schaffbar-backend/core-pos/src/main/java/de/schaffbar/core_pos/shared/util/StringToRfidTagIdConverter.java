package de.schaffbar.core_pos.shared.util;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import de.schaffbar.core_pos.shared.id.RfidTagId;

@Component
public class StringToRfidTagIdConverter implements Converter<String, RfidTagId> {

    @Override
    public RfidTagId convert(String source) {
        return RfidTagId.of(source);
    }

}

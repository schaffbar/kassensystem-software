package de.schaffbar.core_pos.shared.util;

import java.util.UUID;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import de.schaffbar.core_pos.shared.id.ToolCertificationId;

@Component
public class StringToToolCertificationIdConverter implements Converter<String, ToolCertificationId> {

    @Override
    public ToolCertificationId convert(String source) {
        return ToolCertificationId.of(UUID.fromString(source));
    }

}

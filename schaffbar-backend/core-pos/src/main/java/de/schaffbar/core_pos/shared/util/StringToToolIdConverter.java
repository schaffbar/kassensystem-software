package de.schaffbar.core_pos.shared.util;

import java.util.UUID;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import de.schaffbar.core_pos.shared.id.ToolId;

@Component
public class StringToToolIdConverter implements Converter<String, ToolId> {

    @Override
    public ToolId convert(String source) {
        return ToolId.of(UUID.fromString(source));
    }

}

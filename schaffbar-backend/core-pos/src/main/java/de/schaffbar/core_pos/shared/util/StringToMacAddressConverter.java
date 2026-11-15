package de.schaffbar.core_pos.shared.util;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import de.schaffbar.core_pos.shared.id.MacAddress;

@Component
public class StringToMacAddressConverter implements Converter<String, MacAddress> {

    @Override
    public MacAddress convert(String source) {
        return MacAddress.of(source);
    }

}

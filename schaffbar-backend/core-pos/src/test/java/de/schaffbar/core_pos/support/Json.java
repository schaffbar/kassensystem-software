package de.schaffbar.core_pos.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Json {

    private static final ObjectMapper MAPPER = new ObjectMapper() //
            .registerModule(new JavaTimeModule()) //
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public static String write(Object body) {
        try {
            return MAPPER.writeValueAsString(body);
        }
        catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

}

package de.schaffbar.core_pos.support;

import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jayway.jsonpath.JsonPath;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Json {

    private static final ObjectMapper MAPPER = new ObjectMapper() //
            .registerModule(new JavaTimeModule()) //
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // a field of a JSON response as text, for values a test builds its expectation from
    public static String read(MvcTestResult result, String path) {
        String body = new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);

        return JsonPath.read(body, path).toString();
    }

    public static String write(Object body) {
        try {
            return MAPPER.writeValueAsString(body);
        }
        catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

}

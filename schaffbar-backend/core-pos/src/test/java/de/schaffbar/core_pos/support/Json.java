package de.schaffbar.core_pos.support;

import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jayway.jsonpath.JsonPath;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Json {

    // as Boot configures it (JacksonAutoConfiguration, application.yml sets no spring.jackson.*): the builder's
    // defaults – Java time module, unknown fields ignored – and dates as ISO strings, not timestamps
    private static final ObjectMapper MAPPER = Jackson2ObjectMapperBuilder.json() //
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS) //
            .build();

    // for a standalone MockMvc, so a controller test sees the JSON of the running backend
    public static MappingJackson2HttpMessageConverter messageConverter() {
        return new MappingJackson2HttpMessageConverter(MAPPER);
    }

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

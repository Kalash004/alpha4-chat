package core.util;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;

public class JsonUtil {
    private static ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.setSerializationInclusion(Include.NON_NULL);
        MAPPER.configure(SerializationFeature.INDENT_OUTPUT, false);
        MAPPER.configure(SerializationFeature.WRAP_EXCEPTIONS, true);
        MAPPER.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        MAPPER.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
        MAPPER.configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true);
        MAPPER.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        MAPPER.configure(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS, true);
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        MAPPER.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
        MAPPER.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
    }

    public static String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JacksonException e) {
            throw new RuntimeException(String.format("Unable to map %s to json", value.getClass()), e);
        }
    }

    public static <T> T fromJson(byte[] json, Class<T> type) {
        if (json == null) {
            return null;
        }
        try {
            return MAPPER.readValue(json, type);
        } catch (JacksonException e) {
            throw new RuntimeException(String.format("Unable to map json %s to %s", json, type), e);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Unable to map json %s to %s", json, type), e);
        }
    }

    public static <T> T fromJson(String json, Class<T> type) {
        if (json == null) {
            return null;
        }
        try {
            return MAPPER.readValue(json, type);
        } catch (JacksonException e) {
            throw new RuntimeException(String.format("Unable to map json %s to %s", json, type), e);
        }
    }

    public static JsonNode fromJson(String json) {
        if (json == null) {
            return null;
        }
        try {
            return MAPPER.readTree(json);
        } catch (JacksonException e) {
            throw new RuntimeException(String.format("Unable to read json tree %s", json), e);
        }
    }
}

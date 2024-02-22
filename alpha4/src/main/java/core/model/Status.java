package core.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Message status enum.
 */
public enum Status {

    /**
     * Successful response status.
     */
    OK("ok"),
    /**
     * Error status.
     */
    ERROR("error"),
    /**
     * Any other (we don't know) status.
     */
    UNKNOWN("unknown");

    /**
     * Used by JSON mapper to generate string from enum.
     */
    @JsonValue
    private String value;

    Status(String value) {
        this.value = value;
    }

    /**
     * Used by JSON mapper to generate enum from string.
     *
     * @param value
     *            command string representation
     *
     * @return enum value
     */
    @JsonCreator
    public static Status fromString(String value) {
        if (value == null) {
            return null;
        }
        for (Status item : values()) {
            if (item.value.equals(value)) {
                return item;
            }
        }
        return UNKNOWN;
    }

    public String value() {
        return value;
    }

}

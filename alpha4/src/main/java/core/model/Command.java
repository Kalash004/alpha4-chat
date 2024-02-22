package core.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Peer communication commands.
 */
public enum Command {

    /**
     * Peer handshake command.
     */
    HELLO("hello"),
    /**
     * New message command.
     */
    NEW_MESSAGE("new_message"),
    /**
     * Any other (we don't know) command.
     */
    UNKNOWN("unknown");

    /**
     * Used by JSON mapper to generate string from enum.
     */
    @JsonValue
    private String value;

    Command(String value) {
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
    public static Command fromString(String value) {
        if (value == null) {
            return null;
        }
        for (Command item : values()) {
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

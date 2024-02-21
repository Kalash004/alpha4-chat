package a.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Command {

    HELLO("hello"),
    NEW_MESSAGE("new_message"),
    UNKNOWN("unknown");

    @JsonValue
    private String value;

    Command(String value) {
        this.value = value;
    }

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

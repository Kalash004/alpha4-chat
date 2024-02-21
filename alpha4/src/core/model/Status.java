package core.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Status {

    OK("ok"),
    ERROR("error"),
    UNKNOWN("unknown");

    @JsonValue
    private String value;

    Status(String value) {
        this.value = value;
    }

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

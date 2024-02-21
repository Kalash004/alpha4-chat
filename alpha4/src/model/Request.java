package a.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Request(Command command, @JsonProperty("peer_id") String peerId,
        @JsonProperty("message_id") Integer messageId, String message) {
}

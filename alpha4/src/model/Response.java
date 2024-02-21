package a.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public record Response(Status status, Map<Integer, Message> messages, String message,
        @JsonProperty("peer_id") String peerId) {
}

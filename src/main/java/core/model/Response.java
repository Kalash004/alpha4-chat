package core.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Messaging response record.
 */
public record Response(Status status, Map<Long, Message> messages, String message,
        @JsonProperty("peer_id") String peerId) {
}

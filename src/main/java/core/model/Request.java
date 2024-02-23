package core.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Messaging request record.
 */
public record Request(Command command, @JsonProperty("peer_id") String peerId,
        @JsonProperty("message_id") Integer messageId, String message) {
}

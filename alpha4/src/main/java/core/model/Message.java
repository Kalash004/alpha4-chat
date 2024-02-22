package core.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Communication message record.
 */
public record Message(@JsonProperty("peer_id") String peerId, String message) {
}

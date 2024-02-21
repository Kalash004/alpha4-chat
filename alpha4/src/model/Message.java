package a.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Message(@JsonProperty("peer_id") String peerId, String message) {

}

package core;

import java.util.Date;

public record Peer(String peerId, String ipAddress, int port, Date lastCheckinDate) {

}

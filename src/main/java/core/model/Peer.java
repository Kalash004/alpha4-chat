package core.model;

import java.util.Date;

/**
 * Peer details record.
 */
public record Peer(String peerId, String ipAddress, int port, Date lastCheckinDate) {

    /**
     * Override hash code to follow equals and hash code contract.
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((peerId() == null) ? 0 : peerId().hashCode())
                + ((ipAddress() == null) ? 0 : ipAddress.hashCode()) + port;
        return result;
    }

    /**
     * Override equals to compare only peer Id, IP address and port.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || !(other instanceof Peer)) {
            return false;
        }

        Peer peer = (Peer) other;
        return compare(peerId(), peer.peerId()) && compare(ipAddress(), peer.ipAddress()) && port() == peer.port();
    }

    private boolean compare(String arg1, String arg2) {
        if (arg1 == null && arg2 != null || arg1 != null && arg2 == null) {
            return false;
        } else if (arg1 == null && arg2 == null) {
            return true;
        }
        return arg1.equals(arg2);
    }
}

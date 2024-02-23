package core.manager;

import core.model.Peer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Holds known peers and provides API to add new peer, retrieve peers and automatically removes potential offline peers.
 */
public class PeerManager {
    private static final Logger log = LoggerFactory.getLogger(PeerManager.class);

    private final Set<Peer> peers;
    private final int expirationDurationMs;

    /**
     * @param expirationTimeoutMs
     *            timeout in milliseconds after which peer will be treated as offline
     */
    public PeerManager(int expirationTimeoutMs) {
        this.peers = Collections.synchronizedSet(new HashSet<>());
        this.expirationDurationMs = expirationTimeoutMs;
    }

    /**
     * Add peer (existing peer with the same Id and IP address will be updated).
     *
     * @param peerId
     *            peer Id
     * @param ipAddress
     *            IP address
     * @param port
     *            peer port
     */
    public void addPeer(String peerId, String ipAddress, int port) {
        peers.add(new Peer(peerId, ipAddress, port, new Date()));
    }

    /**
     * Returns known peers.
     *
     * @return peers collection
     */
    public Collection<Peer> getPeers() {
        cleanupPeers();
        return peers;
    }

    /**
     * Remove "offline" peers from the known peers list.
     */
    private void cleanupPeers() {
        for (Iterator<Peer> it = peers.iterator(); it.hasNext();) {
            Peer peer = it.next();
            long duration = new Date().getTime() - peer.lastCheckinDate().getTime();
            long lifeTime = TimeUnit.MILLISECONDS.convert(duration, TimeUnit.MILLISECONDS);
            if (lifeTime > expirationDurationMs) {
                log.info("Removing expired ({}) peer {} from {}:{}", peer.lastCheckinDate(), peer.peerId(),
                        peer.ipAddress(), peer.port());
                it.remove();
            }
        }
    }
}

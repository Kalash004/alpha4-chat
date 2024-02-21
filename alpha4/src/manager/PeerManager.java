package manager;

import a.Peer;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PeerManager {

    private Map<String, Peer> peerMap = new ConcurrentHashMap<>();

    public void addPeer(String peerId, String ipAddress, int port) {
        peerMap.put(peerId, new Peer(peerId, ipAddress, port, new Date()));
    }

    public Collection<Peer> getPeers() {
        return peerMap.values();
    }
}

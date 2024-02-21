package core;

import core.manager.MessagesManager;
import core.manager.PeerManager;
import core.model.Command;
import core.model.Message;
import core.model.Request;
import core.model.Response;
import core.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class MessageLocator extends Thread implements Config {
    private static final Logger log = LoggerFactory.getLogger(MessageLocator.class);
    private String peerId;
    private MessagesManager msgMgr;
    private PeerManager peerMgr;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public MessageLocator(String peerId, MessagesManager msgMgr, PeerManager peerMgr) {
        this.peerId = peerId;
        this.msgMgr = msgMgr;
        this.peerMgr = peerMgr;
    }

    @Override
    public void run() {
        String cmd = JsonUtil.toJson(new Request(Command.HELLO, peerId, null, null));

        while (true) {
            List<Future<?>> futures = new ArrayList<>();
            for (Iterator<Peer> it = peerMgr.getPeers().iterator(); it.hasNext();) {
                Peer peer = it.next();

                long lifeTime = TimeUnit.MILLISECONDS.convert(new Date().getTime() - peer.lastCheckinDate().getTime(),
                        TimeUnit.MILLISECONDS);
                if (lifeTime > Config.BROADCAST_TIMEOUT_MS * 3) {
                    log.info("Removing outdated ({}) peer {} from {}:{}", peer.lastCheckinDate(), peer.peerId(),
                            peer.ipAddress(), peer.port());
                    it.remove();
                }

                Future<?> future = executor.submit(() -> {
                    try (Socket socket = new Socket(peer.ipAddress(), peer.port());
                            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));) {
                        out.println(cmd);
                        out.println();
                        out.println();
                        String input = readInput(in);
                        log.debug("Received message {} from peer {} {}:{}", input, peer.peerId(), peer.ipAddress(),
                                peer.port());
                        Response response = JsonUtil.fromJson(input, Response.class);
                        if (response != null && response.messages() != null) {
                            for (Entry<Integer, Message> entry : response.messages().entrySet()) {
                                Integer id = entry.getKey();
                                Message message = entry.getValue();
                                log.debug("Saving message {} {} from peer {}", id, message.message(), message.peerId());
                                msgMgr.addMessage(id, message.peerId(), message.message());
                            }
                        }
                    } catch (IOException e) {
                        String msg = String.format("Unable to send command %s to the peer %s %s:%s", cmd, peer.peerId(),
                                peer.ipAddress(), peer.port());
                        log.error(msg, e);
                        throw new RuntimeException(msg, e);
                    }
                });
                futures.add(future);
            }
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (InterruptedException | ExecutionException e) {
                    // do nothing
                }
            }
            try {
                Thread.sleep(Config.BROADCAST_TIMEOUT_MS);
            } catch (InterruptedException e) {
                // do nothing
            }
        }
    }
}

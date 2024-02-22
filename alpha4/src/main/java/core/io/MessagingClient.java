package core.io;

import core.manager.MessagesManager;
import core.manager.PeerManager;
import core.model.Command;
import core.model.Message;
import core.model.Peer;
import core.model.Request;
import core.model.Response;
import core.util.Config;
import core.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Performing message exchange with known peers by sending stored messages via peer TCP port {@link Peer#port()}.
 */
public class MessagingClient implements Runnable, Config {

    private static final Logger log = LoggerFactory.getLogger(MessagingClient.class);

    private final MessagesManager msgMgr;
    private final PeerManager peerMgr;

    private final String commandString;
    private final int timeout;
    private boolean stop;

    private final ExecutorService executor;

    /**
     * @param peerId
     *            peer id
     * @param timeout
     *            socket timeout
     * @param msgMgr
     *            messaging manager {@see MessagesManager}
     * @param peerMgr
     *            peer manager {@see PeerManager}
     */
    public MessagingClient(int timeout, String peerId, MessagesManager msgMgr, PeerManager peerMgr) {
        this.msgMgr = msgMgr;
        this.timeout = timeout;
        this.peerMgr = peerMgr;
        this.commandString = JsonUtil.toJson(new Request(Command.HELLO, peerId, null, null));
        this.executor = Executors.newCachedThreadPool();
    }

    @Override
    public void run() {
        while (!stop) {
            List<Future<?>> futures = new ArrayList<>();
            for (Peer peer : peerMgr.getPeers()) {
                String peerId = peer.peerId();
                String ipAddress = peer.ipAddress();
                int port = peer.port();

                Future<?> future = executor.submit(() -> {
                    try (Socket socket = new Socket(ipAddress, port);
                            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));) {
                        // send message to the peer
                        out.println(commandString);
                        out.println();
                        log.debug("Sent message {} to peer {} {}:{}", escapeQuotes(commandString), peerId, ipAddress,
                                port);
                        String input = readInput(in);
                        log.debug("Received message {} from peer {} {}:{}", escapeQuotes(input), peerId, ipAddress,
                                port);
                        Response response = JsonUtil.fromJson(input, Response.class);
                        if (response != null && response.messages() != null) {
                            for (Entry<Long, Message> entry : response.messages().entrySet()) {
                                Long id = entry.getKey();
                                Message message = entry.getValue();
                                log.debug("Saving message {} {} from peer {}", id, message.message(), message.peerId());
                                msgMgr.addMessage(id, message.peerId(), message.message());
                            }
                        }
                    } catch (IOException e) {
                        String msg = String.format("Failed to send command %s to the peer %s %s:%s",
                                escapeQuotes(commandString), peerId, ipAddress, port);
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
                Thread.sleep(timeout);
            } catch (InterruptedException e) {
                // do nothing
            }
        }
    }

    /**
     * Stop messaging client.
     */
    public void stop() {
        stop = true;
    }
}

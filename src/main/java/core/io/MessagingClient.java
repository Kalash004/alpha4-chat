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
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Performing message exchange with known peers by sending stored messages via peer TCP port {@link Peer#port()}.
 */
public class MessagingClient implements Runnable, Config {

    private static final Logger log = LoggerFactory.getLogger(MessagingClient.class);

    private static final int SOCKET_TIMEOUT_MS = 1000;
    private static final int EXCHANGE_TIMEOUT_MS = 5000;

    private final MessagesManager msgMgr;
    private final PeerManager peerMgr;

    private final String commandString;
    private final int timeout;
    private boolean stop;

    private final ExecutorService executor;

    private final String peerId;

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
        this.peerId = peerId;
        this.peerMgr = peerMgr;
        this.commandString = JsonUtil.toJson(new Request(Command.HELLO, peerId, null, null));
        this.executor = Executors.newCachedThreadPool();
    }

    @Override
    public void run() {
        while (!stop) {
            List<Future<?>> futures = new ArrayList<>();
            Collection<Peer> peers = peerMgr.getPeers();
            Map<Long, Message> newMsgs = msgMgr.getNewMessages();
            log.debug("Executing messaging exchange for {} peers", peers.size());
            for (Peer peer : peers) {
                String peerId = peer.peerId();
                String ipAddress = peer.ipAddress();
                int port = peer.port();
                Future<?> future = executor.submit(() -> {
                    log.debug("Executing messaging receive for peer {} {}:{}", peerId, ipAddress, port);
                    // handshake
                    Socket socket = null;
                    PrintWriter out = null;
                    BufferedReader in = null;
                    try {
                        socket = new Socket();
                        socket.connect(new InetSocketAddress(ipAddress, port), SOCKET_TIMEOUT_MS);
                        out = new PrintWriter(socket.getOutputStream(), true);
                        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                        log.debug("Sent message {} to peer {} {}:{}", commandString, peerId, ipAddress, port);
                        // send message to the peer
                        out.println(commandString);
                        out.flush();
                        log.debug("Sent message {} to peer {} {}:{}", commandString, peerId, ipAddress, port);
                        String input = readInput(in);
                        log.debug("Received message {} from peer {} {}:{}", input, peerId, ipAddress, port);
                        Response response = JsonUtil.fromJson(input, Response.class);
                        Set<Long> messageIds = new HashSet<>();
                        if (response != null && response.messages() != null) {
                            for (Entry<Long, Message> entry : response.messages().entrySet()) {
                                Long id = entry.getKey();
                                messageIds.add(id);
                                Message message = entry.getValue();
                                log.debug("Saving message {} {} from peer {}", id, message.message(), message.peerId());
                                msgMgr.addMessage(id, message.peerId(), message.message());
                            }
                        }
                        // do not send messages peer has already
                        Map<Long, Message> msgsToSend = newMsgs.entrySet().stream()
                                // filter messages without id
                                .filter(entry -> entry.getKey() != null)
                                // filter messages without message
                                .filter(entry -> entry.getValue() != null)
                                // filter messages from the peer
                                .filter(entry -> !peerId.equals(entry.getValue().peerId()))
                                // filter messages user has already
                                .filter(entry -> !messageIds.contains(entry.getKey()))
                                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                        log.debug("Sending new {} messages to peer {} {}:{}", msgsToSend.size(), peerId, ipAddress,
                                port);
                        PrintWriter outFinal = out;
                        BufferedReader inFinal = in;
                        msgsToSend.entrySet().stream().forEach(entry -> sendMessage(outFinal, inFinal, peerId,
                                ipAddress, port, entry.getKey(), entry.getValue().message()));
                        log.debug("Sent new {} messages to peer {} {}:{}", msgsToSend.size(), peerId, ipAddress, port);
                    } catch (Exception e) {
                        log.error("Failed to perform message exchange with peer {} {}:{}", peerId, ipAddress, port, e);
                    } finally {
                        try {
                            socket.close();
                        } catch (Exception e) {
                        }
                        try {
                            out.close();
                        } catch (Exception e) {
                        }
                        try {
                            in.close();
                        } catch (Exception e) {
                        }
                    }
                });
                futures.add(future);
            }
            for (Future<?> future : futures) {
                try {
                    future.get(EXCHANGE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                } catch (Exception e) {
                    log.warn("Future failed to complete", e);
                }
            }
            futures.clear();
            // clear all new messages
            msgMgr.clearNewMessages();
            log.debug("Executing messaging exchange waits {}ms", timeout);
            try {
                Thread.sleep(timeout);
            } catch (InterruptedException e) {
                // do nothing
            }
        }
    }

    /**
     * Send new message to the peer.
     *
     * @param out
     *            output stream
     * @param peerId
     *            peer Id
     * @param ipAddress
     *            IP address
     * @param port
     *            port
     * @param messages
     *            messages
     */
    private void sendMessage(PrintWriter out, BufferedReader in, String peerId, String ipAddress, int port,
            Long messageId, String message) {
        log.debug("Sending new message to peer {}  {}:{}, Message id {}", peerId, ipAddress, port, messageId);
        String requestString = null;
        requestString = JsonUtil.toJson(new Request(Command.NEW_MESSAGE, this.peerId, messageId, message));
        out.println(requestString);
        out.flush();
        try {
            String inputString = readInput(in);
            if (inputString == null) {
                log.warn("Failed to get sent message confirmation to peer {} {}:{}, message {}", peerId, ipAddress,
                        port, requestString);
            } else {
                log.debug("Received sent message confirmation to peer {} {}:{}, message {} confirmation {}", peerId,
                        ipAddress, port, requestString, inputString);
            }
        } catch (IOException e) {
            log.debug("Failed to send message to peer {} {}:{}, message {}", peerId, ipAddress, port, requestString, e);
        }
    }

    /**
     * Stop messaging client.
     */
    public void stop() {
        stop = true;
    }
}

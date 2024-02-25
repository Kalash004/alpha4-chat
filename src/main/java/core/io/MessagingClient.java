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
import java.util.List;
import java.util.Map;
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

    private static final int SOCKET_TIMEOUT = 1000;

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
                        socket.connect(new InetSocketAddress(ipAddress, port), SOCKET_TIMEOUT);
                        out = new PrintWriter(socket.getOutputStream(), true);
                        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                        // send message to the peer
                        out.println(commandString);
                        out.println();
                        log.debug("Sent message {} to peer {} {}:{}", commandString, peerId, ipAddress, port);
                        String input = readInput(in);
                        log.debug("Received message {} from peer {} {}:{}", input, peerId, ipAddress, port);
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
                        String msg = String.format("Failed to send command %s to the peer %s %s:%s", commandString,
                                peerId, ipAddress, port);
                        log.error(msg, e);
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
                    // sending new messages if any
                    Map<Long, Message> newMsgs = msgMgr.getNewMessages();
                    log.debug("Executing new messaging {} send for peer {} {}:{}", newMsgs.size(), peerId, ipAddress,
                            port);
                    for (Entry<Long, Message> entry : newMsgs.entrySet()) {
                        Long messageId = entry.getKey();
                        String message = entry.getValue().message();
                        String newMsgPeerId = entry.getValue().peerId();
                        if (peerId.equals(newMsgPeerId)) {
                            log.debug("Skip sending new message {} from peer {} to peer {} {}:{}", messageId,
                                    newMsgPeerId, peerId, ipAddress, port);
                            continue;
                        }
                        log.debug("Sending new message to peer {}  {}:{}, Message id {}", peerId, ipAddress, port,
                                messageId);
                        String requestString = null;
                        try {
                            socket = new Socket();
                            socket.connect(new InetSocketAddress(ipAddress, port), SOCKET_TIMEOUT);
                            out = new PrintWriter(socket.getOutputStream(), true);
                            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                            requestString = JsonUtil
                                    .toJson(new Request(Command.NEW_MESSAGE, this.peerId, messageId, message));
                            out.println(requestString);
                            out.println();
                            log.debug("Sent new message to peer {} {}:{}, message {}", peerId, ipAddress, port,
                                    requestString);
                            String input = readInput(in);
                            log.debug("Received response {} from peer {} {}:{}", input, peerId, ipAddress, port);
                        } catch (IOException e) {
                            String msg = String.format("Failed to send command %s to the peer %s %s:%s", requestString,
                                    peerId, ipAddress, port);
                            log.error(msg, e);
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
     * Stop messaging client.
     */
    public void stop() {
        stop = true;
    }
}

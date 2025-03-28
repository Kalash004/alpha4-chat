package core.io;

import core.manager.MessagesManager;
import core.model.Command;
import core.model.Request;
import core.model.Response;
import core.model.Status;
import core.util.Config;
import core.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Listening TCP port {@link MessagingServer#port} for the commands {@link Command#HELLO} or
 * {@link Command#NEW_MESSAGE}. Used for peer-to-peer message exchange.
 */
public class MessagingServer implements Runnable, Config {
    private static final Logger log = LoggerFactory.getLogger(MessagingServer.class);
    private int port;
    private MessagesManager msgMgr;
    private boolean stop;

    /**
     * @param port
     *            message server port
     * @param msgMgr
     *            messages manager {@see MessagesManager}
     */
    public MessagingServer(int port, MessagesManager msgMgr) {
        this.port = port;
        this.msgMgr = msgMgr;
    }

    @Override
    public void run() {
        // open socket only once and then reuse it
        try (ServerSocket server = new ServerSocket(port)) {
            server.setReuseAddress(true);
            while (!stop) {
                // to accept multiple parallel requests we need to process each request in a separate thread
                new Thread(new MessageRunnable(server.accept(), msgMgr)).start();
            }
            if (stop) {
                log.info("Execution stopped");
            }
        } catch (IOException e) {
            String msg = String.format("Failed to open socket on port %s", port);
            log.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }

    /**
     * Stop message server.
     */
    public void stop() {
        stop = true;
    }

    /**
     * Processing incoming request.
     */
    class MessageRunnable implements Runnable, Config {
        private static final Logger log = LoggerFactory.getLogger(MessageRunnable.class);

        private final Socket socket;
        private final MessagesManager msgMgr;

        /**
         * @param socket
         *            connected socket
         * @param msgMgr
         *            messaging manager {@see MessagesManager}
         */
        public MessageRunnable(Socket socket, MessagesManager msgMgr) {
            this.socket = socket;
            this.msgMgr = msgMgr;
        }

        @Override
        public void run() {
            try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));) {
                String address = socket.getInetAddress().getHostAddress();
                int port = socket.getPort();
                Command expectedCommand = Command.HELLO;
                while (true) {
                    String requestMsg = readInput(in);
                    if (requestMsg == null || requestMsg.isEmpty()) {
                        log.info("Received empty message from {}:{}", requestMsg, address, port);
                        break;
                    }
                    log.info("Received message {} from {}:{}", requestMsg, address, port);
                    Response response;
                    try {
                        Request request = JsonUtil.fromJson(requestMsg, Request.class);
                        String peerId = request.peerId();
                        Command command = request.command();
                        if (request.command() == null) {
                            throw new IllegalArgumentException(
                                    String.format("Missing command in the request %s", requestMsg));
                        }
                        if (expectedCommand != command) {
                            throw new IllegalArgumentException(
                                    String.format("Expected command '%s' but received '%s'", expectedCommand.value(),
                                            command.value()));
                        }
                        switch (command) {
                        case HELLO:
                            if (peerId != null) {
                                response = new Response(Status.OK, msgMgr.getMessages(), null, null);
                                expectedCommand = Command.NEW_MESSAGE;
                            } else {
                                throw new IllegalArgumentException(
                                        String.format("Missing peer_id in request %s", requestMsg));
                            }
                            break;
                        case NEW_MESSAGE:
                            if (peerId != null) {
                                msgMgr.addNewMessage(peerId, request.message());
                                response = new Response(Status.OK, null, null, null);
                            } else {
                                throw new IllegalArgumentException(
                                        String.format("Missing peer_id in request %s", requestMsg));
                            }
                            break;
                        case UNKNOWN:
                        default:
                            throw new IllegalArgumentException(String.format("Invalid request %s", requestMsg));
                        }
                    } catch (Exception e) {
                        String msg = String.format("Failed to process message exchange %s", requestMsg);
                        log.warn(msg, e);
                        response = new Response(Status.ERROR, null, e.getMessage(), null);
                        String responseJson = JsonUtil.toJson(response);
                        log.debug("Returning response {} to {}:{}", responseJson, address, port);
                        out.println(responseJson);
                        out.flush();
                        break;
                    }
                    String responseJson = JsonUtil.toJson(response);
                    log.debug("Returning response {} to {}:{}", responseJson, address, port);
                    out.println(responseJson);
                    // send empty line at the end to respect incoming terminal connections,
                    // which sends every line as a separate chunks
                    out.flush();
                }
            } catch (IOException e) {
                log.error("Failed to process request", e);
            } finally {
                try {
                    socket.close();
                } catch (Exception e) {
                    // do nothing
                }
            }
        }
    }
}

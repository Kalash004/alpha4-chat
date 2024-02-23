package core.io;

import core.model.Command;
import core.model.Request;
import core.model.Response;
import core.model.Status;
import core.util.Config;
import core.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Listening UDP port {@link DiscoveryServer#port} for {@link Command#HELLO} and returns {@link DiscoveryServer#peerId}
 * back. Does not return any response if received other than {@link Command#HELLO} command.
 */
public class DiscoveryServer implements Runnable, Config {
    private static final Logger log = LoggerFactory.getLogger(DiscoveryServer.class);

    private int defaultPacketBufferLength;
    private final int port;
    private final String peerId;

    private boolean stop;

    /**
     * @param defaultPacketBufferLength
     *            default packet buffer length
     * @param port
     *            discovery port
     * @param peerId
     *            peer Id
     */
    public DiscoveryServer(int defaultPacketBufferLength, int port, String peerId) {
        this.defaultPacketBufferLength = defaultPacketBufferLength;
        this.port = port;
        this.peerId = peerId;
    }

    @Override
    public void run() {
        // open socket only once and reuse it
        try (DatagramSocket socket = new DatagramSocket(port)) {
            while (!stop) {
                byte[] buf = new byte[defaultPacketBufferLength];
                final DatagramPacket request = new DatagramPacket(buf, buf.length);
                socket.receive(request);
                try {
                    final InetAddress address = request.getAddress();
                    final String addressString = address.getHostAddress();
                    final int port = request.getPort();

                    final String requestMsg = new String(trim(request.getData()));
                    log.info("Received broadcast request {} from {}:{}", requestMsg, addressString, port);

                    final Request requestObj = JsonUtil.fromJson(requestMsg, Request.class);
                    // respond only to the expected command
                    if (requestObj.command() == Command.HELLO) {
                        final Response response;
                        String requestPeerId = requestObj.peerId();
                        if (requestPeerId != null && !requestPeerId.equals(peerId)) {
                            response = new Response(Status.OK, null, null, peerId);
                        } else if (requestPeerId == null) {
                            response = new Response(Status.ERROR, null,
                                    String.format("Missing peer_id: %s", requestMsg), peerId);
                        } else {
                            // do not return anything to ourself
                            continue;
                        }
                        final String responseString = JsonUtil.toJson(response);
                        log.debug("Sending response {} to {}:{}", responseString, addressString, port);
                        final byte[] outBuf = responseString.getBytes();
                        socket.send(new DatagramPacket(outBuf, outBuf.length, address, port));
                    }
                } catch (Exception e) {
                    log.error("Failed to process message on port {}", port, e);
                }
            }
        } catch (Exception e) {
            log.error("Failed to open socket on port {}", port, e);
        }
        if (stop) {
            log.info("Execution stopped");
        }
    }

    /**
     * Stop discovery server execution.
     */
    public void stop() {
        stop = true;
    }
}

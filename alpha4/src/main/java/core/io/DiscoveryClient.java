package core.io;

import core.manager.PeerManager;
import core.model.Command;
import core.model.Request;
import core.model.Response;
import core.util.Config;
import core.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/**
 * Discovering message peers by sending UDP broadcast {@link Command#HELLO} command with {@link DiscoveryClient#timeout}
 * interval. Stores received peer Id's for the future message exchange.
 */
public class DiscoveryClient implements Runnable, Config {
    private static final Logger log = LoggerFactory.getLogger(DiscoveryClient.class);

    private final InetAddress ipAddress;
    private final int port;
    private final int timeout;

    private final String peerId;
    private final PeerManager peerMgr;

    private boolean stop;

    private final String commandString;

    private int defaultPacketBufferLength;

    /**
     * @param defaultPacketBufferLength
     *            default packet buffer length
     * @param ipAddress
     *            broadcast sub-net
     * @param port
     *            broadcast port
     * @param timeout
     *            socket timeout
     * @param peerId
     *            peer Id
     * @param peerMgr
     *            Peer Manager {@see PeerManager}
     */
    public DiscoveryClient(int defaultPacketBufferLength, String ipAddress, int port, int timeout, String peerId,
            PeerManager peerMgr) {
        try {
            this.ipAddress = InetAddress.getByName(ipAddress);
        } catch (UnknownHostException e) {
            String msg = String.format("Invalid ip address %s", ipAddress);
            log.error(msg, e);
            throw new RuntimeException(msg, e);
        }
        this.defaultPacketBufferLength = defaultPacketBufferLength;
        this.port = port;
        this.timeout = timeout;
        this.peerId = peerId;
        this.peerMgr = peerMgr;
        this.commandString = JsonUtil.toJson(new Request(Command.HELLO, peerId, null, null));
    }

    @Override
    public void run() {
        byte[] outBuf = commandString.getBytes();

        // open socket
        try (DatagramSocket socket = new DatagramSocket()) {
            // set broadcast
            socket.setBroadcast(true);
            socket.setSoTimeout(timeout);
            while (!stop) {
                log.debug("Sending broadcard request {} to {}:{}", commandString, ipAddress.getHostAddress(), port);
                // send request
                DatagramPacket request = new DatagramPacket(outBuf, outBuf.length, ipAddress, port);
                socket.send(request);
                long start = System.currentTimeMillis();
                while (System.currentTimeMillis() - start < timeout) {
                    try {
                        log.debug("Waiting for the discovery responses");
                        byte[] inBuf = new byte[defaultPacketBufferLength];
                        DatagramPacket response = new DatagramPacket(inBuf, inBuf.length);
                        socket.receive(response);

                        // peer IP address
                        String peerAddress = response.getAddress().getHostAddress();
                        // peer port
                        int peerPort = response.getPort();
                        String responseString = new String(response.getData()).trim();
                        log.debug("Received message {} from {}:{}", responseString, peerAddress, peerPort);

                        Response responseObj = JsonUtil.fromJson(responseString, Response.class);
                        String peerId = responseObj.peerId();
                        if (peerId == null) {
                            String errorMsg = responseObj.message();
                            if (errorMsg != null) {
                                log.warn("Received error: {} from {}:{}", errorMsg, peerAddress, peerPort);
                            } else {
                                log.warn("Unable to get peer id from: {} from {}:{}", responseString, peerAddress,
                                        peerPort);
                            }
                            // ignore ourself
                        } else if (!this.peerId.equals(peerId)) {
                            peerMgr.addPeer(peerId, peerAddress, peerPort);
                        } else {
                            log.debug("Ignoring out peer {} from {}:{}", peerId, peerAddress, peerPort);
                        }
                    } catch (SocketTimeoutException e) {
                        // do nothing
                        log.debug("Got socket timeout exception, no responses received during timeout {}ms", timeout);
                    } catch (Exception e) {
                        log.debug("Got exception while waiting for the response", e);
                    }
                }
            }
            if (stop) {
                log.info("Execution stopped");
            }
        } catch (IOException e) {
            String msg = String.format("Unable to send command %s to the peer %s:%s", commandString,
                    ipAddress.getHostAddress(), port);
            log.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }

    /**
     * Stop discovery client execution.
     */
    public void stop() {
        stop = true;
    }
}

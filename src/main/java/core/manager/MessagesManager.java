package core.manager;

import core.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.TreeMap;

/**
 * Holds message history and provides API to add message and retrieve messages, ordered by Id (which is time-stamped).
 */
public class MessagesManager {
    private static final Logger log = LoggerFactory.getLogger(MessagesManager.class);
    private TreeMap<Long, Message> messagesMap = new TreeMap<>();

    private int messagesLimit;

    /**
     * @param messagesLimit
     *            messages history limit
     */
    public MessagesManager(int messagesLimit) {
        this.messagesLimit = messagesLimit;
    }

    /**
     * Store new message.
     *
     * @param id
     *            message id
     * @param peerId
     *            peer id
     * @param message
     *            message content
     */
    public void addMessage(long id, String peerId, String message) {
        log.debug("Store message with id {} from peer {}", id, peerId);
        synchronized (messagesMap) {
            messagesMap.put(id, new Message(peerId, message));
            while (messagesMap.size() >= messagesLimit) {
                log.debug("Removing message {} to meet limit {}", messagesMap.size(), messagesLimit);
                messagesMap.pollFirstEntry();
            }
        }
    }

    /**
     * Returns messages.
     *
     * @return messages map
     */
    public Map<Long, Message> getMessages() {
        log.debug("Returning {} messages", messagesMap.size());
        return messagesMap;
    }
}

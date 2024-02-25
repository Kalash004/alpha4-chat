package core.manager;

import core.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * Holds message history and provides API to add message and retrieve messages, ordered by Id (which is time-stamped).
 */
public class MessagesManager {
    private static final Logger log = LoggerFactory.getLogger(MessagesManager.class);
    private TreeMap<Long, Message> messagesMap = new TreeMap<>();
    private TreeMap<Long, Message> newMessagesMap = new TreeMap<>();

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
        messagesMap.put(id, new Message(peerId, message));
        while (messagesMap.size() >= messagesLimit) {
            log.debug("Removing message {} to meet limit {}", messagesMap.size(), messagesLimit);
            messagesMap.pollFirstEntry();
        }
    }

    /**
     * Store new message.
     *
     * @param peerId
     *            peer id
     * @param message
     *            message content
     */
    public void addNewMessage(String peerId, String message) {
        long id = System.currentTimeMillis();
        log.debug("Store new message with id {} from peer {}", id, peerId);
        addMessage(id, peerId, message);
        newMessagesMap.put(id, new Message(peerId, message));
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

    /**
     * Returns new messages.
     *
     * @return messages map
     */
    public Map<Long, Message> getNewMessages() {
        log.debug("Returning new {} messages", newMessagesMap.size());
        return newMessagesMap;
    }

    /**
     * Clear all new messages.
     */
    public void clearNewMessages() {
        log.debug("Clearing new {} messages", newMessagesMap.size());
        for (Entry<Long, Message> entry : newMessagesMap.entrySet()) {
            Long id = entry.getKey();
            Message message = entry.getValue();
            log.debug("Removing message {} {} {}", id, message.peerId(), message.message());
        }
        newMessagesMap.clear();
    }
}

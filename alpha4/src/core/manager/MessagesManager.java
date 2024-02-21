package core.manager;

import core.model.Message;

import java.util.TreeMap;

public class MessagesManager {

    private TreeMap<Integer, Message> messagesMap = new TreeMap<>();

    private int messagesLimit;

    public MessagesManager(int messagesLimit) {
        this.messagesLimit = messagesLimit;
    }

    public void addMessage(int id, String peerId, String message) {
        messagesMap.put(id, new Message(peerId, message));

        while (messagesMap.size() >= messagesLimit) {
            messagesMap.pollFirstEntry();
        }
    }

    public TreeMap<Integer, Message> getMessages() {
        return messagesMap;
    }
}

package io.github.gohoski.numai;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Gleb on 21.08.2025.
 * get and add the messages
 */

class MessageManager {
    private static MessageManager instance;
    private List<Message> messages;

    private MessageManager() {
        messages = new ArrayList<>();
    }

    static synchronized MessageManager getInstance() {
        if (instance == null) {
            instance = new MessageManager();
        }
        return instance;
    }

    void addMessage(Message message) {
        messages.add(message);
    }

    List<Message> getMessages() {
        return messages;
    }

    void clearMessages() {
        messages.clear();
    }
}
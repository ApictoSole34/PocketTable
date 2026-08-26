package com.fizzycoyote.pockettable.network.common;

import com.fizzycoyote.pockettable.models.poker.PokerGameState;

public class GameMessage {
    private MessageType type;
    private Object payload;

    public GameMessage() {}

    public GameMessage(MessageType type, Object payload) {
        this.type = type;
        this.payload = payload;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    public PokerGameState getStatePayload() {
        if (payload instanceof PokerGameState) {
            return (PokerGameState) payload;
        }
        return null;
    }
}
package com.fizzycoyote.pockettable.utils;


import com.fizzycoyote.pockettable.network.PokerClient;

public class ClientHolder {
    private static ClientHolder instance;
    private PokerClient client;

    private ClientHolder() {}

    public static ClientHolder getInstance() {
        if (instance == null) instance = new ClientHolder();
        return instance;
    }

    public void setClient(PokerClient client) {
        this.client = client;
    }

    public PokerClient getClient() {
        return client;
    }

    public void clear() {
        client = null;
    }
}
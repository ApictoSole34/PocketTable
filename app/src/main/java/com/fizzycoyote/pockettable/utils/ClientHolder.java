package com.fizzycoyote.pockettable.utils;


import com.fizzycoyote.pockettable.network.common.GenericGameClient;

public class ClientHolder {
    private static ClientHolder instance;
    private GenericGameClient client;

    private ClientHolder() {}

    public static ClientHolder getInstance() {
        if (instance == null) instance = new ClientHolder();
        return instance;
    }

    public void setClient(GenericGameClient client) {
        this.client = client;
    }

    public GenericGameClient getClient() {
        return client;
    }

    public void clear() {
        client = null;
    }
}
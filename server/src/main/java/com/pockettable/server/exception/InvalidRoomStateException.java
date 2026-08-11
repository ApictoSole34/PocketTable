package com.pockettable.server.exception;

public class InvalidRoomStateException extends RuntimeException {

    public InvalidRoomStateException(String message) {
        super(message);
    }
}

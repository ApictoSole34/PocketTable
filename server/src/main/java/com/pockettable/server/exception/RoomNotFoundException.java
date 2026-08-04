package com.pockettable.server.exception;

public class RoomNotFoundException extends RuntimeException {

    public RoomNotFoundException(String roomCode) {
        super("Room with code " + roomCode + " was not found");
    }
}
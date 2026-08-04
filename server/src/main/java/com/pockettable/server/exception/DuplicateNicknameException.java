package com.pockettable.server.exception;

public class DuplicateNicknameException extends RuntimeException {

    public DuplicateNicknameException(String nickname) {
        super("Nickname '" + nickname + "' already exists in this room");
    }
}
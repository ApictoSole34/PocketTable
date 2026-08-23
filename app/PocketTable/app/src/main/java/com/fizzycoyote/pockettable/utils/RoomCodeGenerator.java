package com.fizzycoyote.pockettable.utils;

import java.security.SecureRandom;

public class RoomCodeGenerator {

    private static final String CHARACTERS =
            "ABCDEFGHJKMNPQRSTUVWXYZ23456789";

    private static final int CODE_LENGTH = 6;

    private final SecureRandom random = new SecureRandom();


    public String generate() {

        StringBuilder code = new StringBuilder();

        for (int i = 0; i < CODE_LENGTH; i++) {
            int index = random.nextInt(CHARACTERS.length());
            code.append(CHARACTERS.charAt(index));
        }

        return code.toString();
    }
}
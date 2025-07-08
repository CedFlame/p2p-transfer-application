package ru.shmatov.util;

import lombok.extern.slf4j.Slf4j;

import java.security.SecureRandom;

@Slf4j
public final class ConfirmationCodeGenerator {

    private static final SecureRandom random = new SecureRandom();
    private static final int CODE_LENGTH = 6;

    private ConfirmationCodeGenerator() {}

    public static String generateCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(random.nextInt(10));
        }
        String code = sb.toString();
        log.debug("Generated confirmation code: {}", code);
        return code;
    }
}

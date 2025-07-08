package ru.shmatov.util;

import lombok.extern.slf4j.Slf4j;

import java.security.SecureRandom;

@Slf4j
public final class AccountNumberGenerator {

    private static final SecureRandom RND = new SecureRandom();
    private static final long MAX_16_DIGIT = 1_0000_0000_0000_0000L;
    private static final int BALANCE_SEQ_LENGTH = 4;

    private AccountNumberGenerator() {}

    public static String generateAccountNumber() {
        long n = Math.abs(RND.nextLong()) % MAX_16_DIGIT;
        String result = String.format("%016d", n);
        log.debug("Generated account number: {}", result);
        return result;
    }

    public static String generateBalanceNumber(String accountNumber, int sequence) {
        String result = accountNumber + String.format("%0" + BALANCE_SEQ_LENGTH + "d", sequence);
        log.debug("Generated balance number: {}", result);
        return result;
    }
}

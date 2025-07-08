package ru.shmatov.controller.enums;

import java.util.Arrays;
import java.util.Optional;

public enum BotCommand {
    HELP("help"),
    WALLET("wallet"),
    P2P("p2p"),
    REGISTER("register"),
    AUTH("auth"),
    LOGOUT("logout"),
    SWITCH_MASTER("switchmaster"),
    DELETE_BALANCE("deletebalance"),
    CREATE_BALANCE("createbalance"),
    DELETE_WALLET("deletewallet"),
    CREATE_WALLET("createwallet"),
    TRANSFER("transfer"),
    CONFIRM("confirm");

    private final String value;

    BotCommand(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static Optional<BotCommand> from(String input) {
        return Arrays.stream(values())
                .filter(cmd -> cmd.value.equalsIgnoreCase(input))
                .findFirst();
    }
}

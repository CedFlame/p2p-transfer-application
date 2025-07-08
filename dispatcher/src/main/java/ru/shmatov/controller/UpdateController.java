package ru.shmatov.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.shmatov.controller.enums.BotCommand;
import ru.shmatov.service.NodeService;
import ru.shmatov.utils.MessageUtils;

import java.util.Arrays;
import java.util.Locale;
import java.util.function.Consumer;

@Slf4j
@Component
public class UpdateController {

    private final MessageUtils messageUtils;
    private final NodeService nodeService;
    private TelegramBot telegramBot;

    public UpdateController(MessageUtils messageUtils, NodeService nodeService) {
        this.messageUtils = messageUtils;
        this.nodeService = nodeService;
    }

    /* --------------------------------------------------
       Telegram‑bot bootstrap
       -------------------------------------------------- */

    public void registerBot(TelegramBot telegramBot) {
        this.telegramBot = telegramBot;
    }

    /* --------------------------------------------------
       Entry point
       -------------------------------------------------- */

    public void processUpdate(Update update) {
        if (update == null || update.getMessage() == null) {
            log.warn("Empty or unsupported update received");
            return;
        }
        Message msg = update.getMessage();
        if (msg.getText() == null) {
            reply(update, "Unsupported message type. Use /help for command list.");
            return;
        }
        routeCommand(update, msg.getText().trim());
    }

    /* --------------------------------------------------
       Command router
       -------------------------------------------------- */

    private void routeCommand(Update update, String text) {
        if (!text.startsWith("/")) {
            reply(update, "This bot understands only commands. Use /help.");
            return;
        }
        String[] tokens = text.substring(1).split("\\s+");
        String rawCmd = tokens[0].toLowerCase(Locale.ROOT);
        String[] args = Arrays.copyOfRange(tokens, 1, tokens.length);

        BotCommand.from(rawCmd).ifPresentOrElse(
                cmd -> dispatch(update, cmd, args),
                () -> reply(update, "Unknown command. Use /help.")
        );
    }

    private void dispatch(Update u, BotCommand cmd, String[] args) {
        switch (cmd) {
            case HELP -> requireNoArgs(u, args, this::sendHelp);
            case WALLET -> auth(u, _u -> showWallet(_u), args, 0);
            case P2P -> auth(u, _u -> reply(_u, "P2P menu is under construction."), args, 0);
            case LOGOUT -> auth(u, this::logout, args, 0);
            case DELETE_WALLET -> auth(u, this::deleteWallet, args, 0);
            case SWITCH_MASTER -> auth(u, bal -> switchMaster(u, bal), args, 1, 20, "switchmaster <balance_number>");
            case DELETE_BALANCE -> auth(u, bal -> deleteBalance(u, bal), args, 1, 20, "deletebalance <balance_number>");
            case CREATE_BALANCE -> auth(u, bal -> createBalance(u, bal), args, 0, 1);
            case CREATE_WALLET -> auth(u, bal -> createWallet(u, bal), args, 0, 1);
            case REGISTER ->
                    checkArgs(u, args, 2, 2, "register <password> <confirm>", () -> register(u, args[0], args[1]));
            case AUTH -> checkArgs(u, args, 1, 1, "auth <password>", () -> login(u, args[0]));
            case TRANSFER -> auth(u, _unused -> transfer(u, args), args, 3);
            case CONFIRM -> auth(u, _unused -> confirm(u, args), args, 1, 6, "confirm <6-digit-code>");
            default -> reply(u, "Unknown command. Use /help.");
        }
    }

    /* --------------------------------------------------
       Argument helpers
       -------------------------------------------------- */

    private void requireNoArgs(Update u, String[] args, Consumer<Update> action) {
        if (args.length == 0) action.accept(u);
        else reply(u, "This command takes no arguments.");
    }

    private void auth(Update u, Consumer<Update> action, String[] args, int expectedArgs) {
        if (!preAuth(u)) return;
        if (args.length == expectedArgs) action.accept(u);
        else reply(u, "Wrong arguments count. Use /help.");
    }

    private void auth(Update u, Consumer<String> action, String[] args, int expectedArgs, int exactLen, String usage) {
        if (!preAuth(u)) return;
        if (args.length == expectedArgs && args[0].length() == exactLen) action.accept(args[0]);
        else reply(u, "Usage: /" + usage);
    }

    private void auth(Update u, Consumer<Long> action, String[] args, int minArgs, int maxArgs) {
        if (!preAuth(u)) return;
        if (args.length < minArgs || args.length > maxArgs) {
            reply(u, "Wrong arguments. Use /help.");
            return;
        }
        long value = 0L;
        if (args.length == 1) {
            try {
                value = Long.parseLong(args[0]);
            } catch (NumberFormatException e) {
                reply(u, "Initial balance must be a number.");
                return;
            }
        }
        action.accept(value);
    }

    private void checkArgs(Update u, String[] args, int min, int max, String usage, Runnable action) {
        if (args.length >= min && args.length <= max) action.run();
        else reply(u, "Usage: /" + usage);
    }

    private boolean preAuth(Update u) {
        if (!nodeService.isRegistered(u)) {
            reply(u, "You are not registered. Use /register.");
            return false;
        }
        if (!nodeService.isAuthenticated(u)) {
            reply(u, "Please login first using /auth <password>.");
            return false;
        }
        return true;
    }

    /* --------------------------------------------------
       Command implementations
       -------------------------------------------------- */

    private void sendHelp(Update u) {
        reply(u, """
                🤖 *P2P Bot commands*
                
                /register <pwd> <confirm> – register new user
                /auth <pwd> – login
                /logout – logout
                
                /createwallet [balance] – create wallet with optional initial balance
                /wallet – show wallet info
                /deletewallet – delete wallet (all balances must be zero)
                
                /createbalance [balance] – create balance
                /deletebalance <number> – delete balance
                /switchmaster <number> – set balance as primary
                
                /transfer <amt> <from> <to> – send money (20‑digit balances)
                /confirm <code> – confirm transfer (6‑digit code)
                
                /help – this help message
                """
        );
    }

    private void register(Update u, String pwd, String confirm) {
        reply(u, nodeService.register(u, pwd, confirm).message());
    }

    private void login(Update u, String pwd) {
        reply(u, nodeService.login(u, pwd).message());
    }

    private void logout(Update u) {
        reply(u, nodeService.logout(u).message());
    }

    private void createWallet(Update u, long initBalance) {
        reply(u, nodeService.create(u, initBalance).message());
    }

    private void createBalance(Update u, long initBalance) {
        reply(u, nodeService.createBalance(u, initBalance).message());
    }

    private void deleteBalance(Update u, String number) {
        reply(u, nodeService.deleteBalance(u, number).message());
    }

    private void switchMaster(Update u, String number) {
        reply(u, nodeService.switchPrimaryBalance(u, number).message());
    }

    private void showWallet(Update u) {
        reply(u, nodeService.getAccount(u).toString());
    }

    private void deleteWallet(Update u) {
        reply(u, nodeService.delete(u).message());
    }

    private void transfer(Update u, String[] args) {
        try {
            long amt = Long.parseLong(args[0]);
            String from = args[1];
            String to = args[2];
            if (!from.matches("\\d{20}") || !to.matches("\\d{20}")) {
                reply(u, "Balance numbers must be 20 digits.");
                return;
            }
            reply(u, nodeService.transfer(u, amt, from, to).message());
        } catch (NumberFormatException e) {
            reply(u, "Amount must be a number.");
        }
    }

    private void confirm(Update u, String[] args) {
        String code = args[0];
        reply(u, nodeService.confirmTransfer(u, code).message());
    }

    private void reply(Update u, String text) {
        telegramBot.sendAnswerMessage(messageUtils.generateSendMessageWithText(u, text));
    }
}

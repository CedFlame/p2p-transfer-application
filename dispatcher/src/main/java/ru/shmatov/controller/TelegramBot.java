package ru.shmatov.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.annotation.PostConstruct;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    @Value("${telegram.bot.token}")
    private String botToken;
    @Value("${telegram.bot.name}")
    private String botName;
    private UpdateController updateController;

    public TelegramBot(UpdateController updateController) {
        this.updateController = updateController;
    }

    @PostConstruct
    public void init() {
        updateController.registerBot(this);
    }

    @Override
    public String getBotUsername() {
        return botName;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        Message originalMessage = update.getMessage();
        log.debug(originalMessage.getText());
        updateController.processUpdate(update);
    }

    public void sendAnswerMessage(SendMessage message) {
        if (message != null) {
            try {
                execute(message);
            } catch (TelegramApiException e) {
                log.error(e.getMessage());
            }
        }
    }
}

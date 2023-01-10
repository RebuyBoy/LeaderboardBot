package com.leaderboardbot.service;

import com.leaderboardbot.config.BotConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
@Slf4j
public class TelegramBot extends TelegramLongPollingBot {
    private final BotConfig botConfig;

    public TelegramBot(BotConfig botConfig) {
        this.botConfig = botConfig;
    }

    @Override
    public String getBotUsername() {
        return botConfig.getName();
    }

    @Override
    public String getBotToken() {
        return botConfig.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            String chatId = update.getMessage().getChatId().toString();
            String name = update.getMessage().getChat().getFirstName();

            switch (messageText) {
                case "/start" -> handleStartCommand(chatId, name);
//                case "/watch"
                default -> sendMessage(chatId, "херню пишешь чел");
            }
        }
    }

    private void handleStartCommand(String chatId, String name) {
        String answer = "Здорова чел " + name + " чел";
        log.info("replied to user" + name);
        sendMessage(chatId, answer);
    }

    private void sendMessage(String chatId, String answer) {
        try {
            execute(SendMessage.builder()
                    .chatId(chatId)
                    .text(answer)
                    .build());
        } catch (TelegramApiException e) {
            log.error("error occurred when sending message " + e.getMessage());
        }

    }
}

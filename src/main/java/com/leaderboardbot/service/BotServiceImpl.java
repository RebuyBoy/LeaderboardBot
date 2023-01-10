package com.leaderboardbot.service;

import com.leaderboardbot.model.MessageType;
import com.leaderboardbot.model.Subscription;
import com.leaderboardbot.service.iface.BotService;
import com.leaderboardbot.service.iface.SubscribeService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Service
@AllArgsConstructor
public class BotServiceImpl implements BotService {

    private SubscribeService subscribeService;

    @Override
    public BotApiMethod<?> getAnswer(Update update) {
        String chatId = update.getMessage().getChatId().toString();

        return switch (getMessageType(update)) {
            case COMMAND -> getSendMessageByCommand(update.getMessage().getText(), chatId);
            default -> SendMessage.builder()
                    .chatId(chatId)
                    .text("wrong")
                    .build();
        };
    }

    private SendMessage getSendMessageByCommand(String command, String chatId) {
        String textAnswer = switch (getCommand(command)) {
            case "/start" -> "Hello";
            case "/watch" -> getWatchAnswer(command, chatId);
            default -> "Command not found";
        };
        return getSendMessage(textAnswer, chatId);
    }

    private String getWatchAnswer(String command, String chatId){
        String textAnswer;
        try {
            Subscription subscription = getSubscription(command, chatId);
            subscribeService.create(subscription);
            textAnswer = "Successful " + subscription.getStake() + " " + subscription.getTargetPoints();
        } catch (Exception e) {
            textAnswer = "Wrong command";
        }
        return textAnswer;
    }

    private Subscription getSubscription(String command, String chatId) {
        String[] data = command.split(" ");
            return Subscription
                    .builder()
                    .stake(data[1])
                    .targetPoints(data[2])
                    .chatId(chatId)
                    .build();

    }

    private String getCommand(String command) {
        return command.split(" ")[0];
    }

    private SendMessage getSendMessage(String text, String chatId) {

        return SendMessage.builder()
                .text(text)
                .chatId(chatId)
                .build();
    }

    private MessageType getMessageType(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            return MessageType.COMMAND;
        }
        return MessageType.IGNORE;
    }


}

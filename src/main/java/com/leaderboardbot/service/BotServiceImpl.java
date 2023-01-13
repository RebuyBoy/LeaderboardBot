package com.leaderboardbot.service;

import com.leaderboardbot.model.MessageType;
import com.leaderboardbot.model.Subscription;
import com.leaderboardbot.service.iface.BotService;
import com.leaderboardbot.service.iface.SubscribeService;
import com.leaderboardbot.validator.Validator;
import com.rebuy.api.scope.dto.response.ResultResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BotServiceImpl implements BotService {
    private final SubscribeService subscribeService;

    //TODO status add smile if points changed
    //TODO help
    @Override
    public SendMessage handleUpdate(Update update) {
        String chatId = update.getMessage().getChatId().toString();
        return switch (getMessageType(update)) {
            case COMMAND -> getSendMessageByCommand(update.getMessage().getText(), chatId);
            default -> SendMessage.builder()
                    .chatId(chatId)
                    .text("wrong command")
                    .build();
        };
    }

    private SendMessage getSendMessageByCommand(String command, String chatId) {
        String textAnswer = switch (getCommand(command)) {
            case "/start" -> "Hello";
            case "/watch" -> getWatchAnswer(command, chatId);
            case "/status" -> getStatusAnswer(chatId);
            case "/stop" -> getStopAnswer(chatId);
            default -> "Command not found";
        };
        return getSendMessage(textAnswer, chatId);
    }

    private String getStopAnswer(String chatId) {
        subscribeService.remove(chatId);
        return "successful deleted";
    }

    private String getWatchAnswer(String command, String chatId) {
        String textAnswer;
        try {
            Subscription subscription = getSubscription(command, chatId);
            subscribeService.create(subscription);
            textAnswer = "Successful " + subscription.getStake() + " " + subscription.getTargetPoints();
        } catch (Exception e) {
            textAnswer = e.getMessage();
        }
        return textAnswer;
    }

    private Subscription getSubscription(String command, String chatId) {
        String[] data = command.split(" ");
        return Subscription
                .builder()
                .stake(Validator.getStake(data[1]))
                .targetPoints(Validator.validateTargetPoint(data[2]))
                .chatId(chatId)
                .build();
    }

    private String getCommand(String command) {
        return command.split(" ")[0];
    }

    public SendMessage getSendMessage(String text, String chatId) {
        return SendMessage.builder()
                .text(text)
                .chatId(chatId)
                .build();
    }

    @Override
    public SendMessage getSendMessageWhenNewPlayersAbove(Set<ResultResponse> newPlayersAbove, Subscription subscription) {
        StringBuilder stringBuilder = new StringBuilder("new players above target");
        stringBuilder.append(getStakeAndTargetLine(subscription));
        for (ResultResponse result : newPlayersAbove) {
            stringBuilder.append(getFormattedPlayerText(result));
        }
        return getSendMessage(stringBuilder.toString(), subscription.getChatId());
    }

    private static String getFormattedPlayerText(ResultResponse result) {
        return String.format("%n%d | %s : %s", result.getRank(), result.getName(), result.getPoints().toString());
    }

    public String getStatusAnswer(String chatId) {
        List<Subscription> subscriptions = subscribeService.getSubscriptions(chatId);
        if (subscriptions.isEmpty()) {
            return "No subscriptions";
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (Subscription subscription : subscriptions) {
            stringBuilder.append(getStakeAndTargetLine(subscription));
            List<ResultResponse> playersBeforeTargetLimit3 = subscription.getPlayersBeforeTargetLimit3();
            if (playersBeforeTargetLimit3 == null) {
                stringBuilder.append(" waiting update...");
            } else {
                for (ResultResponse result : playersBeforeTargetLimit3) {
                    stringBuilder.append(getFormattedPlayerText(result));
                }
            }
        }
        return stringBuilder.toString();
    }

    private static String getStakeAndTargetLine(Subscription subscription) {
        return String.format("%n%n stake %s : target %d ", subscription.getStake(), subscription.getTargetPoints());
    }

    private MessageType getMessageType(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            return MessageType.COMMAND;
        }
        return MessageType.IGNORE;
    }

}

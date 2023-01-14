package com.leaderboardbot.service;

import com.leaderboardbot.model.MessageType;
import com.leaderboardbot.model.Subscription;
import com.leaderboardbot.service.iface.BotService;
import com.leaderboardbot.service.iface.SubscribeService;
import com.leaderboardbot.validator.Validator;
import com.rebuy.api.scope.dto.response.ResultResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class BotServiceImpl implements BotService {

    private final SubscribeService subscribeService;
    //TODO status add smile if points changed
    //TODO send message if bot crushed
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
            case "/start" -> "Hello gonshik";
            case "/watch" -> getWatchAnswer(command, chatId);
            case "/status" -> getStatusAnswer(chatId);
            case "/stop" -> getStopAnswer(chatId);
            case "/help" -> getHelpAnswer();
            default -> "Command not found";
        };
        return getSendMessage(textAnswer, chatId);
    }

    private String getHelpAnswer() {
        return """
                commands:
                /watch [stake] [target points] -> create subscription
                /status -> shows first 3 players below target points
                /stop -> delete all subscriptions
                                
                for example, if you want to follow a player on ante 1$ with 1000 points
                /watch 100 1050
                after first scan 0-10 minutes bot will start watching all players below 1050 points
                scanning every 10 minutes for 3 hours
                if anyone cross target points bot will send message
                """;
    }

    private String getStopAnswer(String chatId) {
        subscribeService.delete(chatId);
        return "successful deleted";
    }

    private String getWatchAnswer(String command, String chatId) {
        String textAnswer;
        try {
            Subscription subscription = getSubscription(command, chatId);
            log.info("Subscription created {}", subscription);
            subscribeService.create(subscription);
            textAnswer = String.format("subscription created: %s", getStakeAndTargetLine(subscription));
        } catch (Exception e) {
            textAnswer = e.getMessage();
        }
        return textAnswer;
    }

    private Subscription getSubscription(String command, String chatId) {
        String[] data = command.split(" ");
        if (data.length < 3) {
            throw new IllegalArgumentException("missed stake or target points");
        }
        return Subscription
                .builder()
                .creationDate(LocalDateTime.now())
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
    public SendMessage getFinishedSubscriptionSendMessage(Set<ResultResponse> newPlayersAbove, Subscription subscription) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder
                .append("ALARM")
                .append("\n")
                .append("player crossed target")
                .append("\n\n")
                .append(getStakeAndTargetLine(subscription));
        for (ResultResponse result : newPlayersAbove) {
            stringBuilder
                    .append("\n")
                    .append(getFormattedPlayerText(result));
        }
        return getSendMessage(stringBuilder.toString(), subscription.getChatId());
    }

    public SendMessage getExpiredSubscriptionSendMessage(Subscription subscription) {
        String expiredMessageText = String.format("subscription expired %s", getStakeAndTargetLine(subscription));
        return getSendMessage(expiredMessageText, subscription.getChatId());
    }

    @Override
    public SendMessage getScanErrorSendMessage(Subscription subscription) {
        String message = String.format("error when fetching data for stake %s, retry in 10 minutes", subscription.getStake().getName());
        return getSendMessage(message, subscription.getChatId());
    }

    private static String getFormattedPlayerText(ResultResponse result) {
        return String.format("%d | %s : %s", result.getRank(), result.getName(), result.getPoints().toString());
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
                    stringBuilder
                            .append("\n")
                            .append(getFormattedPlayerText(result));
                }
            }
        }
        return stringBuilder.toString();
    }

    private static String getStakeAndTargetLine(Subscription subscription) {
        return String.format("stake %s : target %d ", subscription.getStake().getName(), subscription.getTargetPoints());
    }

    private MessageType getMessageType(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            return MessageType.COMMAND;
        }
        return MessageType.IGNORE;
    }

}

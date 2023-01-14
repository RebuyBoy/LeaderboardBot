package com.leaderboardbot.service.iface;

import com.leaderboardbot.model.Subscription;
import com.rebuy.api.scope.dto.response.ResultResponse;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Set;

public interface BotService {

    SendMessage handleUpdate(Update update);

    SendMessage getSendMessage(String text, String chatId);

    SendMessage getFinishedSubscriptionSendMessage(Set<ResultResponse> newPlayersAbove, Subscription subscription);

    SendMessage getExpiredSubscriptionSendMessage(Subscription subscription);

    SendMessage getScanErrorSendMessage(Subscription subscription);

}

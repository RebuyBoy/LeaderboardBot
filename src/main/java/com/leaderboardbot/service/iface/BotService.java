package com.leaderboardbot.service.iface;

import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

public interface BotService {
    BotApiMethod<?> getAnswer(Update update);
}

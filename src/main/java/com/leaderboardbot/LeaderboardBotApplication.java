package com.leaderboardbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@SpringBootApplication
public class LeaderboardBotApplication {

    public static void main(String[] args) throws TelegramApiException {
        SpringApplication.run(LeaderboardBotApplication.class, args);
    }

}

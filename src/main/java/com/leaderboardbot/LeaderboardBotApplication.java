package com.leaderboardbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages={"com.rebuy.api.scope","com.leaderboardbot"})
public class LeaderboardBotApplication {

    public static void main(String[] args)  {
        SpringApplication.run(LeaderboardBotApplication.class, args);
    }

}

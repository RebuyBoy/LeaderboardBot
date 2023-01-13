package com.leaderboardbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages={"com.rebuy.api.scope","com.leaderboardbot"})
@EnableScheduling
public class LeaderboardBotApplication {
    public static void main(String[] args)  {

        SpringApplication.run(LeaderboardBotApplication.class, args);
    }

}

package com.leaderboardbot.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Subscription {
    private String stake;
    private String targetPoints;
    private String chatId;
}

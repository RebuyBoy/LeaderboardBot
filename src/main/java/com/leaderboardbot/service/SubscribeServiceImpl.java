package com.leaderboardbot.service;

import com.leaderboardbot.model.Subscription;
import com.leaderboardbot.service.iface.SubscribeService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class SubscribeServiceImpl implements SubscribeService {
    @Override
    public void create(Subscription subscription) {

    }
}

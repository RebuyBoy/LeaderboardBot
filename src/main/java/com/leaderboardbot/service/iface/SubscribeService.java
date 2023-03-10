package com.leaderboardbot.service.iface;

import com.leaderboardbot.model.Subscription;
import com.rebuy.api.scope.dto.request.StakeRequest;

import java.util.List;
import java.util.Set;

public interface SubscribeService {
    void create(Subscription subscription);

    void delete(Subscription subscription);

    void delete(String chatId);

    void deleteAll();

    boolean hasAnySubscribes();

    Set<StakeRequest> getStakes();

    Set<Subscription> getSubscribesByStake(StakeRequest stake);

    List<Subscription> getSubscriptions(String chatId);


}

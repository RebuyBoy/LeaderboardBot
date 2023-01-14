package com.leaderboardbot.service;

import com.leaderboardbot.model.Subscription;
import com.leaderboardbot.service.iface.SubscribeService;
import com.rebuy.api.scope.dto.request.StakeRequest;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@AllArgsConstructor
public class SubscribeServiceImpl implements SubscribeService {

    private final Map<StakeRequest, Set<Subscription>> subscriptions = new EnumMap<>(StakeRequest.class);

    @Override
    public void delete(Subscription subscription) {
        Set<Subscription> subscriptionSet = subscriptions.get(subscription.getStake());
        subscriptionSet.remove(subscription);
        if (subscriptionSet.isEmpty()) {
            subscriptions.remove(subscription.getStake());
        }
    }

    @Override
    public void delete(String chatId) {
        for (Map.Entry<StakeRequest, Set<Subscription>> entry : subscriptions.entrySet()) {
            Set<Subscription> subs = entry.getValue();
            subs.removeIf(sub -> sub.getChatId().equals(chatId));
            if (subs.isEmpty()) {
                subscriptions.remove(entry.getKey());
            }
        }
    }

    @Override
    public void create(Subscription subscription) {
        StakeRequest stake = subscription.getStake();
        if (subscriptions.containsKey(stake)) {
            subscriptions.get(stake).add(subscription);
        } else {
            Set<Subscription> subs = new HashSet<>();
            subs.add(subscription);
            subscriptions.put(stake, subs);
        }
    }

    public void deleteAll() {
        subscriptions.clear();
    }

    @Override
    public boolean hasAnySubscribes() {
        return subscriptions.size() > 0;
    }

    @Override
    public Set<StakeRequest> getStakes() {
        return subscriptions.keySet();
    }

    @Override
    public Set<Subscription> getSubscribesByStake(StakeRequest stake) {
        return subscriptions.get(stake);
    }

    @Override
    public List<Subscription> getSubscriptions(String chatId) {
        return subscriptions.values()
                .stream()
                .flatMap(Collection::stream)
                .filter(subscription -> subscription.getChatId().equals(chatId))
                .toList();
    }

}

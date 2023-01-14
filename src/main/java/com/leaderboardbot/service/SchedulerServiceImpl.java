package com.leaderboardbot.service;

import com.leaderboardbot.config.TelegramBot;
import com.leaderboardbot.model.Subscription;
import com.leaderboardbot.service.iface.BotService;
import com.leaderboardbot.service.iface.SchedulerService;
import com.leaderboardbot.service.iface.SubscribeService;
import com.rebuy.api.scope.dto.request.StakeRequest;
import com.rebuy.api.scope.dto.response.ResultResponse;
import com.rebuy.api.scope.feignclient.LeaderboardApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@EnableScheduling
public class SchedulerServiceImpl implements SchedulerService {

    private final SubscribeService subscribeService;
    private final LeaderboardApiClient leaderboardApi;
    private final BotService botService;
    private final TelegramBot bot;

    @Override
    @Scheduled(fixedDelayString = "${bot.scan.interval.seconds}", timeUnit = TimeUnit.SECONDS)
    public void run() {
        if (subscribeService.hasAnySubscribes()) {
            LocalDateTime currentTime = ZonedDateTime
                    .now(ZoneId.of("GMT-8"))
                    .toLocalDateTime();
            if (currentTime.getHour() == 0) {
                log.info("All subscriptions deleted at " + currentTime);
                subscribeService.deleteAll();
            }
            subscribeService.getStakes()
                    .forEach(this::startTask);
        }
    }

    private void startTask(StakeRequest stake) {
        Set<Subscription> subscriptions = subscribeService.getSubscribesByStake(stake);
        List<ResultResponse> results = getDataByStake(stake, subscriptions);
        for (Subscription subscription : subscriptions) {
            log.info("handle subscription on {} : {} for chatId: {} ", stake, subscription.getTargetPoints(), subscription.getChatId());
            if (isExpired(subscription)) {
                endExpiredSubscription(subscription);
            } else {
                Set<ResultResponse> playersAboveTargetPoints = getPlayersAboveTargetPoints(results, subscription.getTargetPoints());
                if (subscription.isActive()) {
                    checkChanges(playersAboveTargetPoints, subscription);
                } else {
                    subscription.setActive(true);
                    subscription.setPlayersAboveTarget(playersAboveTargetPoints);
                }
                subscription.setPlayersBeforeTargetLimit3(getPlayersBeforeTargetPoints(results, subscription.getTargetPoints()));
            }
        }
    }

    private List<ResultResponse> getDataByStake(StakeRequest stake, Set<Subscription> subscriptions) {
        try {
            return leaderboardApi.parseCurrentDataByStake(stake);
        } catch (Exception e) {
            warnAllSubscribers(subscriptions);
        }
        throw new IllegalStateException("Scan data was failed retry in 10 minutes");
    }

    private void warnAllSubscribers(Set<Subscription> subscriptions) {
        for (Subscription subscription : subscriptions) {
            SendMessage sendMessage = botService.getScanErrorSendMessage(subscription);
            bot.sendMessage(sendMessage);
        }

    }

    private boolean isExpired(Subscription subscription) {
        return Duration.between(subscription.getCreationDate(), LocalDateTime.now()).toHours() > 3;
    }

    private List<ResultResponse> getPlayersBeforeTargetPoints(List<ResultResponse> results, int targetPoints) {
        return results.stream()
                .filter(result -> result.getPoints().compareTo(BigDecimal.valueOf(targetPoints)) <= 0)
                .sorted(Comparator.comparing(ResultResponse::getPoints).reversed())
                .limit(3)
                .toList();
    }

    private Set<ResultResponse> getPlayersAboveTargetPoints(List<ResultResponse> results, int targetPoints) {
        return results.stream()
                .filter(result -> result.getPoints().compareTo(BigDecimal.valueOf(targetPoints)) >= 0)
                .collect(Collectors.toSet());
    }

    private void checkChanges(Set<ResultResponse> newPlayersAbove, Subscription subscription) {
        for (ResultResponse result : subscription.getPlayersAboveTarget()) {
            newPlayersAbove.removeIf(resultResponse -> resultResponse.getName().equals(result.getName()));
        }
        if (!newPlayersAbove.isEmpty()) {
            endFinishedSubscription(newPlayersAbove, subscription);
        }
    }

    private void endExpiredSubscription(Subscription subscription) {
        SendMessage sendMessage = botService.getExpiredSubscriptionSendMessage(subscription);
        endSubscription(subscription, sendMessage);
    }

    private void endFinishedSubscription(Set<ResultResponse> newPlayersAbove, Subscription subscription) {
        SendMessage sendMessage = botService.getFinishedSubscriptionSendMessage(newPlayersAbove, subscription);
        endSubscription(subscription, sendMessage);
    }

    private void endSubscription(Subscription subscription, SendMessage sendMessage) {
        subscribeService.delete(subscription);
        bot.sendMessage(sendMessage);
    }

}

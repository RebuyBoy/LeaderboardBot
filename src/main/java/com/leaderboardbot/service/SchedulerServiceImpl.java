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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.math.BigDecimal;
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
public class SchedulerServiceImpl implements SchedulerService {
    private final SubscribeService subscribeService;
    private final LeaderboardApiClient leaderboardApi;
    private final BotService botService;
    private final TelegramBot bot;


    //TODO if request failed ?? send message  ->  request failed retry in 10 minutes
    @Override
    @Scheduled(fixedDelay = 10,timeUnit = TimeUnit.MINUTES)
    public void run() {
        if (subscribeService.hasAnySubscribes()) {
            LocalDateTime currentTime = getCurrentZonedDate();
            if (currentTime.getHour() == 0) {
                log.info("Subscriptions cleared at " + currentTime);
                subscribeService.removeAll();
            }
            subscribeService.getStakes()
                    .forEach(this::startTask);
        }
    }

    private LocalDateTime getCurrentZonedDate() {
        return ZonedDateTime
                .now(ZoneId.of("GMT-8"))
                .toLocalDateTime();
    }

    private void startTask(StakeRequest stake) {
        log.info("{} task started", stake);
        List<ResultResponse> results = leaderboardApi.parseCurrentDataByStake(stake);
        Set<Subscription> subscriptions = subscribeService.getSubscribesByStake(stake);
        for (Subscription subscription : subscriptions) {
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
            SendMessage sendMessage = botService.getSendMessageWhenNewPlayersAbove(newPlayersAbove, subscription);
            subscribeService.remove(subscription);
            bot.sendMessage(sendMessage);
        }
    }

}

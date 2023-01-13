package com.leaderboardbot.validator;

import com.rebuy.api.scope.dto.request.StakeRequest;

import java.util.Map;

public class Validator {

    private static final String INVALID_TARGET_POINTS_MESSAGE = "Invalid target points [0 > target < 9999]";
    private static final String INVALID_STAKE_MESSAGE = "Invalid stake [10 25 50 100 ...]";
    private static final Map<String, StakeRequest> stakes = Map.of(
            "10", StakeRequest.SD_10
            , "25", StakeRequest.SD_25
            , "50", StakeRequest.SD_50
            , "100", StakeRequest.SD_100
            , "200", StakeRequest.SD_200
            , "500", StakeRequest.SD_500
            , "1000", StakeRequest.SD_1000);

    private Validator() {
        throw new IllegalStateException("Utility class");
    }

    public static StakeRequest getStake(String stake) {
        if (!stakes.containsKey(stake)) {
            throw new IllegalArgumentException(INVALID_STAKE_MESSAGE);
        }
        return stakes.get(stake);
    }

    public static int validateTargetPoint(String data) {
        int targetPoints;
        try {
            targetPoints = Integer.parseInt(data);
            if (targetPoints < 10 || targetPoints > 9999) {
                throw new IllegalArgumentException(INVALID_TARGET_POINTS_MESSAGE);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(INVALID_TARGET_POINTS_MESSAGE);
        }
        return targetPoints;
    }

}

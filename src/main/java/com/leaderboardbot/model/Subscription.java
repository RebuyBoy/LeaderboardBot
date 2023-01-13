package com.leaderboardbot.model;

import com.rebuy.api.scope.dto.request.StakeRequest;
import com.rebuy.api.scope.dto.response.ResultResponse;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;
import java.util.Objects;
import java.util.Set;

@Builder
@Getter
@Setter
@ToString
public class Subscription {
    //TODO subscription ID equals hashcode by id
    private StakeRequest stake;
    private int targetPoints;
    private String chatId;
    private boolean isActive;
    private Set<ResultResponse> playersAboveTarget;
    private List<ResultResponse> playersBeforeTargetLimit3;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Subscription that = (Subscription) o;
        return getTargetPoints() == that.getTargetPoints() && getStake() == that.getStake() && Objects.equals(getChatId(), that.getChatId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getStake(), getTargetPoints(), getChatId());
    }

}

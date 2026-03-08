package top.enderliquid.audioflow.config;

import lombok.Data;

import java.util.List;

@Data
public class CheckinRewardProperties {
    private List<RewardTier> rewards;

    @Data
    public static class RewardTier {
        private String name;
        private Integer weight;
        private Integer minPoints;
        private Integer maxPoints;
    }
}
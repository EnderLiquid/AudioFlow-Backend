package top.enderliquid.audioflow.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
public class CheckinRewardConfig {
    private final ObjectMapper objectMapper;
    private CheckinRewardProperties properties;
    private int totalWeight;

    public CheckinRewardConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() throws IOException {
        ClassPathResource resource = new ClassPathResource("checkin-reward.json");
        properties = objectMapper.readValue(resource.getInputStream(), CheckinRewardProperties.class);

        List<CheckinRewardProperties.RewardTier> rewards = properties.getRewards();
        if (rewards == null || rewards.isEmpty()) {
            throw new IllegalStateException("签到奖励配置为空");
        }

        totalWeight = rewards.stream()
                .mapToInt(CheckinRewardProperties.RewardTier::getWeight)
                .sum();
        if(totalWeight < 1) throw new IllegalStateException("签到奖励权重总和不能小于1");

        for (CheckinRewardProperties.RewardTier tier : rewards) {
            if (tier.getWeight() < 0) {
                throw new IllegalStateException(
                        "签到奖励档位[" + tier.getName() + "]的权重不能为负数"
                );
            }
            if (tier.getMinPoints() < 1 || tier.getMaxPoints() < 1) {
                throw new IllegalStateException(
                        "签到奖励档位[" + tier.getName() + "]的积分不能小于1"
                );
            }
            if (tier.getMinPoints() > tier.getMaxPoints()) {
                throw new IllegalStateException(
                        "签到奖励档位[" + tier.getName() + "]的最小积分不能大于最大积分"
                );
            }
        }
        log.info("签到奖励配置加载成功");
    }

    public int getRandomReward() {
        Random random = ThreadLocalRandom.current();
        List<CheckinRewardProperties.RewardTier> rewards = properties.getRewards();
        int randomValue = random.nextInt(totalWeight);
        int currentWeight = 0;
        
        for (CheckinRewardProperties.RewardTier tier : rewards) {
            currentWeight += tier.getWeight();
            if (randomValue < currentWeight) {
                return getRandomPointsInTier(tier);
            }
        }
        
        return getRandomPointsInTier(rewards.get(rewards.size() - 1));
    }

    private int getRandomPointsInTier(CheckinRewardProperties.RewardTier tier) {
        int min = tier.getMinPoints();
        int max = tier.getMaxPoints();
        Random random = ThreadLocalRandom.current();
        return min + random.nextInt(max - min + 1);
    }
}
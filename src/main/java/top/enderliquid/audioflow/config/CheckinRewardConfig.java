package top.enderliquid.audioflow.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.List;
import java.util.Random;

@Slf4j
@Component
public class CheckinRewardConfig {
    private final ObjectMapper objectMapper;
    private CheckinRewardProperties properties;
    private final Random random = new Random();

    public CheckinRewardConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() throws IOException {
        ClassPathResource resource = new ClassPathResource("checkin-reward.json");
        properties = objectMapper.readValue(resource.getInputStream(), CheckinRewardProperties.class);
        validateConfig();
        log.info("签到奖励配置加载成功");
    }

    private void validateConfig() {
        List<CheckinRewardProperties.RewardTier> rewards = properties.getRewards();
        if (rewards == null || rewards.isEmpty()) {
            throw new IllegalStateException("签到奖励配置为空");
        }
        
        int totalWeight = rewards.stream()
                .mapToInt(CheckinRewardProperties.RewardTier::getWeight)
                .sum();
        
        if (totalWeight != 100) {
            throw new IllegalStateException("签到奖励权重总和必须为100，当前为: " + totalWeight);
        }
        
        for (CheckinRewardProperties.RewardTier tier : rewards) {
            if (tier.getMinPoints() > tier.getMaxPoints()) {
                throw new IllegalStateException(
                    "签到奖励档位[" + tier.getName() + "]的最小积分不能大于最大积分"
                );
            }
        }
    }

    public int getRandomReward() {
        List<CheckinRewardProperties.RewardTier> rewards = properties.getRewards();
        int randomValue = random.nextInt(100);
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
        return min + random.nextInt(max - min + 1);
    }
}
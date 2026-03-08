package top.enderliquid.audioflow.dto.response.checkin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CheckinResultVO {
    private Integer rewardPoints;
    private Integer totalDays;
    private Integer continuousDays;
    private Integer maxContinuous;
}
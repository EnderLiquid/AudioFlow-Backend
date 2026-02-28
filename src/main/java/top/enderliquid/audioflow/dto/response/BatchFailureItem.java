package top.enderliquid.audioflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BatchFailureItem {
    private Integer index;
    private Long id;
    private String reason;
}
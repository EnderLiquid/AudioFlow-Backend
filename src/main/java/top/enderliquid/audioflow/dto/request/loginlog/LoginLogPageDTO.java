package top.enderliquid.audioflow.dto.request.loginlog;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录流水分页查询DTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginLogPageDTO {
    // null: 默认为 1
    @Min(value = 1, message = "页码必须大于0")
    private Long pageIndex;

    // null: 默认为 10
    @Min(value = 1, message = "分页大小必须大于0")
    private Long pageSize;
}
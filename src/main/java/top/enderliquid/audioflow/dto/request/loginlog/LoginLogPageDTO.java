package top.enderliquid.audioflow.dto.request.loginlog;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;

import static top.enderliquid.audioflow.common.constant.ValidationConstants.PAGE_INDEX_MIN;
import static top.enderliquid.audioflow.common.constant.ValidationConstants.PAGE_SIZE_MIN;

/**
 * 登录流水分页查询DTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginLogPageDTO {

    @Nullable
    @Min(value = PAGE_INDEX_MIN, message = "页码不能小于{value}")
    private Long pageIndex;

    @Nullable
    @Min(value = PAGE_SIZE_MIN, message = "分页大小不能小于{value}")
    private Long pageSize;

    @Nullable
    private Boolean asc;
}
package top.enderliquid.audioflow.dto.request.loginlog;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import static top.enderliquid.audioflow.common.constant.ValidationConstants.PAGE_INDEX_MIN;
import static top.enderliquid.audioflow.common.constant.ValidationConstants.PAGE_SIZE_MIN;

/**
 * 登录流水分页查询DTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginLogPageDTO {
    // null: 默认为 1
    @Min(value = PAGE_INDEX_MIN, message = "页码必须不小于" + PAGE_INDEX_MIN)
    private Long pageIndex;

    // null: 默认为 10
    @Min(value = PAGE_SIZE_MIN, message = "分页大小必须不小于" + PAGE_SIZE_MIN)
    private Long pageSize;
}
package top.enderliquid.audioflow.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import top.enderliquid.audioflow.dto.request.loginlog.LoginLogPageDTO;
import top.enderliquid.audioflow.dto.response.PageVO;
import top.enderliquid.audioflow.dto.response.loginlog.LoginLogVO;

@Validated
public interface LoginLogService {
    /**
     * 分页查询登录流水
     * @param userId 用户ID
     * @param dto 分页参数
     * @return 分页结果
     */
    PageVO<LoginLogVO> page(@NotNull(message = "用户ID不能为空") Long userId,
                            @Valid LoginLogPageDTO dto);
}
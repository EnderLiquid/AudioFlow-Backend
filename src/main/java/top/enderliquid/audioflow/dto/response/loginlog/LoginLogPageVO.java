package top.enderliquid.audioflow.dto.response.loginlog;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import top.enderliquid.audioflow.dto.response.PageResult;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginLogPageVO {
    private PageResult<LoginLogVO> result;
}
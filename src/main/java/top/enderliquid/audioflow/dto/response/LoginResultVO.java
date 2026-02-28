package top.enderliquid.audioflow.dto.response;

import cn.dev33.satoken.stp.SaTokenInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResult {
    private UserVO user;
    private SaTokenInfo tokenInfo;
}

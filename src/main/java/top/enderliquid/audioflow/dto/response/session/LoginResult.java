package top.enderliquid.audioflow.dto.response.session;

import cn.dev33.satoken.stp.SaTokenInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import top.enderliquid.audioflow.dto.response.user.UserVO;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResult {
    private UserVO user;
    private SaTokenInfo tokenInfo;
}

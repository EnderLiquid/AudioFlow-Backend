package top.enderliquid.audioflow.service;

import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import top.enderliquid.audioflow.dto.request.UserSaveDTO;
import top.enderliquid.audioflow.dto.request.UserVerifyPasswordDTO;
import top.enderliquid.audioflow.dto.response.UserVO;

@Validated
public interface UserService {
    UserVO saveUser(@Valid UserSaveDTO dto);

    UserVO saveAdminUser(@Valid UserSaveDTO dto);

    UserVO verifyUserPassword(@Valid UserVerifyPasswordDTO dto);

    UserVO getUser(Long userId);
}

package top.enderliquid.audioflow.service;

import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import top.enderliquid.audioflow.dto.request.session.LoginContext;
import top.enderliquid.audioflow.dto.request.user.UserLoginDTO;
import top.enderliquid.audioflow.dto.response.user.UserVO;

@Validated
public interface SessionService {
    UserVO login(@Valid UserLoginDTO dto, @Valid LoginContext context);
}
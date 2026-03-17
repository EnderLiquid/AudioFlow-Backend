package top.enderliquid.audioflow.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import top.enderliquid.audioflow.common.exception.BusinessException;
import top.enderliquid.audioflow.dto.request.session.LoginContext;
import top.enderliquid.audioflow.dto.request.user.UserLoginDTO;
import top.enderliquid.audioflow.dto.response.user.UserVO;
import top.enderliquid.audioflow.entity.LoginLog;
import top.enderliquid.audioflow.entity.User;
import top.enderliquid.audioflow.manager.DauManager;
import top.enderliquid.audioflow.manager.LoginLogManager;
import top.enderliquid.audioflow.manager.UserManager;
import top.enderliquid.audioflow.service.SessionService;

import java.time.LocalDate;

@Slf4j
@Service
public class SessionServiceImpl implements SessionService {

    @Autowired
    private UserManager userManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DauManager dauManager;

    @Autowired
    private LoginLogManager loginLogManager;

    @Override
    public UserVO login(UserLoginDTO dto, LoginContext context) {
        log.info("用户请求登录，邮箱: {}", dto.getEmail());
        User user = userManager.getByEmail(dto.getEmail());
        LoginLog loginLog = new LoginLog();
        BeanUtils.copyProperties(context, loginLog);

        // 用户不存在
        if (user == null) {
            loginLogManager.addRecord(null, dto.getEmail(), false, "用户不存在", context);
            throw new BusinessException("用户名或密码错误");
        }

        // 密码错误
        if (!passwordEncoder.matches(
                dto.getPassword(), // 明文
                user.getPassword() // 密文
        )) {
            loginLogManager.addRecord(user.getId(), dto.getEmail(), false, "密码错误", context);
            throw new BusinessException("用户名或密码错误");
        }

        // 登录成功
        loginLogManager.addRecord(user.getId(), dto.getEmail(), true, null, context);
        dauManager.recordDau(user.getId(), LocalDate.now());

        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        log.info("用户登录成功，用户ID: {}", user.getId());
        return userVO;
    }
}
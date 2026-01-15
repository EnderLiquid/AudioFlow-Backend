package top.enderliquid.audioflow.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import top.enderliquid.audioflow.common.constant.UserConstant;
import top.enderliquid.audioflow.common.exception.BusinessException;
import top.enderliquid.audioflow.common.transaction.TransactionHelper;
import top.enderliquid.audioflow.dto.request.UserSaveDTO;
import top.enderliquid.audioflow.dto.request.UserVerifyPasswordDTO;
import top.enderliquid.audioflow.dto.response.UserVO;
import top.enderliquid.audioflow.entity.User;
import top.enderliquid.audioflow.manager.UserManager;
import top.enderliquid.audioflow.service.UserService;

//Todo:写日志

@Slf4j
@Service
public class UserServiceImpl implements UserService {
    @Value("${user.password.bcrypt.work-factor}")
    private int bcryptWorkFactor;

    @Autowired
    private TransactionHelper transactionHelper;

    @Autowired
    private UserManager userManager;

    @Override
    public UserVO saveUser(UserSaveDTO dto) {
        User user = new User();
        user.setEmail(dto.getEmail());
        user.setName(dto.getName());
        String encryptedPassword = BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt(bcryptWorkFactor));
        user.setPassword(encryptedPassword);
        user.setRole(UserConstant.Role.User);
        return doSaveUser(user);
    }

    @Override
    public UserVO saveAdminUser(UserSaveDTO dto) {
        User user = new User();
        user.setEmail(dto.getEmail());
        user.setName(dto.getName());
        String encryptedPassword = BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt(bcryptWorkFactor));
        user.setPassword(encryptedPassword);
        user.setRole(UserConstant.Role.Admin);
        return doSaveUser(user);
    }

    private UserVO doSaveUser(User user) {
        transactionHelper.execute(() -> {
            // 检查邮箱是否已存在
            if (userManager.existsByEmail(user.getEmail())) {
                throw new BusinessException("邮箱已被注册");
            }
            // 创建用户
            boolean isSuccessful = userManager.save(user);
            if (!isSuccessful) {
                throw new BusinessException("用户创建失败");
            }
        });
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public UserVO verifyUserPassword(UserVerifyPasswordDTO dto) {
        User user = userManager.getByEmail(dto.getEmail());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (!BCrypt.checkpw(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("密码错误");
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }
}
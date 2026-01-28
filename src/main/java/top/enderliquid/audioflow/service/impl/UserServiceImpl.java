package top.enderliquid.audioflow.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import top.enderliquid.audioflow.common.constant.UserConstant;
import top.enderliquid.audioflow.common.exception.BusinessException;
import top.enderliquid.audioflow.dto.request.UserSaveDTO;
import top.enderliquid.audioflow.dto.request.UserVerifyPasswordDTO;
import top.enderliquid.audioflow.dto.response.UserVO;
import top.enderliquid.audioflow.entity.User;
import top.enderliquid.audioflow.manager.UserManager;
import top.enderliquid.audioflow.service.UserService;

@Slf4j
@Service
public class UserServiceImpl implements UserService {
    @Value("${password.encrypt.bcrypt.work-factor}")
    private int bcryptWorkFactor;

    @Autowired
    private UserManager userManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Override
    public UserVO saveUser(UserSaveDTO dto) {
        log.info("请求注册普通用户，邮箱：{}", dto.getEmail());
        User user = new User();
        user.setEmail(dto.getEmail());
        user.setName(dto.getName());
        String encryptedPassword = BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt(bcryptWorkFactor));
        user.setPassword(encryptedPassword);
        user.setRole(UserConstant.Role.USER);
        return doSaveUser(user);
    }

    @Override
    public UserVO saveAdminUser(UserSaveDTO dto) {
        log.info("请求注册管理员用户，邮箱：{}", dto.getEmail());
        User user = new User();
        user.setEmail(dto.getEmail());
        user.setName(dto.getName());
        String encryptedPassword = BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt(bcryptWorkFactor));
        user.setPassword(encryptedPassword);
        user.setRole(UserConstant.Role.ADMIN);
        return doSaveUser(user);
    }

    private UserVO doSaveUser(User user) {
        transactionTemplate.execute((status) -> {
            // 检查邮箱是否已存在
            if (userManager.existsByEmail(user.getEmail())) {
                throw new BusinessException("邮箱已被注册");
            }
            // 创建用户
            if (!userManager.save(user)) {
                throw new BusinessException("用户创建失败");
            }
            return null;
        });
        log.info("用户注册成功，邮箱：{}，用户ID：{}", user.getEmail(), user.getId());
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public UserVO verifyUserPassword(UserVerifyPasswordDTO dto) {
        log.info("请求校验用户密码，邮箱：{}", dto.getEmail());
        User user = userManager.getByEmail(dto.getEmail());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (!BCrypt.checkpw(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("密码错误");
        }
        log.info("用户密码校验成功，邮箱：{}，用户ID：{}", user.getEmail(), user.getId());
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public UserVO getUser(Long userId) {
        log.info("请求获取用户ID为 {} 的用户信息", userId);
        User user = userManager.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }
}
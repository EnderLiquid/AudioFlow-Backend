package top.enderliquid.audioflow.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import top.enderliquid.audioflow.common.enums.Role;
import top.enderliquid.audioflow.common.exception.BusinessException;
import top.enderliquid.audioflow.common.transaction.TransactionHelper;
import top.enderliquid.audioflow.dto.request.user.UserSaveDTO;
import top.enderliquid.audioflow.dto.request.user.UserUpdatePasswordDTO;
import top.enderliquid.audioflow.dto.request.user.UserVerifyPasswordDTO;
import top.enderliquid.audioflow.dto.response.user.UserVO;
import top.enderliquid.audioflow.entity.User;
import top.enderliquid.audioflow.manager.PointsRecordManager;
import top.enderliquid.audioflow.manager.UserManager;
import top.enderliquid.audioflow.service.UserService;

import static top.enderliquid.audioflow.common.enums.PointsType.USER_REGISTER;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserManager userManager;

    @Autowired
    private PlatformTransactionManager txManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PointsRecordManager pointsRecordManager;

    @Value("${points.register}")
    private int pointsWhenRegister;

    @Override
    public UserVO saveUser(UserSaveDTO dto) {
        log.info("请求注册普通用户，邮箱: {}", dto.getEmail());
        return doSaveUser(dto, Role.USER);
    }

    @Override
    public UserVO saveAdminUser(UserSaveDTO dto) {
        log.info("请求注册管理员用户，邮箱: {}", dto.getEmail());
        return doSaveUser(dto, Role.ADMIN);
    }

    private UserVO doSaveUser(UserSaveDTO dto, Role role) {
        User user = new User();
        user.setEmail(dto.getEmail());
        user.setName(dto.getName());
        String encryptedPassword = passwordEncoder.encode(dto.getPassword());
        user.setPassword(encryptedPassword);
        user.setRole(role);
        user.setPoints(100);
        try (TransactionHelper tx = new TransactionHelper(txManager)) {
            // 检查邮箱是否已存在
            if (userManager.existsByEmail(user.getEmail())) {
                throw new BusinessException("邮箱已被注册");
            }
            // 创建用户
            if (!userManager.save(user)) {
                throw new BusinessException("用户创建失败");
            }
            pointsRecordManager.addRecord(user.getId(), pointsWhenRegister, pointsWhenRegister, USER_REGISTER, null);
            tx.commit();
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        log.info("注册用户成功，用户ID: {}", user.getId());
        return userVO;
    }

    @Override
    public UserVO verifyUserPassword(UserVerifyPasswordDTO dto) {
        log.info("请求校验用户密码，邮箱: {}", dto.getEmail());
        User user = userManager.getByEmail(dto.getEmail());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (!passwordEncoder.matches(
                dto.getPassword(), // 明文
                user.getPassword() // 密文
        )) {
            throw new BusinessException("密码错误");
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        log.info("用户密码校验成功，用户ID: {}", user.getId());
        return userVO;
    }

    @Override
    public UserVO getUser(Long userId) {
        log.info("请求获取用户信息，用户ID: {}", userId);
        User user = userManager.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        log.info("获取用户信息成功");
        return userVO;
    }

    @Override
    public UserVO updateUserPassword(UserUpdatePasswordDTO dto, Long userId) {
        log.info("请求更新用户密码，用户ID: {}", userId);
        if (dto.getNewPassword().equals(dto.getOldPassword())) {
            throw new BusinessException("新密码与旧密码相同");
        }
        User user;
        try (TransactionHelper tx = new TransactionHelper(txManager)) {
            user = userManager.getById(userId);
            if (user == null) {
                throw new BusinessException("用户不存在");
            }
            if (!passwordEncoder.matches(
                    dto.getOldPassword(), // 明文
                    user.getPassword() // 密文
            )) {
                throw new BusinessException("旧密码错误");
            }
            user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
            if (!userManager.updateById(user)) {
                throw new BusinessException("更新密码失败");
            }
            tx.commit();
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        log.info("更新用户密码成功");
        return userVO;
    }
}
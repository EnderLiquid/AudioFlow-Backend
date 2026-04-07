package top.enderliquid.audioflow.manager.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import top.enderliquid.audioflow.entity.User;
import top.enderliquid.audioflow.manager.UserManager;
import top.enderliquid.audioflow.mapper.UserMapper;

@Repository
public class UserManagerImpl extends ServiceImpl<UserMapper, User> implements UserManager {
    @Autowired
    private UserMapper userMapper;

    @Override
    public User getByEmail(String email) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getEmail, email);
        return getOne(wrapper);
    }

    @Override
    public boolean existsByEmail(String email) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getEmail, email);
        return exists(wrapper);
    }

    @Override
    public boolean existsById(Long Id) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getId, Id);
        return exists(wrapper);
    }

    @Override
    public boolean addPoints(Long userId, int delta) {
        return userMapper.addPoints(userId, delta) > 0;
    }

    @Override
    public User getByIdForUpdate(Long id) {
        return userMapper.selectByIdForUpdate(id);
    }
}

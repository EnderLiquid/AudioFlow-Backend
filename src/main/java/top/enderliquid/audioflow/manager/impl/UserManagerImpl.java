package top.enderliquid.audioflow.manager.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import top.enderliquid.audioflow.common.enums.PointsType;
import top.enderliquid.audioflow.entity.PointsRecord;
import top.enderliquid.audioflow.entity.User;
import top.enderliquid.audioflow.manager.UserManager;
import top.enderliquid.audioflow.mapper.PointsRecordMapper;
import top.enderliquid.audioflow.mapper.UserMapper;

@Repository
public class UserManagerImpl extends ServiceImpl<UserMapper, User> implements UserManager {
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PointsRecordMapper pointsRecordMapper;

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

    /*
     * 原子更新积分
     * 注意: User 对象需要手动更新
     */
    @Override
    public int addPoints(Long userId, int delta, PointsType type, Long refId) {
        int affected = userMapper.addPoints(userId, delta);
        if (affected <= 0) {
            return -1;
        }
        User user = userMapper.selectById(userId);
        int balance = user.getPoints();
        PointsRecord record = new PointsRecord();
        record.setUserId(userId);
        record.setDelta(delta);
        record.setBalance(balance);
        record.setType(type);
        record.setRefId(refId);
        pointsRecordMapper.insert(record);
        return balance;
    }

    @Override
    public User getByIdForUpdate(Long id) {
        return userMapper.selectByIdForUpdate(id);
    }
}
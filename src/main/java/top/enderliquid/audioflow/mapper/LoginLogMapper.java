package top.enderliquid.audioflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import top.enderliquid.audioflow.entity.LoginLog;

/**
 * 登录流水 Mapper
 */
@Mapper
public interface LoginLogMapper extends BaseMapper<LoginLog> {
}
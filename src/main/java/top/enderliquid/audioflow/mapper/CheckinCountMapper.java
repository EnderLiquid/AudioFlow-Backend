package top.enderliquid.audioflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import top.enderliquid.audioflow.entity.CheckinCount;

/**
 * 日签到数统计 Mapper
 */
@Mapper
public interface CheckinCountMapper extends BaseMapper<CheckinCount> {
}
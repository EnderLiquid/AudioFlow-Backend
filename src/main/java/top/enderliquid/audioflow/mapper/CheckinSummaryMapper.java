package top.enderliquid.audioflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.data.repository.query.Param;
import top.enderliquid.audioflow.entity.CheckinSummary;

@Mapper
public interface CheckinSummaryMapper extends BaseMapper<CheckinSummary> {
    @Select("SELECT * FROM user_checkin_summary WHERE user_id = #{userId} FOR UPDATE")
    CheckinSummary getByUserIdForUpdate(@Param("userId") Long userId);
}
package top.enderliquid.audioflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import top.enderliquid.audioflow.entity.User;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    @Update("UPDATE user SET points = points + #{delta} WHERE id = #{userId} AND points + #{delta} >= 0")
    int addPoints(@Param("userId") Long userId, @Param("delta") int delta);

    @Select("SELECT * FROM user WHERE id = #{id} FOR UPDATE")
    User selectByIdForUpdate(Long id);
}

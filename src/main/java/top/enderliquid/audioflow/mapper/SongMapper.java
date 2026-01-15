package top.enderliquid.audioflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import top.enderliquid.audioflow.bo.SongBO;
import top.enderliquid.audioflow.entity.Song;

@Mapper
public interface SongMapper extends BaseMapper<Song> {
    IPage<SongBO> selectPageByUploaderInfoOrSongInfo(
            IPage<SongBO> page,
            @Param("uploaderId") Long uploaderId,
            @Param("uploaderKeyword") String uploaderKeyword,
            @Param("songId") Long songId,
            @Param("songKeyword") String songKeyword,
            @Param("isAsc") boolean isAsc
    );
}

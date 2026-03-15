package top.enderliquid.audioflow.manager.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import top.enderliquid.audioflow.common.enums.SongStatus;
import top.enderliquid.audioflow.common.util.id.SnowflakeIdConverter;
import top.enderliquid.audioflow.dto.bo.SongBO;
import top.enderliquid.audioflow.entity.Song;
import top.enderliquid.audioflow.manager.SongManager;
import top.enderliquid.audioflow.mapper.SongMapper;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class SongManagerImpl extends ServiceImpl<SongMapper, Song> implements SongManager {
    @Autowired
    private SongMapper songMapper;

    @Autowired
    private SnowflakeIdConverter snowflakeIdConverter;

    @Override
    public IPage<SongBO> pageByUploaderKeywordAndSongKeyword(
            String uploaderKeyword,
            String songKeyword,
            Boolean asc,
            Long pageIndex,
            Long pageSize
    ) {
        Long uploaderId = snowflakeIdConverter.fromString(uploaderKeyword);
        Long songId = snowflakeIdConverter.fromString(songKeyword);
        Page<SongBO> page = new Page<>(pageIndex, pageSize);
        // 实际上无需赋值，page的值也会被修改
        // 返回page本身，不会返回null
        page = (Page<SongBO>) songMapper.selectPageByUploaderInfoOrSongInfo(
                page,
                uploaderKeyword,
                uploaderId,
                songKeyword,
                songId,
                asc
        );
        return page;
    }

    @Override
    public List<Song> listByStatusesAndBeforeTime(List<SongStatus> statuses, LocalDateTime time) {
        LambdaQueryWrapper<Song> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Song::getStatus, statuses)
                .lt(Song::getCreateTime, time);
        return this.list(queryWrapper);
    }

    @Override
    public Song getByIdForUpdate(Long id) {
        return songMapper.selectByIdForUpdate(id);
    }
}
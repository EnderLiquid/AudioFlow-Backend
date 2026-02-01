package top.enderliquid.audioflow.manager.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import top.enderliquid.audioflow.common.util.id.SnowflakeIdConverter;
import top.enderliquid.audioflow.dto.bo.SongBO;
import top.enderliquid.audioflow.dto.param.SongPageParam;
import top.enderliquid.audioflow.entity.Song;
import top.enderliquid.audioflow.manager.SongManager;
import top.enderliquid.audioflow.mapper.SongMapper;

@Repository
public class SongManagerImpl extends ServiceImpl<SongMapper, Song> implements SongManager {
    @Autowired
    private SongMapper songMapper;

    @Autowired
    private SnowflakeIdConverter snowflakeIdConverter;

    @Override
    public IPage<SongBO> pageByUploaderKeywordAndSongKeyword(SongPageParam param) {
        Long uploaderId = snowflakeIdConverter.fromString(param.getUploaderKeyword());
        Long songId = snowflakeIdConverter.fromString(param.getSongKeyword());
        Page<SongBO> page = new Page<>(param.getPageNum(), param.getPageSize());
        // 实际上无需赋值，page的值也会被修改
        // 返回page本身，不会返回null
        page = (Page<SongBO>) songMapper.selectPageByUploaderInfoOrSongInfo(
                page,
                param.getUploaderKeyword(),
                uploaderId,
                param.getSongKeyword(),
                songId,
                param.getIsAsc()
        );
        return page;
    }
}
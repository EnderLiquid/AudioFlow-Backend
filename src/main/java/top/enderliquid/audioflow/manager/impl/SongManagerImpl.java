package top.enderliquid.audioflow.manager.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import top.enderliquid.audioflow.bo.SongBO;
import top.enderliquid.audioflow.entity.Song;
import top.enderliquid.audioflow.manager.SongManager;
import top.enderliquid.audioflow.mapper.SongMapper;

@Repository
public class SongManagerImpl extends ServiceImpl<SongMapper, Song> implements SongManager {
    @Autowired
    private SongMapper songMapper;

    @Override
    public IPage<SongBO> pageByUploaderKeywordAndSongKeyword(String uploaderKeyword, String songKeyword, boolean isAsc, long pageNum, long pageSize) {
        Page<SongBO> page = new Page<SongBO>();
        page.setCurrent(pageNum);
        page.setSize(pageSize);
        return null;
    }
}
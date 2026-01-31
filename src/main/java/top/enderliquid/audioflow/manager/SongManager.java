package top.enderliquid.audioflow.manager;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import top.enderliquid.audioflow.dto.bo.SongBO;
import top.enderliquid.audioflow.entity.Song;

public interface SongManager extends IService<Song> {
    IPage<SongBO> pageByUploaderKeywordAndSongKeyword(String uploaderKeyword, String songKeyword, boolean isAsc, long pageNum, long pageSize);
}

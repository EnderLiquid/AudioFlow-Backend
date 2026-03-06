package top.enderliquid.audioflow.manager;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import top.enderliquid.audioflow.common.enums.SongStatus;
import top.enderliquid.audioflow.dto.bo.SongBO;
import top.enderliquid.audioflow.dto.param.SongPageParam;
import top.enderliquid.audioflow.entity.Song;

import java.time.LocalDateTime;
import java.util.List;

public interface SongManager extends IService<Song> {
    IPage<SongBO> pageByUploaderKeywordAndSongKeyword(SongPageParam param);

    List<Song> listByStatusAndBeforeTime(SongStatus status, LocalDateTime time);
}

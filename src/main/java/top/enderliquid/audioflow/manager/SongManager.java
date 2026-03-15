package top.enderliquid.audioflow.manager;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import top.enderliquid.audioflow.common.enums.SongStatus;
import top.enderliquid.audioflow.dto.bo.SongBO;
import top.enderliquid.audioflow.entity.Song;

import java.time.LocalDateTime;
import java.util.List;

public interface SongManager extends IService<Song> {
    IPage<SongBO> pageByUploaderKeywordAndSongKeyword(
            String uploaderKeyword,
            String songKeyword,
            Boolean asc,
            Long pageIndex,
            Long pageSize
    );

    List<Song> listByStatusesAndBeforeTime(List<SongStatus> statuses, LocalDateTime time);

    Song getByIdForUpdate(Long songId);
}

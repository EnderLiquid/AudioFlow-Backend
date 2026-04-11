package top.enderliquid.audioflow.dto.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;

/**
 * 歌曲分页查询参数包装类
 * 用于Manager层向Mapper层传递分页查询参数
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SongPageParam {
    /**
     * 上传者关键字（用户名模糊匹配）
     */
    @Nullable
    private String uploaderKeyword;

    /**
     * 上传者ID（精确匹配）
     */
    @Nullable
    private Long uploaderId;

    /**
     * 歌曲关键字（歌名模糊匹配）
     */
    @Nullable
    private String songKeyword;

    /**
     * 歌曲ID（精确匹配）
     */
    @Nullable
    private Long songId;

    /**
     * 是否升序排序
     */
    private boolean asc;
}
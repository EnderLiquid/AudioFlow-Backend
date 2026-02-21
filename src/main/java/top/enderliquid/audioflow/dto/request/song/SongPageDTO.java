package top.enderliquid.audioflow.dto.request.song;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SongPageDTO {
    // null: 不限制用户关键词
    @Size(min = 1, max = 64, message = "关键字长度必须在1-64个字符之间")
    private String uploaderKeyword;

    // null: 不限制歌曲关键词
    @Size(min = 1, max = 64, message = "关键字长度必须在1-64个字符之间")
    private String songKeyword;

    // null: 默认 false ，倒序
    private Boolean isAsc;

    // null: 默认 1
    @Min(value = 1, message = "页码必须大于0")
    private Long pageIndex;

    // null: 默认 10
    @Min(value = 1, message = "分页大小必须大于0")
    private Long pageSize;
}
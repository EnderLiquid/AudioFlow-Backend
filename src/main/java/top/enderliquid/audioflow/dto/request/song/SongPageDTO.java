package top.enderliquid.audioflow.dto.request.song;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import static top.enderliquid.audioflow.common.constant.FieldLengthConstants.SONG_NAME_MAX;
import static top.enderliquid.audioflow.common.constant.FieldLengthConstants.SONG_NAME_MIN;
import static top.enderliquid.audioflow.common.constant.ValidationConstants.PAGE_INDEX_MIN;
import static top.enderliquid.audioflow.common.constant.ValidationConstants.PAGE_SIZE_MIN;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SongPageDTO {
    // null: 不限制用户关键词
    @Size(min = SONG_NAME_MIN, max = SONG_NAME_MAX, message = "关键字长度必须在" + SONG_NAME_MIN + "-" + SONG_NAME_MAX + "个字符之间")
    private String uploaderKeyword;

    // null: 不限制歌曲关键词
    @Size(min = SONG_NAME_MIN, max = SONG_NAME_MAX, message = "关键字长度必须在" + SONG_NAME_MIN + "-" + SONG_NAME_MAX + "个字符之间")
    private String songKeyword;

    // null: 默认 false ，倒序
    private Boolean asc;

    // null: 默认 1
    @Min(value = PAGE_INDEX_MIN, message = "页码必须不小于" + PAGE_INDEX_MIN)
    private Long pageIndex;

    // null: 默认 10
    @Min(value = PAGE_SIZE_MIN, message = "分页大小必须不小于" + PAGE_SIZE_MIN)
    private Long pageSize;
}
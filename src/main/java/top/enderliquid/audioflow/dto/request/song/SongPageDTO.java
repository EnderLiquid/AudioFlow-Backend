package top.enderliquid.audioflow.dto.request.song;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;

import static top.enderliquid.audioflow.common.constant.FieldLengthConstants.SONG_NAME_MAX;
import static top.enderliquid.audioflow.common.constant.FieldLengthConstants.SONG_NAME_MIN;
import static top.enderliquid.audioflow.common.constant.ValidationConstants.PAGE_INDEX_MIN;
import static top.enderliquid.audioflow.common.constant.ValidationConstants.PAGE_SIZE_MIN;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SongPageDTO {
    @Nullable
    @Size(min = SONG_NAME_MIN, max = SONG_NAME_MAX, message = "关键字长度必须在" + SONG_NAME_MIN + "-" + SONG_NAME_MAX + "个字符之间")
    private String uploaderKeyword;

    @Nullable
    @Size(min = SONG_NAME_MIN, max = SONG_NAME_MAX, message = "关键字长度必须在" + SONG_NAME_MIN + "-" + SONG_NAME_MAX + "个字符之间")
    private String songKeyword;

    @Nullable
    private Boolean asc;

    @Nullable
    @Min(value = PAGE_INDEX_MIN, message = "页码必须不小于" + PAGE_INDEX_MIN)
    private Long pageIndex;

    @Nullable
    @Min(value = PAGE_SIZE_MIN, message = "分页大小必须不小于" + PAGE_SIZE_MIN)
    private Long pageSize;
}
package top.enderliquid.audioflow.dto.request.song;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import static top.enderliquid.audioflow.common.constant.FieldLengthConstants.*;
import static top.enderliquid.audioflow.common.constant.ValidationConstants.FILE_SIZE_MIN;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SongPrepareUploadDTO {
    @NotBlank(message = "文件类型不能为空")
    private String mimeType;

    @NotNull(message = "文件大小不能为空")
    @Min(value = FILE_SIZE_MIN, message = "文件大小不能小于{value}")
    private Long size;

    @NotBlank(message = "歌曲名称不能为空")
    @Size(min = SONG_NAME_MIN, max = SONG_NAME_MAX, message = "歌曲名称长度必须在{min}-{max}个字符之间")
    private String name;

    @Size(min = SONG_DESCRIPTION_MIN, max = SONG_DESCRIPTION_MAX, message = "描述长度必须在{min}-{max}个字符之间")
    private String description;
}
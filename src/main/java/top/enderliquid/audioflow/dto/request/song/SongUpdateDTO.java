package top.enderliquid.audioflow.dto.request.song;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import static top.enderliquid.audioflow.common.constant.FieldLengthConstants.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SongUpdateDTO {
    @Size(min = SONG_NAME_MIN, max = SONG_NAME_MAX, message = "歌曲名称长度必须在{min}-{max}个字符之间")
    private String name;

    @Size(min = SONG_DESCRIPTION_MIN, max = SONG_DESCRIPTION_MAX, message = "描述长度必须在{min}-{max}个字符之间")
    private String description;
}

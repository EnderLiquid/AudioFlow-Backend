package top.enderliquid.audioflow.dto.request.song;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SongUpdateDTO {
    @Size(min = 1, max = 64, message = "歌曲名称长度必须在1-64个字符之间")
    private String name;

    @Size(min = 1, max = 128, message = "描述长度必须在1-128个字符之间")
    private String description;
}

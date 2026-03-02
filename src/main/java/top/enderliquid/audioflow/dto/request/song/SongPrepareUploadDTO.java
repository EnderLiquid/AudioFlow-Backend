package top.enderliquid.audioflow.dto.request.song;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SongPrepareUploadDTO {
    @NotBlank(message = "文件类型不能为空")
    private String mimeType;

    @NotNull(message = "文件大小不能为空")
    @Min(value = 1, message = "文件大小必须大于0")
    private Long size;

    @NotBlank(message = "歌曲名称不能为空")
    @Size(min = 1, max = 64, message = "歌曲名称长度必须在1-64个字符之间")
    private String name;

    @Size(min = 1, max = 128, message = "描述长度必须在1-128个字符之间")
    private String description;
}
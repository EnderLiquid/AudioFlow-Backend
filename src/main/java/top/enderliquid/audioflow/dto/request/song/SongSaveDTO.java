package top.enderliquid.audioflow.dto.request.song;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SongSaveDTO {
    @NotNull
    private MultipartFile file;

    // null: 使用文件名
    @Size(min = 1, max = 64, message = "歌曲名称长度必须在1-64个字符之间")
    private String name;

    @Size(min = 1, max = 128, message = "描述长度必须在1-128个字符之间")
    private String description;
}
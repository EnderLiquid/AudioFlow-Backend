package top.enderliquid.audioflow.dto.request;

import jakarta.validation.constraints.NotNull;
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
    private String name;

    // null: 保持为null
    private String description;
}
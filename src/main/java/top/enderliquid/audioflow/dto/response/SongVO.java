package top.enderliquid.audioflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SongVO {
    private Long id;
    String originName;
    String extension;
    private Long size;
    private String fileUrl;
    private LocalDateTime createTime;
    private Long duration;
    private Long uploaderId;
    private String uploaderName;
}
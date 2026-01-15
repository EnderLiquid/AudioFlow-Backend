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
    private String fileName;
    private Long size;
    private LocalDateTime uploadTime;
    private Long duration;
    private Long uploaderId;
    private String uploaderName;
}
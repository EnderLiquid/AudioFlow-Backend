package top.enderliquid.audioflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SongVO {
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long id;
    private String name;
    private String description;
    private String fileUrl;
    private Long size;
    private Long duration;
    private Long uploaderId;
    private String uploaderName;
}
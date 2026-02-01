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
    private String name;
    private String description;
    private Long size;
    private Long duration;
    private Long uploaderId;
    private String uploaderName;
    private LocalDateTime createTime;
}
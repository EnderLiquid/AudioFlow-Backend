package top.enderliquid.audioflow.dto.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SongBO {
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long id;
    private String name;
    private String description;
    private String fileName;
    private String sourceType;
    private Long size;
    private Long duration;
    private Long uploaderId;
    private String uploaderName;
}

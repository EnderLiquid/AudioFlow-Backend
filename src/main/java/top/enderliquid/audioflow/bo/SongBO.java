package top.enderliquid.audioflow.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SongBO {
    Long id;
    String originName;
    String extension;
    Long size;
    LocalDateTime uploadTime;
    Long duration;
    Long uploaderId;
    String uploaderName;
}

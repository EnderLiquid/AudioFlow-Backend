package top.enderliquid.audioflow.dto.response.song;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SongPrepareUploadVO {
    private Long id;
    private String fileName;
    private String uploadUrl;
}
package top.enderliquid.audioflow.dto.response.song;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import top.enderliquid.audioflow.dto.response.PageResult;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SongPageVO {
    private PageResult<SongVO> result;
}
package top.enderliquid.audioflow.dto.param;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SongPageParam {
    private String uploaderKeyword;
    private String songKeyword;
    private Boolean isAsc;
    private Long pageIndex;
    private Long pageSize;
}
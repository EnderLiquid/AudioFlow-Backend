package top.enderliquid.audioflow.dto.request;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SongPageDTO {
    //null或空:不限制用户关键词
    String uploaderKeyword;

    //null或空:不限制歌曲关键词
    String songKeyword;

    //null:默认false，倒序
    Boolean isAsc;

    //null:默认1
    @Min(value = 0, message = "页码不能为负数")
    Long pageNum;

    //null:默认10
    @Min(value = 1, message = "分页大小必须大于0")
    Long pageSize;
}
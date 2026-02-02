package top.enderliquid.audioflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommonPageVO<T> {
    private List<T> list;
    private Long total;
    private Long pageIndex;
    private Long pageSize;
}

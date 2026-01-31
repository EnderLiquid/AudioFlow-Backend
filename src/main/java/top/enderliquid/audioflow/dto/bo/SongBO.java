package top.enderliquid.audioflow.dto.bo;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
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
    String sourceType;
    Long size;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    Long duration;
    Long uploaderId;
    String uploaderName;
}

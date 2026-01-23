package top.enderliquid.audioflow.common.util.id;

import org.springframework.lang.Nullable;

import java.io.Serializable;

public interface IdConverter<T extends Serializable> {
    /**
     * 将字符串转换为ID，转换失败返回null
     *
     * @param s 待处理的字符串
     * @return ID或null
     */
    @Nullable
    public T fromString(String s);
}

package top.enderliquid.audioflow.manager;


import com.baomidou.mybatisplus.extension.service.IService;
import top.enderliquid.audioflow.entity.User;

public interface UserManager extends IService<User> {
    User getByEmail(String email);

    boolean existsByEmail(String email);
}

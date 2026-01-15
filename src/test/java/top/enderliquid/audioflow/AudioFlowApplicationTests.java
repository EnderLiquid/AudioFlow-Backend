package top.enderliquid.audioflow;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import top.enderliquid.audioflow.manager.UserManager;
import top.enderliquid.audioflow.dto.request.UserSaveDTO;
import top.enderliquid.audioflow.entity.User;
import top.enderliquid.audioflow.service.UserService;

import java.util.List;

@SpringBootTest
class AudioFlowApplicationTests {
    @Autowired
    private UserService userService;
    @Autowired
    private UserManager userManager;

    @Test
    public void saveTestUser() {
        UserSaveDTO dto = new UserSaveDTO();
        dto.setName("神人");
        dto.setEmail("114514@homo.com");
        dto.setPassword("1145141919");
        userService.saveUser(dto);
    }
    @Test
    public void listUsers() {
        List<User> list = userManager.list();
        list.forEach(System.out::println);
    }

}

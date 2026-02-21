package top.enderliquid.audioflow.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import top.enderliquid.audioflow.entity.Song;
import top.enderliquid.audioflow.entity.User;
import top.enderliquid.audioflow.manager.SongManager;
import top.enderliquid.audioflow.manager.UserManager;

import java.util.UUID;

@Component
public class TestDataHelper {

    @Autowired
    private UserManager userManager;

    @Autowired
    private SongManager songManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public void cleanDatabase() {
        songManager.lambdaUpdate().remove();
        userManager.lambdaUpdate().remove();
    }

    public User createTestUser() {
        User user = new User();
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        user.setName("test_user_" + uniqueId);
        user.setEmail("test_user_" + uniqueId + "@example.com");
        user.setPassword(passwordEncoder.encode("test_password_123"));
        userManager.save(user);
        return user;
    }

    public Song createTestSong(Long userId) {
        Song song = new Song();
        song.setName("Test Song");
        song.setDescription("Test Description");
        song.setFileName("test-song.mp3");
        song.setSourceType("local");
        song.setSize(1024L);
        song.setDuration(180L);
        song.setUploaderId(userId);
        songManager.save(song);
        return song;
    }
}

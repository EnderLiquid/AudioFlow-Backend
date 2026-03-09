package top.enderliquid.audioflow.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import top.enderliquid.audioflow.common.enums.Role;
import top.enderliquid.audioflow.common.enums.SongStatus;
import top.enderliquid.audioflow.entity.Song;
import top.enderliquid.audioflow.entity.User;
import top.enderliquid.audioflow.manager.*;

import java.util.UUID;

@Component
public class TestDataHelper {
    private static final Logger log = LoggerFactory.getLogger(TestDataHelper.class);

    @Autowired
    private UserManager userManager;

    @Autowired
    private SongManager songManager;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PointsRecordManager pointsRecordManager;

    @Autowired
    private CheckinLogManager checkinLogManager;

    @Autowired
    private CheckinSummaryManager checkinSummaryManager;

    public void cleanAll() {
        long startTime = System.currentTimeMillis();
        try {
            cleanDatabase();
            RedisConnectionFactory connectionFactory = redisTemplate.getConnectionFactory();
            if (connectionFactory != null) {
                redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
            } else {
                throw new RuntimeException("获取redis连接失败");
            }
            long duration = System.currentTimeMillis() - startTime;
            log.debug("清理所有测试数据完成，耗时: {}ms", duration);
        } catch (Exception e) {
            log.error("清理测试数据失败", e);
            throw new RuntimeException("清理测试数据失败", e);
        }
    }

    public void cleanDatabase() {
        songManager.lambdaUpdate().remove();
        userManager.lambdaUpdate().remove();
        pointsRecordManager.lambdaUpdate().remove();
        checkinLogManager.lambdaUpdate().remove();
        checkinSummaryManager.lambdaUpdate().remove();
    }

    public User createTestUser() {
        User user = new User();
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        user.setName("test_user_" + uniqueId);
        user.setEmail("test_user_" + uniqueId + "@example.com");
        user.setPassword(passwordEncoder.encode("test_password_123"));
        user.setPoints(100);
        userManager.save(user);
        return user;
    }

    public User createTestAdmin() {
        User admin = new User();
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        admin.setName("admin_user_" + uniqueId);
        admin.setEmail("admin_user_" + uniqueId + "@example.com");
        admin.setPassword(passwordEncoder.encode("test_password_123"));
        admin.setRole(Role.ADMIN);
        admin.setPoints(100);
        userManager.save(admin);
        return admin;
    }

    public Song createTestSong(Long userId) {
        Song song = new Song();
        song.setName("Test Song");
        song.setDescription("Test Description");
        song.setFileName("test-song.mp3");
        song.setSize(1024L);
        song.setDuration(180L);
        song.setUploaderId(userId);
        song.setStatus(SongStatus.NORMAL);
        songManager.save(song);
        return song;
    }

    public Song createTestUploadingSong(Long userId) {
        Song song = new Song();
        song.setName("Test Uploading Song");
        song.setDescription("Test Uploading Description");
        song.setFileName("test-uploading-song.mp3");
        song.setSize(1024L);
        song.setDuration(180L);
        song.setUploaderId(userId);
        song.setStatus(SongStatus.UPLOADING);
        songManager.save(song);
        return song;
    }
}

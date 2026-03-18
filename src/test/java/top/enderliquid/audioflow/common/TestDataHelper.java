package top.enderliquid.audioflow.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import top.enderliquid.audioflow.common.enums.SongStatus;
import top.enderliquid.audioflow.entity.Song;
import top.enderliquid.audioflow.entity.User;
import top.enderliquid.audioflow.manager.SongManager;
import top.enderliquid.audioflow.manager.UserManager;

import java.util.List;
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
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

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
        // 禁用外键检查以避免约束错误
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        try {
            // 从元数据查询当前数据库的所有表名
            List<String> tables = jdbcTemplate.queryForList(
                "SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE()",
                String.class
            );
            // 批量清空所有表
            for (String table : tables) {
                jdbcTemplate.execute("TRUNCATE TABLE " + table);
            }
        } finally {
            // 重新启用外键检查
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
        }
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

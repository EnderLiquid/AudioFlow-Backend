-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `email` VARCHAR(255) NOT NULL COMMENT '邮箱',
    `name` VARCHAR(100) NOT NULL COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码',
    `role` VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '角色：USER, ADMIN',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_email` (`email`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 歌曲表
CREATE TABLE IF NOT EXISTS `song` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '歌曲ID',
    `origin_name` VARCHAR(255) NOT NULL COMMENT '原始文件名',
    `extension` VARCHAR(20) NOT NULL COMMENT '文件扩展名',
    `size` BIGINT NOT NULL COMMENT '文件大小（字节）',
    `upload_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    `duration` BIGINT NOT NULL DEFAULT 0 COMMENT '音频时长（毫秒）',
    `uploader_id` BIGINT NOT NULL COMMENT '上传者ID',
    PRIMARY KEY (`id`),
    INDEX `idx_upload_time` (`upload_time`),
    INDEX `idx_uploader_id` (`uploader_id`),
    -- 创建外键约束
    CONSTRAINT `fk_song_user`
    FOREIGN KEY (`uploader_id`)
    REFERENCES `user` (`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='歌曲表';

-- 查看表结构
SHOW CREATE TABLE `user`;
SHOW CREATE TABLE `song`;
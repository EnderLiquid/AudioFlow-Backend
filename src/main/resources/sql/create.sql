-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `email` VARCHAR(255) NOT NULL COMMENT '邮箱',
    `name` VARCHAR(255) NOT NULL COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码',
    `role` VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '角色: USER, ADMIN',
    `points` INT NOT NULL DEFAULT 100 COMMENT '积分余额',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `update_time` DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_email` (`email`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 歌曲表
CREATE TABLE IF NOT EXISTS `song` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '歌曲ID',
    `name` VARCHAR(255) NOT NULL COMMENT '歌曲名称',
    `description` VARCHAR(255) COMMENT '歌曲描述',
    `file_name` VARCHAR(255) NOT NULL COMMENT '文件存储名',
    `size` BIGINT COMMENT '文件大小（字节）',
    `duration` BIGINT COMMENT '音频时长（毫秒）',
    `uploader_id` BIGINT NOT NULL COMMENT '上传者ID',
    `status` VARCHAR(20) NOT NULL DEFAULT 'UPLOADING' COMMENT '歌曲状态: UPLOADING, NORMAL',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `update_time` DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_create_time` (`create_time`),
    INDEX `idx_uploader_id` (`uploader_id`),
    INDEX `idx_status_create_time` (`status`, `create_time`),
    -- 创建外键约束
    CONSTRAINT `fk_song_user`
    FOREIGN KEY (`uploader_id`)
    REFERENCES `user` (`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='歌曲表';

-- 积分流水表
CREATE TABLE IF NOT EXISTS `points_record`(
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `delta` INT NOT NULL COMMENT '变动数量',
    `balance` INT NOT NULL COMMENT '变动后余额',
    `type` VARCHAR(30) NOT NULL COMMENT '业务类型',
    `ref_id` BIGINT COMMENT '关联业务ID',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id_create_time` (`user_id`, `create_time`)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci COMMENT ='积分流水表';

-- 签到记录表
CREATE TABLE IF NOT EXISTS `user_checkin_log`(
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `checkin_date` DATE NOT NULL COMMENT '签到日期',
    `create_time` DATETIME NOT NULL COMMENT '实际签到时间',
    `reward_points` INT NOT NULL DEFAULT 0 COMMENT '签到奖励积分',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_date` (`user_id`,`checkin_date`),
    KEY `idx_user_id` (`user_id`)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci COMMENT ='签到记录表';

-- 签到统计表
CREATE TABLE `user_checkin_summary` (
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `total_days` INT NOT NULL DEFAULT 0 COMMENT '累计签到天数',
    `continuous_days` INT NOT NULL DEFAULT 0 COMMENT '当前连续签到天数',
    `max_continuous` INT NOT NULL DEFAULT 0 COMMENT '历史最大连续签到天数',
    `last_checkin_date` DATE DEFAULT NULL COMMENT '最后一次签到日期',
    `update_time` DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`user_id`)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci COMMENT ='签到统计表';

-- 查看表结构
SHOW CREATE TABLE `user`;
SHOW CREATE TABLE `song`;
SHOW CREATE TABLE `points_record`;
SHOW CREATE TABLE `user_checkin_log`;
SHOW CREATE TABLE `user_checkin_summary`;
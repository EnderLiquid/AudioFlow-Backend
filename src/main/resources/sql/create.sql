-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `email` VARCHAR(255) NOT NULL COMMENT '邮箱',
    `name` VARCHAR(255) NOT NULL COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码',
    `points` INT NOT NULL COMMENT '积分余额',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
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
    `status` VARCHAR(20) NOT NULL DEFAULT 'UPLOADING' COMMENT '歌曲状态: UPLOADING, NORMAL, DELETING',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_create_time` (`create_time`),
    INDEX `idx_uploader_id` (`uploader_id`),
    INDEX `idx_status_create_time` (`status`, `create_time`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='歌曲表';

-- 积分流水表
CREATE TABLE IF NOT EXISTS `points_record`(
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `delta` INT NOT NULL COMMENT '变动数量',
    `balance` INT NOT NULL COMMENT '变动后余额',
    `type` VARCHAR(30) NOT NULL COMMENT '业务类型',
    `ref_id` BIGINT COMMENT '关联业务ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id_create_time` (`user_id`, `create_time`)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci COMMENT ='积分流水表';

-- 签到记录表
CREATE TABLE IF NOT EXISTS `checkin_log`(
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `checkin_date` DATE NOT NULL COMMENT '签到日期',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '实际签到时间',
    `reward_points` INT NOT NULL DEFAULT 0 COMMENT '签到奖励积分',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_date` (`user_id`,`checkin_date`),
    KEY `idx_user_id` (`user_id`)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci COMMENT ='签到记录表';

-- 签到统计表
CREATE TABLE IF NOT EXISTS `checkin_summary` (
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `total_days` INT NOT NULL DEFAULT 0 COMMENT '累计签到天数',
    `continuous_days` INT NOT NULL DEFAULT 0 COMMENT '当前连续签到天数',
    `max_continuous` INT NOT NULL DEFAULT 0 COMMENT '历史最大连续签到天数',
    `last_checkin_date` DATE COMMENT '最后一次签到日期',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`user_id`)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci COMMENT ='签到统计表';

-- 登录流水表
CREATE TABLE IF NOT EXISTS `login_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NULL COMMENT '用户ID，账号不存在时为null',
    `email` VARCHAR(255) NOT NULL COMMENT '尝试登录的邮箱',
    `success` TINYINT(1) NOT NULL COMMENT '是否登录成功',
    `fail_reason` VARCHAR(30) NULL COMMENT '失败原因: USER_NOT_FOUND, PASSWORD_WRONG',
    `ip` VARCHAR(45) NULL COMMENT 'IP地址',
    `device_type` VARCHAR(50) NULL COMMENT '设备类型',
    `os` VARCHAR(100) NULL COMMENT '操作系统',
    `browser` VARCHAR(100) NULL COMMENT '浏览器',
    `user_agent` VARCHAR(512) NULL COMMENT '完整的 User-Agent',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_create_time` (`create_time`)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci COMMENT ='登录流水表';

-- 日活统计表
CREATE TABLE IF NOT EXISTS `dau` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `date` DATE NOT NULL COMMENT '统计日期',
    `count` BIGINT NOT NULL DEFAULT 0 COMMENT '日活数',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_date` (`date`)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci COMMENT ='日活统计表';

-- 日签到数统计表
CREATE TABLE IF NOT EXISTS `checkin_count` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `date` DATE NOT NULL COMMENT '统计日期',
    `count` BIGINT NOT NULL DEFAULT 0 COMMENT '签到数',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_date` (`date`)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci COMMENT ='日签到数统计表';

-- 查看表结构
SHOW CREATE TABLE `user`;
SHOW CREATE TABLE `song`;
SHOW CREATE TABLE `points_record`;
SHOW CREATE TABLE `checkin_log`;
SHOW CREATE TABLE `checkin_summary`;
SHOW CREATE TABLE `login_log`;
SHOW CREATE TABLE `dau`;
SHOW CREATE TABLE `checkin_count`;
# 签到功能设计文档

## 一、需求概述

实现用户每日签到功能，用户每天可以签到一次，获得随机积分奖励。积分奖励基于概率分布，通过JSON配置文件管理，便于后续维护。

## 二、数据库设计

### 已有表结构

#### `user_checkin_log` - 签到记录表
- `id`: 记录ID（主键，自增）
- `user_id`: 用户ID
- `checkin_date`: 签到日期
- `create_time`: 实际签到时间
- `reward_points`: 签到奖励积分

#### `user_checkin_summary` - 签到统计表
- `user_id`: 用户ID（主键）
- `total_days`: 累计签到天数
- `continuous_days`: 当前连续签到天数
- `max_continuous`: 历史最大连续签到天数
- `last_checkin_date`: 最后一次签到日期
- `update_time`: 更新时间

## 三、积分奖励配置

### 配置文件格式
文件路径：`src/main/resources/checkin-reward.json`

```json
{
  "rewards": [
    {
      "name": "普通奖励",
      "weight": 60,
      "minPoints": 3,
      "maxPoints": 7
    },
    {
      "name": "良好奖励",
      "weight": 30,
      "minPoints": 8,
      "maxPoints": 12
    },
    {
      "name": "优秀奖励",
      "weight": 9,
      "minPoints": 15,
      "maxPoints": 25
    },
    {
      "name": "幸运奖励",
      "weight": 1,
      "minPoints": 50,
      "maxPoints": 100
    }
  ]
}
```

### 随机算法
1. 根据权重随机选择档位（如60%概率选第一个档位）
2. 在选中档位的 `minPoints` 和 `maxPoints` 之间随机取值（等概率）

## 四、架构设计

### 四层结构
```
Controller (CheckinController)
    ↓
Service (CheckinService)
    ↓
Manager (CheckinLogManager, CheckinSummaryManager, UserManager, PointsRecordManager)
    ↓
Mapper (CheckinLogMapper, CheckinSummaryMapper)
```

### 新增文件清单

**Entity层：**
- `CheckinLog.java` - 签到记录实体
- `CheckinSummary.java` - 签到统计实体

**Mapper层：**
- `CheckinLogMapper.java`
- `CheckinSummaryMapper.java`

**Manager层：**
- `CheckinLogManager.java` + `CheckinLogManagerImpl.java`
- `CheckinSummaryManager.java` + `CheckinSummaryManagerImpl.java`

**Service层：**
- `CheckinService.java` + `CheckinServiceImpl.java`

**Controller层：**
- `CheckinController.java`

**DTO层：**
- `CheckinResultVO.java` - 签到结果返回对象
- `CheckinStatusVO.java` - 签到状态返回对象
- `CheckinSummaryVO.java` - 签到统计返回对象

**配置：**
- `checkin-reward.json` - 积分奖励配置
- `CheckinRewardConfig.java` - 配置读取类
- `CheckinRewardProperties.java` - 配置属性类

## 五、API设计

### 1. 签到接口
```
POST /api/checkins
```
- 需要登录（`@SaCheckLogin`）
- 无请求参数
- 返回：签到结果（积分、连续天数等）

**响应示例：**
```json
{
  "success": true,
  "message": "签到成功",
  "data": {
    "rewardPoints": 5,
    "totalDays": 10,
    "continuousDays": 3,
    "maxContinuous": 5
  }
}
```

### 2. 查询今日签到状态
```
GET /api/checkins/today
```
- 需要登录
- 返回：今日是否已签到

**响应示例：**
```json
{
  "success": true,
  "data": {
    "checkedIn": false
  }
}
```

### 3. 查询签到统计
```
GET /api/checkins/summary
```
- 需要登录
- 返回：累计天数、连续天数、最大连续天数

**响应示例：**
```json
{
  "success": true,
  "data": {
    "totalDays": 10,
    "continuousDays": 3,
    "maxContinuous": 5
  }
}
```

## 六、核心业务逻辑

### 签到流程
1. 检查今日是否已签到
2. 根据配置计算随机积分
3. 开启事务：
   - 添加签到记录到 `user_checkin_log`
   - 更新签到统计 `user_checkin_summary`（累计+1，连续天数逻辑）
   - 更新用户积分
   - 添加积分流水记录
4. 返回签到结果

### 连续天数计算逻辑
- 如果 `last_checkin_date` 是昨天：`continuous_days + 1`
- 如果 `last_checkin_date` 不是昨天：`continuous_days = 1`
- 更新 `max_continuous`（如果当前连续天数 > 历史最大）

## 七、数据流

### 签到成功流程
```
用户请求 → Controller
  ↓
Service.checkin(userId)
  ↓
1. CheckinLogManager.getByUserIdAndDate(userId, today) 
   → 检查是否已签到
  ↓
2. CheckinRewardConfig.getRandomReward()
   → 计算随机积分
  ↓
3. TransactionHelper开启事务：
   a. CheckinLogManager.save(log)
   b. CheckinSummaryManager.updateSummary(...)
   c. UserManager.addPoints(userId, points)
   d. PointsRecordManager.save(...)
   e. tx.commit()
  ↓
返回签到结果
```

## 八、错误处理

### 业务异常
- 今日已签到 → `BusinessException("今日已签到")`
- 用户不存在 → `BusinessException("用户不存在")`

### 配置文件错误
- 配置文件不存在或格式错误 → 启动时失败（fail fast）
- 权重总和不为100 → 启动时校验失败

## 九、日志记录

### Service层日志
- 请求签到：`log.info("请求签到，用户ID: {}", userId)`
- 签到成功：`log.info("签到成功，获得积分: {}", points)`
- 业务失败：`throw new BusinessException("今日已签到")`

## 十、测试要点

1. **正常签到流程**：首次签到、连续签到、断签后重新签到
2. **重复签到**：同一天多次签到应返回错误
3. **积分计算**：验证随机积分在配置范围内
4. **连续天数**：验证连续签到、断签、重新签到的逻辑
5. **事务处理**：验证事务回滚和提交
6. **配置读取**：验证配置文件加载和随机算法

## 十一、注意事项

1. **事务管理**：使用 `TransactionHelper` 进行编程式事务管理，不使用 `@Transactional` 注解
2. **代码规范**：
   - 所有注释、日志、异常信息使用中文
   - 不使用 `var` 关键字，使用显式类型声明
   - DTO命名使用实体名作为前缀
3. **并发控制**：通过数据库唯一约束（`uk_user_date`）防止重复签到
4. **性能优化**：签到统计表使用 `user_id` 作为主键，查询效率高
# 日志清理定时任务模块

## 功能说明

本模块为taobao-xsd项目添加了自动日志清理功能，能够定时删除指定天数之前的日志文件，避免日志文件过多占用磁盘空间。

## 主要特性

1. **自动定时清理**：每天凌晨2点自动执行日志清理任务
2. **可配置保留天数**：默认保留30天的日志文件，可通过配置文件调整
3. **智能文件识别**：自动识别日志文件格式，包括压缩文件(.gz)
4. **手动触发功能**：提供REST API接口支持手动触发清理
5. **状态监控**：可查看当前日志文件状态和清理统计

## 配置说明

### application.yml 配置项

```yaml
# 日志清理配置
log:
  cleanup:
    # 日志文件保留天数，超过此天数的日志文件将被自动删除
    retention:
      days: 30
    # 是否启用定时清理
    enabled: true
    # 定时清理的cron表达式（默认每天凌晨2点执行）
    cron: "0 0 2 * * ?"

# 日志文件路径配置
logging:
  file:
    path: ${user.dir}/LOG/  # 日志文件存放目录
```

## 支持的日志文件格式

- `taobao.log` - 当前日志文件（不会被删除）
- `taobaoxsd.log` - 当前日志文件（不会被删除）
- `taobaoxsd.log.2025-09-10.gz` - 压缩的历史日志文件
- `taobao.log.2025-09-10` - 历史日志文件

## API接口

### 1. 手动触发日志清理

**接口地址**：`POST /api/log/cleanup`

**功能**：手动触发日志清理任务

**响应示例**：
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": "日志清理任务已执行完成"
}
```

### 2. 查看日志状态

**接口地址**：`GET /api/log/status`

**功能**：获取当前日志文件状态信息

**响应示例**：
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "logDirectory": "D:\\java\\taobao-xsd\\LOG\\",
    "retentionDays": 30,
    "currentTime": "2025-09-11 17:08:00",
    "directoryExists": true,
    "totalSize": 1048576,
    "currentLogCount": 5,
    "oldLogCount": 3,
    "cutoffDate": "2025-08-12 17:08:00",
    "logFiles": [
      {
        "name": "taobaoxsd.log.2025-09-10.gz",
        "size": 204800,
        "lastModified": "2025-09-10T23:59:59",
        "isOld": false
      }
    ]
  }
}
```

## 定时任务说明

- **执行时间**：每天凌晨2:00执行
- **Cron表达式**：`0 0 2 * * ?`
- **线程池配置**：使用2个线程的调度线程池，避免任务阻塞

## 日志输出

任务执行时会在应用日志中输出详细信息：

```
2025-09-11 02:00:00 INFO  LogCleanupJobService - 开始执行日志文件清理任务，保留天数：30天
2025-09-11 02:00:00 INFO  LogCleanupJobService - 将删除 2025-08-12 02:00:00 之前的日志文件
2025-09-11 02:00:00 INFO  LogCleanupJobService - 已删除过期日志文件：taobaoxsd.log.2025-08-10.gz, 大小：204800 bytes
2025-09-11 02:00:00 INFO  LogCleanupJobService - 日志清理任务完成，共删除 3 个文件，释放空间：614400 bytes
```

## 部署说明

1. 确保应用已启用定时任务功能（已在LogisticsApplication中添加@EnableScheduling注解）
2. 根据实际需求调整配置文件中的保留天数
3. 确保应用对日志目录有读写权限
4. 重新构建并部署应用

## 测试建议

1. 部署后可通过 `GET /api/log/status` 接口检查配置是否正确
2. 可通过 `POST /api/log/cleanup` 接口手动测试清理功能
3. 观察应用日志确认定时任务是否正常执行

## 注意事项

1. 清理操作是不可逆的，请确保保留天数配置合理
2. 当前正在使用的日志文件不会被删除
3. 建议在生产环境部署前先在测试环境验证功能
4. 如需修改执行时间，可调整配置文件中的cron表达式
# 快速启动指南

## 方法1：在IDEA中直接运行（最简单）

1. 在IDEA中打开项目
2. 等待IDEA自动下载依赖（右下角会显示进度）
3. 找到 `TimeCountApplication.java` 文件
4. 右键点击 → 选择 "Run 'TimeCountApplication'"
5. 等待程序启动，看到 "Started TimeCountApplication" 即成功

## 方法2：使用Maven命令

如果IDEA能成功下载依赖，可以使用命令行运行：

```bash
cd e:\work\code\time-count\time-count
mvn spring-boot:run
```

## 验证程序是否启动成功

程序启动后，访问：http://localhost:8080/api/workhours/health

应该看到：
```json
{
  "status": "UP",
  "service": "WorkHours Service"
}
```

## 使用示例

### 1. 生成当前月份的考勤模板

在浏览器或Postman中访问：
```
POST http://localhost:8080/api/workhours/template/generate
```

或使用curl：
```bash
curl -X POST http://localhost:8080/api/workhours/template/generate
```

这会在 `data` 目录下生成文件：`attendance_2025-10.xlsx`

### 2. 填写考勤数据

1. 打开 `data/attendance_2025-10.xlsx`
2. 在"上班时间"和"下班时间"列填写时间，格式：`08:56`
3. 保存文件

示例：
```
日期         星期    上班时间  下班时间  备注
2025-10-01  星期三   08:56    18:30
2025-10-02  星期四   09:05    19:20    加班
2025-10-03  星期五   08:50    18:00
2025-10-04  星期六                     周末
2025-10-05  星期日                     周末
2025-10-06  星期一   09:10    17:50    早退
```

### 3. 查看工时统计

在浏览器访问：
```
http://localhost:8080/api/workhours/report
```

或使用curl：
```bash
curl http://localhost:8080/api/workhours/report
```

## 常用API接口

### 生成模板
```bash
# 生成当前月份
POST http://localhost:8080/api/workhours/template/generate

# 生成指定月份
POST http://localhost:8080/api/workhours/template/generate?yearMonth=2025-11
```

### 计算工时（JSON格式）
```bash
# 当前月份
GET http://localhost:8080/api/workhours/calculate

# 指定月份
GET http://localhost:8080/api/workhours/calculate?yearMonth=2025-10
```

### 查看格式化报告
```bash
# 当前月份
GET http://localhost:8080/api/workhours/report

# 指定月份
GET http://localhost:8080/api/workhours/report?yearMonth=2025-10
```

## 工时计算规则

1. **基本公式**：工时 = 下班时间 - 上班时间 - 午休 - 晚餐（如适用）

2. **午休扣除**：所有情况扣除1小时

3. **晚餐扣除**：
   - 下班 < 19:00：不扣除
   - 下班 >= 19:00：扣除0.5小时

4. **请假判定**（仅工作日且在今天之前）：
   - 上下班时间为空
   - 上班时间 > 09:00
   - 下班时间 < 18:00

## 配置修改

如需修改默认配置，编辑 `src/main/resources/application.properties`：

```properties
# 期望总工时（默认220小时）
workhours.expected-total-hours=220.0

# 标准上班时间（默认9点）
workhours.standard-start-hour=9

# 标准下班时间（默认18点）
workhours.standard-end-hour=18

# 午休时间（默认1小时）
workhours.lunch-break-hours=1.0

# 晚餐时间（默认0.5小时）
workhours.dinner-break-hours=0.5

# 晚餐时间临界点（默认19点）
workhours.dinner-break-threshold-hour=19
```

## 故障排除

### 问题1：端口8080已被占用

修改 `application.properties`：
```properties
server.port=8081
```

### 问题2：data目录权限问题

确保程序运行目录有写入权限，或修改配置：
```properties
workhours.data-directory=D:/workhours/data
```

### 问题3：Maven依赖下载失败

在IDEA中：
1. File → Settings → Build → Build Tools → Maven
2. 使用IDEA自带的Maven或配置国内镜像

## 完整使用流程

```bash
# 1. 启动程序（在IDEA中运行 TimeCountApplication）

# 2. 生成本月模板
curl -X POST http://localhost:8080/api/workhours/template/generate

# 3. 手动填写 data/attendance_2025-10.xlsx

# 4. 查看统计报告
curl http://localhost:8080/api/workhours/report
```

报告输出示例：
```
============================================================
                   2025-10 工时统计报告
============================================================

【出勤统计】
  当月总工时：168.50 小时
  出勤天数：21 天
  出勤日平均工时：8.02 小时/天

【期望目标】
  期望总工时：220.00 小时
  距离目标还需：51.50 小时

【剩余规划】
  剩余工作日：10 天
  需要日均工时：5.15 小时/天

【请假统计】
  请假天数：2 天
  请假总时长：4.00 小时

============================================================
```

# 工时计算系统 (Time Count System)

这是一个基于Spring Boot的工时计算系统，用于管理和统计每月的考勤工时。

## 功能特点

1. **生成考勤模板**：自动生成指定月份的Excel考勤表格，包含每天的日期和星期信息
2. **工时计算**：根据上下班时间自动计算工时，智能扣除午休和晚餐时间
3. **统计分析**：提供详细的工时统计，包括总工时、平均工时、请假统计等
4. **请假检测**：自动识别请假情况（迟到、早退、缺勤）

## 快速开始

### 1. 在IDEA中运行

1. 打开项目
2. 等待Maven依赖下载完成
3. 运行 `TimeCountApplication` 主类
4. 服务将在 `http://localhost:8080` 启动

### 2. 使用Web界面（推荐）

启动程序后，在浏览器中访问：**http://localhost:8080**

Web界面提供了以下功能：
- ✨ **现代化UI**：美观的界面设计，操作直观简单
- 📝 **一键生成模板**：选择月份，点击按钮即可生成
- 🔢 **实时计算工时**：自动计算并展示详细统计报告
- 📊 **数据可视化**：清晰展示各项工时数据
- 📱 **响应式设计**：支持各种屏幕尺寸

### 3. 使用API

#### (1) 生成考勤模板

**接口**：`POST /api/workhours/template/generate`

**参数**：
- `yearMonth` (可选): 年月，格式 `YYYY-MM`，不传则使用当前月份

**示例**：
```bash
# 生成当前月份模板
curl -X POST http://localhost:8080/api/workhours/template/generate

# 生成指定月份模板
curl -X POST "http://localhost:8080/api/workhours/template/generate?yearMonth=2025-10"
```

**说明**：
- 模板文件将生成在 `data` 目录下
- 文件名格式：`attendance_YYYY-MM.xlsx`
- 如果文件已存在，不会覆盖已填写的数据
- 上下班时间列为空，需要手动填写

#### (2) 填写考勤数据

1. 打开生成的Excel文件（位于 `data` 目录）
2. 在"上班时间"和"下班时间"列填写时间，格式：`HH:mm`（如 `08:56`）
3. 保存文件

#### (3) 计算工时统计

**接口**：`GET /api/workhours/calculate`

**参数**：
- `yearMonth` (可选): 年月，格式 `YYYY-MM`，不传则使用当前月份

**示例**：
```bash
# 计算当前月份工时
curl http://localhost:8080/api/workhours/calculate

# 计算指定月份工时
curl "http://localhost:8080/api/workhours/calculate?yearMonth=2025-10"
```

**返回数据包括**：
- 当月总工时
- 出勤天数
- 出勤日平均工时
- 期望总工时（默认220小时）
- 距离期望总工时的差距
- 剩余工作日数量
- 剩余工作日需要的日平均工时
- 请假统计（天数和时长）

#### (4) 获取格式化报告

**接口**：`GET /api/workhours/report`

**参数**：
- `yearMonth` (可选): 年月，格式 `YYYY-MM`，不传则使用当前月份

**示例**：
```bash
curl http://localhost:8080/api/workhours/report
```

## 工时计算规则

### 1. 基本规则
- **工作时长** = 下班时间 - 上班时间 - 午休时间 - 晚餐时间（如适用）

### 2. 午休时间扣除
- 所有情况下扣除 **1小时** 午休时间

### 3. 晚餐时间扣除
- 下班时间 **早于19:00**：不扣除晚餐时间
- 下班时间 **晚于19:00**：扣除 **0.5小时** 晚餐时间

### 4. 请假判定
对于当前时间之前的工作日，满足以下任一条件视为请假：
- 上下班时间为空（缺勤）
- 上班时间晚于 **09:00**（迟到）
- 下班时间早于 **18:00**（早退）

### 5. 请假时长计算
- 全天缺勤：8小时
- 部分缺勤：按标准工时与实际工时的差值计算

## 配置说明

可在 `application.properties` 中修改以下配置：

```properties
# 数据文件存储目录
workhours.data-directory=data

# 期望总工时（默认220小时）
workhours.expected-total-hours=220.0

# 标准上班时间（小时）
workhours.standard-start-hour=9

# 标准下班时间（小时）
workhours.standard-end-hour=18

# 午休扣除时间（小时）
workhours.lunch-break-hours=1.0

# 晚餐扣除时间（小时）
workhours.dinner-break-hours=0.5

# 晚餐时间临界点（19:00）
workhours.dinner-break-threshold-hour=19
```

## 使用流程示例

### 场景：计算2025年10月的工时

1. **生成模板**
   ```bash
   curl -X POST "http://localhost:8080/api/workhours/template/generate?yearMonth=2025-10"
   ```

2. **填写考勤数据**
   - 打开文件：`data/attendance_2025-10.xlsx`
   - 填写每天的上下班时间，例如：
     ```
     日期         星期    上班时间  下班时间  备注
     2025-10-01  星期三   08:56    18:30
     2025-10-02  星期四   09:05    19:20
     2025-10-03  星期五   08:50    18:00
     ```
   - 保存文件

3. **查看统计报告**
   ```bash
   curl "http://localhost:8080/api/workhours/report?yearMonth=2025-10"
   ```

4. **输出示例**
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

## 注意事项

1. **时间格式**：上下班时间必须使用 `HH:mm` 格式（如 `08:56`）
2. **文件保护**：已生成的模板文件中已填写的数据不会被覆盖
3. **工作日判定**：系统自动识别工作日（周一至周五）
4. **请假统计**：仅对当前时间之前的工作日进行请假判定

## 技术栈

- Spring Boot 3.5.6
- Apache POI 5.2.3 (Excel操作)
- Lombok (减少样板代码)
- Maven (依赖管理)

## 项目结构

```
src/main/java/org/example/timecount/
├── TimeCountApplication.java          # 主启动类
├── config/
│   └── WorkHoursConfig.java          # 配置类
├── controller/
│   └── WorkHoursController.java      # REST接口控制器
├── model/
│   ├── DailyRecord.java              # 每日记录实体
│   └── WorkHoursStatistics.java      # 工时统计实体
└── service/
    ├── ExcelTemplateService.java     # Excel模板服务
    └── WorkHoursCalculationService.java  # 工时计算服务
```

## 许可证

本项目仅供学习和个人使用。

package org.example.timecount.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.timecount.model.WorkHoursStatistics;
import org.example.timecount.service.ExcelTemplateService;
import org.example.timecount.service.WorkHoursCalculationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/workhours")
@RequiredArgsConstructor
@Slf4j
public class WorkHoursController {

    private final ExcelTemplateService templateService;
    private final WorkHoursCalculationService calculationService;

    /**
     * 生成指定月份的考勤表格模板
     *
     * @param yearMonth 年月，格式：YYYY-MM，如果不传则使用当前月份
     * @return 生成结果
     */
    @PostMapping("/template/generate")
    public ResponseEntity<Map<String, Object>> generateTemplate(
            @RequestParam(required = false) String yearMonth) {
        
        try {
            // 如果没有传入年月，使用当前月份
            if (yearMonth == null || yearMonth.trim().isEmpty()) {
                yearMonth = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
            }

            String filePath = templateService.generateTemplate(yearMonth);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "模板生成成功");
            response.put("yearMonth", yearMonth);
            response.put("filePath", filePath);

            log.info("模板生成成功: {}", yearMonth);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("生成模板失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "生成模板失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 计算指定月份的工时统计
     *
     * @param yearMonth 年月，格式：YYYY-MM，如果不传则使用当前月份
     * @return 工时统计结果
     */
    @GetMapping("/calculate")
    public ResponseEntity<Map<String, Object>> calculateWorkHours(
            @RequestParam(required = false) String yearMonth) {
        
        try {
            // 如果没有传入年月，使用当前月份
            if (yearMonth == null || yearMonth.trim().isEmpty()) {
                yearMonth = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
            }

            WorkHoursStatistics statistics = calculationService.calculateWorkHours(yearMonth);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "工时计算成功");
            response.put("statistics", statistics);

            log.info("工时计算成功: {}", yearMonth);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("计算工时失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "计算工时失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 获取工时统计的详细报告（格式化输出）
     *
     * @param yearMonth 年月，格式：YYYY-MM，如果不传则使用当前月份
     * @return 格式化的统计报告
     */
    @GetMapping("/report")
    public ResponseEntity<String> getWorkHoursReport(
            @RequestParam(required = false) String yearMonth) {
        
        try {
            // 如果没有传入年月，使用当前月份
            if (yearMonth == null || yearMonth.trim().isEmpty()) {
                yearMonth = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
            }

            WorkHoursStatistics statistics = calculationService.calculateWorkHours(yearMonth);

            StringBuilder report = new StringBuilder();
            report.append("=".repeat(60)).append("\n");
            report.append(String.format("           %s 工时统计报告\n", statistics.getYearMonth()));
            report.append("=".repeat(60)).append("\n\n");

            report.append("【出勤统计】\n");
            report.append(String.format("  当月总工时：%.2f 小时\n", statistics.getTotalWorkHours()));
            report.append(String.format("  出勤天数：%d 天\n", statistics.getAttendanceDays()));
            report.append(String.format("  出勤日平均工时：%.2f 小时/天\n\n", statistics.getAverageWorkHoursPerDay()));

            report.append("【期望目标】\n");
            report.append(String.format("  期望总工时：%.2f 小时\n", statistics.getExpectedTotalHours()));
            report.append(String.format("  距离目标还需：%.2f 小时\n\n", statistics.getRemainingHoursToTarget()));

            report.append("【剩余规划】\n");
            report.append(String.format("  剩余工作日：%d 天\n", statistics.getRemainingWorkdays()));
            if (statistics.getRemainingWorkdays() > 0) {
                report.append(String.format("  需要日均工时：%.2f 小时/天\n\n", 
                        statistics.getRequiredAverageHoursForRemainingDays()));
            } else {
                report.append("  本月已结束\n\n");
            }

            if (statistics.getLeaveDays() > 0) {
                report.append("【请假统计】\n");
                report.append(String.format("  请假天数：%d 天\n", statistics.getLeaveDays()));
                report.append(String.format("  请假总时长：%.2f 小时\n\n", statistics.getTotalLeaveHours()));
            }

            report.append("=".repeat(60)).append("\n");

            log.info("生成工时报告: {}", yearMonth);
            return ResponseEntity.ok(report.toString());

        } catch (Exception e) {
            log.error("生成报告失败", e);
            return ResponseEntity.status(500).body("生成报告失败: " + e.getMessage());
        }
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "WorkHours Service");
        return ResponseEntity.ok(response);
    }

    /**
     * 获取每日详细记录（用于调试）
     */
    @GetMapping("/debug/daily-records")
    public ResponseEntity<Map<String, Object>> getDailyRecords(
            @RequestParam(required = false) String yearMonth) {
        
        try {
            if (yearMonth == null || yearMonth.trim().isEmpty()) {
                yearMonth = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
            }

            WorkHoursStatistics statistics = calculationService.calculateWorkHours(yearMonth);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("yearMonth", yearMonth);
            response.put("dailyRecords", statistics.getDailyRecords());
            response.put("summary", Map.of(
                "totalWorkHours", statistics.getTotalWorkHours(),
                "attendanceDays", statistics.getAttendanceDays(),
                "averageWorkHours", statistics.getAverageWorkHoursPerDay()
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("获取每日记录失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取每日记录失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}

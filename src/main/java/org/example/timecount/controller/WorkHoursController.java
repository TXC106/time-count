package org.example.timecount.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.timecount.config.WorkHoursConfig;
import org.example.timecount.model.AttendanceRequest;
import org.example.timecount.model.DailyRecord;
import org.example.timecount.model.WorkHoursConfigRequest;
import org.example.timecount.model.WorkHoursStatistics;
import org.example.timecount.service.AttendanceService;
import org.example.timecount.service.ExcelTemplateService;
import org.example.timecount.service.WorkHoursCalculationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
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
    private final AttendanceService attendanceService;
    private final WorkHoursConfig workHoursConfig;

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
            String separator = repeatString("=", 60);
            report.append(separator).append("\n");
            report.append(String.format("           %s 工时统计报告\n", statistics.getYearMonth()));
            report.append(separator).append("\n\n");

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
                
                // 显示请假详情
                report.append("  请假明细：\n");
                if (statistics.getLeaveRecords() != null && !statistics.getLeaveRecords().isEmpty()) {
                    for (DailyRecord leaveRecord : statistics.getLeaveRecords()) {
                        String leaveTimeStr = "";
                        if (leaveRecord.getLeaveStartTime() != null && leaveRecord.getLeaveEndTime() != null) {
                            leaveTimeStr = String.format(" [%s~%s]", 
                                    leaveRecord.getLeaveStartTime(), 
                                    leaveRecord.getLeaveEndTime());
                        }
                        
                        String workTimeStr = "";
                        if (leaveRecord.getStartTime() != null || leaveRecord.getEndTime() != null) {
                            String start = leaveRecord.getStartTime() != null ? 
                                    leaveRecord.getStartTime().toString() : "未打卡";
                            String end = leaveRecord.getEndTime() != null ? 
                                    leaveRecord.getEndTime().toString() : "未打卡";
                            workTimeStr = String.format(" (打卡: %s~%s)", start, end);
                        }
                        
                        report.append(String.format("    - %s%s%s: %.2f 小时\n", 
                                leaveRecord.getDate(), 
                                leaveTimeStr,
                                workTimeStr,
                                leaveRecord.getLeaveHours()));
                    }
                }
                report.append("\n");
            }

            if (statistics.getLateDays() > 0) {
                report.append("【迟到统计】\n");
                report.append(String.format("  迟到天数：%d 天\n", statistics.getLateDays()));
                report.append(String.format("  迟到率：%.1f%%\n", 
                        statistics.getActualAttendanceDays() > 0 ? 
                        (statistics.getLateDays() * 100.0 / statistics.getActualAttendanceDays()) : 0.0));
                report.append(String.format("  标准上班时间：%02d:00\n\n", 
                        workHoursConfig.getStandardStartHour()));
                
                // 显示迟到详情
                report.append("  迟到明细：\n");
                if (statistics.getLateRecords() != null && !statistics.getLateRecords().isEmpty()) {
                    for (DailyRecord lateRecord : statistics.getLateRecords()) {
                        // 计算迟到分钟数
                        LocalTime standardStart = LocalTime.of(workHoursConfig.getStandardStartHour(), 0);
                        long lateMinutes = java.time.Duration.between(standardStart, lateRecord.getStartTime()).toMinutes();
                        
                        String endTimeStr = lateRecord.getEndTime() != null ? 
                                lateRecord.getEndTime().toString() : "未打卡";
                        
                        report.append(String.format("    - %s %s：上班 %s (迟到 %d 分钟), 下班 %s, 工时 %.2fh\n", 
                                lateRecord.getDate(),
                                lateRecord.getDayOfWeek(),
                                lateRecord.getStartTime(),
                                lateMinutes,
                                endTimeStr,
                                lateRecord.getWorkHours()));
                    }
                }
                report.append("\n");
            }

            report.append(separator).append("\n");

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

            Map<String, Object> summary = new HashMap<>();
            summary.put("totalWorkHours", statistics.getTotalWorkHours());
            summary.put("attendanceDays", statistics.getAttendanceDays());
            summary.put("averageWorkHours", statistics.getAverageWorkHoursPerDay());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("yearMonth", yearMonth);
            response.put("dailyRecords", statistics.getDailyRecords());
            response.put("summary", summary);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("获取每日记录失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取每日记录失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 提交考勤记录（打卡）
     */
    @PostMapping("/attendance/submit")
    public ResponseEntity<Map<String, Object>> submitAttendance(
            @RequestBody AttendanceRequest request) {
        
        try {
            attendanceService.submitAttendance(request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "考勤记录提交成功");

            log.info("考勤记录提交成功: {}", request.getDate());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("提交考勤记录失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "提交考勤记录失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 获取工时配置
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfig() {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            Map<String, Object> config = new HashMap<>();
            config.put("expectedTotalHours", workHoursConfig.getExpectedTotalHours());
            config.put("lunchBreakHours", workHoursConfig.getLunchBreakHours());
            config.put("dinnerBreakHours", workHoursConfig.getDinnerBreakHours());
            config.put("dinnerBreakThresholdHour", workHoursConfig.getDinnerBreakThresholdHour());
            config.put("standardStartHour", workHoursConfig.getStandardStartHour());
            config.put("standardEndHour", workHoursConfig.getStandardEndHour());
            config.put("dataDirectory", workHoursConfig.getDataDirectory());
            config.put("fileNameFormat", workHoursConfig.getFileNameFormat());
            response.put("config", config);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("获取配置失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取配置失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 更新工时配置
     */
    @PutMapping("/config")
    public ResponseEntity<Map<String, Object>> updateConfig(
            @RequestBody WorkHoursConfigRequest request) {
        
        try {
            // 更新配置
            if (request.getExpectedTotalHours() != null) {
                workHoursConfig.setExpectedTotalHours(request.getExpectedTotalHours());
            }
            if (request.getLunchBreakHours() != null) {
                workHoursConfig.setLunchBreakHours(request.getLunchBreakHours());
            }
            if (request.getDinnerBreakHours() != null) {
                workHoursConfig.setDinnerBreakHours(request.getDinnerBreakHours());
            }
            if (request.getDinnerBreakThresholdHour() != null) {
                workHoursConfig.setDinnerBreakThresholdHour(request.getDinnerBreakThresholdHour());
            }
            if (request.getStandardStartHour() != null) {
                workHoursConfig.setStandardStartHour(request.getStandardStartHour());
            }
            if (request.getStandardEndHour() != null) {
                workHoursConfig.setStandardEndHour(request.getStandardEndHour());
            }
            if (request.getDataDirectory() != null && !request.getDataDirectory().trim().isEmpty()) {
                workHoursConfig.setDataDirectory(request.getDataDirectory());
            }
            if (request.getFileNameFormat() != null && !request.getFileNameFormat().trim().isEmpty()) {
                workHoursConfig.setFileNameFormat(request.getFileNameFormat());
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "配置更新成功");

            log.info("工时配置更新成功");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("更新配置失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "更新配置失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 获取指定月份的Excel数据
     */
    @GetMapping("/excel/data")
    public ResponseEntity<Map<String, Object>> getExcelData(@RequestParam String yearMonth) {
        try {
            WorkHoursStatistics statistics = calculationService.calculateWorkHours(yearMonth);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", statistics.getDailyRecords());
            response.put("yearMonth", yearMonth);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取Excel数据失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取数据失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 重复字符串（兼容 Java 11 之前的版本）
     */
    private String repeatString(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }
}

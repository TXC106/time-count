package org.example.timecount.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.timecount.config.WorkHoursConfig;
import org.example.timecount.model.DailyRecord;
import org.example.timecount.model.WorkHoursStatistics;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkHoursCalculationService {

    private final WorkHoursConfig config;
    private final HolidayService holidayService;

    /**
     * 计算指定月份的工时统计
     *
     * @param yearMonth 年月，格式：YYYY-MM
     * @return 工时统计结果
     */
    public WorkHoursStatistics calculateWorkHours(String yearMonth) throws IOException {
        // 读取Excel文件
        String fileName = String.format("attendance_%s.xlsx", yearMonth);
        File file = new File(config.getDataDirectory(), fileName);

        if (!file.exists()) {
            throw new IOException("考勤文件不存在: " + file.getAbsolutePath());
        }

        List<DailyRecord> dailyRecords = new ArrayList<>();
        YearMonth ym = YearMonth.parse(yearMonth, DateTimeFormatter.ofPattern("yyyy-MM"));
        LocalDate today = LocalDate.now();

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            
            // 跳过表头，从第二行开始读取
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                DailyRecord record = parseRow(row, today);
                if (record != null) {
                    dailyRecords.add(record);
                }
            }
        }

        // 计算统计信息
        return calculateStatistics(yearMonth, dailyRecords, today);
    }

    /**
     * 解析Excel行数据
     */
    private DailyRecord parseRow(Row row, LocalDate today) {
        try {
            // 读取日期
            Cell dateCell = row.getCell(0);
            if (dateCell == null || dateCell.getCellType() == CellType.BLANK) {
                return null;
            }

            String dateStr = getCellValueAsString(dateCell);
            LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            // 读取上班时间和下班时间
            Cell startTimeCell = row.getCell(2);
            Cell endTimeCell = row.getCell(3);

            String startTimeStr = getCellValueAsString(startTimeCell);
            String endTimeStr = getCellValueAsString(endTimeCell);

            LocalTime startTime = parseTime(startTimeStr);
            LocalTime endTime = parseTime(endTimeStr);
            
            // 读取请假类型（第5列）
            Cell leaveTypeCell = row.getCell(4);
            String leaveTypeStr = getCellValueAsString(leaveTypeCell);
            DailyRecord.LeaveType leaveType = parseLeaveType(leaveTypeStr);
            
            // 读取备注（第6列）
            Cell remarkCell = row.getCell(5);
            String remark = getCellValueAsString(remarkCell);
            
            // 添加调试日志
            log.debug("解析日期 {} - 上班时间: {} -> {}, 下班时间: {} -> {}, 请假类型: {}", 
                    dateStr, startTimeStr, startTime, endTimeStr, endTime, leaveType);

            // 判断是否为工作日（周一到周五）
            int dayOfWeek = date.getDayOfWeek().getValue();
            String dayOfWeekStr = getDayOfWeekString(dayOfWeek);
            
            // 判断是否为法定节假日
            boolean isHoliday = holidayService.isHoliday(date);
            if (isHoliday) {
                log.info("检测到法定节假日: {}", date);
            }
            
            // 判断是否为调休工作日
            boolean isMakeupWorkday = holidayService.isMakeupWorkday(date);
            if (isMakeupWorkday) {
                log.info("检测到调休工作日: {}", date);
            }
            
            // 工作日判断：(周一到周五且不是法定节假日) 或 (调休工作日)
            boolean isWorkday = ((dayOfWeek >= 1 && dayOfWeek <= 5) && !isHoliday) || isMakeupWorkday;

            DailyRecord record = DailyRecord.builder()
                    .date(date)
                    .dayOfWeek(dayOfWeekStr)
                    .startTime(startTime)
                    .endTime(endTime)
                    .isWorkday(isWorkday)
                    .isHoliday(isHoliday)
                    .leaveType(leaveType)
                    .remark(remark)
                    .build();

            // 判断是否请假（仅对当前时间之前的工作日，且不是法定节假日）
            if (isWorkday && !date.isAfter(today) && !isHoliday) {
                checkLeaveStatus(record);
            }

            // 计算工时
            if (startTime != null && endTime != null) {
                double workHours = calculateDailyWorkHours(startTime, endTime, leaveType);
                record.setWorkHours(workHours);
                log.debug("日期 {} 计算工时: {} 小时", dateStr, workHours);
            } else {
                log.debug("日期 {} 上下班时间不完整，跳过工时计算", dateStr);
            }

            return record;

        } catch (Exception e) {
            log.warn("解析行数据失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 检查请假状态
     */
    private void checkLeaveStatus(DailyRecord record) {
        LocalTime startTime = record.getStartTime();
        LocalTime endTime = record.getEndTime();

        // 标准上班时间和下班时间
        LocalTime standardStart = LocalTime.of(config.getStandardStartHour(), 0);
        LocalTime standardEnd = LocalTime.of(config.getStandardEndHour(), 0);

        // 如果上下班时间为空，或上班晚于9点，或下班早于18点，视为请假
        if (startTime == null || endTime == null ||
                startTime.isAfter(standardStart) ||
                endTime.isBefore(standardEnd)) {

            record.setLeave(true);

            // 计算请假时长
            double leaveHours = 0.0;

            if (startTime == null && endTime == null) {
                // 全天请假：标准工时 9-12 + 13-18 = 8小时
                leaveHours = 8.0;
            } else if (startTime == null) {
                // 上午请假
                if (endTime.isBefore(LocalTime.of(13, 0))) {
                    leaveHours = Duration.between(standardStart, LocalTime.of(12, 0)).toMinutes() / 60.0;
                } else {
                    leaveHours = Duration.between(standardStart, LocalTime.of(12, 0)).toMinutes() / 60.0;
                }
            } else if (endTime == null) {
                // 下午请假
                if (startTime.isBefore(LocalTime.of(13, 0))) {
                    leaveHours = Duration.between(LocalTime.of(13, 0), standardEnd).toMinutes() / 60.0;
                } else {
                    leaveHours = Duration.between(startTime, standardEnd).toMinutes() / 60.0;
                }
            } else {
                // 迟到或早退
                double actualHours = calculateDailyWorkHours(startTime, endTime, record.getLeaveType() != null ? record.getLeaveType() : DailyRecord.LeaveType.NONE);
                double standardHours = 8.0; // 标准工时
                leaveHours = Math.max(0, standardHours - actualHours);
            }

            record.setLeaveHours(leaveHours);
        }
    }

    /**
     * 计算每日工时
     */
    private double calculateDailyWorkHours(LocalTime startTime, LocalTime endTime, DailyRecord.LeaveType leaveType) {
        if (startTime == null || endTime == null) {
            return 0.0;
        }

        // 计算总工作时长（分钟）
        long totalMinutes = Duration.between(startTime, endTime).toMinutes();
        double totalHours = totalMinutes / 60.0;
        
        log.debug("  原始时长: {} - {} = {} 小时", endTime, startTime, String.format("%.2f", totalHours));

        // 判断是否需要扣除午休时间
        // 如果是下午请假，则不扣除午休时间
        if (leaveType != DailyRecord.LeaveType.AFTERNOON) {
            totalHours -= config.getLunchBreakHours();
            log.debug("  扣除午休 {} 小时后: {} 小时", config.getLunchBreakHours(), String.format("%.2f", totalHours));
        } else {
            log.debug("  下午请假，不扣除午休时间");
        }

        // 如果下班时间晚于19:00，扣除晚餐时间
        LocalTime dinnerThreshold = LocalTime.of(config.getDinnerBreakThresholdHour(), 0);
        if (endTime.isAfter(dinnerThreshold)) {
            totalHours -= config.getDinnerBreakHours();
            log.debug("  下班晚于{}:00，扣除晚餐 {} 小时后: {} 小时", 
                    config.getDinnerBreakThresholdHour(), config.getDinnerBreakHours(), String.format("%.2f", totalHours));
        }

        double finalHours = Math.max(0, totalHours);
        log.debug("  最终工时: {} 小时", String.format("%.2f", finalHours));
        
        return finalHours;
    }

    /**
     * 计算统计信息
     */
    private WorkHoursStatistics calculateStatistics(String yearMonth, List<DailyRecord> dailyRecords, LocalDate today) {
        double totalWorkHours = 0.0;
        int attendanceDays = 0;
        double totalLeaveHours = 0.0;
        int leaveDays = 0;
        List<DailyRecord> leaveRecords = new ArrayList<>();
        int lateNightCheckInCount = 0;
        int actualAttendanceDays = 0;

        for (DailyRecord record : dailyRecords) {
            if (record.getWorkHours() > 0) {
                totalWorkHours += record.getWorkHours();
                attendanceDays++;
            }
            if (record.isLeave()) {
                totalLeaveHours += record.getLeaveHours();
                leaveDays++;
                leaveRecords.add(record);
            }
            
            // 统计晚上九点后打卡次数
            if (record.getEndTime() != null && record.getEndTime().isAfter(LocalTime.of(21, 0))) {
                lateNightCheckInCount++;
            }
            
            // 统计实际出勤天数（工作日且有上班或下班打卡记录）
            if (record.isWorkday() && (record.getStartTime() != null || record.getEndTime() != null)) {
                actualAttendanceDays++;
            }
        }

        // 计算平均工时
        double averageWorkHoursPerDay = attendanceDays > 0 ? totalWorkHours / attendanceDays : 0.0;

        // 计算距离期望总工时的差距
        double remainingHoursToTarget = config.getExpectedTotalHours() - totalWorkHours;

        // 计算剩余工作日
        YearMonth ym = YearMonth.parse(yearMonth, DateTimeFormatter.ofPattern("yyyy-MM"));
        int remainingWorkdays = calculateRemainingWorkdays(ym, today);

        // 计算剩余工作日需要的日平均工时
        double requiredAverageHoursForRemainingDays = remainingWorkdays > 0
                ? remainingHoursToTarget / remainingWorkdays
                : 0.0;

        return WorkHoursStatistics.builder()
                .yearMonth(yearMonth)
                .totalWorkHours(Math.round(totalWorkHours * 100.0) / 100.0)
                .attendanceDays(attendanceDays)
                .averageWorkHoursPerDay(Math.round(averageWorkHoursPerDay * 100.0) / 100.0)
                .expectedTotalHours(config.getExpectedTotalHours())
                .remainingHoursToTarget(Math.round(remainingHoursToTarget * 100.0) / 100.0)
                .remainingWorkdays(remainingWorkdays)
                .requiredAverageHoursForRemainingDays(Math.round(requiredAverageHoursForRemainingDays * 100.0) / 100.0)
                .totalLeaveHours(Math.round(totalLeaveHours * 100.0) / 100.0)
                .leaveDays(leaveDays)
                .dailyRecords(dailyRecords)
                .leaveRecords(leaveRecords)
                .lateNightCheckInCount(lateNightCheckInCount)
                .actualAttendanceDays(actualAttendanceDays)
                .build();
    }

    /**
     * 计算剩余工作日
     */
    private int calculateRemainingWorkdays(YearMonth yearMonth, LocalDate today) {
        int count = 0;
        LocalDate date = today.plusDays(1); // 从明天开始计算
        LocalDate endOfMonth = yearMonth.atEndOfMonth();

        while (!date.isAfter(endOfMonth)) {
            int dayOfWeek = date.getDayOfWeek().getValue();
            boolean isHoliday = holidayService.isHoliday(date);
            boolean isMakeupWorkday = holidayService.isMakeupWorkday(date);
            
            // 工作日判断：(周一到周五且不是法定节假日) 或 (调休工作日)
            if (((dayOfWeek >= 1 && dayOfWeek <= 5) && !isHoliday) || isMakeupWorkday) {
                count++;
            }
            date = date.plusDays(1);
        }

        return count;
    }

    /**
     * 解析请假类型
     */
    private DailyRecord.LeaveType parseLeaveType(String typeStr) {
        if (typeStr == null || typeStr.trim().isEmpty()) {
            return DailyRecord.LeaveType.NONE;
        }
        
        String type = typeStr.trim();
        if (type.contains("上午")) {
            return DailyRecord.LeaveType.MORNING;
        } else if (type.contains("下午")) {
            return DailyRecord.LeaveType.AFTERNOON;
        } else if (type.contains("全天")) {
            return DailyRecord.LeaveType.FULL_DAY;
        }
        
        return DailyRecord.LeaveType.NONE;
    }
    
    /**
     * 解析时间字符串
     */
    private LocalTime parseTime(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return null;
        }

        try {
            // 支持格式：HH:mm 或 H:mm
            if (timeStr.contains(":")) {
                String[] parts = timeStr.split(":");
                int hour = Integer.parseInt(parts[0].trim());
                int minute = Integer.parseInt(parts[1].trim());
                return LocalTime.of(hour, minute);
            }
        } catch (Exception e) {
            log.warn("解析时间失败: {}", timeStr);
        }

        return null;
    }

    /**
     * 获取单元格值为字符串
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    // 检查是否为时间格式
                    try {
                        LocalTime time = cell.getLocalDateTimeCellValue().toLocalTime();
                        return time.format(DateTimeFormatter.ofPattern("H:mm"));
                    } catch (Exception e1) {
                        // 如果不是时间，尝试解析为日期
                        try {
                            return cell.getLocalDateTimeCellValue().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                        } catch (Exception e2) {
                            log.warn("日期格式解析失败: {}", e2.getMessage());
                            return "";
                        }
                    }
                } else {
                    return String.valueOf((int) cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
    
    /**
     * 获取星期的中文字符串
     */
    private String getDayOfWeekString(int dayOfWeek) {
        switch (dayOfWeek) {
            case 1: return "星期一";
            case 2: return "星期二";
            case 3: return "星期三";
            case 4: return "星期四";
            case 5: return "星期五";
            case 6: return "星期六";
            case 7: return "星期日";
            default: return "";
        }
    }
}

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
            
            // 读取请假开始时间（第5列）
            Cell leaveStartCell = row.getCell(4);
            String leaveStartStr = getCellValueAsString(leaveStartCell);
            LocalTime leaveStartTime = parseTime(leaveStartStr);
            
            // 读取请假结束时间（第6列）
            Cell leaveEndCell = row.getCell(5);
            String leaveEndStr = getCellValueAsString(leaveEndCell);
            LocalTime leaveEndTime = parseTime(leaveEndStr);
            
            // 读取备注（第7列）
            Cell remarkCell = row.getCell(6);
            String remark = getCellValueAsString(remarkCell);
            
            // 添加调试日志
            log.debug("解析日期 {} - 上班: {}, 下班: {}, 请假: {} ~ {}", 
                    dateStr, startTime, endTime, leaveStartTime, leaveEndTime);

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

            // 计算请假时长（根据请假时间段）
            double leaveHours = 0.0;
            if (leaveStartTime != null && leaveEndTime != null) {
                leaveHours = Duration.between(leaveStartTime, leaveEndTime).toMinutes() / 60.0;
                log.debug("日期 {} 请假时间段: {} ~ {}, 请假时长: {} 小时", 
                        dateStr, leaveStartTime, leaveEndTime, leaveHours);
            }
            
            DailyRecord record = DailyRecord.builder()
                    .date(date)
                    .dayOfWeek(dayOfWeekStr)
                    .startTime(startTime)
                    .endTime(endTime)
                    .leaveStartTime(leaveStartTime)
                    .leaveEndTime(leaveEndTime)
                    .isWorkday(isWorkday)
                    .isHoliday(isHoliday)
                    .isLeave(leaveHours > 0)
                    .leaveHours(leaveHours)
                    .remark(remark)
                    .build();

            // 计算工时
            if (startTime != null && endTime != null) {
                double workHours = calculateDailyWorkHours(startTime, endTime, leaveStartTime, leaveEndTime);
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
     * 计算每日工时
     * 新规则：出勤时长 = 下班卡 - 上班卡 - 用餐时间
     * 
     * 用餐时间扣除规则：
     * 1. 下午请假（请假时间包含13:00-18:00）：不扣除用餐时间
     * 2. 19点前打下班卡：扣减1小时用餐时间（午餐）
     * 3. 19点及之后打下班卡：扣减1.5小时用餐时间（午餐+晚餐）
     */
    private double calculateDailyWorkHours(LocalTime startTime, LocalTime endTime, 
                                          LocalTime leaveStartTime, LocalTime leaveEndTime) {
        if (startTime == null || endTime == null) {
            return 0.0;
        }

        // 计算总工作时长（分钟）
        long totalMinutes = Duration.between(startTime, endTime).toMinutes();
        double totalHours = totalMinutes / 60.0;
        
        log.debug("  原始时长: {} - {} = {} 小时", endTime, startTime, String.format("%.2f", totalHours));

        // 根据下班时间和请假情况判断用餐时间扣减
        LocalTime dinnerThreshold = LocalTime.of(config.getDinnerBreakThresholdHour(), 0); // 19:00
        LocalTime afternoonStart = LocalTime.of(13, 0);  // 下午开始时间
        LocalTime afternoonEnd = LocalTime.of(18, 0);    // 下午结束时间
        double mealTimeDeduction = 0.0;
        
        // 判断是否下午请假（请假时间包含13:00-18:00）
        boolean isAfternoonLeave = false;
        if (leaveStartTime != null && leaveEndTime != null) {
            // 请假时间包含下午时段
            if (leaveStartTime.isBefore(afternoonEnd) && leaveEndTime.isAfter(afternoonStart)) {
                isAfternoonLeave = true;
                log.debug("  检测到下午请假: {} ~ {}", leaveStartTime, leaveEndTime);
            }
        }
        
        if (isAfternoonLeave) {
            // 下午请假，不扣除用餐时间
            log.debug("  下午请假，不扣除用餐时间");
        } else if (endTime.isBefore(dinnerThreshold)) {
            // 19点前打下班卡，扣减1小时用餐时间
            mealTimeDeduction = 1.0;
            totalHours -= mealTimeDeduction;
            log.debug("  下班时间 {} 早于 19:00，扣除用餐时间 {} 小时后: {} 小时", 
                    endTime, mealTimeDeduction, String.format("%.2f", totalHours));
        } else {
            // 19点及之后打下班卡，扣减1.5小时用餐时间（1小时午餐 + 0.5小时晚餐）
            mealTimeDeduction = 1.5;
            totalHours -= mealTimeDeduction;
            log.debug("  下班时间 {} 晚于或等于 19:00，扣除用餐时间 {} 小时后: {} 小时", 
                    endTime, mealTimeDeduction, String.format("%.2f", totalHours));
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
     * 计算剩余工作日（包含当天）
     */
    private int calculateRemainingWorkdays(YearMonth yearMonth, LocalDate today) {
        int count = 0;
        LocalDate date = today; // 从今天开始计算
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

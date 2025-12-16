package org.example.timecount.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkHoursStatistics {
    
    /**
     * 年月（格式：YYYY-MM）
     */
    private String yearMonth;
    
    /**
     * 当月总工时
     */
    private double totalWorkHours;
    
    /**
     * 出勤天数
     */
    private int attendanceDays;
    
    /**
     * 出勤日平均工时
     */
    private double averageWorkHoursPerDay;
    
    /**
     * 期望总工时
     */
    private double expectedTotalHours;
    
    /**
     * 距离期望总工时还需要的工时
     */
    private double remainingHoursToTarget;
    
    /**
     * 剩余工作日数量
     */
    private int remainingWorkdays;
    
    /**
     * 剩余工作日需要的日平均工时
     */
    private double requiredAverageHoursForRemainingDays;
    
    /**
     * 当月请假总时长（小时）
     */
    private double totalLeaveHours;
    
    /**
     * 请假天数
     */
    private int leaveDays;
    
    /**
     * 每日记录详情
     */
    private List<DailyRecord> dailyRecords;
    
    /**
     * 请假记录详情（仅包含有请假的记录）
     */
    private List<DailyRecord> leaveRecords;
    
    /**
     * 晚上九点后打卡次数
     */
    private int lateNightCheckInCount;
    
    /**
     * 实际出勤天数（有打卡记录的工作日天数）
     */
    private int actualAttendanceDays;
    
    /**
     * 迟到天数（晚于标准上班时间打卡且未请假）
     */
    private int lateDays;
    
    /**
     * 迟到记录详情（仅包含迟到的记录）
     */
    private List<DailyRecord> lateRecords;
}

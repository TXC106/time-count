package org.example.timecount.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyRecord {
    
    /**
     * 日期
     */
    private LocalDate date;
    
    /**
     * 星期
     */
    private String dayOfWeek;
    
    /**
     * 上班时间
     */
    private LocalTime startTime;
    
    /**
     * 下班时间
     */
    private LocalTime endTime;
    
    /**
     * 当天工时
     */
    private double workHours;
    
    /**
     * 是否为工作日
     */
    private boolean isWorkday;
    
    /**
     * 是否请假
     */
    private boolean isLeave;
    
    /**
     * 请假开始时间
     */
    private LocalTime leaveStartTime;
    
    /**
     * 请假结束时间
     */
    private LocalTime leaveEndTime;
    
    /**
     * 请假时长（小时）- 根据请假时间段自动计算
     */
    private double leaveHours;
    
    /**
     * 备注
     */
    private String remark;
    
    /**
     * 是否为法定节假日
     */
    private boolean isHoliday;
    
}

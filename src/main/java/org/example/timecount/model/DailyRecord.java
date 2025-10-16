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
     * 请假时长（小时）
     */
    private double leaveHours;
    
    /**
     * 请假类型：MORNING(上午)、AFTERNOON(下午)、FULL_DAY(全天)、NONE(无)
     */
    private LeaveType leaveType;
    
    /**
     * 备注
     */
    private String remark;
    
    /**
     * 是否为法定节假日
     */
    private boolean isHoliday;
    
    /**
     * 请假类型枚举
     */
    public enum LeaveType {
        NONE("无"),
        MORNING("上午"),
        AFTERNOON("下午"),
        FULL_DAY("全天");
        
        private final String description;
        
        LeaveType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}

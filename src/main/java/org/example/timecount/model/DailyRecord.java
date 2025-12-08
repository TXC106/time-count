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
     * 下班时间原始字符串（包含+1标记）
     */
    private String endTimeRaw;
    
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
     * 请假类型
     */
    private LeaveType leaveType;
    
    /**
     * 请假开始时间（仅当leaveType为CUSTOM时使用）
     */
    private LocalTime leaveStartTime;
    
    /**
     * 请假结束时间（仅当leaveType为CUSTOM时使用）
     */
    private LocalTime leaveEndTime;
    
    /**
     * 请假时长（小时）- 根据请假类型或时间段自动计算
     */
    private double leaveHours;
    
    /**
     * 请假类型枚举
     */
    public enum LeaveType {
        NONE("正常"),
        MORNING("上午请假"),
        AFTERNOON("下午请假"),
        FULL_DAY("全天请假"),
        CUSTOM("自定义时间段");
        
        private final String description;
        
        LeaveType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
        
        public static LeaveType fromString(String str) {
            if (str == null || str.trim().isEmpty() || str.equals("正常")) {
                return NONE;
            }
            for (LeaveType type : values()) {
                if (type.description.equals(str) || type.name().equalsIgnoreCase(str)) {
                    return type;
                }
            }
            return NONE;
        }
    }
    
    /**
     * 备注
     */
    private String remark;
    
    /**
     * 是否为法定节假日
     */
    private boolean isHoliday;
    
}

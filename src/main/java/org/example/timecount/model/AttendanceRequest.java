package org.example.timecount.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 考勤打卡请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceRequest {
    
    /**
     * 日期，格式：yyyy-MM-dd
     */
    private String date;
    
    /**
     * 上班时间，格式：HH:mm
     */
    private String startTime;
    
    /**
     * 下班时间，格式：HH:mm
     */
    private String endTime;
    
    /**
     * 请假类型：正常、上午请假、下午请假、全天请假、自定义时间段
     */
    private String leaveType;
    
    /**
     * 请假开始时间，格式：HH:mm（仅当leaveType为"自定义时间段"时使用）
     */
    private String leaveStartTime;
    
    /**
     * 请假结束时间，格式：HH:mm
     */
    private String leaveEndTime;
    
    /**
     * 备注
     */
    private String remark;
}

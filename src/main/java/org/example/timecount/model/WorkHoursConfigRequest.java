package org.example.timecount.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工时配置更新请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkHoursConfigRequest {
    
    /**
     * 期望总工时
     */
    private Double expectedTotalHours;
    
    /**
     * 午休扣除时间（小时）
     */
    private Double lunchBreakHours;
    
    /**
     * 晚餐扣除时间（小时）
     */
    private Double dinnerBreakHours;
    
    /**
     * 晚餐时间临界点（小时）
     */
    private Integer dinnerBreakThresholdHour;
    
    /**
     * 标准上班时间（小时）
     */
    private Integer standardStartHour;
    
    /**
     * 标准下班时间（小时）
     */
    private Integer standardEndHour;
    
    /**
     * 数据文件存储目录
     */
    private String dataDirectory;
    
    /**
     * 文件名格式
     */
    private String fileNameFormat;
}

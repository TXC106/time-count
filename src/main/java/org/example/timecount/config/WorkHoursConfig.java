package org.example.timecount.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "workhours")
@Data
public class WorkHoursConfig {
    
    /**
     * 数据文件存储目录
     */
    private String dataDirectory = "data";
    
    /**
     * 期望总工时（默认220小时）
     */
    private double expectedTotalHours = 220.0;
    
    /**
     * 标准上班时间（小时）
     */
    private int standardStartHour = 9;
    
    /**
     * 标准下班时间（小时）
     */
    private int standardEndHour = 18;
    
    /**
     * 午休扣除时间（小时）
     */
    private double lunchBreakHours = 1.0;
    
    /**
     * 晚餐扣除时间（小时）
     */
    private double dinnerBreakHours = 0.5;
    
    /**
     * 晚餐时间临界点（19:00）
     */
    private int dinnerBreakThresholdHour = 19;
}

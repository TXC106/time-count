package org.example.timecount.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * 法定节假日服务
 * 用于判断某个日期是否为法定节假日
 */
@Service
@Slf4j
public class HolidayService {
    
    /**
     * 法定节假日集合（包括调休的法定节假日）
     * 这里预定义了2025年的法定节假日
     */
    private final Set<LocalDate> holidays = new HashSet<>();
    
    /**
     * 调休工作日集合（周末需要上班的日期）
     */
    private final Set<LocalDate> makeupWorkdays = new HashSet<>();
    
    public HolidayService() {
        initHolidays();
        initMakeupWorkdays();
    }
    
    /**
     * 初始化法定节假日
     * 2025年法定节假日安排
     */
    private void initHolidays() {
        // 元旦：2024年12月30日至2025年1月1日
        holidays.add(LocalDate.of(2024, 12, 30));
        holidays.add(LocalDate.of(2024, 12, 31));
        holidays.add(LocalDate.of(2025, 1, 1));
        
        // 春节：2025年1月28日至2月4日
        holidays.add(LocalDate.of(2025, 1, 28));
        holidays.add(LocalDate.of(2025, 1, 29));
        holidays.add(LocalDate.of(2025, 1, 30));
        holidays.add(LocalDate.of(2025, 1, 31));
        holidays.add(LocalDate.of(2025, 2, 1));
        holidays.add(LocalDate.of(2025, 2, 2));
        holidays.add(LocalDate.of(2025, 2, 3));
        holidays.add(LocalDate.of(2025, 2, 4));
        
        // 清明节：2025年4月4日至6日
        holidays.add(LocalDate.of(2025, 4, 4));
        holidays.add(LocalDate.of(2025, 4, 5));
        holidays.add(LocalDate.of(2025, 4, 6));
        
        // 劳动节：2025年5月1日至5日
        holidays.add(LocalDate.of(2025, 5, 1));
        holidays.add(LocalDate.of(2025, 5, 2));
        holidays.add(LocalDate.of(2025, 5, 3));
        holidays.add(LocalDate.of(2025, 5, 4));
        holidays.add(LocalDate.of(2025, 5, 5));
        
        // 端午节：2025年5月31日至6月2日
        holidays.add(LocalDate.of(2025, 5, 31));
        holidays.add(LocalDate.of(2025, 6, 1));
        holidays.add(LocalDate.of(2025, 6, 2));
        
        // 国庆节+中秋节：2025年10月1日至8日（连休）
        holidays.add(LocalDate.of(2025, 10, 1));
        holidays.add(LocalDate.of(2025, 10, 2));
        holidays.add(LocalDate.of(2025, 10, 3));
        holidays.add(LocalDate.of(2025, 10, 4));
        holidays.add(LocalDate.of(2025, 10, 5));
        holidays.add(LocalDate.of(2025, 10, 6));
        holidays.add(LocalDate.of(2025, 10, 7));
        holidays.add(LocalDate.of(2025, 10, 8));
        
        log.info("已加载 {} 个法定节假日", holidays.size());
    }
    
    /**
     * 初始化调休工作日
     * 2025年调休工作日安排（周末需要上班的日期）
     */
    private void initMakeupWorkdays() {
        // 春节调休：2025年1月26日（周日）上班
        makeupWorkdays.add(LocalDate.of(2025, 1, 26));
        
        // 清明节调休：暂无
        
        // 劳动节调休：2025年4月27日（周日）上班
        makeupWorkdays.add(LocalDate.of(2025, 4, 27));
        
        // 端午节调休：暂无
        
        // 国庆节调休：2025年9月28日（周日）、10月11日（周六）上班
        makeupWorkdays.add(LocalDate.of(2025, 9, 28));
        makeupWorkdays.add(LocalDate.of(2025, 10, 11));
        
        log.info("已加载 {} 个调休工作日", makeupWorkdays.size());
    }
    
    /**
     * 判断指定日期是否为法定节假日
     * 
     * @param date 日期
     * @return true表示是法定节假日，false表示不是
     */
    public boolean isHoliday(LocalDate date) {
        boolean result = holidays.contains(date);
        if (result) {
            log.debug("日期 {} 是法定节假日", date);
        }
        return result;
    }
    
    /**
     * 判断指定日期是否为调休工作日（周末需要上班）
     * 
     * @param date 日期
     * @return true表示是调休工作日，false表示不是
     */
    public boolean isMakeupWorkday(LocalDate date) {
        boolean result = makeupWorkdays.contains(date);
        if (result) {
            log.debug("日期 {} 是调休工作日", date);
        }
        return result;
    }
    
    /**
     * 添加自定义节假日
     * 
     * @param date 日期
     */
    public void addHoliday(LocalDate date) {
        holidays.add(date);
        log.info("添加自定义节假日: {}", date);
    }
    
    /**
     * 移除节假日
     * 
     * @param date 日期
     */
    public void removeHoliday(LocalDate date) {
        holidays.remove(date);
        log.info("移除节假日: {}", date);
    }
    
    /**
     * 获取所有节假日
     * 
     * @return 节假日集合
     */
    public Set<LocalDate> getAllHolidays() {
        return new HashSet<>(holidays);
    }
}

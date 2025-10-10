package org.example.timecount.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.timecount.config.WorkHoursConfig;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelTemplateService {

    private final WorkHoursConfig config;

    /**
     * 生成指定月份的考勤表格模板
     *
     * @param yearMonth 年月，格式：YYYY-MM
     * @return 生成的文件路径
     */
    public String generateTemplate(String yearMonth) throws IOException {
        YearMonth ym = YearMonth.parse(yearMonth, DateTimeFormatter.ofPattern("yyyy-MM"));
        
        // 创建data目录
        File dataDir = new File(config.getDataDirectory());
        if (!dataDir.exists()) {
            dataDir.mkdirs();
            log.info("创建数据目录: {}", dataDir.getAbsolutePath());
        }

        // 文件路径
        String fileName = String.format("attendance_%s.xlsx", yearMonth);
        File file = new File(dataDir, fileName);

        Workbook workbook;
        Sheet sheet;

        // 如果文件已存在，读取现有数据
        if (file.exists()) {
            log.info("文件已存在，将保留已填写的数据: {}", file.getAbsolutePath());
            try (FileInputStream fis = new FileInputStream(file)) {
                workbook = new XSSFWorkbook(fis);
                sheet = workbook.getSheetAt(0);
            }
            // 检查是否需要添加新的日期行（如果模板不完整）
            updateExistingTemplate(sheet, ym);
        } else {
            // 创建新的工作簿
            workbook = new XSSFWorkbook();
            sheet = workbook.createSheet("考勤记录");
            createNewTemplate(workbook, sheet, ym);
        }

        // 写入文件
        try (FileOutputStream fos = new FileOutputStream(file)) {
            workbook.write(fos);
            log.info("模板文件已生成/更新: {}", file.getAbsolutePath());
        }

        workbook.close();
        return file.getAbsolutePath();
    }

    /**
     * 创建新模板
     */
    private void createNewTemplate(Workbook workbook, Sheet sheet, YearMonth yearMonth) {
        // 创建样式
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dateStyle = createDateStyle(workbook);
        CellStyle timeStyle = createTimeStyle(workbook);

        // 创建表头
        Row headerRow = sheet.createRow(0);
        String[] headers = {"日期", "星期", "上班时间", "下班时间", "请假类型", "备注"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 设置列宽
        sheet.setColumnWidth(0, 3000);  // 日期
        sheet.setColumnWidth(1, 2500);  // 星期
        sheet.setColumnWidth(2, 3000);  // 上班时间
        sheet.setColumnWidth(3, 3000);  // 下班时间
        sheet.setColumnWidth(4, 3000);  // 请假类型
        sheet.setColumnWidth(5, 5000);  // 备注

        // 填充每一天的行
        int daysInMonth = yearMonth.lengthOfMonth();
        LocalDate startDate = yearMonth.atDay(1);

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = startDate.plusDays(day - 1);
            Row row = sheet.createRow(day);

            // 日期列
            Cell dateCell = row.createCell(0);
            dateCell.setCellValue(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            dateCell.setCellStyle(dateStyle);

            // 星期列
            Cell weekdayCell = row.createCell(1);
            weekdayCell.setCellValue(getWeekdayName(date.getDayOfWeek().getValue()));
            weekdayCell.setCellStyle(dateStyle);

            // 上班时间列（空白，等待用户填写）
            Cell startTimeCell = row.createCell(2);
            startTimeCell.setCellStyle(timeStyle);

            // 下班时间列（空白，等待用户填写）
            Cell endTimeCell = row.createCell(3);
            endTimeCell.setCellStyle(timeStyle);

            // 请假类型列（空白）
            Cell leaveTypeCell = row.createCell(4);
            leaveTypeCell.setCellStyle(timeStyle);

            // 备注列
            Cell remarkCell = row.createCell(5);
            remarkCell.setCellStyle(timeStyle);
        }
    }

    /**
     * 更新已存在的模板（保留已填写的数据）
     */
    private void updateExistingTemplate(Sheet sheet, YearMonth yearMonth) {
        int lastRowNum = sheet.getLastRowNum();
        int daysInMonth = yearMonth.lengthOfMonth();

        // 如果现有行数少于应有天数，补充缺失的行
        if (lastRowNum < daysInMonth) {
            log.info("补充缺失的日期行");
            LocalDate startDate = yearMonth.atDay(1);
            
            for (int day = lastRowNum; day <= daysInMonth; day++) {
                LocalDate date = startDate.plusDays(day - 1);
                Row row = sheet.createRow(day);

                // 日期列
                Cell dateCell = row.createCell(0);
                dateCell.setCellValue(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));

                // 星期列
                Cell weekdayCell = row.createCell(1);
                weekdayCell.setCellValue(getWeekdayName(date.getDayOfWeek().getValue()));

                // 其他列留空
                row.createCell(2);
                row.createCell(3);
                row.createCell(4);
                row.createCell(5);
            }
        }
    }

    /**
     * 创建表头样式
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    /**
     * 创建日期列样式
     */
    private CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    /**
     * 创建时间列样式
     */
    private CellStyle createTimeStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    /**
     * 获取星期名称
     */
    private String getWeekdayName(int dayOfWeek) {
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

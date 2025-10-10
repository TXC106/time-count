package org.example.timecount.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.timecount.config.WorkHoursConfig;
import org.example.timecount.model.AttendanceRequest;
import org.example.timecount.model.DailyRecord;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceService {

    private final WorkHoursConfig config;

    /**
     * 提交考勤记录（打卡或请假）
     *
     * @param request 考勤请求
     */
    public void submitAttendance(AttendanceRequest request) throws IOException {
        LocalDate date = LocalDate.parse(request.getDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String yearMonth = date.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        
        String fileName = String.format("attendance_%s.xlsx", yearMonth);
        File file = new File(config.getDataDirectory(), fileName);

        if (!file.exists()) {
            throw new IOException("考勤文件不存在，请先生成模板: " + file.getAbsolutePath());
        }

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            
            // 查找对应日期的行
            Row targetRow = null;
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Cell dateCell = row.getCell(0);
                if (dateCell != null) {
                    String cellDate = getCellValueAsString(dateCell);
                    if (request.getDate().equals(cellDate)) {
                        targetRow = row;
                        break;
                    }
                }
            }

            if (targetRow == null) {
                throw new IOException("未找到日期为 " + request.getDate() + " 的记录");
            }

            // 更新上班时间和下班时间
            if (request.getStartTime() != null && !request.getStartTime().trim().isEmpty()) {
                Cell startTimeCell = targetRow.getCell(2);
                if (startTimeCell == null) {
                    startTimeCell = targetRow.createCell(2);
                }
                startTimeCell.setCellValue(request.getStartTime());
            }

            if (request.getEndTime() != null && !request.getEndTime().trim().isEmpty()) {
                Cell endTimeCell = targetRow.getCell(3);
                if (endTimeCell == null) {
                    endTimeCell = targetRow.createCell(3);
                }
                endTimeCell.setCellValue(request.getEndTime());
            }

            // 更新请假类型（存储在备注列，假设为第5列）
            if (request.getLeaveType() != null && request.getLeaveType() != DailyRecord.LeaveType.NONE) {
                Cell leaveTypeCell = targetRow.getCell(4);
                if (leaveTypeCell == null) {
                    leaveTypeCell = targetRow.createCell(4);
                }
                leaveTypeCell.setCellValue(request.getLeaveType().getDescription());
            }

            // 更新备注（假设为第6列）
            if (request.getRemark() != null && !request.getRemark().trim().isEmpty()) {
                Cell remarkCell = targetRow.getCell(5);
                if (remarkCell == null) {
                    remarkCell = targetRow.createCell(5);
                }
                remarkCell.setCellValue(request.getRemark());
            }

            // 保存文件
            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }

            log.info("考勤记录提交成功: {}", request.getDate());

        } catch (Exception e) {
            log.error("提交考勤记录失败", e);
            throw new IOException("提交考勤记录失败: " + e.getMessage(), e);
        }
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
                    try {
                        return cell.getLocalDateTimeCellValue().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    } catch (Exception e) {
                        return "";
                    }
                } else {
                    return String.valueOf((int) cell.getNumericCellValue());
                }
            default:
                return "";
        }
    }
}

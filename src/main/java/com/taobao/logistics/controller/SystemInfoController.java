package com.taobao.logistics.controller;

import com.taobao.logistics.model.SystemInfo;
import com.taobao.logistics.service.SystemInfoService;
import com.taobao.logistics.utils.AjaxResult;
import com.taobao.logistics.utils.PageResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.DateUtil;

/**
 * 系统信息Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/systemInfo")
public class SystemInfoController {

    @Autowired
    private SystemInfoService systemInfoService;

    /**
     * 查询所有系统信息
     * @return 系统信息列表
     */
    @GetMapping("/list")
    public AjaxResult selectSystemInfoList() {
        List<SystemInfo> list = systemInfoService.selectSystemInfoList();
        return AjaxResult.success(list);
    }

    /**
     * 分页查询系统信息
     * @param current 当前页码，默认1
     * @param size 每页大小，默认10
     * @return 分页结果
     */
    @GetMapping("/listByPage")
    public AjaxResult selectSystemInfoListByPage(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size) {
        log.info("分页查询系统信息，当前页码：{}，每页大小：{}", current, size);
        
        PageResult<SystemInfo> pageResult = systemInfoService.selectSystemInfoListByPage(current, size);
        return AjaxResult.success(pageResult);
    }

    /**
     * 根据ID查询系统信息
     * @param id 主键ID
     * @return 系统信息
     */
    @GetMapping("/info/{id}")
    public AjaxResult selectSystemInfoById(@PathVariable Long id) {
        SystemInfo systemInfo = systemInfoService.selectSystemInfoById(id);
        return AjaxResult.success(systemInfo);
    }

    /**
     * 新增系统信息
     * @param systemInfo 系统信息
     * @return 结果
     */
    @PostMapping("/add")
    public AjaxResult insertSystemInfo(@RequestBody SystemInfo systemInfo) {
        int rows = systemInfoService.insertSystemInfo(systemInfo);
        if (rows > 0) {
            return AjaxResult.success("新增成功");
        }
        return AjaxResult.error("新增失败");
    }

    /**
     * 批量新增系统信息
     * @param systemInfoList 系统信息列表
     * @return 结果
     */
    @PostMapping("/batchAdd")
    public AjaxResult batchInsertSystemInfo(@RequestBody List<SystemInfo> systemInfoList) {
        int rows = systemInfoService.batchInsertSystemInfo(systemInfoList);
        if (rows > 0) {
            return AjaxResult.success("批量新增成功");
        }
        return AjaxResult.error("批量新增失败");
    }

    /**
     * 修改系统信息
     * @param systemInfo 系统信息
     * @return 结果
     */
    @PutMapping("/update")
    public AjaxResult updateSystemInfo(@RequestBody SystemInfo systemInfo) {
        int rows = systemInfoService.updateSystemInfo(systemInfo);
        if (rows > 0) {
            return AjaxResult.success("修改成功");
        }
        return AjaxResult.error("修改失败");
    }

    /**
     * 删除系统信息
     * @param id 主键ID
     * @return 结果
     */
    @DeleteMapping("/delete/{id}")
    public AjaxResult deleteSystemInfoById(@PathVariable Long id) {
        int rows = systemInfoService.deleteSystemInfoById(id);
        if (rows > 0) {
            return AjaxResult.success("删除成功");
        }
        return AjaxResult.error("删除失败");
    }

    /**
     * 批量删除系统信息
     * @param ids 主键ID列表
     * @return 结果
     */
    @DeleteMapping("/batchDelete")
    public AjaxResult batchDeleteSystemInfo(@RequestBody Long[] ids) {
        int rows = systemInfoService.batchDeleteSystemInfo(ids);
        if (rows > 0) {
            return AjaxResult.success("批量删除成功");
        }
        return AjaxResult.error("批量删除失败");
    }

    /**
     * 导入系统信息（支持CSV和Excel格式）
     * @param file 导入文件
     * @return 结果
     */
    @PostMapping("/import")
    public AjaxResult importSystemInfo(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return AjaxResult.error("文件不能为空");
            }

            List<SystemInfo> systemInfoList = new ArrayList<>();
            String fileName = file.getOriginalFilename();
            
            // 判断文件类型
            if (fileName.endsWith(".csv")) {
                // CSV文件处理
                systemInfoList = parseCsvFile(file);
            } else if (fileName.endsWith(".xls") || fileName.endsWith(".xlsx")) {
                // Excel文件处理
                systemInfoList = parseExcelFile(file);
            } else {
                return AjaxResult.error("不支持的文件格式，请上传CSV或Excel文件");
            }

            if (systemInfoList.isEmpty()) {
                return AjaxResult.error("文件中没有有效数据");
            }

            // 批量插入数据
            int rows = systemInfoService.batchInsertSystemInfo(systemInfoList);
            if (rows > 0) {
                return AjaxResult.success("导入成功，共导入 " + rows + " 条数据");
            }
            return AjaxResult.error("导入失败");
        } catch (Exception e) {
            e.printStackTrace();
            return AjaxResult.error("导入失败：" + e.getMessage());
        }
    }

    /**
     * 解析CSV文件
     * @param file CSV文件
     * @return 系统信息列表
     * @throws Exception 异常
     */
    private List<SystemInfo> parseCsvFile(MultipartFile file) throws Exception {
        List<SystemInfo> systemInfoList = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), "UTF-8"));
        String line;
        // 跳过表头
        reader.readLine();
        // 读取数据行
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            
            // 分割CSV行，处理引号内的逗号
            List<String> fields = parseCsvLine(line);
            if (fields.size() < 8) {
                continue; // 跳过无效行
            }

            SystemInfo systemInfo = new SystemInfo();
            systemInfo.setSystemName(fields.get(0).trim());
            systemInfo.setFunctionDesc(fields.get(1).trim());
            systemInfo.setResponsiblePerson(fields.get(2).trim());
            systemInfo.setSource(fields.get(3).trim());
            systemInfo.setType(fields.get(4).trim());
            systemInfo.setIsUsing(fields.get(5).trim());
            systemInfo.setCanPromote(fields.get(6).trim());
            if (fields.size() > 7) {
                systemInfo.setIdleFunctions(fields.get(7).trim());
            }

            systemInfoList.add(systemInfo);
        }
        reader.close();
        return systemInfoList;
    }

    /**
     * 解析Excel文件
     * @param file Excel文件
     * @return 系统信息列表
     * @throws Exception 异常
     */
    private List<SystemInfo> parseExcelFile(MultipartFile file) throws Exception {
        List<SystemInfo> systemInfoList = new ArrayList<>();
        Workbook workbook = null;
        
        try {
            // 根据文件类型创建Workbook
            if (file.getOriginalFilename().endsWith(".xls")) {
                // .xls格式
                workbook = new HSSFWorkbook(file.getInputStream());
            } else {
                // .xlsx格式
                workbook = new XSSFWorkbook(file.getInputStream());
            }
            
            // 获取第一个工作表
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                return systemInfoList;
            }
            
            // 遍历行，从第二行开始（跳过表头）
            for (int rowNum = 1; rowNum <= sheet.getLastRowNum(); rowNum++) {
                Row row = sheet.getRow(rowNum);
                if (row == null) {
                    continue;
                }
                
                // 获取单元格数据
                List<String> fields = new ArrayList<>();
                for (int cellNum = 0; cellNum < 8; cellNum++) {
                    Cell cell = row.getCell(cellNum);
                    fields.add(getCellValue(cell));
                }
                
                // 获取第9个单元格（如果有）
                Cell cell8 = row.getCell(8);
                if (cell8 != null) {
                    fields.add(getCellValue(cell8));
                }
                
                if (fields.get(0).isEmpty()) {
                    continue; // 跳过空行
                }
                
                SystemInfo systemInfo = new SystemInfo();
                systemInfo.setSystemName(fields.get(0).trim());
                systemInfo.setFunctionDesc(fields.get(1).trim());
                systemInfo.setResponsiblePerson(fields.get(2).trim());
                systemInfo.setSource(fields.get(3).trim());
                systemInfo.setType(fields.get(4).trim());
                systemInfo.setIsUsing(fields.get(5).trim());
                systemInfo.setCanPromote(fields.get(6).trim());
                if (fields.size() > 7) {
                    systemInfo.setIdleFunctions(fields.get(7).trim());
                }
                
                systemInfoList.add(systemInfo);
            }
        } finally {
            if (workbook != null) {
                workbook.close();
            }
        }
        
        return systemInfoList;
    }

    /**
     * 获取Excel单元格的值
     * @param cell 单元格
     * @return 单元格值
     */
    private String getCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        
        String value = "";
        // 使用DecimalFormat确保数值以普通格式显示，避免科学计数法
        java.text.DecimalFormat df = new java.text.DecimalFormat("0.##########");
        
        switch (cell.getCellType()) {
            case STRING:
                value = cell.getStringCellValue();
                break;
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    value = cell.getDateCellValue().toString();
                } else {
                    // 使用DecimalFormat格式化数值
                    value = df.format(cell.getNumericCellValue());
                    // 移除小数位的.0
                    if (value.endsWith(".0")) {
                        value = value.substring(0, value.length() - 2);
                    }
                }
                break;
            case BOOLEAN:
                value = String.valueOf(cell.getBooleanCellValue());
                break;
            case FORMULA:
                try {
                    value = cell.getStringCellValue();
                } catch (IllegalStateException e) {
                    // 使用DecimalFormat格式化数值
                    value = df.format(cell.getNumericCellValue());
                    if (value.endsWith(".0")) {
                        value = value.substring(0, value.length() - 2);
                    }
                }
                break;
            default:
                value = "";
        }
        
        return value;
    }

    /**
     * 解析CSV行，处理引号内的逗号
     * @param line CSV行
     * @return 字段列表
     */
    private List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;

        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(currentField.toString());
                currentField.setLength(0);
            } else {
                currentField.append(c);
            }
        }
        fields.add(currentField.toString());

        return fields;
    }

    /**
     * 导出系统信息到Excel
     * @param response 响应对象
     * @throws Exception 异常
     */
    @GetMapping("/export")
    public void exportSystemInfo(HttpServletResponse response) throws Exception {
        log.info("导出系统信息到Excel");

        // 查询所有系统信息
        List<SystemInfo> systemInfoList = systemInfoService.selectSystemInfoList();
        if (systemInfoList.isEmpty()) {
            response.getWriter().write("没有数据可导出");
            return;
        }

        // 创建工作簿
        Workbook workbook = new HSSFWorkbook();
        Sheet sheet = workbook.createSheet("系统信息");

        // 设置列宽
        sheet.setColumnWidth(0, 256 * 25); // 系统/接口名称
        sheet.setColumnWidth(1, 256 * 40); // 功能简介
        sheet.setColumnWidth(2, 256 * 15); // 责任人
        sheet.setColumnWidth(3, 256 * 15); // 来源
        sheet.setColumnWidth(4, 256 * 15); // 类型
        sheet.setColumnWidth(5, 256 * 15); // 使用状态
        sheet.setColumnWidth(6, 256 * 15); // 推广状态
        sheet.setColumnWidth(7, 256 * 30); // 闲置功能盘点

        // 创建标题行
        Row headerRow = sheet.createRow(0);
        String[] headers = {"系统/接口名称", "功能简介", "责任人", "来源", "类型", "使用状态", "推广状态", "闲置功能盘点"};
        
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 创建数据行
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        for (int rowNum = 0; rowNum < systemInfoList.size(); rowNum++) {
            SystemInfo systemInfo = systemInfoList.get(rowNum);
            Row row = sheet.createRow(rowNum + 1);

            // 系统/接口名称
            Cell cell0 = row.createCell(0);
            cell0.setCellValue(systemInfo.getSystemName() != null ? systemInfo.getSystemName() : "");
            cell0.setCellStyle(dataStyle);

            // 功能简介
            Cell cell1 = row.createCell(1);
            cell1.setCellValue(systemInfo.getFunctionDesc() != null ? systemInfo.getFunctionDesc() : "");
            cell1.setCellStyle(dataStyle);

            // 责任人
            Cell cell2 = row.createCell(2);
            cell2.setCellValue(systemInfo.getResponsiblePerson() != null ? systemInfo.getResponsiblePerson() : "");
            cell2.setCellStyle(dataStyle);

            // 来源
            Cell cell3 = row.createCell(3);
            cell3.setCellValue(systemInfo.getSource() != null ? systemInfo.getSource() : "");
            cell3.setCellStyle(dataStyle);

            // 类型
            Cell cell4 = row.createCell(4);
            cell4.setCellValue(systemInfo.getType() != null ? systemInfo.getType() : "");
            cell4.setCellStyle(dataStyle);

            // 使用状态
            Cell cell5 = row.createCell(5);
            cell5.setCellValue(systemInfo.getIsUsing() != null ? systemInfo.getIsUsing() : "");
            cell5.setCellStyle(dataStyle);

            // 推广状态
            Cell cell6 = row.createCell(6);
            cell6.setCellValue(systemInfo.getCanPromote() != null ? systemInfo.getCanPromote() : "");
            cell6.setCellStyle(dataStyle);

            // 闲置功能盘点
            Cell cell7 = row.createCell(7);
            cell7.setCellValue(systemInfo.getIdleFunctions() != null ? systemInfo.getIdleFunctions() : "");
            cell7.setCellStyle(dataStyle);
        }

        // 设置响应头
        String fileName = "系统信息_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".xls";
        response.setContentType("application/vnd.ms-excel");
        response.setHeader("Content-Disposition", "attachment;filename=" + new String(fileName.getBytes("UTF-8"), "ISO-8859-1"));

        // 写入响应流
        OutputStream outputStream = response.getOutputStream();
        workbook.write(outputStream);
        outputStream.flush();
        outputStream.close();
        workbook.close();

        log.info("系统信息导出完成，共导出 {} 条记录", systemInfoList.size());
    }

    /**
     * 导出选中的系统信息到Excel
     * @param ids 选中的ID列表
     * @param response 响应对象
     * @throws Exception 异常
     */
    @PostMapping("/exportSelected")
    public void exportSelectedSystemInfo(@RequestParam("ids") String ids, HttpServletResponse response) throws Exception {
        log.info("导出选中的系统信息，ID列表：{}", ids);

        if (ids == null || ids.trim().isEmpty()) {
            response.getWriter().write("请先选择要导出的数据");
            return;
        }

        // 解析ID字符串
        String[] idArray = ids.split(",");
        List<Long> idList = new ArrayList<>();
        for (String idStr : idArray) {
            try {
                idList.add(Long.parseLong(idStr.trim()));
            } catch (NumberFormatException e) {
                log.warn("无效的ID格式：{}", idStr);
            }
        }

        if (idList.isEmpty()) {
            response.getWriter().write("没有有效的选中数据");
            return;
        }

        // 查询选中的系统信息
        List<SystemInfo> selectedSystemInfoList = new ArrayList<>();
        for (Long id : idList) {
            SystemInfo systemInfo = systemInfoService.selectSystemInfoById(id);
            if (systemInfo != null) {
                selectedSystemInfoList.add(systemInfo);
            }
        }

        if (selectedSystemInfoList.isEmpty()) {
            response.getWriter().write("选中的数据不存在或已被删除");
            return;
        }

        // 创建工作簿
        Workbook workbook = new HSSFWorkbook();
        Sheet sheet = workbook.createSheet("选中系统信息");

        // 设置列宽
        sheet.setColumnWidth(0, 256 * 25); // 系统/接口名称
        sheet.setColumnWidth(1, 256 * 40); // 功能简介
        sheet.setColumnWidth(2, 256 * 15); // 责任人
        sheet.setColumnWidth(3, 256 * 15); // 来源
        sheet.setColumnWidth(4, 256 * 15); // 类型
        sheet.setColumnWidth(5, 256 * 15); // 使用状态
        sheet.setColumnWidth(6, 256 * 15); // 推广状态
        sheet.setColumnWidth(7, 256 * 30); // 闲置功能盘点

        // 创建标题行
        Row headerRow = sheet.createRow(0);
        String[] headers = {"系统/接口名称", "功能简介", "责任人", "来源", "类型", "使用状态", "推广状态", "闲置功能盘点"};
        
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 创建数据行
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        for (int rowNum = 0; rowNum < selectedSystemInfoList.size(); rowNum++) {
            SystemInfo systemInfo = selectedSystemInfoList.get(rowNum);
            Row row = sheet.createRow(rowNum + 1);

            // 系统/接口名称
            Cell cell0 = row.createCell(0);
            cell0.setCellValue(systemInfo.getSystemName() != null ? systemInfo.getSystemName() : "");
            cell0.setCellStyle(dataStyle);

            // 功能简介
            Cell cell1 = row.createCell(1);
            cell1.setCellValue(systemInfo.getFunctionDesc() != null ? systemInfo.getFunctionDesc() : "");
            cell1.setCellStyle(dataStyle);

            // 责任人
            Cell cell2 = row.createCell(2);
            cell2.setCellValue(systemInfo.getResponsiblePerson() != null ? systemInfo.getResponsiblePerson() : "");
            cell2.setCellStyle(dataStyle);

            // 来源
            Cell cell3 = row.createCell(3);
            cell3.setCellValue(systemInfo.getSource() != null ? systemInfo.getSource() : "");
            cell3.setCellStyle(dataStyle);

            // 类型
            Cell cell4 = row.createCell(4);
            cell4.setCellValue(systemInfo.getType() != null ? systemInfo.getType() : "");
            cell4.setCellStyle(dataStyle);

            // 使用状态
            Cell cell5 = row.createCell(5);
            cell5.setCellValue(systemInfo.getIsUsing() != null ? systemInfo.getIsUsing() : "");
            cell5.setCellStyle(dataStyle);

            // 推广状态
            Cell cell6 = row.createCell(6);
            cell6.setCellValue(systemInfo.getCanPromote() != null ? systemInfo.getCanPromote() : "");
            cell6.setCellStyle(dataStyle);

            // 闲置功能盘点
            Cell cell7 = row.createCell(7);
            cell7.setCellValue(systemInfo.getIdleFunctions() != null ? systemInfo.getIdleFunctions() : "");
            cell7.setCellStyle(dataStyle);
        }

        // 设置响应头
        String fileName = "选中系统信息_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".xls";
        response.setContentType("application/vnd.ms-excel");
        response.setHeader("Content-Disposition", "attachment;filename=" + new String(fileName.getBytes("UTF-8"), "ISO-8859-1"));

        // 写入响应流
        OutputStream outputStream = response.getOutputStream();
        workbook.write(outputStream);
        outputStream.flush();
        outputStream.close();
        workbook.close();

        log.info("选中系统信息导出完成，共导出 {} 条记录", selectedSystemInfoList.size());
    }
}
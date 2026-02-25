package com.taobao.logistics.controller;

import com.taobao.logistics.model.WorkHour;
import com.taobao.logistics.service.WorkHourService;
import com.taobao.logistics.utils.AjaxResult;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * 员工工时Controller
 */
@RestController
@RequestMapping("/api/workHour")
public class WorkHourController {

    @Autowired
    private WorkHourService workHourService;

    /**
     * 查询所有工时记录
     * @return 工时记录列表
     */
    @GetMapping("/list")
    public AjaxResult selectWorkHourList() {
        List<WorkHour> list = workHourService.selectWorkHourList();
        return AjaxResult.success(list);
    }

    /**
     * 根据ID查询工时记录
     * @param id 主键ID
     * @return 工时记录
     */
    @GetMapping("/info/{id}")
    public AjaxResult selectWorkHourById(@PathVariable Long id) {
        WorkHour workHour = workHourService.selectWorkHourById(id);
        return AjaxResult.success(workHour);
    }

    /**
     * 新增工时记录
     * @param workHour 工时记录
     * @return 结果
     */
    @PostMapping("/add")
    public AjaxResult insertWorkHour(@RequestBody WorkHour workHour) {
        int rows = workHourService.insertWorkHour(workHour);
        if (rows > 0) {
            return AjaxResult.success("新增成功");
        }
        return AjaxResult.error("新增失败");
    }

    /**
     * 批量新增工时记录
     * @param workHourList 工时记录列表
     * @return 结果
     */
    @PostMapping("/batchAdd")
    public AjaxResult batchInsertWorkHour(@RequestBody List<WorkHour> workHourList) {
        int rows = workHourService.batchInsertWorkHour(workHourList);
        if (rows > 0) {
            return AjaxResult.success("批量新增成功");
        }
        return AjaxResult.error("批量新增失败");
    }

    /**
     * 修改工时记录
     * @param workHour 工时记录
     * @return 结果
     */
    @PutMapping("/update")
    public AjaxResult updateWorkHour(@RequestBody WorkHour workHour) {
        int rows = workHourService.updateWorkHour(workHour);
        if (rows > 0) {
            return AjaxResult.success("修改成功");
        }
        return AjaxResult.error("修改失败");
    }

    /**
     * 删除工时记录
     * @param id 主键ID
     * @return 结果
     */
    @DeleteMapping("/delete/{id}")
    public AjaxResult deleteWorkHourById(@PathVariable Long id) {
        int rows = workHourService.deleteWorkHourById(id);
        if (rows > 0) {
            return AjaxResult.success("删除成功");
        }
        return AjaxResult.error("删除失败");
    }

    /**
     * 批量删除工时记录
     * @param ids 主键ID列表
     * @return 结果
     */
    @DeleteMapping("/batchDelete")
    public AjaxResult batchDeleteWorkHour(@RequestBody Long[] ids) {
        int rows = workHourService.batchDeleteWorkHour(ids);
        if (rows > 0) {
            return AjaxResult.success("批量删除成功");
        }
        return AjaxResult.error("批量删除失败");
    }
    
    /**
     * 按费用归属部门统计执行工时合计
     * @return 统计结果列表
     */
    @GetMapping("/statistics")
    public AjaxResult selectWorkHourStatisticsByCostDepartment() {
        List<java.util.Map<String, Object>> statistics = workHourService.selectWorkHourStatisticsByCostDepartment();
        return AjaxResult.success(statistics);
    }
    
    /**
     * 按系统统计执行工时合计
     * @return 统计结果列表
     */
    @GetMapping("/statistics/system")
    public AjaxResult selectWorkHourStatisticsBySystem() {
        List<java.util.Map<String, Object>> statistics = workHourService.selectWorkHourStatisticsBySystem();
        return AjaxResult.success(statistics);
    }
    
    /**
     * 按员工统计执行工时合计
     * @return 统计结果列表
     */
    @GetMapping("/statistics/employee")
    public AjaxResult selectWorkHourStatisticsByEmployee() {
        List<java.util.Map<String, Object>> statistics = workHourService.selectWorkHourStatisticsByEmployee();
        return AjaxResult.success(statistics);
    }
    
    /**
     * 按系统和员工统计执行工时合计
     * @return 统计结果列表
     */
    @GetMapping("/statistics/systemEmployee")
    public AjaxResult selectWorkHourStatisticsBySystemAndEmployee() {
        List<java.util.Map<String, Object>> statistics = workHourService.selectWorkHourStatisticsBySystemAndEmployee();
        return AjaxResult.success(statistics);
    }
    
    /**
     * 获取系统发展时间轴数据
     * @param system 系统名称
     * @return 发展时间轴数据列表
     */
    @GetMapping("/developmentTimeline")
    public AjaxResult selectDevelopmentTimelineBySystem(String system) {
        List<java.util.Map<String, Object>> timelineData = workHourService.selectDevelopmentTimelineBySystem(system);
        return AjaxResult.success(timelineData);
    }

    /**
     * 导入工时记录（支持Excel格式）
     * @param file 导入文件
     * @return 结果
     */
    @PostMapping("/import")
    public AjaxResult importWorkHour(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return AjaxResult.error("文件不能为空");
            }

            List<WorkHour> workHourList = new ArrayList<>();
            String fileName = file.getOriginalFilename();
            
            // 判断文件类型，只支持Excel文件
            if (fileName.endsWith(".xls") || fileName.endsWith(".xlsx")) {
                // Excel文件处理
                workHourList = parseExcelFile(file);
            } else {
                return AjaxResult.error("不支持的文件格式，请上传Excel文件(.xls, .xlsx)");
            }

            if (workHourList.isEmpty()) {
                return AjaxResult.error("文件中没有有效数据");
            }

            // 批量插入数据
            int rows = workHourService.batchInsertWorkHour(workHourList);
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
     * 解析Excel文件
     * @param file Excel文件
     * @return 工时记录列表
     * @throws Exception 异常
     */
    private List<WorkHour> parseExcelFile(MultipartFile file) throws Exception {
        List<WorkHour> workHourList = new ArrayList<>();
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
                return workHourList;
            }
            
            // 遍历行，从第二行开始（跳过表头）
            for (int rowNum = 1; rowNum <= sheet.getLastRowNum(); rowNum++) {
                Row row = sheet.getRow(rowNum);
                if (row == null) {
                    continue;
                }
                
                // 获取单元格数据
                List<String> fields = new ArrayList<>();
                for (int cellNum = 0; cellNum < 12; cellNum++) {
                    Cell cell = row.getCell(cellNum);
                    fields.add(getCellValue(cell));
                }
                
                // 检查系统字段是否为空，为空则跳过
                if (fields.get(0).trim().isEmpty()) {
                    continue;
                }
                
                WorkHour workHour = new WorkHour();
                workHour.setSystem(fields.get(0).trim());
                workHour.setWorkMonth(fields.get(1).trim());
                String workWeek = fields.get(2).trim();
                workHour.setWorkWeek(workWeek);
                
                // 计算工作周最后一天的日期
                String workWeekEndDate = calculateWorkWeekEndDate(workWeek);
                workHour.setWorkWeekEndDate(workWeekEndDate);
                
                workHour.setEmployee(fields.get(3).trim());
                workHour.setWorkCategory(fields.get(4).trim());
                workHour.setType(fields.get(5).trim());
                workHour.setItem(fields.get(6).trim());
                workHour.setReporter(fields.get(7).trim());
                workHour.setCostDepartment(fields.get(8).trim());
                
                // 计划工时和执行工时转换为Double类型
                try {
                    if (!fields.get(9).trim().isEmpty()) {
                        workHour.setPlannedHours(Double.parseDouble(fields.get(9).trim()));
                    }
                } catch (NumberFormatException e) {
                    // 忽略格式错误，默认为null
                }
                
                try {
                    if (!fields.get(10).trim().isEmpty()) {
                        workHour.setActualHours(Double.parseDouble(fields.get(10).trim()));
                    }
                } catch (NumberFormatException e) {
                    // 忽略格式错误，默认为null
                }
                
                workHourList.add(workHour);
            }
        } finally {
            if (workbook != null) {
                workbook.close();
            }
        }
        
        return workHourList;
    }

    /**
     * 计算工作周最后一天的日期
     * @param workWeek 工作周，格式如"周1"、"第1周"或直接是数字
     * @return 工作周最后一天的日期，格式为yyyyMMdd，如20250105
     */
    private String calculateWorkWeekEndDate(String workWeek) {
        try {
            // 提取工作周数字
            int weekNumber = 1;
            if (workWeek.contains("周")) {
                // 格式如"周1"或"第1周"
                String weekStr = workWeek.replaceAll("[^0-9]", "");
                if (!weekStr.isEmpty()) {
                    weekNumber = Integer.parseInt(weekStr);
                }
            } else {
                // 直接是数字
                weekNumber = Integer.parseInt(workWeek);
            }
            
            // 使用当前年份
            int year = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
            
            // 计算工作周最后一天的日期
            java.util.Calendar calendar = java.util.Calendar.getInstance();
            calendar.setMinimalDaysInFirstWeek(4); // ISO 8601标准，每年第一周至少包含4天
            calendar.set(java.util.Calendar.YEAR, year);
            calendar.set(java.util.Calendar.WEEK_OF_YEAR, weekNumber);
            calendar.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.SATURDAY); // 周六作为工作周最后一天
            
            // 格式化日期为yyyyMMdd
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyyMMdd");
            return sdf.format(calendar.getTime());
        } catch (Exception e) {
            // 如果计算失败，返回空字符串
            return "";
        }
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
        switch (cell.getCellType()) {
            case STRING:
                value = cell.getStringCellValue();
                break;
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    value = cell.getDateCellValue().toString();
                } else {
                    value = String.valueOf(cell.getNumericCellValue());
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
                    value = String.valueOf(cell.getNumericCellValue());
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
}
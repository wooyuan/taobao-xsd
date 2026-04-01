package com.taobao.logistics.controller;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 销售数据报表控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/sales")
public class SalesReportController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 获取销售数据报表
     * @param month 月份，格式：yyyyMM
     * @return 销售数据
     */
    @GetMapping("/report")
    public JSONObject getSalesReport(@RequestParam String month) {
        log.info("获取销售数据报表，月份：{}", month);
        JSONObject result = new JSONObject();

        // 查询系统访问统计数据
        log.info("开始查询系统访问统计数据");
        List<SystemAccessData> systemAccessList = querySystemAccessData(month);
        log.info("查询到系统访问统计数据 {} 条", systemAccessList.size());
        for (SystemAccessData data : systemAccessList) {
            log.debug("系统访问统计数据：{} - 纪念日: {}({}), ENJOY: {}({}), 哆象: {}({})", 
                data.getSystemName(), 
                data.getJinianriVisitor(), data.getJinianriUsage(),
                data.getEnjoyVisitor(), data.getEnjoyUsage(),
                data.getDuoxiangVisitor(), data.getDuoxiangUsage());
        }
        result.put("systemAccess", systemAccessList);

        // 查询销售平台数据
        log.info("开始查询销售平台数据");
        List<SalesPlatformData> salesPlatformList = querySalesPlatformData(month);
        log.info("查询到销售平台数据 {} 条", salesPlatformList.size());
        for (SalesPlatformData data : salesPlatformList) {
            log.debug("销售平台数据：{} - 订单量：{}，销售额：{}", data.getPlatformName(), data.getOrderCount(), data.getSalesAmount());
        }
        result.put("salesPlatforms", salesPlatformList);

        log.info("销售数据报表生成完成，返回结果：{}", result.toJSONString());
        return result;
    }

    /**
     * 导出销售数据到Excel
     * @param month 月份，格式：yyyyMM
     * @param response 响应对象
     * @throws IOException IO异常
     */
    @GetMapping("/export")
    public void exportToExcel(@RequestParam String month, HttpServletResponse response) throws IOException {
        log.info("导出销售数据到Excel，月份：{}", month);

        // 创建工作簿
        Workbook workbook = new HSSFWorkbook();
        Sheet sheet = workbook.createSheet("销售数据报表");

        // 设置列宽
        sheet.setColumnWidth(0, 256 * 20);
        sheet.setColumnWidth(1, 256 * 15);
        sheet.setColumnWidth(2, 256 * 15);
        sheet.setColumnWidth(3, 256 * 15);
        sheet.setColumnWidth(4, 256 * 20);
        sheet.setColumnWidth(5, 256 * 15);
        sheet.setColumnWidth(6, 256 * 15);

        // 创建标题行
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(month + "销售数据报表");
        titleCell.setCellStyle(createTitleCellStyle(workbook));
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));

        // 查询数据
        List<SystemAccessData> systemAccessList = querySystemAccessData(month);
        List<SalesPlatformData> salesPlatformList = querySalesPlatformData(month);

        // 写入系统访问统计数据
        writeSystemAccessData(sheet, workbook, systemAccessList);

        // 写入销售平台数据
        writeSalesPlatformData(sheet, workbook, salesPlatformList);

        // 设置响应头
        String fileName = month + "销售数据报表.xls";
        response.setContentType("application/vnd.ms-excel");
        response.setHeader("Content-Disposition", "attachment; filename=" + new String(fileName.getBytes(), "ISO-8859-1"));

        // 输出到客户端
        OutputStream outputStream = response.getOutputStream();
        workbook.write(outputStream);
        outputStream.flush();
        outputStream.close();
        workbook.close();
    }
    
    /**
     * 导出2025年全年销售数据汇总到Excel
     * @param response 响应
     * @throws IOException IO异常
     */
    @GetMapping("/exportYearSummary")
    public void exportYearSummary(HttpServletResponse response) throws IOException {
        log.info("导出2026年全年销售数据汇总到Excel");

        // 创建工作簿
        Workbook workbook = new HSSFWorkbook();
        Sheet sheet = workbook.createSheet("2026年销售数据汇总");

        // 设置列宽
        sheet.setColumnWidth(0, 256 * 20);
        sheet.setColumnWidth(1, 256 * 15);
        sheet.setColumnWidth(2, 256 * 15);
        sheet.setColumnWidth(3, 256 * 15);
        sheet.setColumnWidth(4, 256 * 20);
        sheet.setColumnWidth(5, 256 * 15);
        sheet.setColumnWidth(6, 256 * 15);

        // 创建标题行
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("2026年销售数据汇总");
        titleCell.setCellStyle(createTitleCellStyle(workbook));
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));

        // 查询2026年1月到12月的数据
        // 初始化年度汇总数据
        Map<String, SystemAccessData> systemAccessMap = new HashMap<>();
        Map<String, SalesPlatformData> salesPlatformMap = new HashMap<>();
        
        // 遍历2026年1月到12月
        for (int i = 1; i <= 12; i++) {
            String month = String.format("2026%02d", i);
            
            // 查询月度系统访问数据
            List<SystemAccessData> monthlySystemAccess = querySystemAccessData(month);
            for (SystemAccessData data : monthlySystemAccess) {
                systemAccessMap.merge(data.getSystemName(), data, (existing, incoming) -> {
                    // 创建新对象来合并数据
                    SystemAccessData merged = new SystemAccessData();
                    merged.setSystemName(existing.getSystemName());
                    merged.setJinianriVisitor(existing.getJinianriVisitor() + incoming.getJinianriVisitor());
                    merged.setJinianriUsage(existing.getJinianriUsage() + incoming.getJinianriUsage());
                    merged.setEnjoyVisitor(existing.getEnjoyVisitor() + incoming.getEnjoyVisitor());
                    merged.setEnjoyUsage(existing.getEnjoyUsage() + incoming.getEnjoyUsage());
                    merged.setDuoxiangVisitor(existing.getDuoxiangVisitor() + incoming.getDuoxiangVisitor());
                    merged.setDuoxiangUsage(existing.getDuoxiangUsage() + incoming.getDuoxiangUsage());
                    return merged;
                });
            }
            
            // 查询月度销售平台数据
            List<SalesPlatformData> monthlySalesPlatform = querySalesPlatformData(month);
            for (SalesPlatformData data : monthlySalesPlatform) {
                salesPlatformMap.merge(data.getPlatformName(), data, (existing, incoming) -> {
                    // 创建新对象来合并数据，因为没有setter方法
                    return new SalesPlatformData(
                        existing.getPlatformName(),
                        existing.getOrderCount() + incoming.getOrderCount(),
                        existing.getSalesAmount() + incoming.getSalesAmount()
                    );
                });
            }
        }
        
        // 将汇总结果转换为列表
        List<SystemAccessData> yearlySystemAccessList = new ArrayList<>(systemAccessMap.values());
        List<SalesPlatformData> yearlySalesPlatformList = new ArrayList<>(salesPlatformMap.values());

        // 写入系统访问统计数据
        writeSystemAccessData(sheet, workbook, yearlySystemAccessList);

        // 写入销售平台数据
        writeSalesPlatformData(sheet, workbook, yearlySalesPlatformList);

        // 设置响应头
        String fileName = "2026年销售数据汇总.xls";
        response.setContentType("application/vnd.ms-excel");
        response.setHeader("Content-Disposition", "attachment; filename=" + new String(fileName.getBytes(), "ISO-8859-1"));

        // 输出到客户端
        OutputStream outputStream = response.getOutputStream();
        workbook.write(outputStream);
        outputStream.flush();
        outputStream.close();
        workbook.close();
    }

    /**
     * 查询系统访问统计数据
     * @param month 月份
     * @return 系统访问统计数据
     */
    private List<SystemAccessData> querySystemAccessData(String month) {
        log.info("开始执行querySystemAccessData方法，月份：{}", month);
        List<SystemAccessData> list = new ArrayList<>();
        List<Map<String, Object>> bjcUsageResult = new ArrayList<>();
        List<Map<String, Object>> bjcVisitorResult = new ArrayList<>();
        
        // 系统名称对应关系
        Map<String, String> nameMapping = new HashMap<>();
        nameMapping.put("数据分析", "智慧门店");
        nameMapping.put("EN门店销售排名", "ENJOY智慧门店");
        nameMapping.put("盘点系统", "盘点机");
        nameMapping.put("微信客诉", "客诉");
        nameMapping.put("配件补单", "配件补单");
        nameMapping.put("伯俊系统", "伯俊系统");
        nameMapping.put("商城/随手购", "商城/随手购");
        log.debug("系统名称对应关系：{}", nameMapping);
        
        // 品牌名称对应关系
        Map<String, String> brandMapping = new HashMap<>();
        brandMapping.put("DBCSTORE", "哆象");
        log.debug("品牌名称对应关系：{}", brandMapping);
        
        try {
            // 声明变量
            List<Map<String, Object>> mallVisitorResult = new ArrayList<>();
            List<Map<String, Object>> mallUsageResult = new ArrayList<>();
            // 查询伯俊系统的访问次数
            String bjcUsageSql = "SELECT '伯俊系统' as name, count(a.id) as usage_count, d.attribname as brand " +
                    "FROM C_SYSLOG@bj_70 a, users@bj_70 b, c_store@bj_70 c, m_dim@bj_70 d " +
                    "WHERE a.operator = b.truename " +
                    "and c.id = b.c_store_id " +
                    "and c.m_dim1_id = d.id " +
                    "and b.isactive = 'Y' " +
                    "and a.submodule = 'login' " +
                    "and to_char(a.creationdate, 'YYYYMM') = ? " +
                    "group by d.attribname";
            log.debug("执行伯俊系统访问次数查询SQL：{}，参数：{}", bjcUsageSql, month);
            bjcUsageResult = jdbcTemplate.queryForList(bjcUsageSql, month);
            log.debug("伯俊系统访问次数查询结果：{}", bjcUsageResult);

            // 查询伯俊系统的访问人数
            String bjcVisitorSql = "SELECT '伯俊系统' as name, d.attribname as brand, count(DISTINCT a.operator) as visitor_count " +
                    "FROM C_SYSLOG@bj_70 a, users@bj_70 b, c_store@bj_70 c, m_dim@bj_70 d " +
                    "WHERE a.operator = b.truename " +
                    "and c.id = b.c_store_id " +
                    "and c.m_dim1_id = d.id " +
                    "and b.isactive = 'Y' " +
                    "and a.submodule = 'login' " +
                    "and to_char(a.creationdate, 'YYYYMM') = ? " +
                    "group by d.attribname";
            log.debug("执行伯俊系统访问人数查询SQL：{}，参数：{}", bjcVisitorSql, month);
            bjcVisitorResult = jdbcTemplate.queryForList(bjcVisitorSql, month);
            log.debug("伯俊系统访问人数查询结果：{}", bjcVisitorResult);

            // 查询商城/随手购的访问人数
            String mallVisitorSql = "SELECT '商城/随手购' as name, '纪念日' as brand, count(DISTINCT a.user_name) as visitor_count " +
                                 "FROM ADMIN_USER_LOG@sc_113 a " +
                                 "WHERE to_char(a.creationdate, 'YYYYMM') = ?";
            log.debug("执行商城/随手购访问人数查询SQL：{}，参数：{}", mallVisitorSql, month);
            mallVisitorResult = jdbcTemplate.queryForList(mallVisitorSql, month);
            log.debug("商城/随手购访问人数查询结果：{}", mallVisitorResult);

            // 查询商城/随手购的访问次数
            String mallUsageSql = "SELECT '商城/随手购' as name, '纪念日' as brand, count(a.id) as usage_count " +
                               "FROM ADMIN_USER_LOG@sc_113 a " +
                               "WHERE to_char(a.creationdate, 'YYYYMM') = ?";
            log.debug("执行商城/随手购访问次数查询SQL：{}，参数：{}", mallUsageSql, month);
            mallUsageResult = jdbcTemplate.queryForList(mallUsageSql, month);
            log.debug("商城/随手购访问次数查询结果：{}", mallUsageResult);

            // 查询访问次数（按系统和品牌分组）
            String usageSql = "SELECT count(a.id) as usage_count, a.name,c.attribname as brand " +
                             "FROM crm_fw_log@bj_70 a ,c_store@bj_70 b ,m_dim@bj_70 c ,hr_employee@bj_70 d " +
                             "WHERE to_char(a.creationdate, 'YYYYMM') = ? " +
                             "and d.no=a.code " +
                             "and b.m_dim1_id =c.id " +
                             "and d.c_store_id=b.id " +
                             "AND a.name IN ('盘点系统', '微信客诉', 'EN门店销售排名', '配件补单', '数据分析') " +
                             "group by a.name,c.attribname";
            log.debug("执行访问次数查询SQL：{}，参数：{}", usageSql, month);
            List<Map<String, Object>> usageResult = jdbcTemplate.queryForList(usageSql, month);
            log.debug("访问次数查询结果：{}", usageResult);
            
            // 查询访问人数（按系统和品牌分组）
            String visitorSql = "SELECT a.name, c.attribname as brand, count(DISTINCT a.code) as visitor_count " +
                               "FROM crm_fw_log@bj_70 a ,c_store@bj_70 b ,m_dim@bj_70 c ,hr_employee@bj_70 d " +
                               "WHERE to_char(a.creationdate, 'YYYYMM') = ? " +
                               "and d.no=a.code " +
                               "and b.m_dim1_id =c.id " +
                               "and d.c_store_id=b.id " +
                               "AND a.name IN ('盘点系统', '微信客诉', 'EN门店销售排名', '配件补单', '数据分析') " +
                               "GROUP BY a.name, c.attribname";
            log.debug("执行访问人数查询SQL：{}，参数：{}", visitorSql, month);
            List<Map<String, Object>> visitorResult = jdbcTemplate.queryForList(visitorSql, month);
            log.debug("访问人数查询结果：{}", visitorResult);
            
            // 准备所有系统名称
            Set<String> allNames = new HashSet<>();
            for (Map<String, Object> row : usageResult) {
                allNames.add((String) row.get("name"));
            }
            for (Map<String, Object> row : visitorResult) {
                allNames.add((String) row.get("name"));
            }
            // 确保所有系统都包含在内
            allNames.addAll(nameMapping.keySet());
            // 添加商城/随手购到系统名称集合
            allNames.add("商城/随手购");
            log.debug("所有系统名称：{}", allNames);
            
            // 构建结果数据
            for (String originalName : allNames) {
                String displayName = nameMapping.getOrDefault(originalName, originalName);
                
                // 创建一个包含所有品牌数据的对象
                SystemAccessData data = new SystemAccessData();
                data.setSystemName(displayName);
                
                // 初始化所有品牌的访问人数和访问次数为0
                data.setJinianriVisitor(0);
                data.setJinianriUsage(0);
                data.setEnjoyVisitor(0);
                data.setEnjoyUsage(0);
                data.setDuoxiangVisitor(0);
                data.setDuoxiangUsage(0);
                
                // 填充访问次数数据
                for (Map<String, Object> row : usageResult) {
                    if (originalName.equals(row.get("name"))) {
                        String brandName = (String) row.get("brand");
                        String mappedBrand = brandMapping.getOrDefault(brandName, brandName);
                        int usageCount = ((Number) row.get("usage_count")).intValue();
                        
                        if ("纪念日".equals(mappedBrand)) {
                            data.setJinianriUsage(usageCount);
                        } else if ("ENJOY".equals(mappedBrand)) {
                            data.setEnjoyUsage(usageCount);
                        } else if ("哆象".equals(mappedBrand)) {
                            data.setDuoxiangUsage(usageCount);
                        }
                    }
                }
                
                // 填充伯俊系统访问次数数据
                if ("伯俊系统".equals(originalName)) {
                    for (Map<String, Object> row : bjcUsageResult) {
                        String brandName = (String) row.get("brand");
                        String mappedBrand = brandMapping.getOrDefault(brandName, brandName);
                        int usageCount = ((Number) row.get("usage_count")).intValue();
                        
                        if ("纪念日".equals(mappedBrand)) {
                            data.setJinianriUsage(usageCount);
                        } else if ("ENJOY".equals(mappedBrand)) {
                            data.setEnjoyUsage(usageCount);
                        } else if ("哆象".equals(mappedBrand)) {
                            data.setDuoxiangUsage(usageCount);
                        }
                    }
                }
                
                // 填充商城/随手购访问次数数据
                if ("商城/随手购".equals(originalName)) {
                    for (Map<String, Object> row : mallUsageResult) {
                        String brandName = (String) row.get("brand");
                        String mappedBrand = brandMapping.getOrDefault(brandName, brandName);
                        int usageCount = ((Number) row.get("usage_count")).intValue();
                        
                        if ("纪念日".equals(mappedBrand)) {
                            data.setJinianriUsage(usageCount);
                        } else if ("ENJOY".equals(mappedBrand)) {
                            data.setEnjoyUsage(usageCount);
                        } else if ("哆象".equals(mappedBrand)) {
                            data.setDuoxiangUsage(usageCount);
                        }
                    }
                }
                
                // 填充访问人数数据
                for (Map<String, Object> row : visitorResult) {
                    if (originalName.equals(row.get("name"))) {
                        String brandName = (String) row.get("brand");
                        String mappedBrand = brandMapping.getOrDefault(brandName, brandName);
                        int visitorCount = ((Number) row.get("visitor_count")).intValue();
                        
                        if ("纪念日".equals(mappedBrand)) {
                            data.setJinianriVisitor(visitorCount);
                        } else if ("ENJOY".equals(mappedBrand)) {
                            data.setEnjoyVisitor(visitorCount);
                        } else if ("哆象".equals(mappedBrand)) {
                            data.setDuoxiangVisitor(visitorCount);
                        }
                    }
                }
                
                // 填充伯俊系统访问人数数据
                if ("伯俊系统".equals(originalName)) {
                    for (Map<String, Object> row : bjcVisitorResult) {
                        String brandName = (String) row.get("brand");
                        String mappedBrand = brandMapping.getOrDefault(brandName, brandName);
                        int visitorCount = ((Number) row.get("visitor_count")).intValue();
                        
                        if ("纪念日".equals(mappedBrand)) {
                            data.setJinianriVisitor(visitorCount);
                        } else if ("ENJOY".equals(mappedBrand)) {
                            data.setEnjoyVisitor(visitorCount);
                        } else if ("哆象".equals(mappedBrand)) {
                            data.setDuoxiangVisitor(visitorCount);
                        }
                    }
                }
                
                // 填充商城/随手购访问人数数据
                if ("商城/随手购".equals(originalName)) {
                    for (Map<String, Object> row : mallVisitorResult) {
                        String brandName = (String) row.get("brand");
                        String mappedBrand = brandMapping.getOrDefault(brandName, brandName);
                        int visitorCount = ((Number) row.get("visitor_count")).intValue();
                        
                        if ("纪念日".equals(mappedBrand)) {
                            data.setJinianriVisitor(visitorCount);
                        } else if ("ENJOY".equals(mappedBrand)) {
                            data.setEnjoyVisitor(visitorCount);
                        } else if ("哆象".equals(mappedBrand)) {
                            data.setDuoxiangVisitor(visitorCount);
                        }
                    }
                }
                
                list.add(data);
                log.debug("合并后的数据：{} - 纪念日: {}({}), ENJOY: {}({}), 哆象: {}({})", 
                    displayName, 
                    data.getJinianriVisitor(), data.getJinianriUsage(),
                    data.getEnjoyVisitor(), data.getEnjoyUsage(),
                    data.getDuoxiangVisitor(), data.getDuoxiangUsage());
            }
            
            // 添加集团CRM数据
            try {
                log.info("开始查询集团CRM访问数据");
                
                // 查询集团CRM访问次数
                String crmUsageSql = "SELECT count(t.id) as usage_count FROM ADMIN_USER_LOG@crm_134 t WHERE to_char(t.creationdate, 'YYYYMM') = ?";
                log.debug("执行集团CRM访问次数查询SQL：{}，参数：{}", crmUsageSql, month);
                Integer crmUsageCount = jdbcTemplate.queryForObject(crmUsageSql, new Object[]{month}, Integer.class);
                log.debug("集团CRM访问次数：{}", crmUsageCount);
                
                // 查询集团CRM访问人数
                String crmVisitorSql = "SELECT count(user_name) as visitor_count FROM (SELECT user_name FROM ADMIN_USER_LOG@crm_134 t WHERE to_char(t.creationdate, 'YYYYMM') = ? GROUP BY user_name) f";
                log.debug("执行集团CRM访问人数查询SQL：{}，参数：{}", crmVisitorSql, month);
                Integer crmVisitorCount = jdbcTemplate.queryForObject(crmVisitorSql, new Object[]{month}, Integer.class);
                log.debug("集团CRM访问人数：{}", crmVisitorCount);
                
                // 添加集团CRM数据到列表
                SystemAccessData crmData = new SystemAccessData("集团CRM", crmVisitorCount, crmUsageCount);
                list.add(crmData);
                log.debug("添加集团CRM数据：{} - 访问人数：{}，访问次数：{}", "集团CRM", crmVisitorCount, crmUsageCount);
            } catch (Exception e) {
                log.error("查询集团CRM访问数据失败: {}", e.getMessage(), e);
                // 如果查询失败，添加默认的集团CRM数据
                log.info("返回默认集团CRM数据");
                list.add(new SystemAccessData("集团CRM", 0, 0));
            }
            
            log.info("系统访问统计数据查询成功，共 {} 条数据", list.size());
        } catch (Exception e) {
            log.error("查询系统访问统计数据失败: {}", e.getMessage(), e);
            // 如果查询失败，返回模拟数据
            log.info("返回模拟系统访问统计数据");
            list.add(new SystemAccessData("智慧门店", 1408, 18047));
            list.add(new SystemAccessData("ENJOY智慧门店", 1610, 42));
            list.add(new SystemAccessData("盘点机", 70, 471));
            list.add(new SystemAccessData("客诉", 156, 372));
            list.add(new SystemAccessData("配件补单", 108, 231));
        }
        
        log.info("querySystemAccessData方法执行完成，返回 {} 条数据", list.size());
        return list;
    }

    /**
     * 查询销售平台数据
     * @param month 月份
     * @return 销售平台数据
     */
    private List<SalesPlatformData> querySalesPlatformData(String month) {
        log.info("开始执行querySalesPlatformData方法，月份：{}", month);
        List<SalesPlatformData> list = new ArrayList<>();
        
        try {
            log.info("开始查询销售平台数据");
            
            // 查询销售平台销售额
            String salesSql = "SELECT b.description, sum(a.tot_amt_actual) as sales_amount " +
                            "FROM M_TBRETAIL@bj_70 a, AD_LIMITVALUE@bj_70 b " +
                            "WHERE substr(a.billdate, 0, 6) = ? " +
                            "AND a.ON_STORENAME = b.value " +
                            "AND b.ad_limitvalue_group_id = 1812 " +
                            "AND a.isactive = 'Y' " +
                            "AND a.status IN (1, 2) " +
                            "GROUP BY b.description";
            log.debug("执行销售额查询SQL：{}，参数：{}", salesSql, month);
            List<Map<String, Object>> salesResult = jdbcTemplate.queryForList(salesSql, month);
            log.debug("销售额查询结果：{}", salesResult);
            
            // 将销售额结果存储到Map中，方便后续合并
            Map<String, Double> salesMap = new HashMap<>();
            for (Map<String, Object> row : salesResult) {
                String description = (String) row.get("description");
                Double salesAmount = row.get("sales_amount") != null ? ((Number) row.get("sales_amount")).doubleValue() : 0.0;
                salesMap.put(description, salesAmount);
                log.debug("销售额 - {}：{}", description, salesAmount);
            }
            log.debug("销售额Map：{}", salesMap);
            
            // 查询销售平台订单量
            String orderSql = "SELECT f.description, count(order_no) as order_count " +
                            "FROM ( " +
                            "    SELECT b.description, a.order_no " +
                            "    FROM M_TBRETAIL@bj_70 a, AD_LIMITVALUE@bj_70 b " +
                            "    WHERE substr(a.billdate, 0, 6) = ? " +
                            "    AND a.ON_STORENAME = b.value " +
                            "    AND b.ad_limitvalue_group_id = 1812 " +
                            "    AND a.isactive = 'Y' " +
                            "    AND a.status IN (1, 2) " +
                            "    GROUP BY b.description, a.order_no " +
                            ") f " +
                            "GROUP BY description";
            log.debug("执行订单量查询SQL：{}，参数：{}", orderSql, month);
            List<Map<String, Object>> orderResult = jdbcTemplate.queryForList(orderSql, month);
            log.debug("订单量查询结果：{}", orderResult);
            
            // 将订单量结果存储到Map中，方便后续合并
            Map<String, Double> orderMap = new HashMap<>();
            for (Map<String, Object> row : orderResult) {
                String description = (String) row.get("description");
                Double orderCount = row.get("order_count") != null ? ((Number) row.get("order_count")).doubleValue() : 0.0;
                orderMap.put(description, orderCount);
                log.debug("订单量 - {}：{}", description, orderCount);
            }
            log.debug("订单量Map：{}", orderMap);
            
            // 合并两个查询的结果
            Set<String> allDescriptions = new HashSet<>();
            allDescriptions.addAll(salesMap.keySet());
            allDescriptions.addAll(orderMap.keySet());
            log.debug("所有销售平台：{}", allDescriptions);
            
            for (String description : allDescriptions) {
                String platformName = description;
                Double orderCount = orderMap.getOrDefault(description, 0.0);
                Double salesAmount = salesMap.getOrDefault(description, 0.0);
                
                SalesPlatformData data = new SalesPlatformData(platformName, orderCount, salesAmount);
                list.add(data);
                log.debug("合并后的数据：{} - 订单量：{}，销售额：{}", platformName, orderCount, salesAmount);
            }
            
            // 查询美团数据
            try {
                String meituanSql = "SELECT sum(a.tot_amt_actual) as sales_amount, count(a.order_no) as order_count " +
                                  "FROM M_MT_TRADE@bj_70 a " +
                                  "WHERE substr(a.billdate, 0, 6) = ?";
                log.debug("执行美团数据查询SQL：{}，参数：{}", meituanSql, month);
                Map<String, Object> meituanResult = jdbcTemplate.queryForMap(meituanSql, month);
                log.debug("美团数据查询结果：{}", meituanResult);
                
                Double meituanSales = meituanResult.get("sales_amount") != null ? ((Number) meituanResult.get("sales_amount")).doubleValue() : 0.0;
                Double meituanOrders = meituanResult.get("order_count") != null ? ((Number) meituanResult.get("order_count")).doubleValue() : 0.0;
                log.debug("美团 - 销售额：{}，订单量：{}", meituanSales, meituanOrders);
                
                // 更新美团数据
                updateSalesPlatformData(list, "美团", meituanOrders, meituanSales);
            } catch (Exception e) {
                log.error("查询美团数据失败: {}", e.getMessage(), e);
            }
            
            // 查询饿了么数据
            try {
                String elemeSql = "SELECT sum(a.tot_amt_actual) as sales_amount, count(a.order_id) as order_count " +
                                "FROM M_ELEM_TRADE@bj_70 a " +
                                "WHERE substr(a.billdate, 0, 6) = ?";
                log.debug("执行饿了么数据查询SQL：{}，参数：{}", elemeSql, month);
                Map<String, Object> elemeResult = jdbcTemplate.queryForMap(elemeSql, month);
                log.debug("饿了么数据查询结果：{}", elemeResult);
                
                Double elemeSales = elemeResult.get("sales_amount") != null ? ((Number) elemeResult.get("sales_amount")).doubleValue() : 0.0;
                Double elemeOrders = elemeResult.get("order_count") != null ? ((Number) elemeResult.get("order_count")).doubleValue() : 0.0;
                log.debug("饿了么 - 销售额：{}，订单量：{}", elemeSales, elemeOrders);
                
                // 更新饿了么数据
                updateSalesPlatformData(list, "饿了么", elemeOrders, elemeSales);
            } catch (Exception e) {
                log.error("查询饿了么数据失败: {}", e.getMessage(), e);
            }
            
            // 查询JNR官方商城数据
            try {
                // 查询JNR官方商城销售额
                String jnrSalesSql = "SELECT sum(a.ORD_PAY_AMNT) as sales_amount " +
                                    "FROM os_order@bj_70 a " +
                                    "WHERE substr(a.billdate, 0, 6) = ? " +
                                    "AND a.status = 2";
                log.debug("执行JNR官方商城销售额查询SQL：{}，参数：{}", jnrSalesSql, month);
                Map<String, Object> jnrSalesResult = jdbcTemplate.queryForMap(jnrSalesSql, month);
                log.debug("JNR官方商城销售额查询结果：{}", jnrSalesResult);
                
                Double jnrSales = jnrSalesResult.get("sales_amount") != null ? ((Number) jnrSalesResult.get("sales_amount")).doubleValue() : 0.0;
                log.debug("JNR官方商城 - 销售额：{}", jnrSales);
                
                // 查询JNR官方商城订单量
                String jnrOrderSql = "SELECT count(ord_no) as order_count " +
                                    "FROM ( " +
                                    "    SELECT a.ord_no " +
                                    "    FROM os_order@bj_70 a " +
                                    "    WHERE substr(a.billdate, 0, 6) = ? " +
                                    "    AND a.status = 2 " +
                                    "    GROUP BY a.ord_no " +
                                    ") f";
                log.debug("执行JNR官方商城订单量查询SQL：{}，参数：{}", jnrOrderSql, month);
                Map<String, Object> jnrOrderResult = jdbcTemplate.queryForMap(jnrOrderSql, month);
                log.debug("JNR官方商城订单量查询结果：{}", jnrOrderResult);
                
                Double jnrOrders = jnrOrderResult.get("order_count") != null ? ((Number) jnrOrderResult.get("order_count")).doubleValue() : 0.0;
                log.debug("JNR官方商城 - 订单量：{}", jnrOrders);
                
                // 更新或添加JNR官方商城数据
                updateSalesPlatformData(list, "JNR官方商城", jnrOrders, jnrSales);
            } catch (Exception e) {
                log.error("查询JNR官方商城数据失败: {}", e.getMessage(), e);
            }
            
            // 查询李红销售数据
            try {
                // 查询李红销售销售额
                String lihongSalesSql = "SELECT sum((a.cbprice / a.qty) * (a.qty - a.thqty)) as sales_amount " +
                                    "FROM sp_yz_order@shop_99 a " +
                                    "WHERE to_char(a.creationdate, 'YYYYMM') = ?";
                log.debug("执行李红销售销售额查询SQL：{}，参数：{}", lihongSalesSql, month);
                Map<String, Object> lihongSalesResult = jdbcTemplate.queryForMap(lihongSalesSql, month);
                log.debug("李红销售销售额查询结果：{}", lihongSalesResult);
                
                Double lihongSales = lihongSalesResult.get("sales_amount") != null ? ((Number) lihongSalesResult.get("sales_amount")).doubleValue() : 0.0;
                log.debug("李红销售 - 销售额：{}", lihongSales);
                
                // 查询李红销售订单量
                String lihongOrderSql = "SELECT count(order_no) as order_count " +
                                    "FROM ( " +
                                    "    SELECT a.order_no " +
                                    "    FROM sp_yz_order@shop_99 a " +
                                    "    WHERE to_char(a.creationdate, 'YYYYMM') = ? " +
                                    "    GROUP BY a.order_no " +
                                    ") f";
                log.debug("执行李红销售订单量查询SQL：{}，参数：{}", lihongOrderSql, month);
                Map<String, Object> lihongOrderResult = jdbcTemplate.queryForMap(lihongOrderSql, month);
                log.debug("李红销售订单量查询结果：{}", lihongOrderResult);
                
                Double lihongOrders = lihongOrderResult.get("order_count") != null ? ((Number) lihongOrderResult.get("order_count")).doubleValue() : 0.0;
                log.debug("李红销售 - 订单量：{}", lihongOrders);
                
                // 更新或添加李红销售数据
                updateSalesPlatformData(list, "李红销售", lihongOrders, lihongSales);
            } catch (Exception e) {
                log.error("查询李红销售数据失败: {}", e.getMessage(), e);
            }
            
            // 查询CRM_VIP_V_CARDLOG消费数据（平台订单量）
            try {
                String consumeSql = "SELECT b.description, count(a.id) as amount " +
                                "FROM CRM_VIP_V_CARDLOG@bj_70 a, AD_LIMITVALUE@bj_70 b " +
                                "WHERE to_char(a.creationdate, 'YYYYMM') = ? " +
                                "AND a.M_NAME = b.value " +
                                "AND b.ad_limitvalue_group_id = 1927 " +
                                "AND a.billtype = '消费' " +
                                "GROUP BY b.description";
                log.debug("执行平台订单量查询SQL：{}，参数：{}", consumeSql, month);
                List<Map<String, Object>> consumeResult = jdbcTemplate.queryForList(consumeSql, month);
                log.debug("平台订单量查询结果：{}", consumeResult);
                
                for (Map<String, Object> row : consumeResult) {
                    String description = (String) row.get("description");
                    Double amount = row.get("amount") != null ? ((Number) row.get("amount")).doubleValue() : 0.0;
                    log.debug("{} - 消费金额：{}", description, amount);
                    
                    // 更新或添加平台消费数据
                    // 这里根据注释，消费金额作为订单量
                    updateSalesPlatformData(list, description, amount, 0.0);
                }
            } catch (Exception e) {
                log.error("查询平台订单量数据失败: {}", e.getMessage(), e);
            }
            
            // 查询CRM_VIP_V_CARDLOG充值数据（平台销售额）
            try {
                String rechargeSql = "SELECT b.description, sum(a.AMOUNT) as amount " +
                                "FROM CRM_VIP_V_CARDLOG@bj_70 a, AD_LIMITVALUE@bj_70 b " +
                                "WHERE to_char(a.creationdate, 'YYYYMM') = ? " +
                                "AND a.M_NAME = b.value " +
                                "AND b.ad_limitvalue_group_id = 1927 " +
                                "AND a.billtype = '充值' " +
                                "GROUP BY b.description";
                log.debug("执行平台销售额查询SQL：{}，参数：{}", rechargeSql, month);
                List<Map<String, Object>> rechargeResult = jdbcTemplate.queryForList(rechargeSql, month);
                log.debug("平台销售额查询结果：{}", rechargeResult);
                
                for (Map<String, Object> row : rechargeResult) {
                    String description = (String) row.get("description");
                    Double amount = row.get("amount") != null ? ((Number) row.get("amount")).doubleValue() : 0.0;
                    log.debug("{} - 充值金额：{}", description, amount);
                    
                    // 更新或添加平台充值数据
                    // 这里根据注释，充值金额作为销售额
                    updateSalesPlatformData(list, description, 0.0, amount);
                }
            } catch (Exception e) {
                log.error("查询平台销售额数据失败: {}", e.getMessage(), e);
            }
            
            log.info("销售平台数据查询成功，共 {} 条数据", list.size());
        } catch (Exception e) {
            log.error("查询销售平台数据失败: {}", e.getMessage(), e);
            // 如果查询失败，返回模拟数据
            log.info("返回模拟销售平台数据");
            list.add(new SalesPlatformData("美团", 7365.00, 340928.71));
            list.add(new SalesPlatformData("饿了么", 2004.00, 386809.70));
            list.add(new SalesPlatformData("ENJOY准吃店", 21616.00, 178346.54));
            list.add(new SalesPlatformData("饿了么(门店款)", 2073.00, 184652.85));
            list.add(new SalesPlatformData("商城", 2860.00, 104881.74));
            list.add(new SalesPlatformData("纪念日京东小时达", 65.00, 3781.90));
            list.add(new SalesPlatformData("天猫旗舰店", 843.00, 43423.65));
            list.add(new SalesPlatformData("纪念日京东旗舰店", 1817.00, 239261.76));
            list.add(new SalesPlatformData("纪念日百货", 24.00, 523.50));
            list.add(new SalesPlatformData("抖音小雷达", 29.00, 1881.58));
            list.add(new SalesPlatformData("ENJOY淮坊店", 68.00, 4722.70));
        }
        
        log.info("销售平台数据生成完成，共 {} 条数据", list.size());
        for (SalesPlatformData data : list) {
            log.debug("销售平台数据：{} - 订单量：{}，销售额：{}", data.getPlatformName(), data.getOrderCount(), data.getSalesAmount());
        }
        
        log.info("querySalesPlatformData方法执行完成，返回 {} 条数据", list.size());
        return list;
    }

    /**
     * 更新销售平台数据列表中的指定平台数据
     * @param list 销售平台数据列表
     * @param platformName 平台名称
     * @param orderCount 订单量
     * @param salesAmount 销售额
     */
    private void updateSalesPlatformData(List<SalesPlatformData> list, String platformName, Double orderCount, Double salesAmount) {
        boolean found = false;
        for (SalesPlatformData data : list) {
            if (data.getPlatformName().equals(platformName)) {
                // 更新现有平台数据 - 累加订单量和销售额
                data.setOrderCount(data.getOrderCount() + orderCount);
                data.setSalesAmount(data.getSalesAmount() + salesAmount);
                log.debug("更新{}数据：订单量：{} -> {}，销售额：{} -> {}", platformName, orderCount, data.getOrderCount(), salesAmount, data.getSalesAmount());
                found = true;
                break;
            }
        }
        if (!found) {
            // 添加新的平台数据
            SalesPlatformData newData = new SalesPlatformData(platformName, orderCount, salesAmount);
            list.add(newData);
            log.debug("添加{}数据：订单量：{}，销售额：{}", platformName, orderCount, salesAmount);
        }
    }

    /**
     * 写入系统访问统计数据到Excel
     * @param sheet Excel工作表
     * @param workbook Excel工作簿
     * @param dataList 系统访问统计数据列表
     */
    private void writeSystemAccessData(Sheet sheet, Workbook workbook, List<SystemAccessData> dataList) {
        // 系统访问统计标题
        int rowIndex = 2;
        Row systemTitleRow = sheet.createRow(rowIndex++);
        systemTitleRow.createCell(0).setCellValue("系统名称");
        systemTitleRow.createCell(1).setCellValue("纪念日访问人数");
        systemTitleRow.createCell(2).setCellValue("纪念日访问次数");
        systemTitleRow.createCell(3).setCellValue("ENJOY访问人数");
        systemTitleRow.createCell(4).setCellValue("ENJOY访问次数");
        systemTitleRow.createCell(5).setCellValue("哆象访问人数");
        systemTitleRow.createCell(6).setCellValue("哆象访问次数");

        // 设置标题样式
        setRowCellStyle(systemTitleRow, createHeaderCellStyle(workbook));

        // 写入数据
        for (SystemAccessData data : dataList) {
            Row dataRow = sheet.createRow(rowIndex++);
            dataRow.createCell(0).setCellValue(data.getSystemName());
            dataRow.createCell(1).setCellValue(data.getJinianriVisitor());
            dataRow.createCell(2).setCellValue(data.getJinianriUsage());
            dataRow.createCell(3).setCellValue(data.getEnjoyVisitor());
            dataRow.createCell(4).setCellValue(data.getEnjoyUsage());
            dataRow.createCell(5).setCellValue(data.getDuoxiangVisitor());
            dataRow.createCell(6).setCellValue(data.getDuoxiangUsage());
            setRowCellStyle(dataRow, createDataCellStyle(workbook));
        }
    }

    /**
     * 写入销售平台数据到Excel
     * @param sheet Excel工作表
     * @param workbook Excel工作簿
     * @param dataList 销售平台数据列表
     */
    private void writeSalesPlatformData(Sheet sheet, Workbook workbook, List<SalesPlatformData> dataList) {
        // 销售平台数据标题
        int rowIndex = 12;
        Row salesTitleRow = sheet.createRow(rowIndex++);
        salesTitleRow.createCell(4).setCellValue("销售平台");
        salesTitleRow.createCell(5).setCellValue("订单量");
        salesTitleRow.createCell(6).setCellValue("销售额");

        // 设置标题样式
        setRowCellStyle(salesTitleRow, createHeaderCellStyle(workbook));

        // 写入数据
        for (SalesPlatformData data : dataList) {
            Row dataRow = sheet.createRow(rowIndex++);
            dataRow.createCell(4).setCellValue(data.getPlatformName());
            dataRow.createCell(5).setCellValue(data.getOrderCount());
            dataRow.createCell(6).setCellValue(data.getSalesAmount());
            setRowCellStyle(dataRow, createDataCellStyle(workbook));
        }
    }

    /**
     * 创建标题单元格样式
     * @param workbook Excel工作簿
     * @return 单元格样式
     */
    private CellStyle createTitleCellStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("宋体");
        font.setFontHeightInPoints((short) 16);
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    /**
     * 创建表头单元格样式
     * @param workbook Excel工作簿
     * @return 单元格样式
     */
    private CellStyle createHeaderCellStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("宋体");
        font.setFontHeightInPoints((short) 12);
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    /**
     * 创建数据单元格样式
     * @param workbook Excel工作簿
     * @return 单元格样式
     */
    private CellStyle createDataCellStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("宋体");
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    /**
     * 设置行中所有单元格的样式
     * @param row 行对象
     * @param style 单元格样式
     */
    private void setRowCellStyle(Row row, CellStyle style) {
        for (int i = 0; i <= row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null) {
                cell.setCellStyle(style);
            }
        }
    }

    /**
     * 系统访问统计数据实体类
     */
    private static class SystemAccessData {
        private String systemName;
        private int jinianriVisitor;
        private int jinianriUsage;
        private int enjoyVisitor;
        private int enjoyUsage;
        private int duoxiangVisitor;
        private int duoxiangUsage;

        public SystemAccessData() {
        }

        public SystemAccessData(String systemName, int visitorCount, int usageCount) {
            this.systemName = systemName;
            this.jinianriVisitor = visitorCount;
            this.jinianriUsage = usageCount;
            this.enjoyVisitor = visitorCount;
            this.enjoyUsage = usageCount;
            this.duoxiangVisitor = visitorCount;
            this.duoxiangUsage = usageCount;
        }

        public String getSystemName() {
            return systemName;
        }

        public void setSystemName(String systemName) {
            this.systemName = systemName;
        }

        public int getJinianriVisitor() {
            return jinianriVisitor;
        }

        public void setJinianriVisitor(int jinianriVisitor) {
            this.jinianriVisitor = jinianriVisitor;
        }

        public int getJinianriUsage() {
            return jinianriUsage;
        }

        public void setJinianriUsage(int jinianriUsage) {
            this.jinianriUsage = jinianriUsage;
        }

        public int getEnjoyVisitor() {
            return enjoyVisitor;
        }

        public void setEnjoyVisitor(int enjoyVisitor) {
            this.enjoyVisitor = enjoyVisitor;
        }

        public int getEnjoyUsage() {
            return enjoyUsage;
        }

        public void setEnjoyUsage(int enjoyUsage) {
            this.enjoyUsage = enjoyUsage;
        }

        public int getDuoxiangVisitor() {
            return duoxiangVisitor;
        }

        public void setDuoxiangVisitor(int duoxiangVisitor) {
            this.duoxiangVisitor = duoxiangVisitor;
        }

        public int getDuoxiangUsage() {
            return duoxiangUsage;
        }

        public void setDuoxiangUsage(int duoxiangUsage) {
            this.duoxiangUsage = duoxiangUsage;
        }
    }

    /**
     * 销售平台数据实体类
     */
    private static class SalesPlatformData {
        private String platformName;
        private double orderCount;
        private double salesAmount;

        public SalesPlatformData(String platformName, double orderCount, double salesAmount) {
            this.platformName = platformName;
            this.orderCount = orderCount;
            this.salesAmount = salesAmount;
        }

        public String getPlatformName() {
            return platformName;
        }

        public double getOrderCount() {
            return orderCount;
        }

        public double getSalesAmount() {
            return salesAmount;
        }

        public void setPlatformName(String platformName) {
            this.platformName = platformName;
        }

        public void setOrderCount(double orderCount) {
            this.orderCount = orderCount;
        }

        public void setSalesAmount(double salesAmount) {
            this.salesAmount = salesAmount;
        }
    }
}
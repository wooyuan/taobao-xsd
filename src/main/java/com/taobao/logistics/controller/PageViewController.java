package com.taobao.logistics.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 页面视图控制器
 * 专门用于处理HTML页面的返回
 */
@Slf4j
@Controller
public class PageViewController {

    /**
     * 显示首页（默认跳转到库存查询页面）
     * 
     * @return 重定向到库存查询页面
     */
    @GetMapping("/")
    public String showIndex() {
        return "redirect:/view/inventoryQuery";
    }
    
    /**
     * 显示乐橙监控摄像头排查结果统计表
     * 
     * @return 摄像头排查结果报告页面
     */
    @GetMapping("/view/cameraReport")
    public String showCameraReport() {
        return "camera_report.html";
    }

    /**
     * 显示销售数据报表
     * 
     * @return 销售数据报表页面
     */
    @GetMapping("/view/salesReport")
    public String showSalesReport() {
        return "sales_report";
    }

    /**
     * 显示系统发展时间轴和员工工时统计
     * 
     * @return 系统仪表盘页面
     */
    @GetMapping("/view/systemDashboard")
    public String showSystemDashboard() {
        return "system_dashboard";
    }
    
    /**
     * 显示系统信息管理页面
     * 
     * @return 系统信息管理页面
     */
    @GetMapping("/view/systemInfo")
    public String showSystemInfo() {
        return "system_info";
    }
    
    /**
     * 显示员工工时管理页面
     * 
     * @return 员工工时管理页面
     */
    @GetMapping("/view/workHour")
    public String showWorkHour() {
        return "work_hour";
    }
    
    /**
     * 显示工时统计报表页面
     * 
     * @return 工时统计报表页面
     */
    @GetMapping("/view/workHourStatistics")
    public String showWorkHourStatistics() {
        return "work_hour_statistics";
    }
    
    /**
     * 显示员工工时统计报表页面
     * 
     * @return 员工工时统计报表页面
     */
    @GetMapping("/view/workHourStatistics/employee")
    public String showWorkHourStatisticsEmployee() {
        return "work_hour_statistics_employee";
    }
    
    /**
     * 显示库存查询页面
     * 
     * @return 库存查询页面
     */
    @GetMapping("/view/inventoryQuery")
    public String showInventoryQuery() {
        return "inventory_query";
    }
    
    /**
     * 显示CSV导入页面
     * 
     * @return CSV导入页面
     */
    @GetMapping("/view/csvImport")
    public String showCSVImport() {
        return "csv_import";
    }

    /**
     * 显示企业微信行为数据查询页面
     * 
     * @return 企业微信行为数据查询页面
     */
    @GetMapping("/view/workwechatBehavior")
    public String showWorkWechatBehavior() {
        return "workwechat_behavior";
    }
}
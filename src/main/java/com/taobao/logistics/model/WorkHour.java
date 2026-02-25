package com.taobao.logistics.model;

import java.util.Date;

/**
 * 员工工时实体类
 */
public class WorkHour {
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 系统名称
     */
    private String system;
    
    /**
     * 工作月
     */
    private String workMonth;
    
    /**
     * 工作周
     */
    private String workWeek;
    
    /**
     * 工作周最后一天日期
     */
    private String workWeekEndDate;
    
    /**
     * 员工姓名
     */
    private String employee;
    
    /**
     * 工作项类别
     */
    private String workCategory;
    
    /**
     * 类型（BUG、新功能、维护）
     */
    private String type;
    
    /**
     * 事项
     */
    private String item;
    
    /**
     * 提报人
     */
    private String reporter;
    
    /**
     * 费用归属部门
     */
    private String costDepartment;
    
    /**
     * 计划工时
     */
    private Double plannedHours;
    
    /**
     * 执行工时
     */
    private Double actualHours;
    
    /**
     * 创建时间
     */
    private Date createTime;
    
    /**
     * 更新时间
     */
    private Date updateTime;

    // getter和setter方法
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSystem() {
        return system;
    }

    public void setSystem(String system) {
        this.system = system;
    }

    public String getWorkMonth() {
        return workMonth;
    }

    public void setWorkMonth(String workMonth) {
        this.workMonth = workMonth;
    }

    public String getWorkWeek() {
        return workWeek;
    }

    public void setWorkWeek(String workWeek) {
        this.workWeek = workWeek;
    }

    public String getWorkWeekEndDate() {
        return workWeekEndDate;
    }

    public void setWorkWeekEndDate(String workWeekEndDate) {
        this.workWeekEndDate = workWeekEndDate;
    }

    public String getEmployee() {
        return employee;
    }

    public void setEmployee(String employee) {
        this.employee = employee;
    }

    public String getWorkCategory() {
        return workCategory;
    }

    public void setWorkCategory(String workCategory) {
        this.workCategory = workCategory;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public String getReporter() {
        return reporter;
    }

    public void setReporter(String reporter) {
        this.reporter = reporter;
    }

    public String getCostDepartment() {
        return costDepartment;
    }

    public void setCostDepartment(String costDepartment) {
        this.costDepartment = costDepartment;
    }

    public Double getPlannedHours() {
        return plannedHours;
    }

    public void setPlannedHours(Double plannedHours) {
        this.plannedHours = plannedHours;
    }

    public Double getActualHours() {
        return actualHours;
    }

    public void setActualHours(Double actualHours) {
        this.actualHours = actualHours;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
package com.taobao.logistics.model;

import java.util.Date;

/**
 * 系统信息实体类
 * 对应数据库表 SYSTEM_INFO
 */
public class SystemInfo {
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 系统/接口名称
     */
    private String systemName;
    
    /**
     * 功能简介
     */
    private String functionDesc;
    
    /**
     * 责任人
     */
    private String responsiblePerson;
    
    /**
     * 来源
     */
    private String source;
    
    /**
     * 类型
     */
    private String type;
    
    /**
     * 是否使用
     */
    private String isUsing;
    
    /**
     * 是否可以推广到事业部
     */
    private String canPromote;
    
    /**
     * 闲置功能盘点
     */
    private String idleFunctions;
    
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

    public String getSystemName() {
        return systemName;
    }

    public void setSystemName(String systemName) {
        this.systemName = systemName;
    }

    public String getFunctionDesc() {
        return functionDesc;
    }

    public void setFunctionDesc(String functionDesc) {
        this.functionDesc = functionDesc;
    }

    public String getResponsiblePerson() {
        return responsiblePerson;
    }

    public void setResponsiblePerson(String responsiblePerson) {
        this.responsiblePerson = responsiblePerson;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getIsUsing() {
        return isUsing;
    }

    public void setIsUsing(String isUsing) {
        this.isUsing = isUsing;
    }

    public String getCanPromote() {
        return canPromote;
    }

    public void setCanPromote(String canPromote) {
        this.canPromote = canPromote;
    }

    public String getIdleFunctions() {
        return idleFunctions;
    }

    public void setIdleFunctions(String idleFunctions) {
        this.idleFunctions = idleFunctions;
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
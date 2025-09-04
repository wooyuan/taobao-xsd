package com.taobao.logistics.entity;

/**
 * 物流过程实体类
 */
public class LogisticsProcedure {
    /** 状态码 */
    private int statusCode;
    /** 响应数据 */
    private String responseData;
    /** 错误信息 */
    private String errorMessage;
    /** 请求参数 */
    private String requestParams;
    /** 存储过程名称 */
    private String procedureName;

    // Getter and Setter 方法
    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getResponseData() {
        return responseData;
    }

    public void setResponseData(String responseData) {
        this.responseData = responseData;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getRequestParams() {
        return requestParams;
    }

    public void setRequestParams(String requestParams) {
        this.requestParams = requestParams;
    }

    public String getProcedureName() {
        return procedureName;
    }

    public void setProcedureName(String procedureName) {
        this.procedureName = procedureName;
    }



}